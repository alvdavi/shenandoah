/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Platform-specific management interface for a garbage collector
 * which performs collections in cycles.
 * <p>
 * GarbageCollectorMXBean is an interface used by the management system to
 * access garbage collector properties and reset the values of associated
 * counter and accumulator properties. 
 *
 * <p> This platform extension is only available to the garbage
 * collection implementation that supports this extension.
 *
 * @author  Mandy Chung, Paul Hohensee, David Alvarez
 * @since   1.5, 18
 */
public interface GarbageCollectorMXBean
    extends java.lang.management.GarbageCollectorMXBean, MemoryManagerMXBean {

    /**
     * Returns a {@link GcInfo} object that contains detailed information
     * about the most recently completed garbage collection.
     * <p>
     * Collector specific attributes, if any, can be obtained
     * via the {@link CompositeData CompositeData} interface.
     * <p>
     * <b>MBeanServer access:</b>
     * The mapped type of {@code GcInfo} is {@code CompositeData}
     * with attributes specified in {@link GcInfo#from GcInfo}.
     *
     * @return a {@code GcInfo} object representing the most recent GC
     *         information; or {@code null} if no GC information is
     *         available.
     */
    public GcInfo getLastGcInfo();

    /**
     * Returns a {@link GcDetails} object that contains detailed
     * information about the most recently completed collection by
     * this collector.
     * <p>
     * Collector specific attributes, if any, can be obtained
     * via the {@link CompositeData CompositeData} interface.
     * <p>
     * <b>MBeanServer access:</b>
     * The mapped type of {@code GcDetails} is {@code CompositeData}
     * with attributes specified in {@link GcDetails#from GcDetails}.
     *
     * @return a {@code GcDetails} object representing the most recent GC
     *         information; or {@code null} if no GC information is
     *         available.
     */
    public GcDetails getLastGcDetails();

    /**
     * The total elapsed wall clock time in nanoseconds spent in
     * collections completed by this collector since Java virtual
     * machine launch. The value is updated after the first and
     * each subsequent collection completes, and includes wall clock
     * time spent running both concurrently and during required
     * pauses. This method returns {@code -1} if elapsed running
     * time is undefined for this collector.
     * <p>
     * This method is a convenience method whose value is equal to the
     * the accumulated wall clock running time required by this
     * collector between all calls to
     * {@link #resetCumulativeStats resetCumulativeStats}, plus those
     * that have occurred since the last such call. See
     * {@link #getCumulativeRunningTime getCumulativeRunningTime}.
     *
     * @return the  wall clock time spent in completed collections;
     *         or {@code -1} if running time is undefined.
     */
    public long getRunningTimeNanos();

    /**
     * The approximate total wall clock time in milliseconds
     * spent in collections completed by this collector since Java
     * virtual machine launch. The value is updated after the first
     * and each subsequent collection completes, and includes wall 
     * clock time spent running both concurrently and during required
     * pauses. This method returns {@code -1} if elapsed running
     * time is undefined for this collector.
     * <p>
     * This method is a convenience method that returns an
     * approximation of the value returned by
     * {@link #getRunningTimeNanos getRunningTimeNanos}.
     * If a collection is very short, this method may return the
     * same value even if the collection count returned by
     * {@link getCollectionCount} has been incremented.
     *
     * @return the wall clock time spent in completed collections;
     *         or {@code -1} if running time is undefined.
     */
    public default long getRunningTimeMillis() {
        return getRunningTimeNanos() / 1000_000L;
    }

    /**
     * The approximate total wall clock time (in seconds as a double)
     * spent in collections completed by this collector since Java
     * virtual machine launch. The value is updated after the first
     * and each subsequent collection completes, and includes wall 
     * clock time spent running both concurrently and during required
     * pauses. This method returns {@code -1.0} if elapsed running
     * time is undefined for this collector.
     * <p>
     * This method is a convenience method that returns an
     * approximation of the value returned by
     * {@link #getRunningTimeNanos getRunningTimeNanos}.
     *
     * @return the wall clock time spent in completed collections;
     *         or {@code -1.0} if running time is undefined.
     */
    public default double getRunningTimeSeconds() {
        return (double)getRunningTimeNanos() / 1.0e9;
    }

    /**
     * The total number of pauses required by this collector during
     * collections completed by this collector since Java virtual
     * machine launch. The value is updated after the first and each
     * subsequent collection completes. This method returns {@code -1}
     * if the pause count is undefined for this collector.
     *
     * @return the total number of pauses required by this collector
     *         during collections run by this collector, or
     *         {@code -1} if the pause count is undefined.
     */
    public long getPauseCount();

    /**
     * The total elapsed wall clock time in nanoseconds spent in all
     * types of pauses required by this collector during collections
     * completed by this collector since Java virtual machine launch.
     * The value is updated after the first and each subsequent collection
     * completes. This method returns {@code -1} if elapsed pause time
     * is undefined for this collector.
     * <p>
     * This method may return a value more precise than returned by,
     * and is usually functionally equivalent to, the imprecisely
     * specified {@link getCollectionTime}.
     * <p>
     * This method is a convenience method whose value is equal to the
     * the accumulated durations of all pauses required by this collector
     * that have occurred between all calls to
     * {@link #resetCumulativeStats resetCumulativeStats}, plus those
     * that have occurred since the last such call. See
     * {@link #getCumulativePauseTime getCumulativePauseTime}.
     *
     * @return the elapsed pause time in nanoseconds; or {@code -1} if
     *         pause time is undefined.
     */
    public long getPauseTimeNanos();

    /**
     * The approximate total elapsed wall clock time in milliseconds
     * spent in all types of pauses required by this collector during
     * collections completed by this collector since Java virtual machine
     * launch. The value is updated after the first and each subsequent
     * collection completes. This method returns {@code -1} if elapsed
     * pause time is undefined for this collector.
     * <p>
     * This method is a convenience method that returns an approximation
     * of the value returned by {@link #getPauseTimeNanos getPauseTimeNanos}.
     * If a pause is very short, this method may return the same value
     * even if the pause count returned by
     * {@link #getPauseCount getPauseCount} has been incremented. It is
     * is usually functionally equivalent to the imprecisely specified
     * {@link getCollectionTime}.
     *
     * @return the elapsed pause time in milliseconds; or {@code -1} if
     *         pause time is undefined.
     */
    public default long getPauseTimeMillis() {
        return getPauseTimeNanos() / 1000_000L;
    }

    /**
     * The approximate total wall clock time (in seconds as a double)
     * spent in all types of pauses required by this collector during
     * collections completed by this collector since Java virtual
     * machine launch. The value is updated after the first and each
     * subsequent collection completes. This method returns
     * {@code -1.0} if elapsed pause time is undefined for this
     * collector.
     * <p>
     * This method is a convenience method that returns an approximation
     * of the value returned by
     * {@link #getPauseTimeNanos getPauseTimeNanos}.
     *
     * @return the approximate total wall clock time spent in pauses
     *         by this collector; or {@code -1.0} if pause time
     *         is undefined.
     */
    public default double getPauseTimeSeconds() {
        return (double)getPauseTimeNanos() / 1.0e9;
    }

    /**
     * The number of collections completed by this collector since
     * the last reset by {@link #resetCumulativeStats resetCumulativeStats}.
     * This method returns {@code -1} if cumulative statistics are
     * not supported.
     *
     * @return this cumulative number of completed collections; or
     *         {@code -1} if cumulative statistics are not supported.
     */
    public long getCumulativeCollectionCount();

    /**
     * The approximate cumulative wall clock time in nanoseconds spent
     * in collections completed by this collector since the last reset by
     * {@link #resetCumulativeStats resetCumulativeStats}. The value is
     * updated after the first and each subsequent collection completes,
     * and includes wall clock time spent running both concurrently
     * and during required pauses. This method returns {@code -1} if
     * cumulative statistics are not supported.
     *
     * @return the cumulative wall clock time spent in completed
     *         collections; or {@code -1} if cumulative statistics
     *         are not supported.
     */
    public long getCumulativeRunningTimeNanos();

    /**
     * The approximate cumulative wall clock time in milliseconds spent
     * in collections completed by this collector since the last reset by
     * {@link #resetCumulativeStats resetCumulativeStats}. The value is
     * updated after the first and each subsequent collection completes,
     * and includes wall clock time spent running both concurrently
     * and during required pauses. This method returns {@code -1} if
     * cumulative statistics are not supported.
     * <p>
     * This method is a convenience method that returns an approximation
     * of the value returned by
     * {@link #getCumulativeRunningTimeNanos getCumulativeRunningTimeNanos}.
     * If a pause is very short, this method may return the same value
     * even if the collection count returned by
     * {@link #getCumulativeCollectionCount getCumulativeCollectionCount}
     * has been incremented.
     *
     * @return the cumulative wall clock time spent in completed
     *         collections; or {@code -1} if cumulative statistics
     *         are not supported.
     */
    public default long getCumulativeRunningTimeMillis() {
        return getCumulativeRunningTimeNanos() / 1000_000L;
    }

    /**
     * The approximate cumulative wall clock time (in seconds as a double)
     * spent in collections completed by this collector since the last
     * reset by {@link #resetCumulativeStats resetCumulativeStats}.
     * The value is updated after the first and each subsequent collection
     * completes, and includes wall clock time spent running both
     * concurrently and during pauses. This method returns {@code -1.0} if
     * cumulative statistics are not supported.
     * <p>
     * This method is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getCumulativeRunningTimeNanos getCumulativeRunningTimeNanos}.
     *
     * @return the cumulative wall clock time spent in completed
     *         collections; or {@code -1.0} if cumulative statistics
     *         are not supported.
     */
    public default double getCumulativeRunningTimeSeconds() {
        return (double)getCumulativeRunningTimeNanos() / 1.0e9;
    }

    /**
     * The number of pauses of all types required by this collector during
     * its completed collections since the last reset by
     * {@link #resetCumulativeStats resetCumulativeStats}. The value is
     * updated after the first and each subsequent collection completes.
     * This method returns {@code -1} if cumulative statistics are not
     * supported.
     *
     * @return the cumulative number of required pauses during
     *         completed collections; or {@code -1} if cumulative
     *         statistics are not supported.
     */
    public long getCumulativePauseCount();

    /**
     * The approximate cumulative wall clock time in nanoseconds spent
     * in all types of pauses required by this collector during
     * collections completed by this collector since the last reset by
     * {@link #resetCumulativeStats resetCumulativeStats}. The value is
     * updated after the first and each subsequent collection completes,
     * and is always less than or equal to the value returned by
     * {@link #getCumulativeRunningTimeNanos getCumulativeRunningTimeNanos}.
     * This method returns {@code -1} if cumulative statistics are not
     * supported.
     * <p>
     * If {@link #resetCumulativeStats resetCumulativeStats} has not been
     * called since Java virtual machine launch, this method may return a
     * value more precise than returned by, and is usually functionally
     * equivalent to, the imprecisely specified {@link getCollectionTime}.
     *
     * @return the cumulative wall clock time spent in required pauses
     *         during completed collections; or {@code -1} if cumulative
     *         statistics are not supported.
     */
    public long getCumulativePauseTimeNanos();

    /**
     * The approximate cumulative wall clock time in milliseconds spent
     * in all types of pauses required by this collector during
     * collections completed by this collector since the last reset by
     * {@link #resetCumulativeStats resetCumulativeStats}. The value is
     * updated after the first and each subsequent collection completes,
     * and is always less than or equal to the value returned by
     * {@link #getCumulativeRunningTimeMillis getCumulativeRunningTimeMillis}.
     * This method returns {@code -1} if cumulative statistics are not
     * supported.
     * <p>
     * This method is a convenience method that returns an approximation
     * of the value returned by
     * {@link #getCumulativePauseTimeNanos getCumulativePauseTimeNanos}.
     * If a pause is very short, this method may return the same value
     * even if the pause count returned by
     * {@link #getCumulativePauseCount getCumulativePauseCount} has been
     * incremented. If {@link #resetCumulativeStats resetCumulativeStats}
     * has not been called since Java virtual machine launch, this method
     * is usually functionally equivalent to the imprecisely specified
     * {@link getCollectionTime}.
     *
     * @return the cumulative wall clock time spent in required pauses
     *         during completed collections; or {@code -1} if cumulative
     *         statistics are not supported.
     */
    public default long getCumulativePauseTimeMillis() {
        return getCumulativePauseTimeNanos() / 1000_000L;
    }

    /**
     * The approximate cumulative wall clock time (in seconds as a double)
     * spent in all types of pauses required by this collector during
     * collections completed by this collector since the last reset by
     * {@link #resetCumulativeStats resetCumulativeStats}. The value is
     * updated after the first and each subsequent collection completes,
     * and is always less than or equal to the value returned by
     * {@link #getCumulativeRunningTimeSeconds getCumulativeRunningTimeSeconds}.
     * This method returns {@code -1.0} if cumulative statistics are not
     * supported.
     * <p>
     * This method is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getCumulativePauseTimeSeconds getCumulativePauseTimeSeconds}.
     *
     * @return the cumulative wall clock time spent in required pauses
     *         during completed collections; or {@code -1.0} if cumulative
     *         statistics are not supported.
     */
    public default double getCumulativePauseTimeSeconds() {
        return (double)getCumulativePauseTimeNanos() / 1.0e9;
    }

    /**
     * Reset to zero the cumulative values returned by
     * {@link #getCumulativeCollectionCount getCumulativeCollectionCount},
     * {@link #getCumulativeRunningTimeNanos getCumulativeRunningTimeNanos},
     * {@link #getCumulativeRunningTimeMillis getCumulativeRunningTimeMillis},
     * {@link #getCumulativeRunningTimeSeconds getCumulativeRunningTimeSeconds},
     * {@link #getCumulativePauseCount getCumulativePauseCount},
     * {@link #getCumulativePauseTimeNanos getCumulativePauseTimeNanos},
     * {@link #getCumulativePauseTimeMillis getCumulativePauseTimeMillis}, and
     * {@link #getCumulativePauseTimeSeconds getCumulativePauseTimeSeconds},
     * and return the time when the reset occured in nanoseconds since
     * Java virtual machine launch. This method returns {@code -1} if
     * cumulative statistics are not supported.
     * <p>
     * An implicit call to {@link #resetCumulativeStats resetCumulativeStats}
     * with return value zero occurs when the Java virtual machine starts.
     *
     * @return the time when the reset occured in nanoseconds since Java
     *         virtual machine launch; {@code -1} if cumulative statistics
     *         are not supported.
     *
     * @throws SecurityException if a security manager
     *         exists and the caller does not have
     *         ManagementPermission("control").
     */
    public long resetCumulativeStats();

    /**
     * The time elapsed in nanoseconds between Java virtual machine launch
     * and the most recent reset done by
     * {@link #resetCumulativeStats resetCumulativeStats}. This method
     * returns {@code -1} if cumulative statistics are not supported.
     *
     * This is a convenience method that returns the value returned by
     * the most recent call to {@link #resetCumulativeStats resetCumulativeStats}.
     * <p>
     * An implicit call to {@link #resetCumulativeStats resetCumulativeStats}
     * occurs when the Java virtual machine starts, so if the value returned by
     * this method is zero, no user call to
     * {@link #resetCumulativeStats resetCumulativeStats} has yet occurred.
     *
     * @return the elapsed time between Java virtual machine launch and the
     *         most recent reset; or {@code -1} if cumulative statistics
     *         are not supported.
     */
    public long getLastResetCumulativeStatsTime();

    /**
     * The number of garbage collection threads in the pool of garbage collection
     * threads for this manager.
     * 
     * @return the number of garbage collection threads in the pool of garbage
     * collection threads for this manager.
     */
    public long getGarbageCollectorThreadCount();
}
