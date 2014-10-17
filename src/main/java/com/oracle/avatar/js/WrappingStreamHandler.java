/*
 * Copyright (c) 2013-2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.avatar.js;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URL WrappingStreamHandler to deal with script boxing. A script is isolated inside
 * a function object. This wrapping is not present in node apps, it has to be done on-the-fly.
 * To keep the original codeBase, we do the wrapping when the script is loaded by nashorn.
 * URL examples:
 * <ul>
 * <li>avatar:file://{path to script file}</li>
 * <li>avatar:jar:file:{path to script file inside jar file}</li>
 * </ul>
 */
public final class WrappingStreamHandler extends URLStreamHandler {

    private static final class ContentWrapper {

        private final byte[] content;
        private int offset = 0;

        private ContentWrapper(final byte[] content) {
            this.content = content;
        }

        private boolean hasMore() {
            return offset < content.length;
        }

        private int read() {
            return content[offset++];
        }

        public int read(final byte[] b, final int o, final int len) {
            int off = o;
            int currentOffset = offset;
            if (len >= offset) {
                while (offset < content.length) {
                    b[off] = content[offset];
                    off++;
                    offset++;
                }
            } else {
                for (int i = 0; i < len; i++) {
                    b[off] = content[offset];
                    off++;
                    offset++;
                    if (offset == content.length - 1) {
                        break;
                    }
                }
            }
            return offset - currentOffset;
        }
    }

    protected static final class WrapperInputStream extends InputStream {

        private static final byte[] WRAP_PREFIX = Loader.utf8(Loader.PREFIX);
        private static final byte[] WRAP_SUFFIX = Loader.utf8(Loader.SUFFIX);
        private static final int WRAP_LENGTH = WRAP_PREFIX.length + WRAP_SUFFIX.length;

        private final ContentWrapper prefix = new ContentWrapper(WRAP_PREFIX);
        private final ContentWrapper suffix = new ContentWrapper(WRAP_SUFFIX);
        private final InputStream wrapped;
        private boolean wrappedHasMore = true;

        public WrapperInputStream(final InputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int read() throws IOException {
            if (prefix.hasMore()) {
                return prefix.read();
            } else if (wrappedHasMore) {
                final int read = wrapped.read();
                if (read >= 0) {
                    return read;
                } else {
                    wrappedHasMore = false;
                    return suffix.read();
                }
            } else {
                if (suffix.hasMore()) {
                    return suffix.read();
                } else {
                    return -1;
                }
            }
        }

        // read method has to be overridden for performance purpose.
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (prefix.hasMore()) {
                return prefix.read(b, off, len);
            } else if (wrappedHasMore) {
                final int read = wrapped.read(b, off, len);
                if (read >= 0) {
                    return read;
                } else {
                    wrappedHasMore = false;
                    return suffix.read(b, off, len);
                }
            } else {
                if (suffix.hasMore()) {
                    return suffix.read(b, off, len);
                } else {
                    return -1;
                }
            }
        }
    }

    private static final class WrappedURLConnection extends URLConnection {

        private final URLConnection wrapped;

        WrappedURLConnection(final URL url, final URLConnection wrapped) {
            super(url);
            this.wrapped = wrapped;
        }

        @Override
        public void connect() throws IOException {
            //NOOP
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new WrapperInputStream(wrapped.getInputStream());
        }

        @Override
        public int getContentLength() {
            return wrapped.getContentLength() + WrapperInputStream.WRAP_LENGTH;
        }

        @Override
        public long getLastModified() {
            return wrapped.getLastModified();
        }
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        // Reconstruct a URL without handler.
        final URL withoutHandler = new URL(u.toExternalForm());
        return new WrappedURLConnection(withoutHandler, withoutHandler.openConnection());
    }
}
