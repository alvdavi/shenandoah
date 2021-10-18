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
 * A {@code ConcurrentInfo} object represents a concurrent phase
 * within a garbage collection. It includes a timestamp specifying
 * the time since the launch of the Java virtual machine process,
 * the wall clock time duration of the phase, plus additional,
 * optional, very detailed information.
 *
 * @author Paul Hohensee
 * @since  18
 */
public class ConcurrentInfo implements PhaseInfo /*, CompositeDataView*/ {
    private final long index;
    private final long startTime;
    private final long endTime;
    private final long threadCount;

    public ConcurrentInfo(long index, long startTime, long endTime, long threadCount) {
	    this.index = index;
        this.startTime = startTime;
        this.endTime = endTime;
        this.threadCount = threadCount;
    }

    @Override
    public String getPhaseType() {
        return "Concurrent";
    }

    @Override
    public long getId() {
        return index;
    }

    @Override
    public long getStartTimeNanos() {
        return startTime;
    }

    @Override
    public long getEndTimeNanos() {
        return endTime;
    }

    @Override
    public long getPhaseGarbageCollectorThreadCount() {
        return threadCount;
    }
}
