/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.avatar.js.metrics.impl;

import com.oracle.avatar.js.metrics.spi.DurationEvent;

/**
 * Default implementation of the DurationEvent interface.
 */
public final class DefaultDurationEvent implements DurationEvent {

    private long start, stop, duration;
    private final String name;

    /**
     * Constructor
     * @param name The name of the event
     */
    public DefaultDurationEvent(final String name) {
        this.name = name;
        reset();
    }

    /**
     * Sets the start time for the event
     */
    @Override
    public void begin() {
        start = System.nanoTime();
    }

    /**
     * Sets the end time for the event
     */
    @Override
    public void end() {
        stop = System.nanoTime();
    }

    /**
     * Ends the event and calculates the duration
     */
    @Override
    public void commit() {
        if (stop == 0) {
            this.end();
        }
        duration = stop - start;
    }

    /**
     * Resets the counters used for calculating the duration of the event
     */
    @Override
    public void reset() {
        start = 0;
        stop = 0;
        duration = 0;
    }

    /**
     * Returns the duration for this event in milliseconds
     *
     * @return The duration
     */
    public long duration() {
        return duration;
    }

    /**
     * Get the name for this event
     * @return The event name
     */
    public String name() {
        return this.name;
    }

}
