/*
 * Copyright (c) 2013, 2019, Red Hat, Inc. All rights reserved.
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

#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "services/memoryPool.hpp"

#include "gc/shenandoah/shenandoahGeneration.hpp"
#include "gc/shenandoah/shenandoahMemoryManager.hpp"
#include "gc/shenandoah/shenandoahOldGeneration.hpp"
#include "gc/shenandoah/shenandoahYoungGeneration.hpp"


ShenandoahMemoryManager::ShenandoahMemoryManager(ShenandoahHeap* heap,
                                                 ShenandoahGeneration* generation,
                                                 const char* name,
                                                 const char* gc_end_message) :
        ConcurrentGCMemoryManager(name, gc_end_message),
        _heap(heap),
        _generation(generation),
        _allocated_since_previous_start(0L)
        {}


void ShenandoahMemoryManager::gc_begin(bool recordGCBeginTime, 
                                             bool recordPreGCUsage,
                                             bool recordAccumulatedGCTime) {

  ConcurrentGCMemoryManager::gc_begin(recordGCBeginTime, recordPreGCUsage, recordAccumulatedGCTime);
  _current_gc_stat->set_allocated_since_previous(_allocated_since_previous_start - _last_gc_stat->get_allocated_during_collection());
  if (_heap->mode()->is_generational() && _generation->generation_mode() != OLD) {
    // Discount how much the old gen sees now from the total. This value should always be zero when not in generational mode or for old gen.
    _current_gc_stat->set_copied_between_pools(-_heap->old_generation()->bytes_allocated_since_gc_start());
  }


}

void ShenandoahMemoryManager::gc_end(bool recordPostGCUsage,
                                           bool recordAccumulatedGCTime,
                                           bool recordGCEndTime, bool countCollection,
                                           GCCause::Cause cause,
                                           bool allMemoryPoolsAffected) {

  if (_heap->mode()->is_generational() && _generation->generation_mode() != OLD) {
    // Final update of the copied between pools. This value should always be zero when not in generational mode or for old gen.
    _current_gc_stat->set_copied_between_pools(_current_gc_stat->get_copied_between_pools() + _heap->old_generation()->bytes_allocated_since_gc_start());
  }
  _current_gc_stat->set_allocated_during_collection(_generation->bytes_allocated_since_gc_start());

  jlong live_memory = 0L;
  for (int i = 0; i < num_memory_pools(); i++) {
    MemoryPool* pool = get_memory_pool(i);
    live_memory += pool->used_in_bytes();
  }

  _current_gc_stat->set_live_in_pools_after_gc(live_memory);

  // Live before gc is not calculated at the beginning, but by checking the live at the end minus the amount that was allocated during the collection.
  _current_gc_stat->set_live_in_pools_before_gc(live_memory - _current_gc_stat->get_allocated_during_collection());


  ConcurrentGCMemoryManager::gc_end(recordPostGCUsage, recordAccumulatedGCTime,
                recordGCEndTime, countCollection, cause,allMemoryPoolsAffected);
}

void ShenandoahMemoryManager::gc_requested() {
  // This method is called before the actual gc starts, so we can record information related to the previous cycle
  _allocated_since_previous_start = _generation->bytes_allocated_since_gc_start();
}

void ShenandoahMemoryManager::update_copied_between_pools(jlong copied_before_reset) {
  // A reset of the bytes_allocated_since_gc_start of old gen is happening. Update the amount promoted before it happens.
  _current_gc_stat->set_copied_between_pools(_current_gc_stat->get_copied_between_pools() + copied_before_reset);
}


void ShenandoahMemoryManager::report_garbage(jlong found, jlong collected) {
  _current_gc_stat->set_garbage_found(found);
  _current_gc_stat->set_garbage_collected(collected);
}


//
// Global Memory Manager
//
ShenandoahGlobalMemoryManager::ShenandoahGlobalMemoryManager(ShenandoahHeap* heap) :
        ShenandoahMemoryManager(heap, heap->global_generation(), "Shenandoah Global", "end of global collection") 
        {}



//
// Young Gen memory manager
//
ShenandoahYoungGenMemoryManager::ShenandoahYoungGenMemoryManager(ShenandoahHeap* heap) :
        ShenandoahMemoryManager(heap, heap->young_generation(), "Shenandoah Young Gen", "end of young generation collection") 
        {}


//
// Old Gen memory manager
//
ShenandoahOldGenMemoryManager::ShenandoahOldGenMemoryManager(ShenandoahHeap* heap) :
        ShenandoahMemoryManager(heap, heap->old_generation(), "Shenandoah Old Gen", "end of old generation collection")
        {}

