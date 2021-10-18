/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.management.internal;

import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Map;
import java.io.InvalidObjectException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.OpenDataException;
import com.sun.management.PauseInfo;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.management.LazyCompositeData;
import static sun.management.LazyCompositeData.getLong;
import static sun.management.LazyCompositeData.getString;
import sun.management.MappedMXBeanType;
import sun.management.Util;

/**
 * A CompositeData for PauseInfo for the local management support.
 * This class avoids the performance penalty paid to the
 * construction of a CompositeData use in the local case.
 */
public class PauseInfoCompositeData extends LazyCompositeData implements java.io.Serializable {
    private final PauseInfo pauseInfo;

    public PauseInfoCompositeData(PauseInfo pauseInfo) {
        this.pauseInfo = pauseInfo;
    }

    public PauseInfo getPauseInfo() {
        return this.pauseInfo;
    }


    public static CompositeData toCompositeData(final PauseInfo info) {
        PauseInfoCompositeData picd =
            new PauseInfoCompositeData(info);
        return picd.getCompositeData();
    }

    protected CompositeData getCompositeData() {
        // CONTENTS OF THIS ARRAY MUST BE SYNCHRONIZED WITH
        // basePauseInfoItemNames!
        final Object[] pauseInfoItemValues;

        pauseInfoItemValues = new Object[] {
            pauseInfo.getId(),
            pauseInfo.getStartTimeNanos(),
            pauseInfo.getStartTimeMillis(),
            pauseInfo.getStartTimeSeconds(),
            pauseInfo.getEndTimeNanos(),
            pauseInfo.getEndTimeMillis(),
            pauseInfo.getEndTimeSeconds(),
            pauseInfo.getDurationNanos(),
            pauseInfo.getDurationMillis(),
            pauseInfo.getDurationSeconds(),
            pauseInfo.getPauseCause(),
            pauseInfo.getThreadsSuspensionTimeNanos(),
            pauseInfo.getThreadsSuspensionTimeMillis(),
            pauseInfo.getThreadsSuspensionTimeSeconds(),
            pauseInfo.getOperationTimeNanos(),
            pauseInfo.getOperationTimeMillis(),
            pauseInfo.getOperationTimeSeconds(),
            pauseInfo.getPostOperationCleanupTimeNanos(),
            pauseInfo.getPostOperationCleanupTimeMillis(),
            pauseInfo.getPostOperationCleanupTimeSeconds(),
            pauseInfo.getPhaseGarbageCollectorThreadCount()
        };

        try {
            return new CompositeDataSupport(pauseInfoCompositeType,
                                            PAUSE_INFO_ITEM_NAMES,
                                            pauseInfoItemValues);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    private static final String ID                     = "id";
    private static final String START_TIME_NANOS        = "startTimeNanos";
    private static final String START_TIME_MILLIS       = "startTimeMillis";
    private static final String START_TIME_SECONDS      = "startTimeSeconds";
    private static final String END_TIME_NANOS          = "endTimeNanos";
    private static final String END_TIME_MILLIS         = "endTimeMillis";
    private static final String END_TIME_SECONDS        = "endTimeSeconds";
    private static final String DURATION_NANOS          = "durationNanos";
    private static final String DURATION_MILLIS         = "durationMillis";
    private static final String DURATION_SECONDS        = "durationSeconds";

    private static final String PAUSE_CAUSE                     = "pauseCause";
    private static final String THREADS_NOTIFY_TIME_NANOS       = "threadsNotifyTimeNanos";
    private static final String THREADS_NOTIFY_TIME_MILLIS      = "threadsNotifyTimeMillis";
    private static final String THREADS_NOTIFY_TIME_SECONDS     = "threadsNotifyTimeSeconds";
    private static final String THREADS_SUSPENSION_TIME_NANOS   = "threadsSuspensionTimeNanos";
    private static final String THREADS_SUSPENSION_TIME_MILLIS  = "threadsSuspensionTimeMillis";
    private static final String THREADS_SUSPENSION_TIME_SECONDS = "threadsSuspensionTimeSeconds";
    private static final String OPERATION_TIME_NANOS            = "operationTimeNanos";
    private static final String OPERATION_TIME_MILLIS           = "operationTimeMillis";
    private static final String OPERATION_TIME_SECONDS          = "operationTimeSeconds";
    private static final String CLEANUP_TIME_NANOS              = "postOperationCleanupTimeNanos";
    private static final String CLEANUP_TIME_MILLIS             = "postOperationCleanupTimeMillis";
    private static final String CLEANUP_TIME_SECONDS            = "postOperationCleanupTimeSeconds";
    private static final String THREAD_COUNT                    = "phaseGarbageCollectorThreadCount";

    private static final String[] PAUSE_INFO_ITEM_NAMES = {
        ID,
        START_TIME_NANOS,
        START_TIME_MILLIS,
        START_TIME_SECONDS,
        END_TIME_NANOS,
        END_TIME_MILLIS,
        END_TIME_SECONDS,
        DURATION_NANOS,
        DURATION_MILLIS,
        DURATION_SECONDS,
        
        PAUSE_CAUSE,
        THREADS_SUSPENSION_TIME_NANOS,
        THREADS_SUSPENSION_TIME_MILLIS,
        THREADS_SUSPENSION_TIME_SECONDS,
        OPERATION_TIME_NANOS,
        OPERATION_TIME_MILLIS,
        OPERATION_TIME_SECONDS,
        CLEANUP_TIME_NANOS,
        CLEANUP_TIME_MILLIS,
        CLEANUP_TIME_SECONDS,
        THREAD_COUNT        
    };

    private static final OpenType<?>[] PAUSE_INFO_ITEM_TYPES = {
        SimpleType.LONG,    // ID,
        SimpleType.LONG,    // START_TIME_NANOS,
        SimpleType.LONG,    // START_TIME_MILLIS,
        SimpleType.DOUBLE,  // START_TIME_SECONDS,
        SimpleType.LONG,    // END_TIME_NANOS,
        SimpleType.LONG,    // END_TIME_MILLIS,
        SimpleType.DOUBLE,  // END_TIME_SECONDS,
        SimpleType.LONG,    // DURATION_NANOS,
        SimpleType.LONG,    // DURATION_MILLIS,
        SimpleType.DOUBLE,  // DURATION_SECONDS,
        
        SimpleType.STRING,  // PAUSE_CAUSE,
        SimpleType.LONG,    // THREADS_SUSPENSION_TIME_NANOS,
        SimpleType.LONG,    // THREADS_SUSPENSION_TIME_MILLIS,
        SimpleType.DOUBLE,  // THREADS_SUSPENSION_TIME_SECONDS,
        SimpleType.LONG,    // OPERATION_TIME_NANOS,
        SimpleType.LONG,    // OPERATION_TIME_MILLIS,
        SimpleType.DOUBLE,  // OPERATION_SECONDS,
        SimpleType.LONG,    // CLEANUP_TIME_NANOS,
        SimpleType.LONG,    // CLEANUP_TIME_MILLIS,
        SimpleType.DOUBLE,  // CLEANUP_TIME_SECONDS     
        SimpleType.LONG     // THREAD_COUNT
    };

    public static long getId(CompositeData cd) {
        return getLong(cd, ID);
    }

    public static long getStartTimeNanos(CompositeData cd) {
        return getLong(cd, START_TIME_NANOS);
    }

    public static long getEndTimeNanos(CompositeData cd) {
        return getLong(cd, END_TIME_NANOS);
    }

    public static String getPauseCause(CompositeData cd) {
        return getString(cd, PAUSE_CAUSE);
    }

    public static long getThreadsSuspensionTimeNanos(CompositeData cd) {
        return getLong(cd, THREADS_SUSPENSION_TIME_NANOS);
    }

    public static long getOperationTimeNanos(CompositeData cd) {
        return getLong(cd, OPERATION_TIME_NANOS);
    }

    public static long getCleanupTimeNanos(CompositeData cd) {
        return getLong(cd, CLEANUP_TIME_NANOS);
    }

    public static long getThreadCount(CompositeData cd) {
        return getLong(cd, THREAD_COUNT);
    }

    private static final CompositeType pauseInfoCompositeType;
    static {
        try {
            pauseInfoCompositeType = new CompositeType("sun.management.PauseInfoCompositeType",
            "CompositeType for PauseInfo ",
            PAUSE_INFO_ITEM_NAMES,
            PAUSE_INFO_ITEM_NAMES,
            PAUSE_INFO_ITEM_TYPES);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }
    
    /**
     * Returns true if the input CompositeData has the expected
     * CompositeType (i.e. contain all attributes with expected
     * names and types).  Otherwise, return false.
     */
    public static void validateCompositeData(CompositeData cd) {
        if (cd == null) {
            throw new NullPointerException("Null CompositeData");
        }

        if (!isTypeMatched(getPauseInfoCompositeType(),
                           cd.getCompositeType())) {
           throw new IllegalArgumentException(
                "Unexpected composite type for PauseInfo");
        }
    }
    
    public static CompositeType getPauseInfoCompositeType() {
        return pauseInfoCompositeType;
    }

    private static final long serialVersionUID = -375579312992308331L;
}