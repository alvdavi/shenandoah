/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#include "precompiled.hpp"

#include "gc/shared/barrierSetNMethod.hpp"
#include "gc/shared/collectorCounters.hpp"
#include "gc/shenandoah/shenandoahBreakpoint.hpp"
#include "gc/shenandoah/shenandoahCollectorPolicy.hpp"
#include "gc/shenandoah/shenandoahConcurrentGC.hpp"
#include "gc/shenandoah/shenandoahFreeSet.hpp"
#include "gc/shenandoah/shenandoahGeneration.hpp"
#include "gc/shenandoah/shenandoahLock.hpp"
#include "gc/shenandoah/shenandoahMark.inline.hpp"
#include "gc/shenandoah/shenandoahMonitoringSupport.hpp"
#include "gc/shenandoah/shenandoahOopClosures.inline.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahReferenceProcessor.hpp"
#include "gc/shenandoah/shenandoahRootProcessor.inline.hpp"
#include "gc/shenandoah/shenandoahMemoryManager.hpp"
#include "gc/shenandoah/shenandoahStackWatermark.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "gc/shenandoah/shenandoahVerifier.hpp"
#include "gc/shenandoah/shenandoahVMOperations.hpp"
#include "gc/shenandoah/shenandoahWorkGroup.hpp"
#include "gc/shenandoah/shenandoahWorkerPolicy.hpp"
#include "memory/allocation.hpp"
#include "prims/jvmtiTagMap.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/events.hpp"

// Breakpoint support
class ShenandoahBreakpointGCScope : public StackObj {
public:
  ShenandoahBreakpointGCScope() {
    ShenandoahBreakpoint::at_before_gc();
  }

  ~ShenandoahBreakpointGCScope() {
    ShenandoahBreakpoint::at_after_gc();
  }
};

class ShenandoahBreakpointMarkScope : public StackObj {
public:
  ShenandoahBreakpointMarkScope() {
    ShenandoahBreakpoint::at_after_marking_started();
  }

  ~ShenandoahBreakpointMarkScope() {
    ShenandoahBreakpoint::at_before_marking_completed();
  }
};

ShenandoahConcurrentGC::ShenandoahConcurrentGC(ShenandoahGeneration* generation, bool do_old_gc_bootstrap) :
  _mark(generation),
  _degen_point(ShenandoahDegenPoint::_degenerated_unset),
  _mixed_evac (false),
  _do_old_gc_bootstrap(do_old_gc_bootstrap),
  _generation(generation) {
}

ShenandoahGC::ShenandoahDegenPoint ShenandoahConcurrentGC::degen_point() const {
  return _degen_point;
}

bool ShenandoahConcurrentGC::collect(GCCause::Cause cause) {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  if (cause == GCCause::_wb_breakpoint) {
    ShenandoahBreakpoint::start_gc();
  }
  ShenandoahBreakpointGCScope breakpoint_gc_scope;

  // Reset for upcoming marking
  entry_reset();

  // Start initial mark under STW
  vmop_entry_init_mark();

  {
    ShenandoahBreakpointMarkScope breakpoint_mark_scope;

    // Reset task queue stats here, rather than in mark_concurrent_roots
    // because remembered set scan will `push` oops into the queues and
    // resetting after this happens will lose those counts.
    TASKQUEUE_STATS_ONLY(_mark.task_queues()->reset_taskqueue_stats());

    // Concurrent remembered set scanning
    if (_generation->generation_mode() == YOUNG) {
      _generation->scan_remembered_set();
    }

    // Concurrent mark roots
    entry_mark_roots();
    if (check_cancellation_and_abort(ShenandoahDegenPoint::_degenerated_outside_cycle)) return false;

    // Continue concurrent mark
    entry_mark();
    if (check_cancellation_and_abort(ShenandoahDegenPoint::_degenerated_mark)) return false;
  }

  // Complete marking under STW, and start evacuation
  vmop_entry_final_mark();

  // Concurrent stack processing
  if (heap->is_evacuation_in_progress()) {
    entry_thread_roots();
  }

  // Process weak roots that might still point to regions that would be broken by cleanup
  if (heap->is_concurrent_weak_root_in_progress()) {
    entry_weak_refs();
    entry_weak_roots();
  }

  // Final mark might have reclaimed some immediate garbage, kick cleanup to reclaim
  // the space. This would be the last action if there is nothing to evacuate.  Note that
  // we will not age young-gen objects in the case that we skip evacuation.
  entry_cleanup_early();

  {
    ShenandoahHeapLocker locker(heap->lock());
    heap->free_set()->log_status();
  }

  // Perform concurrent class unloading
  if (heap->unload_classes() &&
      heap->is_concurrent_weak_root_in_progress()) {
    entry_class_unloading();
  }

  // Processing strong roots
  // This may be skipped if there is nothing to update/evacuate.
  // If so, strong_root_in_progress would be unset.
  if (heap->is_concurrent_strong_root_in_progress()) {
    entry_strong_roots();
  }

  if (!heap->cancelled_gc() && heap->mode()->is_generational() && _generation->generation_mode() == GLOBAL) {
    entry_global_coalesce_and_fill();
  }

  // Continue the cycle with evacuation and optional update-refs.
  // This may be skipped if there is nothing to evacuate.
  // If so, evac_in_progress would be unset by collection set preparation code.
  if (heap->is_evacuation_in_progress()) {
    // Concurrently evacuate
    entry_evacuate();
    if (check_cancellation_and_abort(ShenandoahDegenPoint::_degenerated_evac)) return false;

    // Perform update-refs phase.
    vmop_entry_init_updaterefs();
    entry_updaterefs();
    if (check_cancellation_and_abort(ShenandoahDegenPoint::_degenerated_updaterefs)) return false;

    // Concurrent update thread roots
    entry_update_thread_roots();
    if (check_cancellation_and_abort(ShenandoahDegenPoint::_degenerated_updaterefs)) return false;

    vmop_entry_final_updaterefs();

    // Update references freed up collection set, kick the cleanup to reclaim the space.
    entry_cleanup_complete();
  } else {
    vmop_entry_final_roots();
  }

  return true;
}

void ShenandoahConcurrentGC::vmop_entry_init_mark() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->stw_collection_counters());
  ShenandoahTimingsTracker timing(ShenandoahPhaseTimings::init_mark_gross);
  TraceMemoryManagerPauseStats pause_stats(heap->memory_manager(_generation->generation_mode()),
        /* pauseType = */               "InitMark", 
        /* recordAccumulatedGCTime = */ false,
        /* countPauses = */             false,
        /* recordIndividualPauses = */  true,
        /* bool recordDuration = */     true,
        /* bool recordOperationTime */  false,
        /* bool recordPauseType */      false,
        /* cyclePause = */              true);

  heap->try_inject_alloc_failure();
  VM_ShenandoahInitMark op(this, _do_old_gc_bootstrap);
  VMThread::execute(&op); // jump to entry_init_mark() under safepoint
}

void ShenandoahConcurrentGC::vmop_entry_final_mark() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->stw_collection_counters());
  ShenandoahTimingsTracker timing(ShenandoahPhaseTimings::final_mark_gross);
  TraceMemoryManagerPauseStats pause_stats(heap->memory_manager(_generation->generation_mode()),
        /* pauseType = */               "FinalMarkStartEvac", 
        /* recordAccumulatedGCTime = */ false,
        /* countPauses = */             false,
        /* recordIndividualPauses = */  true,
        /* bool recordDuration = */     true,
        /* bool recordOperationTime */  false,
        /* bool recordPauseType */      false,
        /* cyclePause = */              true);

  heap->try_inject_alloc_failure();
  VM_ShenandoahFinalMarkStartEvac op(this);
  VMThread::execute(&op); // jump to entry_final_mark under safepoint
}

void ShenandoahConcurrentGC::vmop_entry_init_updaterefs() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->stw_collection_counters());
  ShenandoahTimingsTracker timing(ShenandoahPhaseTimings::init_update_refs_gross);
  TraceMemoryManagerPauseStats pause_stats(heap->memory_manager(_generation->generation_mode()),
      /* pauseType = */               "InitUpdateRefs", 
      /* recordAccumulatedGCTime = */ false,
      /* countPauses = */             false,
      /* recordIndividualPauses = */  true,
      /* bool recordDuration = */     true,
      /* bool recordOperationTime */  false,
      /* bool recordPauseType */      false,
      /* cyclePause = */              true);

  heap->try_inject_alloc_failure();
  VM_ShenandoahInitUpdateRefs op(this);
  VMThread::execute(&op);
}

void ShenandoahConcurrentGC::vmop_entry_final_updaterefs() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->stw_collection_counters());
  ShenandoahTimingsTracker timing(ShenandoahPhaseTimings::final_update_refs_gross);
  TraceMemoryManagerPauseStats pause_stats(heap->memory_manager(_generation->generation_mode()),
      /* pauseType = */               "FinalUpdateRefs", 
      /* recordAccumulatedGCTime = */ false,
      /* countPauses = */             false,
      /* recordIndividualPauses = */  true,
      /* bool recordDuration = */     true,
      /* bool recordOperationTime */  false,
      /* bool recordPauseType */      false,
      /* cyclePause = */              true);

  heap->try_inject_alloc_failure();
  VM_ShenandoahFinalUpdateRefs op(this);
  VMThread::execute(&op);
}

void ShenandoahConcurrentGC::vmop_entry_final_roots() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->stw_collection_counters());
  ShenandoahTimingsTracker timing(ShenandoahPhaseTimings::final_roots_gross);
  TraceMemoryManagerPauseStats pause_stats(heap->memory_manager(_generation->generation_mode()),
      /* pauseType = */               "FinalRoots", 
      /* recordAccumulatedGCTime = */ false,
      /* countPauses = */             false,
      /* recordIndividualPauses = */  true,
      /* bool recordDuration = */     true,
      /* bool recordOperationTime */  false,
      /* bool recordPauseType */      false,
      /* cyclePause = */              true);

  // This phase does not use workers, no need for setup
  heap->try_inject_alloc_failure();
  VM_ShenandoahFinalRoots op(this);
  VMThread::execute(&op);
}

void ShenandoahConcurrentGC::entry_init_mark() {
  char msg[1024];
  init_mark_event_message(msg, sizeof(msg));
  ShenandoahPausePhase gc_phase(msg, ShenandoahPhaseTimings::init_mark);
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(ShenandoahHeap::heap()->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_init_marking(),
                              "init marking");

  if (ShenandoahHeap::heap()->mode()->is_generational()
    && (_generation->generation_mode() == YOUNG || (_generation->generation_mode() == GLOBAL && ShenandoahVerify))) {
    // The current implementation of swap_remembered_set() copies the write-card-table
    // to the read-card-table. The remembered sets are also swapped for GLOBAL collections
    // so that the verifier works with the correct copy of the card table when verifying.
    _generation->swap_remembered_set();
  }

  op_init_mark();
}

void ShenandoahConcurrentGC::entry_final_mark() {
  char msg[1024];
  final_mark_event_message(msg, sizeof(msg));
  ShenandoahPausePhase gc_phase(msg, ShenandoahPhaseTimings::final_mark);
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(ShenandoahHeap::heap()->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_final_marking(),
                              "final marking");

  op_final_mark();
}

void ShenandoahConcurrentGC::entry_init_updaterefs() {
  static const char* msg = "Pause Init Update Refs";
  ShenandoahPausePhase gc_phase(msg, ShenandoahPhaseTimings::init_update_refs);
  EventMark em("%s", msg);

  // No workers used in this phase, no setup required
  op_init_updaterefs();
}

void ShenandoahConcurrentGC::entry_final_updaterefs() {
  static const char* msg = "Pause Final Update Refs";
  ShenandoahPausePhase gc_phase(msg, ShenandoahPhaseTimings::final_update_refs);
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(ShenandoahHeap::heap()->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_final_update_ref(),
                              "final reference update");

  op_final_updaterefs();
}

void ShenandoahConcurrentGC::entry_final_roots() {
  static const char* msg = "Pause Final Roots";
  ShenandoahPausePhase gc_phase(msg, ShenandoahPhaseTimings::final_roots);
  EventMark em("%s", msg);

  op_final_roots();
}

void ShenandoahConcurrentGC::entry_reset() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());
  static const char* msg = "Concurrent reset";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_reset, "Reset", _generation->generation_mode());
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_reset(),
                              "concurrent reset");

  heap->try_inject_alloc_failure();
  op_reset();
}

void ShenandoahConcurrentGC::entry_mark_roots() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());
  const char* msg = "Concurrent marking roots";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_mark_roots, "MarkingRoots", _generation->generation_mode());
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_marking(),
                              "concurrent marking roots");

  heap->try_inject_alloc_failure();
  op_mark_roots();
}

void ShenandoahConcurrentGC::entry_mark() {
  char msg[1024];
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());
  conc_mark_event_message(msg, sizeof(msg));
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_mark, "Marking", _generation->generation_mode());
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_marking(),
                              "concurrent marking");

  heap->try_inject_alloc_failure();
  op_mark();
}

void ShenandoahConcurrentGC::entry_thread_roots() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  static const char* msg = "Concurrent thread roots";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_thread_roots, "ThreadRoots", _generation->generation_mode());
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_root_processing(),
                              msg);

  heap->try_inject_alloc_failure();
  op_thread_roots();
}

void ShenandoahConcurrentGC::entry_weak_refs() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  static const char* msg = "Concurrent weak references";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_weak_refs, "WeakReferences", _generation->generation_mode());
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_refs_processing(),
                              "concurrent weak references");

  heap->try_inject_alloc_failure();
  op_weak_refs();
}

void ShenandoahConcurrentGC::entry_weak_roots() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());
  static const char* msg = "Concurrent weak roots";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_weak_roots, "WeakRoots", _generation->generation_mode());
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_root_processing(),
                              "concurrent weak root");

  heap->try_inject_alloc_failure();
  op_weak_roots();
}

void ShenandoahConcurrentGC::entry_class_unloading() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());
  static const char* msg = "Concurrent class unloading";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_class_unload, "ClassUnloading", _generation->generation_mode());
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_root_processing(),
                              "concurrent class unloading");

  heap->try_inject_alloc_failure();
  op_class_unloading();
}

void ShenandoahConcurrentGC::entry_strong_roots() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());
  static const char* msg = "Concurrent strong roots";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_strong_roots, "StrongRoots", _generation->generation_mode());
  EventMark em("%s", msg);

  ShenandoahGCWorkerPhase worker_phase(ShenandoahPhaseTimings::conc_strong_roots);

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_root_processing(),
                              "concurrent strong root");

  heap->try_inject_alloc_failure();
  op_strong_roots();
}

void ShenandoahConcurrentGC::entry_cleanup_early() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());
  static const char* msg = "Concurrent cleanup";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_cleanup_early, "CleanupEarly", _generation->generation_mode(), true /* log_heap_usage */);
  EventMark em("%s", msg);

  // This phase does not use workers, no need for setup
  heap->try_inject_alloc_failure();
  op_cleanup_early();
}

void ShenandoahConcurrentGC::entry_evacuate() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());

  static const char* msg = "Concurrent evacuation";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_evac, "Evacuation", _generation->generation_mode());
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_evac(),
                              "concurrent evacuation");

  heap->try_inject_alloc_failure();
  op_evacuate();
}

void ShenandoahConcurrentGC::entry_update_thread_roots() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());

  static const char* msg = "Concurrent update thread roots";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_update_thread_roots, "UpdateThreadRoots", _generation->generation_mode());
  EventMark em("%s", msg);

  // No workers used in this phase, no setup required
  heap->try_inject_alloc_failure();
  op_update_thread_roots();
}

void ShenandoahConcurrentGC::entry_updaterefs() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());
  static const char* msg = "Concurrent update references";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_update_refs, "ReferenceUpdate", _generation->generation_mode());
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_update_ref(),
                              "concurrent reference update");

  heap->try_inject_alloc_failure();
  op_updaterefs();
}

void ShenandoahConcurrentGC::entry_cleanup_complete() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());
  static const char* msg = "Concurrent cleanup";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::conc_cleanup_complete, "CleanupComplete", _generation->generation_mode(), true /* log_heap_usage */);
  EventMark em("%s", msg);

  // This phase does not use workers, no need for setup
  heap->try_inject_alloc_failure();
  op_cleanup_complete();
}

void ShenandoahConcurrentGC::entry_global_coalesce_and_fill() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();

  const char* msg = "Coalescing and filling old regions in global collect";
  ShenandoahConcurrentPhase gc_phase(msg, ShenandoahPhaseTimings::coalesce_and_fill, "CoalesceAndFill", _generation->generation_mode());

  TraceCollectorStats tcs(heap->monitoring_support()->concurrent_collection_counters());
  EventMark em("%s", msg);
  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_conc_marking(),
                              "concurrent coalesce and fill");

  op_global_coalesce_and_fill();
}

void ShenandoahConcurrentGC::op_reset() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  if (ShenandoahPacing) {
    heap->pacer()->setup_for_reset();
  }
  _generation->prepare_gc(_do_old_gc_bootstrap);
}

class ShenandoahInitMarkUpdateRegionStateClosure : public ShenandoahHeapRegionClosure {
private:
  ShenandoahMarkingContext* const _ctx;
public:
  ShenandoahInitMarkUpdateRegionStateClosure() : _ctx(ShenandoahHeap::heap()->marking_context()) {}

  void heap_region_do(ShenandoahHeapRegion* r) {
    assert(!r->has_live(), "Region " SIZE_FORMAT " should have no live data", r->index());
    if (r->is_active()) {
      // Check if region needs updating its TAMS. We have updated it already during concurrent
      // reset, so it is very likely we don't need to do another write here.  Since most regions
      // are not "active", this path is relatively rare.
      if (_ctx->top_at_mark_start(r) != r->top()) {
        _ctx->capture_top_at_mark_start(r);
      }
    } else {
      assert(_ctx->top_at_mark_start(r) == r->top(),
             "Region " SIZE_FORMAT " should already have correct TAMS", r->index());
    }
  }

  bool is_thread_safe() { return true; }
};

void ShenandoahConcurrentGC::op_init_mark() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "Should be at safepoint");
  assert(Thread::current()->is_VM_thread(), "can only do this in VMThread");

  assert(_generation->is_bitmap_clear(), "need clear marking bitmap");
  assert(!_generation->is_mark_complete(), "should not be complete");
  assert(!heap->has_forwarded_objects(), "No forwarded objects on this path");

  if (ShenandoahVerify) {
    heap->verifier()->verify_before_concmark();
  }

  if (VerifyBeforeGC) {
    Universe::verify();
  }

  _generation->set_concurrent_mark_in_progress(true);

  if (_do_old_gc_bootstrap) {
    // Update region state for both young and old regions
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::init_update_region_states);
    ShenandoahInitMarkUpdateRegionStateClosure cl;
    heap->parallel_heap_region_iterate(&cl);
    heap->old_generation()->parallel_heap_region_iterate(&cl);
  } else {
    // Update region state for only young regions
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::init_update_region_states);
    ShenandoahInitMarkUpdateRegionStateClosure cl;
    _generation->parallel_heap_region_iterate(&cl);
  }

  // Weak reference processing
  ShenandoahReferenceProcessor* rp = _generation->ref_processor();
  rp->reset_thread_locals();
  rp->set_soft_reference_policy(heap->soft_ref_policy()->should_clear_all_soft_refs());

  // Make above changes visible to worker threads
  OrderAccess::fence();

  // Arm nmethods for concurrent marking. When a nmethod is about to be executed,
  // we need to make sure that all its metadata are marked. alternative is to remark
  // thread roots at final mark pause, but it can be potential latency killer.
  if (heap->unload_classes()) {
    ShenandoahCodeRoots::arm_nmethods();
  }

  ShenandoahStackWatermark::change_epoch_id();
  if (ShenandoahPacing) {
    heap->pacer()->setup_for_mark();
  }
}

void ShenandoahConcurrentGC::op_mark_roots() {
  _mark.mark_concurrent_roots();
}

void ShenandoahConcurrentGC::op_mark() {
  _mark.concurrent_mark();
}

void ShenandoahConcurrentGC::op_final_mark() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "Should be at safepoint");
  assert(!heap->has_forwarded_objects(), "No forwarded objects on this path");

  if (ShenandoahVerify) {
    heap->verifier()->verify_roots_no_forwarded();
  }

  if (!heap->cancelled_gc()) {
    _mark.finish_mark();
    assert(!heap->cancelled_gc(), "STW mark cannot OOM");

    // Notify JVMTI that the tagmap table will need cleaning.
    JvmtiTagMap::set_needs_cleaning();

    bool mixed_evac = _generation->prepare_regions_and_collection_set(true /*concurrent*/);
    heap->set_mixed_evac(mixed_evac);

    // Has to be done after cset selection
    heap->prepare_concurrent_roots();

    if (!heap->collection_set()->is_empty()) {
      if (ShenandoahVerify) {
        heap->verifier()->verify_before_evacuation();
      }

      heap->set_evacuation_in_progress(true);
      // From here on, we need to update references.
      heap->set_has_forwarded_objects(true);

      // Verify before arming for concurrent processing.
      // Otherwise, verification can trigger stack processing.
      if (ShenandoahVerify) {
        heap->verifier()->verify_during_evacuation();
      }

      // Arm nmethods/stack for concurrent processing
      ShenandoahCodeRoots::arm_nmethods();
      ShenandoahStackWatermark::change_epoch_id();

      // Notify JVMTI that oops are changed.
      JvmtiTagMap::set_needs_rehashing();

      if (ShenandoahPacing) {
        heap->pacer()->setup_for_evac();
      }
    } else {
      if (ShenandoahVerify) {
        heap->verifier()->verify_after_concmark();
      }

      if (VerifyAfterGC) {
        Universe::verify();
      }
    }
  }
}

class ShenandoahConcurrentEvacThreadClosure : public ThreadClosure {
private:
  OopClosure* const _oops;

public:
  ShenandoahConcurrentEvacThreadClosure(OopClosure* oops);
  void do_thread(Thread* thread);
};

ShenandoahConcurrentEvacThreadClosure::ShenandoahConcurrentEvacThreadClosure(OopClosure* oops) :
  _oops(oops) {
}

void ShenandoahConcurrentEvacThreadClosure::do_thread(Thread* thread) {
  JavaThread* const jt = JavaThread::cast(thread);
  StackWatermarkSet::finish_processing(jt, _oops, StackWatermarkKind::gc);
}

class ShenandoahConcurrentEvacUpdateThreadTask : public AbstractGangTask {
private:
  ShenandoahJavaThreadsIterator _java_threads;

public:
  ShenandoahConcurrentEvacUpdateThreadTask(uint n_workers) :
    AbstractGangTask("Shenandoah Evacuate/Update Concurrent Thread Roots"),
    _java_threads(ShenandoahPhaseTimings::conc_thread_roots, n_workers) {
  }

  void work(uint worker_id) {
    // ShenandoahEvacOOMScope has to be setup by ShenandoahContextEvacuateUpdateRootsClosure.
    // Otherwise, may deadlock with watermark lock
    ShenandoahContextEvacuateUpdateRootsClosure oops_cl;
    ShenandoahConcurrentEvacThreadClosure thr_cl(&oops_cl);
    _java_threads.threads_do(&thr_cl, worker_id);
  }
};

void ShenandoahConcurrentGC::op_thread_roots() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert(heap->is_evacuation_in_progress(), "Checked by caller");
  ShenandoahGCWorkerPhase worker_phase(ShenandoahPhaseTimings::conc_thread_roots);
  ShenandoahConcurrentEvacUpdateThreadTask task(heap->workers()->active_workers());
  heap->workers()->run_task(&task);
}

void ShenandoahConcurrentGC::op_weak_refs() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert(heap->is_concurrent_weak_root_in_progress(), "Only during this phase");
  // Concurrent weak refs processing
  ShenandoahGCWorkerPhase worker_phase(ShenandoahPhaseTimings::conc_weak_refs);
  ShenandoahBreakpoint::at_after_reference_processing_started();
  _generation->ref_processor()->process_references(ShenandoahPhaseTimings::conc_weak_refs, heap->workers(), true /* concurrent */);
}

class ShenandoahEvacUpdateCleanupOopStorageRootsClosure : public BasicOopIterateClosure {
private:
  ShenandoahHeap* const _heap;
  ShenandoahMarkingContext* const _mark_context;
  bool  _evac_in_progress;
  Thread* const _thread;

public:
  ShenandoahEvacUpdateCleanupOopStorageRootsClosure();
  void do_oop(oop* p);
  void do_oop(narrowOop* p);
};

ShenandoahEvacUpdateCleanupOopStorageRootsClosure::ShenandoahEvacUpdateCleanupOopStorageRootsClosure() :
  _heap(ShenandoahHeap::heap()),
  _mark_context(ShenandoahHeap::heap()->marking_context()),
  _evac_in_progress(ShenandoahHeap::heap()->is_evacuation_in_progress()),
  _thread(Thread::current()) {
}

void ShenandoahEvacUpdateCleanupOopStorageRootsClosure::do_oop(oop* p) {
  const oop obj = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(obj)) {
    if (!_mark_context->is_marked(obj)) {
      if (_heap->is_in_active_generation(obj)) {
        // TODO: This worries me. Here we are asserting that an unmarked from-space object is 'correct'.
        // Normally, I would call this a bogus assert, but there seems to be a legitimate use-case for
        // accessing from-space objects during class unloading. However, the from-space object may have
        // been "filled". We've made no effort to prevent old generation classes being unloaded by young
        // gen (and vice-versa).
        shenandoah_assert_correct(p, obj);
        ShenandoahHeap::atomic_clear_oop(p, obj);
      }
    } else if (_evac_in_progress && _heap->in_collection_set(obj)) {
      oop resolved = ShenandoahBarrierSet::resolve_forwarded_not_null(obj);
      if (resolved == obj) {
        resolved = _heap->evacuate_object(obj, _thread);
      }
      ShenandoahHeap::atomic_update_oop(resolved, p, obj);
      assert(_heap->cancelled_gc() ||
             _mark_context->is_marked(resolved) && !_heap->in_collection_set(resolved),
             "Sanity");
    }
  }
}

void ShenandoahEvacUpdateCleanupOopStorageRootsClosure::do_oop(narrowOop* p) {
  ShouldNotReachHere();
}

class ShenandoahIsCLDAliveClosure : public CLDClosure {
public:
  void do_cld(ClassLoaderData* cld) {
    cld->is_alive();
  }
};

class ShenandoahIsNMethodAliveClosure: public NMethodClosure {
public:
  void do_nmethod(nmethod* n) {
    n->is_unloading();
  }
};

// This task not only evacuates/updates marked weak roots, but also "NULL"
// dead weak roots.
class ShenandoahConcurrentWeakRootsEvacUpdateTask : public AbstractGangTask {
private:
  ShenandoahVMWeakRoots<true /*concurrent*/> _vm_roots;

  // Roots related to concurrent class unloading
  ShenandoahClassLoaderDataRoots<true /* concurrent */, true /* single thread*/>
                                             _cld_roots;
  ShenandoahConcurrentNMethodIterator        _nmethod_itr;
  ShenandoahPhaseTimings::Phase              _phase;

public:
  ShenandoahConcurrentWeakRootsEvacUpdateTask(ShenandoahPhaseTimings::Phase phase) :
    AbstractGangTask("Shenandoah Evacuate/Update Concurrent Weak Roots"),
    _vm_roots(phase),
    _cld_roots(phase, ShenandoahHeap::heap()->workers()->active_workers()),
    _nmethod_itr(ShenandoahCodeRoots::table()),
    _phase(phase) {
    if (ShenandoahHeap::heap()->unload_classes()) {
      MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
      _nmethod_itr.nmethods_do_begin();
    }
  }

  ~ShenandoahConcurrentWeakRootsEvacUpdateTask() {
    if (ShenandoahHeap::heap()->unload_classes()) {
      MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
      _nmethod_itr.nmethods_do_end();
    }
    // Notify runtime data structures of potentially dead oops
    _vm_roots.report_num_dead();
  }

  void work(uint worker_id) {
    ShenandoahConcurrentWorkerSession worker_session(worker_id);
    {
      ShenandoahEvacOOMScope oom;
      // jni_roots and weak_roots are OopStorage backed roots, concurrent iteration
      // may race against OopStorage::release() calls.
      ShenandoahEvacUpdateCleanupOopStorageRootsClosure cl;
      _vm_roots.oops_do(&cl, worker_id);
    }

    // If we are going to perform concurrent class unloading later on, we need to
    // cleanup the weak oops in CLD and determinate nmethod's unloading state, so that we
    // can cleanup immediate garbage sooner.
    if (ShenandoahHeap::heap()->unload_classes()) {
      // Applies ShenandoahIsCLDAlive closure to CLDs, native barrier will either NULL the
      // CLD's holder or evacuate it.
      {
        ShenandoahIsCLDAliveClosure is_cld_alive;
        _cld_roots.cld_do(&is_cld_alive, worker_id);
      }

      // Applies ShenandoahIsNMethodAliveClosure to registered nmethods.
      // The closure calls nmethod->is_unloading(). The is_unloading
      // state is cached, therefore, during concurrent class unloading phase,
      // we will not touch the metadata of unloading nmethods
      {
        ShenandoahWorkerTimingsTracker timer(_phase, ShenandoahPhaseTimings::CodeCacheRoots, worker_id);
        ShenandoahIsNMethodAliveClosure is_nmethod_alive;
        _nmethod_itr.nmethods_do(&is_nmethod_alive);
      }
    }
  }
};

void ShenandoahConcurrentGC::op_weak_roots() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert(heap->is_concurrent_weak_root_in_progress(), "Only during this phase");
  // Concurrent weak root processing
  {
    ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_weak_roots_work);
    ShenandoahGCWorkerPhase worker_phase(ShenandoahPhaseTimings::conc_weak_roots_work);
    ShenandoahConcurrentWeakRootsEvacUpdateTask task(ShenandoahPhaseTimings::conc_weak_roots_work);
    heap->workers()->run_task(&task);
  }

  // Perform handshake to flush out dead oops
  {
    ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_weak_roots_rendezvous);
    heap->rendezvous_threads();
  }
}

void ShenandoahConcurrentGC::op_class_unloading() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert (heap->is_concurrent_weak_root_in_progress() &&
          heap->unload_classes(),
          "Checked by caller");
  heap->do_class_unloading();
}

class ShenandoahEvacUpdateCodeCacheClosure : public NMethodClosure {
private:
  BarrierSetNMethod* const                  _bs;
  ShenandoahEvacuateUpdateMetadataClosure<> _cl;

public:
  ShenandoahEvacUpdateCodeCacheClosure() :
    _bs(BarrierSet::barrier_set()->barrier_set_nmethod()),
    _cl() {
  }

  void do_nmethod(nmethod* n) {
    ShenandoahNMethod* data = ShenandoahNMethod::gc_data(n);
    ShenandoahReentrantLocker locker(data->lock());
    // Setup EvacOOM scope below reentrant lock to avoid deadlock with
    // nmethod_entry_barrier
    ShenandoahEvacOOMScope oom;
    data->oops_do(&_cl, true/*fix relocation*/);
    _bs->disarm(n);
  }
};

class ShenandoahConcurrentRootsEvacUpdateTask : public AbstractGangTask {
private:
  ShenandoahPhaseTimings::Phase                 _phase;
  ShenandoahVMRoots<true /*concurrent*/>        _vm_roots;
  ShenandoahClassLoaderDataRoots<true /*concurrent*/, false /*single threaded*/> _cld_roots;
  ShenandoahConcurrentNMethodIterator           _nmethod_itr;

public:
  ShenandoahConcurrentRootsEvacUpdateTask(ShenandoahPhaseTimings::Phase phase) :
    AbstractGangTask("Shenandoah Evacuate/Update Concurrent Strong Roots"),
    _phase(phase),
    _vm_roots(phase),
    _cld_roots(phase, ShenandoahHeap::heap()->workers()->active_workers()),
    _nmethod_itr(ShenandoahCodeRoots::table()) {
    if (!ShenandoahHeap::heap()->unload_classes()) {
      MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
      _nmethod_itr.nmethods_do_begin();
    }
  }

  ~ShenandoahConcurrentRootsEvacUpdateTask() {
    if (!ShenandoahHeap::heap()->unload_classes()) {
      MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
      _nmethod_itr.nmethods_do_end();
    }
  }

  void work(uint worker_id) {
    ShenandoahConcurrentWorkerSession worker_session(worker_id);
    {
      ShenandoahEvacOOMScope oom;
      {
        // vm_roots and weak_roots are OopStorage backed roots, concurrent iteration
        // may race against OopStorage::release() calls.
        ShenandoahContextEvacuateUpdateRootsClosure cl;
        _vm_roots.oops_do<ShenandoahContextEvacuateUpdateRootsClosure>(&cl, worker_id);
      }

      {
        ShenandoahEvacuateUpdateMetadataClosure<> cl;
        CLDToOopClosure clds(&cl, ClassLoaderData::_claim_strong);
        _cld_roots.cld_do(&clds, worker_id);
      }
    }

    // Cannot setup ShenandoahEvacOOMScope here, due to potential deadlock with nmethod_entry_barrier.
    if (!ShenandoahHeap::heap()->unload_classes()) {
      ShenandoahWorkerTimingsTracker timer(_phase, ShenandoahPhaseTimings::CodeCacheRoots, worker_id);
      ShenandoahEvacUpdateCodeCacheClosure cl;
      _nmethod_itr.nmethods_do(&cl);
    }
  }
};

void ShenandoahConcurrentGC::op_strong_roots() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert(heap->is_concurrent_strong_root_in_progress(), "Checked by caller");
  ShenandoahConcurrentRootsEvacUpdateTask task(ShenandoahPhaseTimings::conc_strong_roots);
  heap->workers()->run_task(&task);
  heap->set_concurrent_strong_root_in_progress(false);
}

void ShenandoahConcurrentGC::op_cleanup_early() {
  ShenandoahHeap::heap()->free_set()->recycle_trash();
}

void ShenandoahConcurrentGC::op_evacuate() {
  ShenandoahHeap::heap()->evacuate_collection_set(true /*concurrent*/);
}

void ShenandoahConcurrentGC::op_init_updaterefs() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  heap->set_evacuation_in_progress(false);
  heap->set_concurrent_weak_root_in_progress(false);
  heap->prepare_update_heap_references(true /*concurrent*/);
  heap->set_update_refs_in_progress(true);
  if (ShenandoahVerify) {
    heap->verifier()->verify_before_updaterefs();
  }
  if (ShenandoahPacing) {
    heap->pacer()->setup_for_updaterefs();
  }
}

void ShenandoahConcurrentGC::op_updaterefs() {
  ShenandoahHeap::heap()->update_heap_references(true /*concurrent*/);
}

class ShenandoahUpdateThreadClosure : public HandshakeClosure {
private:
  ShenandoahUpdateRefsClosure _cl;
public:
  ShenandoahUpdateThreadClosure();
  void do_thread(Thread* thread);
};

ShenandoahUpdateThreadClosure::ShenandoahUpdateThreadClosure() :
  HandshakeClosure("Shenandoah Update Thread Roots") {
}

void ShenandoahUpdateThreadClosure::do_thread(Thread* thread) {
  if (thread->is_Java_thread()) {
    JavaThread* jt = JavaThread::cast(thread);
    ResourceMark rm;
    jt->oops_do(&_cl, NULL);
  }
}

void ShenandoahConcurrentGC::op_update_thread_roots() {
  ShenandoahUpdateThreadClosure cl;
  Handshake::execute(&cl);
}

void ShenandoahConcurrentGC::op_final_updaterefs() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "must be at safepoint");
  assert(!heap->_update_refs_iterator.has_next(), "Should have finished update references");

  heap->finish_concurrent_roots();

  // Clear cancelled GC, if set. On cancellation path, the block before would handle
  // everything.
  if (heap->cancelled_gc()) {
    heap->clear_cancelled_gc(true /* clear oom handler */);
  }

  // Has to be done before cset is clear
  if (ShenandoahVerify) {
    heap->verifier()->verify_roots_in_to_space();
  }

  heap->update_heap_region_states(true /*concurrent*/);

  if (heap->is_concurrent_old_mark_in_progress()) {
    // Purge the SATB buffers, transferring any valid, old pointers to the
    // old generation mark queue. From here on, no mutator will have access
    // to anything that will be trashed and recycled.
    heap->purge_old_satb_buffers(false /* abandon */);
  }

  heap->set_update_refs_in_progress(false);
  heap->set_has_forwarded_objects(false);

  // Aging_cycle is only relevant during evacuation cycle for individual objects and during final mark for
  // entire regions.  Both of these relevant operations occur before final update refs.
  heap->set_aging_cycle(false);

  if (ShenandoahVerify) {
    heap->verifier()->verify_after_updaterefs();
  }

  if (VerifyAfterGC) {
    Universe::verify();
  }

  heap->rebuild_free_set(true /*concurrent*/);
}

void ShenandoahConcurrentGC::op_final_roots() {
  ShenandoahHeap::heap()->set_concurrent_weak_root_in_progress(false);
}

void ShenandoahConcurrentGC::op_cleanup_complete() {
  ShenandoahHeap::heap()->free_set()->recycle_trash();
}

void ShenandoahConcurrentGC::op_global_coalesce_and_fill() {
  ShenandoahHeap::heap()->coalesce_and_fill_old_regions();
}

bool ShenandoahConcurrentGC::check_cancellation_and_abort(ShenandoahDegenPoint point) {
  if (ShenandoahHeap::heap()->cancelled_gc()) {
    _degen_point = point;
    return true;
  }
  return false;
}

void ShenandoahConcurrentGC::init_mark_event_message(char* buf, size_t len) const {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert(!heap->has_forwarded_objects(), "Should not have forwarded objects here");
  if (heap->unload_classes()) {
    jio_snprintf(buf, len, "Pause Init Mark (%s) (unload classes)", _generation->name());
  } else {
    jio_snprintf(buf, len, "Pause Init Mark (%s)", _generation->name());
  }
}

void ShenandoahConcurrentGC::final_mark_event_message(char* buf, size_t len) const {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert(!heap->has_forwarded_objects() || heap->is_concurrent_old_mark_in_progress(),
         "Should not have forwarded objects during final mark (unless old gen concurrent mark is running)");
  if (heap->unload_classes()) {
    jio_snprintf(buf, len, "Pause Final Mark (%s) (unload classes)", _generation->name());
  } else {
    jio_snprintf(buf, len, "Pause Final Mark (%s)", _generation->name());
  }
}

void ShenandoahConcurrentGC::conc_mark_event_message(char* buf, size_t len) const {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  assert(!heap->has_forwarded_objects() || heap->is_concurrent_old_mark_in_progress(),
         "Should not have forwarded objects concurrent mark (unless old gen concurrent mark is running");
  if (heap->unload_classes()) {
    jio_snprintf(buf, len, "Concurrent marking (%s) (unload classes)", _generation->name());
  } else {
    jio_snprintf(buf, len, "Concurrent marking (%s)", _generation->name());
  }
}
