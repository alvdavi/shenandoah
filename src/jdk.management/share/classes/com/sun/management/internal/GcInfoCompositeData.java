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
import com.sun.management.GcInfo;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.management.LazyCompositeData;
import static sun.management.LazyCompositeData.getLong;
import static sun.management.LazyCompositeData.getString;
import sun.management.MappedMXBeanType;
import sun.management.Util;

/**
 * A CompositeData for GcInfo for the local management support.
 * This class avoids the performance penalty paid to the
 * construction of a CompositeData use in the local case.
 */
public class GcInfoCompositeData extends LazyCompositeData {
    private final GcInfo info;
    private final GcInfoBuilder builder;
    private final Object[] gcExtItemValues;

    public GcInfoCompositeData(GcInfo info,
                        GcInfoBuilder builder,
                        Object[] gcExtItemValues) {
        this.info = info;
        this.builder = builder;
        this.gcExtItemValues = gcExtItemValues;
    }

    public GcInfo getGcInfo() {
        return info;
    }

    public static CompositeData toCompositeData(final GcInfo info) {
        @SuppressWarnings("removal")
        final GcInfoBuilder builder = AccessController.doPrivileged (new PrivilegedAction<GcInfoBuilder>() {
                        public GcInfoBuilder run() {
                            try {
                                Class<?> cl = Class.forName("com.sun.management.GcInfo");
                                Field f = cl.getDeclaredField("builder");
                                f.setAccessible(true);
                                return (GcInfoBuilder)f.get(info);
                            } catch(ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                                return null;
                            }
                        }
                    });
        @SuppressWarnings("removal")
        final Object[] extAttr = AccessController.doPrivileged (new PrivilegedAction<Object[]>() {
                        public Object[] run() {
                            try {
                                Class<?> cl = Class.forName("com.sun.management.GcInfo");
                                Field f = cl.getDeclaredField("extAttributes");
                                f.setAccessible(true);
                                return (Object[])f.get(info);
                            } catch(ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                                return null;
                            }
                        }
                    });
        GcInfoCompositeData gcicd =
            new GcInfoCompositeData(info,builder,extAttr);
        return gcicd.getCompositeData();
    }

    protected CompositeData getCompositeData() {
        // CONTENTS OF THIS ARRAY MUST BE SYNCHRONIZED WITH
        // baseGcInfoItemNames!
        final Object[] baseGcInfoItemValues;

        try {
            baseGcInfoItemValues = new Object[] {
                info.getId(),
                info.getStartTimeMillis(),
                info.getEndTimeMillis(),
                info.getDurationMillis(),
                memoryUsageMapType.toOpenTypeData(info.getMemoryUsageBeforeGc()),
                memoryUsageMapType.toOpenTypeData(info.getMemoryUsageAfterGc()),
                info.getStartTimeNanos(),
                info.getStartTimeMillis(),
                info.getStartTimeSeconds(),
                info.getEndTimeNanos(),
                info.getEndTimeMillis(),
                info.getEndTimeSeconds(),
                info.getDurationNanos(),
                info.getDurationMillis(),
                info.getDurationSeconds(),
                info.isValid(),
                info.getGarbageCollectionCause(),
                info.getLiveInPoolsBeforeGc(),
                info.getLiveInPoolsAfterGc(),
                info.getGarbageFound(),
                info.getGarbageCollected(),
                info.getCopiedBetweenPools(),
                info.getTimeFromEndOfPreviousToStartNanos(),
                info.getTimeFromEndOfPreviousToStartMillis(),
                info.getTimeFromEndOfPreviousToStartSeconds(),
                info.getPercentageOfTimeCollectorWasRunning(),
                info.getAllocRateDuringCollection(),
                info.getAllocRateBetweenEndOfPreviousAndStart(),
                info.getPreviousEndTimeNanos(),
                info.getPreviousEndTimeMillis(),
                info.getPreviousEndTimeSeconds(),
                info.getAllocatedDuringCollection(),
                info.getAllocatedBetweenEndOfPreviousAndStart()
            };
        } catch (OpenDataException e) {
        
            // Should never reach here
            throw new AssertionError(e);
        }

        // Get the item values for the extension attributes
        final int gcExtItemCount = builder.getGcExtItemCount();
        if (gcExtItemCount == 0 &&
            gcExtItemValues != null && gcExtItemValues.length != 0) {
            throw new AssertionError("Unexpected Gc Extension Item Values");
        }

        if (gcExtItemCount > 0 && (gcExtItemValues == null ||
             gcExtItemCount != gcExtItemValues.length)) {
            throw new AssertionError("Unmatched Gc Extension Item Values");
        }

        Object[] values = new Object[baseGcInfoItemValues.length +
                                     gcExtItemCount];
        System.arraycopy(baseGcInfoItemValues, 0, values, 0,
                         baseGcInfoItemValues.length);

        if (gcExtItemCount > 0) {
            System.arraycopy(gcExtItemValues, 0, values,
                             baseGcInfoItemValues.length, gcExtItemCount);
        }

        try {
            return new CompositeDataSupport(builder.getGcInfoCompositeType(),
                                            builder.getItemNames(),
                                            values);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    private static final String ID                     = "id";
    private static final String START_TIME             = "startTime";
    private static final String END_TIME               = "endTime";
    private static final String DURATION               = "duration";
    private static final String MEMORY_USAGE_BEFORE_GC = "memoryUsageBeforeGc";
    private static final String MEMORY_USAGE_AFTER_GC  = "memoryUsageAfterGc";

    private static final String START_TIME_NANOS        = "startTimeNanos";
    private static final String START_TIME_MILLIS       = "startTimeMillis";
    private static final String START_TIME_SECONDS      = "startTimeSeconds";
    private static final String END_TIME_NANOS          = "endTimeNanos";
    private static final String END_TIME_MILLIS         = "endTimeMillis";
    private static final String END_TIME_SECONDS        = "endTimeSeconds";
    private static final String DURATION_NANOS          = "durationNanos";
    private static final String DURATION_MILLIS         = "durationMillis";
    private static final String DURATION_SECONDS        = "durationSeconds";

    private static final String VALID                   = "valid";
    private static final String GC_CAUSE                = "gcCause";
    private static final String LIVE_IN_POOLS_BEFORE_GC = "liveInPoolsBeforeGc";
    private static final String LIVE_IN_POOLS_AFTER_GC  = "liveInPoolsAfterGc";
    private static final String GARBAGE_FOUND           = "garbageFound";
    private static final String GARBAGE_COLLECTED       = "garbageCollected";
    private static final String COPIED_BETWEEN_POOLS    = "copiedBetweenPools";

    private static final String TIME_FROM_END_OF_PREVIOUS_TO_START_NANOS     = "timeFromEndOfPreviousToStartNanos";
    private static final String TIME_FROM_END_OF_PREVIOUS_TO_START_MILLIS    = "timeFromEndOfPreviousToStartMillis";
    private static final String TIME_FROM_END_OF_PREVIOUS_TO_START_SECONDS   = "timeFromEndOfPreviousToStartSeconds";
    private static final String PERCENTAGE_OF_TIME_COLLECTOR_IS_RUNNING      = "percentageOfTimeCollectorIsRunning";
    private static final String ALLOC_RATE_DURING_COLLECTION                 = "allocRateDuringCollection";
    private static final String ALLOC_RATE_BETWEEN_END_OF_PRECIOUS_AND_START = "allocRateBetweenEndOfPreviousAndStart";

    private static final String PREVIOUS_END_TIME_NANOS                      = "previousEndTimeNanos";
    private static final String PREVIOUS_END_TIME_MILLIS                     = "previousEndTimeMillis";
    private static final String PREVIOUS_END_TIME_SECONDS                    = "previousEndTimeSeconds";
    private static final String ALLOCATED_DURING_COLLECTION                  = "allocatedDuringCollection";
    private static final String ALLOCATED_BETWEEN_END_OF_PREVIOUS_AND_START  = "allocatedBetweenEndOfPreviousAndStart";


    private static final String[] baseGcInfoItemNames = {
        ID,
        START_TIME,
        END_TIME,
        DURATION,
        MEMORY_USAGE_BEFORE_GC,
        MEMORY_USAGE_AFTER_GC,

        START_TIME_NANOS,
        START_TIME_MILLIS,
        START_TIME_SECONDS,
        END_TIME_NANOS,
        END_TIME_MILLIS,
        END_TIME_SECONDS,
        DURATION_NANOS,
        DURATION_MILLIS,
        DURATION_SECONDS,
        
        VALID,
        GC_CAUSE,
        LIVE_IN_POOLS_BEFORE_GC,
        LIVE_IN_POOLS_AFTER_GC,
        GARBAGE_FOUND,
        GARBAGE_COLLECTED,
        COPIED_BETWEEN_POOLS,
        TIME_FROM_END_OF_PREVIOUS_TO_START_NANOS,
        TIME_FROM_END_OF_PREVIOUS_TO_START_MILLIS,
        TIME_FROM_END_OF_PREVIOUS_TO_START_SECONDS,
        PERCENTAGE_OF_TIME_COLLECTOR_IS_RUNNING,
        ALLOC_RATE_DURING_COLLECTION,
        ALLOC_RATE_BETWEEN_END_OF_PRECIOUS_AND_START,

        PREVIOUS_END_TIME_NANOS,
        PREVIOUS_END_TIME_MILLIS,
        PREVIOUS_END_TIME_SECONDS,
        ALLOCATED_DURING_COLLECTION,
        ALLOCATED_BETWEEN_END_OF_PREVIOUS_AND_START
        
    };


    private static MappedMXBeanType memoryUsageMapType;
    static {
        try {
            Method m = GcInfo.class.getMethod("getMemoryUsageBeforeGc");
            memoryUsageMapType =
                MappedMXBeanType.getMappedType(m.getGenericReturnType());
        } catch (NoSuchMethodException | OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    static String[] getBaseGcInfoItemNames() {
        return baseGcInfoItemNames;
    }

    private static OpenType<?>[] baseGcInfoItemTypes = null;
    static synchronized OpenType<?>[] getBaseGcInfoItemTypes() {
        if (baseGcInfoItemTypes == null) {
            OpenType<?> memoryUsageOpenType = memoryUsageMapType.getOpenType();
            baseGcInfoItemTypes = new OpenType<?>[] {
                SimpleType.LONG,       // ID
                SimpleType.LONG,       // START_TIME
                SimpleType.LONG,       // END_TIME
                SimpleType.LONG,       // DURATION

                memoryUsageOpenType,   // MEMORY_USAGE_BEFORE_GC
                memoryUsageOpenType,   // MEMORY_USAGE_AFTER_GC

                SimpleType.LONG,       // START_TIME_NANOS
                SimpleType.LONG,       // START_TIME_MILLIS
                SimpleType.DOUBLE,     // START_TIME_SECONDS
                SimpleType.LONG,       // END_TIME_NANOS
                SimpleType.LONG,       // END_TIME_MILLIS
                SimpleType.DOUBLE,     // END_TIME_SECONDS
                SimpleType.LONG,       // DURATION_NANOS
                SimpleType.LONG,       // DURATION_MILLIS
                SimpleType.DOUBLE,     // DURATION_SECONDS
                
                SimpleType.BOOLEAN,    // VALID
                SimpleType.STRING,     // GC_CAUSE
                SimpleType.LONG,       // LIVE_IN_POOLS_BEFORE_GC
                SimpleType.LONG,       // LIVE_IN_POOLS_AFTER_GC
                SimpleType.LONG,       // GARBAGE_FOUND
                SimpleType.LONG,       // GARBAGE_COLLECTED
                SimpleType.LONG,       // COPIED_BETWEEN_POOLS
                SimpleType.LONG,       // TIME_FROM_END_OF_PREVIOUS_TO_START_NANOS
                SimpleType.LONG,       // TIME_FROM_END_OF_PREVIOUS_TO_START_MILLIS
                SimpleType.DOUBLE,     // TIME_FROM_END_OF_PREVIOUS_TO_START_SECONDS
                SimpleType.DOUBLE,     // PERCENTAGE_OF_TIME_COLLECTOR_IS_RUNNING
                SimpleType.DOUBLE,     // ALLOC_RATE_DURING_COLLECTION
                SimpleType.DOUBLE,     // ALLOC_RATE_BETWEEN_END_OF_PREVIOUS_AND_START    

                SimpleType.LONG,       // PREVIOUS_END_TIME_NANOS
                SimpleType.LONG,       // PREVIOUS_END_TIME_MILLIS
                SimpleType.DOUBLE,     // PREVIOUS_END_TIME_SECONDS
                SimpleType.LONG,       // ALLOCATED_DURING_COLLECTION
                SimpleType.LONG,       // ALLOCATED_BETWEEN_END_OF_PREVIOUS_AND_START    
                   
            };
        }
        return baseGcInfoItemTypes;
    }

    public static long getId(CompositeData cd) {
        return getLong(cd, ID);
    }
    public static long getStartTime(CompositeData cd) {
        return getLong(cd, START_TIME);
    }
    public static long getEndTime(CompositeData cd) {
        return getLong(cd, END_TIME);
    }

    public static long getStartTimeNanos(CompositeData cd) {
        return getLong(cd, START_TIME_NANOS);
    }

    public static double getStartTimeSeconds(CompositeData cd) {
        return getDouble(cd, START_TIME_SECONDS);
    }

    public static long getEndTimeNanos(CompositeData cd) {
        return getLong(cd, END_TIME_NANOS);
    }

    public static double getEndTimeSeconds(CompositeData cd) {
        return getDouble(cd, END_TIME_SECONDS);
    }

    public static long getDurationNanos(CompositeData cd) {
        return getLong(cd, DURATION_NANOS);
    }

    public static double getDurationSeconds(CompositeData cd) {
        return getDouble(cd, DURATION_SECONDS);
    }

    public static boolean isValid(CompositeData cd) {
        return getBoolean(cd, VALID);
    }

    public static String getGarbageCollectionCause(CompositeData cd) {
        return getString(cd, GC_CAUSE);
    }

    public static long getLiveInPoolsBeforeGc(CompositeData cd) {
        return getLong(cd, LIVE_IN_POOLS_BEFORE_GC);
    }

    public static long getLiveInPoolsAfterGc(CompositeData cd) {
        return getLong(cd, LIVE_IN_POOLS_AFTER_GC);
    }

    public static long getGarbageFound(CompositeData cd) {
        return getLong(cd, GARBAGE_FOUND);
    }

    public static long getGarbageCollected(CompositeData cd) {
        return getLong(cd, GARBAGE_COLLECTED);
    }

    public static long getCopiedBetweenPools(CompositeData cd) {
        return getLong(cd, COPIED_BETWEEN_POOLS);
    }

    public static long getTimeFromEndOfPreviousToStartNanos(CompositeData cd) {
        return getLong(cd, TIME_FROM_END_OF_PREVIOUS_TO_START_NANOS);
    }

    public static long getTimeFromEndOfPreviousToStartMillis(CompositeData cd) {
        return getLong(cd, TIME_FROM_END_OF_PREVIOUS_TO_START_MILLIS);
    }

    public static double getTimeFromEndOfPreviousToStartSeconds(CompositeData cd) {
        return getDouble(cd, TIME_FROM_END_OF_PREVIOUS_TO_START_SECONDS);
    }

    public static double getPercentageOfTimeCollectorWasRunning(CompositeData cd) {
        return getDouble(cd, PERCENTAGE_OF_TIME_COLLECTOR_IS_RUNNING);
    }

    public static double getAllocRateDuringCollection(CompositeData cd) {
        return getDouble(cd, ALLOC_RATE_DURING_COLLECTION);
    }

    public static double getAllocRateBetweenEndOfPreviousAndStart(CompositeData cd) {
        return getDouble(cd, ALLOC_RATE_BETWEEN_END_OF_PRECIOUS_AND_START);
    }

    public static long getPreviousEndTimeNanos(CompositeData cd) {
        return getLong(cd, PREVIOUS_END_TIME_NANOS);
    }

    public static long getPreviousEndTimeMillis(CompositeData cd) {
        return getLong(cd, PREVIOUS_END_TIME_MILLIS);
    }

    public static double getPreviousEndTimeSeconds(CompositeData cd) {
        return getDouble(cd, PREVIOUS_END_TIME_SECONDS);
    }

    public static long getAllocatedDuringCollection(CompositeData cd) {
        return getLong(cd, ALLOCATED_DURING_COLLECTION);
    }

    public static long getAllocatedBetweenEndOfPreviousAndStart(CompositeData cd) {
        return getLong(cd, ALLOCATED_BETWEEN_END_OF_PREVIOUS_AND_START);
    }


    public static Map<String, MemoryUsage>
            getMemoryUsageBeforeGc(CompositeData cd) {
        try {
            TabularData td = (TabularData) cd.get(MEMORY_USAGE_BEFORE_GC);
            return cast(memoryUsageMapType.toJavaTypeData(td));
        } catch (InvalidObjectException | OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, MemoryUsage> cast(Object x) {
        return (Map<String, MemoryUsage>) x;
    }
    public static Map<String, MemoryUsage>
            getMemoryUsageAfterGc(CompositeData cd) {
        try {
            TabularData td = (TabularData) cd.get(MEMORY_USAGE_AFTER_GC);
            //return (Map<String,MemoryUsage>)
            return cast(memoryUsageMapType.toJavaTypeData(td));
        } catch (InvalidObjectException | OpenDataException e) {
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

        if (!isTypeMatched(getBaseGcInfoCompositeType(),
                           cd.getCompositeType())) {
           throw new IllegalArgumentException(
                "Unexpected composite type for GcInfo");
        }
    }

    // This is only used for validation.
    private static CompositeType baseGcInfoCompositeType = null;
    static synchronized CompositeType getBaseGcInfoCompositeType() {
        if (baseGcInfoCompositeType == null) {
            try {
                baseGcInfoCompositeType =
                    new CompositeType("sun.management.BaseGcInfoCompositeType",
                                      "CompositeType for Base GcInfo",
                                      getBaseGcInfoItemNames(),
                                      getBaseGcInfoItemNames(),
                                      getBaseGcInfoItemTypes());
            } catch (OpenDataException e) {
                // shouldn't reach here
                throw new RuntimeException(e);
            }
        }
        return baseGcInfoCompositeType;
    }

    public static double getDouble(CompositeData cd, String itemName) {
        if (cd == null)
            throw new IllegalArgumentException("Null CompositeData");

        return ((Double) cd.get(itemName));
    }

    private static final long serialVersionUID = -5716428894085882742L;
}
