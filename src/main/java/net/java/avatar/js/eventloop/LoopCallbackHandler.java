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

package net.java.avatar.js.eventloop;

import java.nio.ByteBuffer;

import net.java.libuv.Address;
import net.java.libuv.Stats;
import net.java.libuv.cb.CallbackHandler;
import net.java.libuv.cb.CheckCallback;
import net.java.libuv.cb.FileCallback;
import net.java.libuv.cb.FileEventCallback;
import net.java.libuv.cb.FilePollCallback;
import net.java.libuv.cb.FilePollStopCallback;
import net.java.libuv.cb.FileCloseCallback;
import net.java.libuv.cb.FileOpenCallback;
import net.java.libuv.cb.FileReadCallback;
import net.java.libuv.cb.FileReadDirCallback;
import net.java.libuv.cb.FileReadLinkCallback;
import net.java.libuv.cb.FileStatsCallback;
import net.java.libuv.cb.FileUTimeCallback;
import net.java.libuv.cb.FileWriteCallback;
import net.java.libuv.cb.IdleCallback;
import net.java.libuv.cb.ProcessCloseCallback;
import net.java.libuv.cb.ProcessExitCallback;
import net.java.libuv.cb.SignalCallback;
import net.java.libuv.cb.StreamCloseCallback;
import net.java.libuv.cb.StreamConnectCallback;
import net.java.libuv.cb.StreamConnectionCallback;
import net.java.libuv.cb.StreamRead2Callback;
import net.java.libuv.cb.StreamReadCallback;
import net.java.libuv.cb.StreamShutdownCallback;
import net.java.libuv.cb.StreamWriteCallback;
import net.java.libuv.cb.TimerCallback;
import net.java.libuv.cb.UDPCloseCallback;
import net.java.libuv.cb.UDPRecvCallback;
import net.java.libuv.cb.UDPSendCallback;

final class LoopCallbackHandler implements CallbackHandler {

    private final EventLoop eventLoop;

    public LoopCallbackHandler(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    @Override
    public void handleCheckCallback(final CheckCallback cb, final int status) {
        try {
            cb.call(status);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleSignalCallback(final SignalCallback cb, final int signum) {
        try {
            cb.call(signum);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamReadCallback(final StreamReadCallback cb, final ByteBuffer data) {
        try {
            cb.onRead(data);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamRead2Callback(final StreamRead2Callback cb, final ByteBuffer data, final long handle, final int type) {
        try {
            cb.onRead2(data, handle, type);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamWriteCallback(final StreamWriteCallback cb, final int status, final Exception error) {
        try {
            cb.onWrite(status, error);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamConnectCallback(final StreamConnectCallback cb, final int status, final Exception error) {
        try {
            cb.onConnect(status, error);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamConnectionCallback(final StreamConnectionCallback cb, final int status, final Exception error) {
        try {
            cb.onConnection(status, error);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamCloseCallback(final StreamCloseCallback cb) {
        try {
            cb.onClose();
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleStreamShutdownCallback(final StreamShutdownCallback cb, final int status, final Exception error) {
        try {
            cb.onShutdown(status, error);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileCallback(final FileCallback cb, final int id, final Exception error) {
        try {
            cb.call(id, error);
            eventLoop.processQueuedEvents();
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }


    @Override
    public void handleFileCloseCallback(final FileCloseCallback cb, final int callbackId, final int fd, final Exception error) {
        try {
            cb.onClose(callbackId, fd, error);
            eventLoop.processQueuedEvents();
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileOpenCallback(final FileOpenCallback cb, final int callbackId, final int fd, final Exception error) {
        try {
            cb.onOpen(callbackId, fd, error);
            eventLoop.processQueuedEvents();
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileReadCallback(final FileReadCallback cb, final int callbackId, final int bytesRead, final byte[] data, final Exception error) {
        try {
            cb.onRead(callbackId, bytesRead, data, error);
            eventLoop.processQueuedEvents();
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileReadDirCallback(final FileReadDirCallback cb, final int callbackId, final String[] names, final Exception error) {
        try {
            cb.onReadDir(callbackId, names, error);
            eventLoop.processQueuedEvents();
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileReadLinkCallback(final FileReadLinkCallback cb, final int callbackId, final String name, final Exception error) {
        try {
            cb.onReadLink(callbackId, name, error);
            eventLoop.processQueuedEvents();
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileStatsCallback(final FileStatsCallback cb, final int callbackId, final Stats stats, final Exception error) {
        try {
            cb.onStats(callbackId, stats, error);
            eventLoop.processQueuedEvents();
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileUTimeCallback(final FileUTimeCallback cb, final int callbackId, final long time, final Exception error) {
        try {
            cb.onUTime(callbackId, time, error);
            eventLoop.processQueuedEvents();
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileWriteCallback(final FileWriteCallback cb, final int callbackId, final int bytesWritten, final Exception error) {
        try {
            cb.onWrite(callbackId, bytesWritten, error);
            eventLoop.processQueuedEvents();
        } catch (final Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFileEventCallback(FileEventCallback cb, int status, String event, String filename) {
        try {
            cb.call(status, event, filename);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }


    @Override
    public void handleFilePollCallback(FilePollCallback cb, int status, Stats previous, Stats current) {
        try {
            cb.onPoll(status, previous, current);
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleFilePollStopCallback(FilePollStopCallback cb) {
        try {
            cb.onStop();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleProcessCloseCallback(ProcessCloseCallback cb) {
        try {
            cb.onClose();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleProcessExitCallback(ProcessExitCallback cb, int status, int signal, Exception error) {
        try {
            cb.onExit(status, signal, error);
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleTimerCallback(final TimerCallback cb, final int status) {
        try {
            cb.call(status);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleUDPRecvCallback(final UDPRecvCallback cb, final int nread, final ByteBuffer data, final Address address) {
        try {
            cb.onRecv(nread, data, address);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleUDPSendCallback(final UDPSendCallback cb, final int status, final Exception error) {
        try {
            cb.onSend(status, error);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleUDPCloseCallback(final UDPCloseCallback cb) {
        try {
            cb.onClose();
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }

    @Override
    public void handleIdleCallback(IdleCallback cb, int status) {
        try {
            cb.call(status);
            eventLoop.processQueuedEvents();
        } catch (Exception ex) {
            eventLoop.loop().getExceptionHandler().handle(ex);
        }
    }
}
