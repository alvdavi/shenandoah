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

/**
 * The {@code PhaseDetails} class is the common superclass of
 * {@code ConcurrentDetails} and {@code PauseDetails}. Objects of these
 * two classes describe the concurrent and stop-the-world phases of a
 * garbage collection and are made available via a {@code GcDetails}
 * object. {@code PhaseDetails} includes a type, an integer identifier
 * whose value is the number of phases of that type done by the
 * associated garbage collector, a timestamp specifying the time since
 * the launch of the Java virtual machine at the start of the phase,
 * and the wall clock duration of the phase. The type is a
 * {@link String} string describing the phase type, is specific to
 * the implementation of the Java virtual machine and unique across
 * all phase typess.
 *
 * @author Paul Hohensee
 * @since  18
 */
public interface PhaseInfo {


    // protected PhaseInfo(long index, long startTime, long endTime) {
    //     this.index = index;
    //     this.startTime = startTime;
    //     this.endTime = endTime;
    // }



    /**
     * Returns the identifier of this phase, which is the number of
     * phases of this type done by this collector.
     *
     * @return the identifier of this phase, which is the number of
     *         phases of this type done by this collector
     */
    public long getId();

    /**
     * The type of this phase. Types are unique across the Java virtual
     * machine.
     *
     * @return a {@code String} representing the type of this pause.
     */
    public String getPhaseType();

    /**
     * The approximate start time of this phase in nanoseconds since
     * the Java virtual machine was started.
     *
     * @return the start time of this phase.
     */
    public long getStartTimeNanos();

    /**
     * The approximate start time of this phase in milliseconds since
     * Java virtual machine launch.
     * <p>
     * This is a convenience method that returns an approximation of the
     * value returned by {@link #getStartTimeNanos getStartTimeNanos}.
     *
     * @return the start time of this phase.
     */
    default public long getStartTimeMillis() {
        return getStartTimeNanos() / 1000_000L;
    }

    /**
     * The approximate start time of this phase (in seconds as a double)
     * since Java virtual machine launch.
     * <p>
     * This is a convenience method that returns an approximation of the
     * value returned by {@link #getStartTimeNanos getStartTimeNanos}.
     *
     * @return the start time of this phase.
     */
    default public double getStartTimeSeconds() {
        return (double)getStartTimeNanos() / 1.0e9;
    }

    /**
     * The approximate end time of this phase in nanoseconds since
     * Java virtual machine launch.
     *
     * @return the end time of this phase.
     */
    public long getEndTimeNanos();

    /**
     * The approximate end time of this phase in milliseconds since
     * Java virtual machine launch.
     * <p>
     * This is a convenience method that returns an approximation of the
     * value returned by {@link #getEndTimeNanos getEndTimeNanos}.
     *
     * @return the end time of this phase.
     */
    default public long getEndTimeMillis() {
        return getEndTimeNanos() / 1000_000L;
    }

    /**
     * The approxmiate end time of this phase (as a double in seconds)
     * since Java virtual machine launch.
     * <p>
     * This is a convenience method that returns an approximation of the
     * value returned by {@link #getEndTimeNanos getEndTimeNanos}.
     *
     * @return the end time of this phase.
     */
    default public double getEndTimeSeconds() {
        return (double)getEndTimeNanos() / 1.0e9;
    }

    /**
     * The approximate elapsed time in nanoseconds (wall clock time)
     * between the start and end of this phase.
     *
     * @return the elapsed time of this phase.
     */
    default long getDurationNanos() {
        return getEndTimeNanos() - getStartTimeNanos();
    }

    /**
     * Approximate elapsed time in milliseconds (wall clock time)
     * between the start and end of this phase. The returned value is
     * equal to {@link #getEndTimeMillis getEndTimeMillis}() -
     * {@link #getStartTimeMillis getStartTimeMillis}(), which is not
     * the same as {@link #getDurationNanos getDurationNanos}() /
     * 1000_000L.
     *
     * @return the elapsed time of this phase.
     */
    default long getDurationMillis() {
        return getEndTimeMillis() - getStartTimeMillis();
    }

    /**
     * Approximate elapsed time (in seconds as a double, wall clock
     * time) between the start and end of this phase. The returned
     * value is equal to
     * {@link #getEndTimeSeconds getEndTimeSeconds}() -
     * {@link #getStartTimeSeconds getStartTimeSeconds}(), which is
     * not the same as
     * (double){@link #getDurationNanos getDurationNanos}().
     *
     * @return the elapsed time of this phase.
     */
    default double getDurationSeconds() {
        return getEndTimeSeconds() - getStartTimeSeconds();
    }

    /**
     * The number of garbage collection threads used during this phase
     * from the pool of garbage collection threads of the associated
     * collector.
     * 
     * @return the number of garbage collection threads used during
     *         this phase.
     */
    long getPhaseGarbageCollectorThreadCount();
}
