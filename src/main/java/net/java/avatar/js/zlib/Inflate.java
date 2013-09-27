/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package net.java.avatar.js.zlib;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import net.java.avatar.js.eventloop.EventLoop;

/**
 * Read Deflate compressed input and write uncompressed to output.
 */
public class Inflate extends UncompressWriter {

    public Inflate(final EventLoop eventLoop) {
        super(eventLoop);
    }

    private Inflater inflater;
    @Override
    protected InputStream createInputStream(final byte[] rawChunk, final InputStream istream) throws IOException {
        inflater = newInflater();
        return new InflaterInputStream(istream, inflater);
    }

    @Override
    protected boolean shouldRetry() {
        if (inflater.needsDictionary()) {
            if (getDictionary().capacity() == 0) {
                throw new IllegalArgumentException("Missing dictionary");
            }
            try {
                inflater.setDictionary(getDictionary().array());
            } catch (Throwable thr) {
                throw new IllegalArgumentException("Bad dictionary");
            }
            return true;
        }
        return false;
    }

     protected Inflater newInflater() {
        return new Inflater();
    }
}
