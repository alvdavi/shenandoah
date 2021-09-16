/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) Amazon.com Inc. or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */

package com.sun.management;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;

import java.lang.String;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import java.lang.management.MemoryPoolMXBean;  // May switch to com.sun.management
import java.lang.management.MemoryUsage;       // May switch to com.sun.management
import java.lang.String;

import com.sun.management.GarbageCollectorMXBean;
import com.sun.management.GcInfo;

/**
 * Garbage collection information for a single collection, including
 * before and after details about the most recently completed garbage
 * for the associated collector. The returned information includes
 * internal Java virtual machine and system information as well as
 * information on the affected memory pools' sizes at the start and
 * end of the collection.
 * <p>
 * In addition to the information contained
 * in {@link GcInfo} for one garbage collection, it contains the following
 * GC-specific attributes:
 * <blockquote>
 * <ul>
 *   <li>...</li>
 * </ul>
 * </blockquote>
 *
 * <p>
 * {@code GcDetails} is a {@link CompositeData CompositeData}
 * The GC-specific attributes can be obtained via the CompositeData
 * interface. This is a historical relic, and other classes should
 * not copy this pattern. Use {@link CompositeDataView} instead.
 *
 * <h2>MXBean Mapping</h2>
 * {@code GcDetails} is mapped to a {@link CompositeData CompositeData}
 * with attributes as specified in the {@link #from from} method.
 *
 * @author  Paul Hohensee
 * @since   18
 */
public class GcDetails /*extends GcInfo*/ {
    private final long startTime = 0; // Nanoseconds
    private final long endTime = 0;   // Nanoseconds

    public GcDetails() {
	super();
    }

    /**
     * Whether the data returned by this {@code GCDetails} object has passed
     * all validity tests.
     *
     * @return {@code true} if the data in this {@code GCDetails} is valid;
     *         otherwise, {@code false}.
     */
    public boolean isValid() {
        return false;
    }

    /**
     * The reason for this collection. The most common one is when the Java
     * virtual machine detects that all available Java heap space has been
     * occupied, or that the Java virtual machine estimates that all available
     * space will soon be occupied. A Java virtual machine might use "Max" as
     * the string to identify this cause for starting a collection, or
     * possibly "Alo" for an object allocation failure. The reason is unique
     * across all collectors supported by the Java virtual machine.
     *
     * @return a {@link String} representing the reason for this collection.
     */
    public String getGarbageCollectionCause() {
        return null;
    }

    /**
     * At the start of this collection, the approximate number of bytes occupied
     * by live objects in the memory pools affected by the associated collector.
     * If this operation is not supported, this method returns {@code -1}.
     * <p>
     * Note that "the memory pools affected by the associated collector" may
     * not be the same as "the memory pools affected by this collection".
     *
     * @return the approximate number of bytes occupied by live objects in the
     *         memory pools affected by the associated collector at the start
     *         of this collection; or {@code -1} if this operation is not
     *         supported.
     */
    public long getLiveInPoolsBeforeGc() {
        return -1;
    }

    /**
     * At the end of this collection, the approximate number of bytes occupied
     * by live objects in the memory pools affected by the associated collector.
     * If this operation is not supported, this method returns {@code -1}.
     * <p>
     * Note that "the memory pools affected by the associated collector" may
     * not be the same as "the memory pools affected by this collection".
     *
     * @return the approximate number of bytes occupied by live objects in the
     *         memory pools affected by the associated collector at the end of
     *         this collection; or {@code -1} if this operation is not supported.
     */
    public long getLiveInPoolsAfterGc() {
        return -1;
    }

    /**
     * The aggregate size, in bytes, of all unreachable objects discovered
     * by the associated collector during this collection.
     *
     * @return the aggregate size, in bytes, of all unreachable objects
     *         discovered by the associated collector during this collection.
     */
    public long getGarbageFound() {
        return 0;
    }

    /**
     * The aggregate size, in bytes, of all objects collected by the
     * associated collector during this collection.
     *
     * @return the aggregate size, in bytes, of all objects collected by
     *         the associated collector during this collection.
    */
    public long getGarbageCollected() {
        return 0;
    }

    /**
     * If objects were copied between the memory pools affected by this
     * collection, their aggregate size in bytes. If this is a young
     * collection in a two-generation system, this value is the total
     * size of objects promoted to the old generation. This value is
     * not the same as the difference in before and after {@link MemoryUsage}
     * occupancy because allocation in the target {@link MemoryPoolMXBean MemoryPool}(s)
     * may have been done by other than the GC threads that ran this
     * collection.
     *
     * @return the aggregate size in bytes of objects copied between
     *         the memory pools affected by this collection.
     */
    public long getCopiedBetweenPools() {
        return 0;
    }

    /**
     * The approximate start time of this collection in nanoseconds
     * since Java virtual machine launch.
     *
     * @return the start time of this collection.
     */
    public long getStartTimeNanos() {
        return startTime;
    }

    /**
     * The approximate start time of this collection in milliseconds
     * since Java virtual machine launch.
     * <p>
     * This method is a convenience method that returns an
     * approximation of the value returned by
     * {@link #getStartTimeNanos getStartTimeNanos}.
     *
     * @return the start time of this collection.
     */
    public long getStartTimeMillis() {
        return startTime / 1000_000L;
    }

    /**
     * The approximate start time of this collection (in seconds
     * as a double) since Java virtual machine launch.
     * <p>
     * This method is a convenience method that returns an
     * approximation of the value returned by
     * {@link #getStartTimeNanos getStartTimeNanos}.
     *
     * @return the start time of this collection.
     */
    public double getStartTimeSeconds() {
        return (double)startTime / 1.0e9;
    }

    /**
     * The approximate end time of this collection in nanoseconds
     * since Java virtual machine launch.
     *
     * @return the end time of this collection.
     */
    public long getEndTimeNanos() {
        return endTime;
    }

    /**
     * The approximate end time of this collection in milliseconds
     * since Java virtual machine launch.
     * <p>
     * This method is a convenience method that returns an
     * approximation of the value returned by
     * {@link #getEndTimeNanos getEndTimeNanos}.
     *
     * @return the end time of this collection.
     */
    public long getEndTimeMillis() {
        return endTime / 1000_000L;
    }

    /**
     * The approximate end time of this collection (in seconds
     * as a double) since Java virtual machine launch.
     * <p>
     * This method is a convenience method that returns an
     * approximation of the value returned by
     * {@link #getEndTimeNanos getEndTimeNanos}.
     *
     * @return the end time of this collection.
     */
    public double getEndTimeSeconds() {
        return (double)endTime / 1.0e9;
    }

    /**
     * The approximate elapsed wall clock time in nanoseconds between
     * the start and end of this collection. The returned value is
     * equal to {@link #getEndTimeNanos getEndTimeNanos}() -
     * {@link #getStartTimeNanos getStartTimeNanos}().
     *
     * @return the elapsed time of this collection. 
     */
    public long getDurationNanos() {
        return endTime - startTime;
    }

    /**
     * The approximate elapsed wall clock time in millisconds between
     * the start and end of this collection. The returned value is
     * equal to {@link #getEndTimeMillis getEndTimeMillis}() -
     * {@link #getStartTimeMillis getStartTimeMillis}(), which is not
     * the same as {@link #getDurationNanos getDurationNanos}() /
     * 1000_000L.
     *
     * @return the elapsed time of this collection.
     */
    public double getDurationMillis() {
        return getEndTimeMillis() - getStartTimeMillis();
    }

    /**
     * The approximate elapsed wall clock time (in seconds as a double)
     * between the start and end of this collection. The returned value is
     * equal to {@link #getEndTimeSeconds getEndTimeSeconds}() -
     * {@link #getStartTimeSeconds getStartTimeSeconds}(), which is not
     * the same as (double){@link #getDurationNanos getDurationNanos}().
     *
     * @return the elapsed time of this collection.
     */
    public double getDurationSeconds() {
        return getEndTimeSeconds() - getStartTimeSeconds();
    }

    /** 
     * The approximate amount of time in nanoseconds from the end of
     * the last previous collection run by the associated collector
     * to the start of this collection. The time interval represents
     * the amount of time the collector did not run.
     *
     * @return the amount of time from the end of the last previous
     *         collection run by the associated collector to the start
     *         of this collection.
     */
    public long getTimeFromEndOfPreviousToStartNanos() {
        return 0;
    }

    /** 
     * The approximate amount of time in milliseconds from the end of
     * the last previous collection run by the associated collector
     * to the start of this collection. The time interval represents
     * the amount of time the collector did not run.
     * <p>
     * This method is a convenience method that returns an
     * approximation of the value returned by
     * {@link #getTimeFromEndOfPreviousToStartNanos getTimeFromEndOfPreviousToStartNanos}.
     *
     * @return the amount of time from the end of the last previous
     *         collection run by the associated collector to the start
     *         of this collection.
     */
    public long getTimeFromEndOfPreviousToStartMillis() {
        return getTimeFromEndOfPreviousToStartNanos() / 1000_000L;
    }

    /** 
     * The approximate amount of time (in seconds as a double) from
     * the end of the last previous collection run by the associated
     * collector to the start of this collection. The time interval
     * represents the amount of time the collector did not run.
     * <p>
     * This method is a convenience method that returns an
     * approximation of the value returned by
     * {@link #getTimeFromEndOfPreviousToStartNanos getTimeFromEndOfPreviousToStartNanos}.
     *
     * @return the amount of time from the end of the last previous
     *         collection run by the associated collector to the start
     *         of this collection.
     */
    public double getTimeFromEndOfPreviousToStartSeconds() {
        return (double)getTimeFromEndOfPreviousToStartNanos() / 1.0e9;
    }

    /**
     * The percentage of time that the associated collector ran over the
     * time interval between the end of the last previous collection run
     * by it and the end of this collection.
     *
     * The percentage is calculated by:
     * ( {@link #getDurationSeconds getDurationSeconds}() /
     * ( (double){@link #getTimeFromEndOfPreviousToStartNanos getTimeFromEndOfPreviousToStartNanos}()
     * + {@link #getDurationSeconds getDurationSeconds}() ) ) * 100.
     *
     * @return the percentage of time the associated collector ran
     *         between the end of its last previous collection and
     *         the end of this collection.
     */
    public double getPercentageOfTimeCollectorWasRunning() {
        return 0.0;
    }

    /**
     * The approximate object allocation rate in megabytes per second in
     * the memory pools affected by the associated collector during the
     * time interval returned by {@link #getDurationNanos getDurationNanos}.
     * <p>
     * Note that "the memory pools affected by the associated collector" may
     * not be the same as "the memory pools affected by this collection".
     *
     * @return the object allocation rate in megabytes per second during the
     *         time interval between the start and end of this collection.
     */
    public double getAllocRateDuringCollection() {
        return 0.0;
    }

    /**
     * The approximate object allocation rate in megabytes per second in
     * the memory pools affected by the associated collector during the
     * time interval returned by
     * {@link #getTimeFromEndOfPreviousToStartNanos getTimeFromEndOfPreviousToStartNanos}.
     * <p>
     * Note that "the memory pools affected by the associated collector" may
     * not be the same as "the memory pools affected by this collection".
     *
     * @return the object allocation rate in megabytes per second during the
     *         time interval between the end of the last previous collection
     *         and the start of this collection.
     */
    public double getAllocRateBetweenEndOfPreviousAndStart() {
        return 0.0;
    }

    /**
     * The maximum number of garbage collection threads used during this
     * collection from the pool of garbage collection threads of the
     * associated collector.
     * 
     * @return the maximum number of garbage collection threads used in
     *         this collection.
     */
    public long getMaxGarbageCollectorThreadCount() {
        return 0;
    }

    /**
     * The number of application threads in the running process at the
     * end of of this collection.
     *
     * @return the number of application threads in the running process
     *         at the end of this collection.
     */
    public long getAppThreadCountAfterGc() {
        return 0;
    }

    /**
     * For the associated collector, the number of application threads
     * were required to pause waiting for object allocation, in the time
     * interval between the end of the previous collection and the end
     * of this collection.
     *
     * @return the number of application threads required to pause waiting
     *         for object allocation in the time interval between the end
     *         of the previous collection and the end of this collection.
     */
    public long getNumberOfAppThreadsDelayed() {
        return 0;
    }

    /**
     * For the associated collector, the approximate mean pause time in
     * nanoseconds that all application threads were required to pause
     * waiting for object allocation during the time interval between
     * the end of the previous collection and the end of this collection.
     *
     * @return the mean pause time that all application threads were
     *         required to pause waiting for object allocation in
     *         the time interval between the end of the previous
     *         collection and the end of this collection.
     */
    public long getMeanAppThreadDelayNanos() {
        return 0;
    }

    /**
     * For the associated collector, the approximate mean pause time in
     * milliseconds that all application threads were required to pause
     * waiting for object allocation during the time interval between
     * the end of the previous collection and the end of this collection.
     * <p>
     * This method is a convenience method that returns an approximation
     * of the value returned by
     * {@link #getMeanAppThreadDelayNanos getMeanAppThreadDelayNanos}.
     *
     * @return the mean pause time that all application threads were
     *         required to pause waiting for object allocation in
     *         the time interval between the end of the previous
     *         collection and the end of this collection.
     */
    public long getMeanAppThreadDelayMillis() {
        return getMeanAppThreadDelayNanos() / 1000_000L;
    }

    /**
     * For the associated collector, the approximate mean pause time (in
     * seconds as a double) that all application threads were required to
     * pause waiting for object allocation during the time interval
     * between the end of the previous collection and the end of this
     * collection.
     * <p>
     * This method is a convenience method that returns an approximation
     * of the value returned by
     * {@link #getMeanAppThreadDelayNanos getMeanAppThreadDelayNanos}.
     *
     * @return the mean pause time that all application threads were
     *         required to pause waiting for object allocation in
     *         the time interval between the end of the previous
     *         collection and the end of this collection.
     */
    public double getMeanAppThreadDelaySeconds() {
        return (double)getMeanAppThreadDelayNanos() / 1.0e9;
    }

    /**
     * For the associated collector, the approximate maximum pause time
     * in nanoseconds across all application threads that were required
     * to pause waiting for object allocation, in the time interval
     * between the end of the previous collection and the end of this
     * collection.
     *
     * @return maximum pause time across all application threads that
     *         were required to pause waiting for object allocation
     *         in the time interval between the end of the previous
     *         collection and the end of this collection.
     */
    public long getMaxAppThreadDelayNanos() {
        return 0;
    }

    /**
     * For the associated collector, the approximate maximum pause time
     * in milliseconds across all application threads that were required
     * to pause waiting for object allocation, in the time interval
     * between the end of the previous collection and the end of this
     * collection.
     * <p>
     * This method is a convenience method that returns an approximation
     * of the value returned by
     * {@link getMaxAppThreadDelayNanos getMaxAppThreadDelayNanos}.
     *
     * @return maximum pause time across all application threads that
     *         were required to pause waiting for object allocation
     *         in the time interval between the end of the previous
     *         collection and the end of this collection.
     */
    public long getMaxAppThreadDelayMillis() {
        return getMaxAppThreadDelayNanos() / 1000_000L;
    }

    /**
     * For the associated collector, the approximate maximum pause time
     * (in seconds as a double) across all application threads that were
     * required to pause waiting for object allocation, in the time
     * interval between the end of the previous collection and the end
     * of this collection.
     * <p>
     * This method is a convenience method that returns an approximation
     * of the value returned by
     * {@link getMaxAppThreadDelayNanos getMaxAppThreadDelayNanos}.
     *
     * @return maximum pause time across all application threads that
     *         were required to pause waiting for object allocation
     *         in the time interval between the end of the previous
     *         collection and the end of this collection.
     */
    public double getMaxAppThreadDelaySeconds() {
        return (double)getMaxAppThreadDelayNanos() / 1.0e9;
    }

    /**
     * A {@link List} of {@link ConcurrentDetails} for the concurrent
     * phases of this collection.
     *
     * @return a List of {@link ConcurrentDetails} for the concurrent
     *         phases of this collection.
     */
    public List<PauseDetails> getConcurrentDetails() {
//        return Collections.unmodifiableList<ConcurrentDetails>(new ArrayList<ConcurrentDetails>(0));
	return null;
    }

    /**
     * A {@link List} of {@link PauseDetails} for the pauses required during
     * this collection.
     *
     * @return a List of PauseDetails for the pauses during this collection.
     */
    public List<PauseDetails> getPauseDetails() {
//        return Collections.unmodifiableList<PauseDetails>(new ArrayList<PauseDetails>(0));
	return null;
    }

   /**
     * Returns a {@code GcInfo} object represented by the
     * given {@code CompositeData}. The given
     * {@code CompositeData} must contain
     * all the following attributes:
     *
     * <blockquote>
     * <table class="striped"><caption style="display:none">description</caption>
     * <thead>
     * <tr>
     *   <th scope="col" style="text-align:left">Attribute Name</th>
     *   <th scope="col" style="text-align:left">Type</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <th scope="row">index</th>
     *   <td>{@code java.lang.Long}</td>
     * </tr>
     * <tr>
     *   <th scope="row">startTime</th>
     *   <td>{@code java.lang.Long}</td>
     * </tr>
     * <tr>
     *   <th scope="row">endTime</th>
     *   <td>{@code java.lang.Long}</td>
     * </tr>
     * <tr>
     *   <th scope="row">memoryUsageBeforeGc</th>
     *   <td>{@code javax.management.openmbean.TabularData}</td>
     * </tr>
     * <tr>
     *   <th scope="row">memoryUsageAfterGc</th>
     *   <td>{@code javax.management.openmbean.TabularData}</td>
     * </tr>
     * </tbody>
     * </table>
     * </blockquote>
     *
     * @throws IllegalArgumentException if {@code cd} does not
     *   represent a {@code GcInfo} object with the attributes
     *   described above.
     *
     * @return a {@code GcInfo} object represented by {@code cd}
     * if {@code cd} is not {@code null}; {@code null} otherwise.
     */
    public static GcInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }

	//        if (cd instanceof GcDetailsCompositeData) {
	//            return ((GcDetailsCompositeData) cd).getGcDetails();
	//        } else {
	//            return new GcDetails(cd);
	//        }
	return null;
    }
}
