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

package com.oracle.avatar.js.metrics;

import com.oracle.avatar.js.metrics.spi.DurationEvent;
import com.oracle.avatar.js.metrics.spi.MetricsFactory;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * The MetricsService class loads the MetricsFactory implementations available at runtime.
 *
 * @author Irfan Ahmed
 * @since 07/19/2014
 */
public class MetricsService {
    private static MetricsService service;
    private ServiceLoader<MetricsFactory> loader;
    private MetricsFactory factory;
    
    private MetricsService() {
        loader = ServiceLoader.load(MetricsFactory.class);
        Iterator<MetricsFactory> factories = loader.iterator();
        if(factories.hasNext()) {
            factory = factories.next();
        } else {
            factory = null;
        }
    }
    
    /**
     * The singleton instance of the MetricsService class.
     * 
     * @return The MetricsService instance
     */
    public static synchronized MetricsService getInstance() {
        if (service == null) {
            service = new MetricsService();
        }
        return service;
    }
    
    /**
     * Returns true if a factory class implementing the {@link MetricsFactory} interface is available and the
     * factory has the capability to produce metrics.
     * 
     * @return boolean indicating if Metrics are available
     */
    public boolean metricsAvailable() {
        return (factory != null) && factory.metricsAvailable();
    }
    
    
    /**
     * Creates a new DurationEvent if a capable {@link MetricsFactory} is available.
      *
     * @param name The name of the event
     * @return The DurationEvent instance or null
     */
    public DurationEvent newDurationEvent(String name) {
        return metricsAvailable() ? factory.getDurationEvent(name) : null;
    }
}
