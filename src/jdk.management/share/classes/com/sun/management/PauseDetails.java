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
 * A {@code PauseDetails} object represents a pause event impacting
 * all threads in a running Java virtual machine. It includes a
 * {@link String} cause describing the reason for the pause which is
 * specific to the implementation of the Java virtual machine, and
 * unique across all pause reasons.
 * <p>
 * {@code PauseDetails} also has additional, optional, details of the time
 * required for individual steps executed during the pause. The times for
 * the individual steps make it easy to identify potential areas for
 * reducing the total pause duration. Generally, the sum of the
 * additional detailed timing information's times should add up to the
 * total pause duration.
 *
 * @author Paul Hohensee
 * @since  18
 */
public class PauseDetails extends PhaseDetails /*implements CompositeView*/ {

    public PauseDetails() {
	super();
    }

    /**
     * The reason that for this pause. Reasons are unique across the Java
     * virtual machine.
     *
     * @return a{@code String} representing the reason for this pause.
     */
    public String getPauseCause() {
        return null;
    }

    /**
     * Approximate time in nanoseconds required to obtain the threads
     * lock at the beginning of this pause's processing.
     *
     * @return the time required to obtain the threads lock
     */
    //    public long getInitialThreadsLockAcquireTimeNanos() {
    // 	return 0;
    //    }

    /**
     * Approximate time in milliseconds required to obtain the threads
     * lock at the beginning of this pause's processing.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getInitialThreadsLockAcquireTimeNanos getInitialThreadsLockAcquireTimeNanos}.
     *
     * @return the time required to obtain the threads lock
     */
    //    public long getInitialThreadsLockAcquireTimeMillis() {
    // 	return getInitialThreadsLockAcquireTimeNanos() / 1000_000L;
    //    }

    /**
     * Approximate time (in seconds as a double) required to obtain
     * the threads lock at the beginning of this pause's processing.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getInitialThreadsLockAcquireTimeNanos getInitialThreadsLockAcquireTimeNanos}.
     *
     * @return the time required to obtain the threads lock
     */
    //    public double getInitialThreadsLockAcquireTimeSeconds() {
    // 	return (double)getInitialThreadsLockAcquireTimeNanos() / 1.0e9;
    //    }

   /**
     * Approximate time in nanoseconds required to suspend any
     * running collector during preparation to suspend the process's
     * threads.
     *
     * @return the time required to suspend any running collector.
     */
    //    public long getCollectorSuspendTimeNanos() {
    //        return 0;
    //    }

    /**
     * Approximate time in milliseconds required to suspend any
     * running collector during preparation to suspend the process's
     * threads.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getCollectorSuspendTimeNanos getCollectorSuspendTimeNanos}.
     *
     * @return the time required to suspend any running collector.
     */
    //    public long getCollectorSuspendTimeMillis() {
    //        return getCollectorSuspendTimeNanos() / 1000_000L;
    //    }

    /**
     * Approximate time required (in seconds as a double) to suspend
     * any running collector during preparation to suspend the process's
     * threads.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getCollectorSuspendTimeNanos getCollectorSuspendTimeNanos}.
     *
     * @return the time required to suspend any running collector.
     */
    //    public double getCollectorSuspendTimeSeconds() {
    //        return (double)getCollectorSuspendTimeNanos() / 1.0e9;
    //    }

    /**
     * Approximate time in nanoseconds required to notify all running
     * threads to suspend themselves.
     *
     * @return the time required to notify running threads to
     *         suspend themselves.
     */
    //    public long getThreadsNotifyTimeNanos() {
    //        return 0;
    //    }

    /**
     * Approximate time in milliseconds required to notify all running
     * threads to suspend themselves.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getThreadsNotifyTimeNanos getThreadsNotifyTimeNanos}.
     *
     * @return the time required to notify running threads to
     *         suspend themselves.
     */
    //    public long getThreadsNotifyTimeMillis() {
    //        return getThreadsNotifyTimeNanos() / 1000_000L;
    //    }

    /**
     * Approximate time (in seconds as a double) required to notify all
     * running threads to suspend themselves.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getThreadsNotifyTimeNanos getThreadsNotifyTimeNanos}.
     *
     * @return the time required to notify running threads to
     *         suspend themselves.
     */
    //    public double getThreadsNotifyTimeSeconds() {
    //        return (double)getThreadsNotifyTimeNanos() / 1.0e9;
    //    }

    /**
     * Approximate time in nanoseconds spent waiting for all threads
     * to suspend. The time returned from this method is commonly
     * known as Time To Safepoint. 
     *
     * @return the time spent waiting for all threads to suspend
     */
    public long getThreadsSuspensionTimeNanos() {
        return 0;
    }

    /**
     * Approximate time in milliseconds spent waiting for all threads
     * to suspend. The time returned from this method is commonly
     * known as Time To Safepoint. 
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getThreadsSuspensionTimeNanos getThreadsSuspensionTimeNanos}.
     *
     * @return the time spent waiting for all threads to suspend
     */
    public long getThreadsSuspensionTimeMillis() {
        return getThreadsSuspensionTimeNanos() / 1000_000L;
    }

    /**
     * Approximate time (in seconds as a double) spent waiting for all
     * threads to suspend. The time returned from this method is commonly
     * known as the Time To Safepoint. 
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getThreadsSuspensionTimeNanos getThreadsSuspensionTimeNanos}.
     *
     * @return the time spent waiting for all threads to suspend
     */
    public double getThreadsSuspensionTimeSeconds() {
        return (double)getThreadsSuspensionTimeNanos() / 1.0e9;
    }

    /**
     * Approximate time in nanoseconds required to perform the operation
     * for which suspension of all threads is required. The interval
     * begins when all threads have been suspended and ends when the
     * operation has been completed.
     *
     * @return the time spent on the operation associated with the pause.
     */
    public long getOperationTimeNanos() {
 	return 0;
    }

    /**
     * Approximate time in milliseconds required to perform the operation
     * for which suspension of all threads is required. The interval
     * begins when all threads have been suspended and ends when the
     * operation has been completed.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getOperationTimeNanos getOperationTimeNanos}.
     *
     * @return the time spent on the operation associated with the pause.
     */
    public long getOperationTimeMillis() {
 	return getOperationTimeNanos() / 1000_000L;
    }

    /**
     * Approximate time (in seconds as a double) required to perform the
     * peration for which suspension of all threads is required. The
     * interval begins when all threads have been suspended and ends
     * when the operation has been completed.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getOperationTimeNanos getOperationTimeNanos}.
     *
     * @return the time spent on the operation associated with the pause.
     */
    public double getOperationTimeSeconds() {
 	return (double)getOperationTimeNanos() / 1.0e9;
    }

    /**
     * Approximate time in nanoseconds required for cleanup activities
     * just prior to resumption of application thread execution.
     *
     * @return the time required for cleanup activities.
     */
    //    public long getPostOperationCleanupTimeNanos() {
    // 	return 0;
    //    }

    /**
     * Approximate required in milliseconds required for cleanup activities
     * just prior to resumption of application thread execution.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getPostOperationCleanupTimeNanos getPostOperationCleanupTimeNanos}.
     *
     * @return the time required for cleanup activities.
     */
    //    public long getPostOperationCleanupTimeMillis() {
    // 	return getPostOperationCleanupTimeNanos() / 1000_000;
    //    }

    /**
     * Approximate time (in seconds as a double) required for cleanup
     * activities just prior to resumption of application thread execution.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getPostOperationCleanupTimeNanos getPostOperationCleanupTimeNanos}.
     *
     * @return the time required for cleanup activities.
     */
    //    public double getPostOperationCleanupTimeSeconds() {
    // 	return (double)getPostOperationCleanupTimeNanos() / 1.0e9;
    //    }

    /**
     * Approximate time in nanoseconds required for all application
     * threads to resume execution after the notification of each
     * thread to wake up and resume execution.
     *
     * @return the time required for all threads to resume execution.
     */
    //    public long getWakeupThreadsTimeNanos() {
    // 	return 0;
    //    }

    /**
     * Approximate time in milliseconds required for all application
     * threads to resume execution after the notification of each
     * thread to wake up and resume execution.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getWakeupThreadsTimeNanos getWakeupThreadsTimeNanos}.
     *
     * @return the time required for all threads to resume execution.
     */
    //    public long getWakeupThreadsTimeMillis() {
    // 	return getWakeupThreadsTimeNanos() / 1000_000L;
    //    }

    /**
     * Approximate time (in seconds as a double) required for all
     * threads to resume execution after the notification of each
     * thread to wake up and resume execution.
     * <p>
     * This is a convenience method that returns an approximation of
     * the value returned by
     * {@link #getWakeupThreadsTimeNanos getWakeupThreadsTimeNanos}.
     *
     * @return the time required for all threads to resume execution
     */
    //    public double getWakeupThreadsTimeSeconds() {
    // 	return (double)getWakeupThreadsTimeNanos() / 1.0e9;
    //    }
}
