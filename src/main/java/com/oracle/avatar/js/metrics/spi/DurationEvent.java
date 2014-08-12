/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of the GNU General
 * Public License Version 2 only ("GPL"). You may not use this file except
 * in compliance with the License.  You can obtain a copy of the License at
 * https://avatar.java.net/license.html or legal/LICENSE.txt.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.avatar.js.metrics.spi;

/**
 * DurationEvent Interface
 * 
 * @author Irfan Ahmed
 * @since 07/19/2014
 */
public interface DurationEvent {
    // interface methods
    public void begin();
    public void end();
    public void commit();
    public void reset();
    
    // default implementation
    /**
     * The function returns a default implementation for the DurationEvent interface.
     * 
     * @param name The name of the event
     * @return An implementation of the DurationEvent interface
     */
    public static DurationEvent defaultDurationEvent (String name) {
        return new DefaultDurationEvent(name);
    }
    
    /**
     * The default implementation for the DurationEvent interface
     */
    public static final class DefaultDurationEvent implements DurationEvent {
        private long start, stop, duration;
        private String name;
        
        /**
         * Constructor
         * @param name The name of the event
         */
        public DefaultDurationEvent(String name) {
            this.name = name;
        }
        
        /**
         * Sets the start time for the event
         */
        @Override
        public void begin() {
            start = System.currentTimeMillis();
        }

        /**
         * Sets the end time for the event
         */
        @Override
        public void end() {
            stop = System.currentTimeMillis();
        }

        /**
         * Ends the event and calculates the duration
         */
        @Override
        public void commit() {
            if(stop == 0) {
                this.end();
            }
            duration = stop-start;
        }

        /**
         * Returns the duration for this event in milliseconds
         * 
         * @return The duration
         */
        public long getDuration() {
            return duration;
        }

        /**
         * Get the name for this event
         * @return The event name
         */
        public String getName() {
            return this.name;
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
    }
}
