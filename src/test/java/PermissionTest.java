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

import java.io.File;
import java.io.FilePermission;
import java.net.SocketPermission;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.security.AccessControlException;
import java.security.Permission;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.concurrent.Callable;

import javax.script.ScriptEngine;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.oracle.avatar.js.Loader;
import com.oracle.avatar.js.Server;
import com.oracle.avatar.js.log.Logging;
import com.oracle.libuv.LibUVPermission;

/**
 * Test user scripts permissions. This test doesn't rely on a policy file.
 * Permissions are configured in each test.
 *
 */
public class PermissionTest {

    private static final String OS = System.getProperty("os.name");
    private static final String ADDRESS = "127.0.0.1";
    private static int p = 61234;
    private static final String SCRIPT_PORT = "SCRIPT_PORT";
    private static final String SCRIPT_ON_EXIT = "SCRIPT_ON_EXIT";
    private static final String SCRIPT_OK = "SCRIPT_OK";
    private static final String SCRIPT_PIPE = "SCRIPT_PIPE";

    private static int getPort() {
        return ++p;
    }

    /**
     * In memory policy configuration, no need for a policy file.
     */
    public static class SimplePolicy extends Policy {

        private final Permissions permissions;
        private final URL location;
        private final File parentDir;

        public SimplePolicy(Permissions perms, URL location) throws Exception {
            permissions = new Permissions();
            Enumeration<Permission> enumeration = perms.elements();
            while (enumeration.hasMoreElements()) {
                permissions.add(enumeration.nextElement());
            }

            // Default java.policy grants listen to localhost
            permissions.add(new SocketPermission("localhost:1024-", "listen"));
            permissions.add(new PropertyPermission("os.arch", "read"));
            this.location = location;
            this.parentDir = new File(location.toURI()).getParentFile();
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            File parent = null;
            try {
                File f = new File(domain.getCodeSource().getLocation().toURI());
                parent = f.getParentFile();
            } catch (Exception ex) {
                // XXX OK
            }
            // Only check permissions for the script location or any script in the same directory
            if (domain.getCodeSource().getLocation().equals(location) ||
                    parentDir.equals(parent)) {
                return permissions.implies(permission);
            } else {
                return true;
            }
        }
    }

    @BeforeMethod
    public void before() {
        if (System.getSecurityManager() != null) {
            throw new Error("Security manager is already set");
        }
    }

    @AfterMethod
    public void after() {
        // would have to grant all testng required
        // permissions. Better to set it back to null. We are in a test....
        // So do unsafe stuff!
        System.setSecurityManager(null);
    }

    private static ScriptEngine newEngine(Map<String, Object> b) throws Exception {
        ScriptEngine engine = Server.newEngine();
        for (String k : b.keySet()) {
            engine.put(k, b.get(k));
        }
        return engine;
    }

    private static Server newServer(ScriptEngine engine) throws Exception {
        Server server = new Server(engine, new Loader.Core(), new Logging(false),
                System.getProperty("user.dir"));
        return server;
    }

    @Test
    public void testTCPNoAuth() throws Throwable {
        int port = getPort();
        File f = new File("src/test/js/security/tcp.js");
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_PORT, port);
        bindings.put(SCRIPT_ON_EXIT, false);
        doFail(bindings, f);
    }

    @Test
    public void testTCPNoAccept() throws Throwable {
        int port = getPort();
        File f = new File("src/test/js/security/tcp.js");
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_PORT, port);
        bindings.put(SCRIPT_ON_EXIT, false);
        Permissions permissions = new Permissions();
        permissions.add(new SocketPermission(ADDRESS + ":" + port, "listen"));
        permissions.add(new SocketPermission(ADDRESS + ":" + port, "connect"));
        doFail(bindings, f, permissions);
    }

    @Test
    public void testTCPNoConnect() throws Throwable {
        int port = getPort();
        File f = new File("src/test/js/security/tcp.js");
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_PORT, port);
        bindings.put(SCRIPT_ON_EXIT, false);
        Permissions permissions = new Permissions();
        permissions.add(new SocketPermission(ADDRESS + ":" + port, "listen"));
        doFail(bindings, f, permissions);
    }

    @Test
    public void testTCPAuth() throws Throwable {
        int port = getPort();
        Permissions permissions = new Permissions();
        permissions.add(new SocketPermission(ADDRESS + ":" + port, "listen"));
        permissions.add(new SocketPermission(ADDRESS + ":" + port, "connect"));
        permissions.add(new SocketPermission(ADDRESS + ":*", "accept"));
        permissions.add(new RuntimePermission("avatar-js"));

        File f = new File("src/test/js/security/tcp.js");
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_PORT, port);
        bindings.put(SCRIPT_ON_EXIT, true);
        doSuccess(bindings, f, permissions);
    }

    @Test
    public void testUDPNoAuth() throws Throwable {
        int port = getPort();
        File f = new File("src/test/js/security/udp.js");
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_PORT, port);
        bindings.put(SCRIPT_ON_EXIT, false);
        doFail(bindings, f);

    }

    @Test
    public void testUDPAuth() throws Throwable {
        int port = getPort();
        Permissions permissions = new Permissions();
        // required by dns.js
        permissions.add(new PropertyPermission("os.name", "read"));
        permissions.add(new SocketPermission(ADDRESS + ":" + port, "connect"));
        permissions.add(new RuntimePermission("avatar-js"));

        File f = new File("src/test/js/security/udp.js");
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_PORT, port);
        bindings.put(SCRIPT_ON_EXIT, true);
        doSuccess(bindings, f, permissions);
    }

    @Test
    public void testSpawnNoAuth() throws Throwable {
        File f = new File("src/test/js/security/spawn.js");
        Map<String, Object> bindings = new HashMap<>();
        // exePath
        Permissions permissions = new Permissions();
        permissions.add(new PropertyPermission("java.class.path", "read"));
        permissions.add(new PropertyPermission("java.library.path", "read"));
        // parent env
        permissions.add(new RuntimePermission("getenv.*"));
        doFail(bindings, f, permissions);

    }

    @Test
    public void testSpawnAuth() throws Throwable {
        Permissions permissions = new Permissions();
        permissions.add(new PropertyPermission("java.class.path", "read"));
        permissions.add(new PropertyPermission("java.library.path", "read"));
        // parent env
        permissions.add(new RuntimePermission("getenv.*"));
        // execute
        permissions.add(new FilePermission("<<ALL FILES>>", "execute"));
        permissions.add(new RuntimePermission("avatar-js"));

        File f = new File("src/test/js/security/spawn.js");
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_ON_EXIT, true);
        doSuccess(bindings, f, permissions);
    }

    @Test
    public void testPipeNoAuth() throws Throwable {
        File f = new File("src/test/js/security/pipe.js");
        final String PIPE_NAME;

        if (OS.startsWith("Windows")) {
            PIPE_NAME = "\\\\.\\pipe\\uv-java-test-pipe";
        } else {
            PIPE_NAME = "/tmp/uv-java-test-pipe";
        }

        Files.deleteIfExists(FileSystems.getDefault().getPath(PIPE_NAME));
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_PIPE, PIPE_NAME);
        Permissions permissions = new Permissions();
        //platform
        permissions.add(new PropertyPermission("os.name", "read"));
        doFail(bindings, f, permissions);
    }

    @Test
    public void testPipeAuth() throws Throwable {
        File f = new File("src/test/js/security/pipe.js");
        final String PIPE_NAME;

        if (OS.startsWith("Windows")) {
            PIPE_NAME = "\\\\.\\pipe\\uv-java-test-pipe";
        } else {
            PIPE_NAME = "/tmp/uv-java-test-pipe";
        }

        Files.deleteIfExists(FileSystems.getDefault().getPath(PIPE_NAME));
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_PIPE, PIPE_NAME);
        bindings.put(SCRIPT_ON_EXIT, true);
        Permissions permissions = new Permissions();

        // On windows: process.env.NODE_PENDING_PIPE_INSTANCES
        if (OS.startsWith("Windows")) {
            permissions.add(new RuntimePermission("getenv.*"));
        }
        permissions.add(new PropertyPermission("os.name", "read"));
        permissions.add(new LibUVPermission("libuv.pipe.connect"));
        permissions.add(new LibUVPermission("libuv.pipe.bind"));
        permissions.add(new LibUVPermission("libuv.pipe.accept"));
        permissions.add(new RuntimePermission("avatar-js"));

        doSuccess(bindings, f, permissions);
    }

    @Test
    public void testPipeNoAccept() throws Throwable {
        File f = new File("src/test/js/security/pipe.js");
        final String PIPE_NAME;

        if (OS.startsWith("Windows")) {
            PIPE_NAME = "\\\\.\\pipe\\uv-java-test-pipe";
        } else {
            PIPE_NAME = "/tmp/uv-java-test-pipe";
        }

        Files.deleteIfExists(FileSystems.getDefault().getPath(PIPE_NAME));
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_PIPE, PIPE_NAME);
        bindings.put(SCRIPT_ON_EXIT, false);
        Permissions permissions = new Permissions();

        //platform
        permissions.add(new PropertyPermission("os.name", "read"));
        permissions.add(new LibUVPermission("libuv.pipe.connect"));
        permissions.add(new LibUVPermission("libuv.pipe.bind"));

        doFail(bindings, f, permissions);
    }

    @Test
    public void testPipeNoConnect() throws Throwable {
        File f = new File("src/test/js/security/pipe.js");
        final String PIPE_NAME;

        if (OS.startsWith("Windows")) {
            PIPE_NAME = "\\\\.\\pipe\\uv-java-test-pipe";
        } else {
            PIPE_NAME = "/tmp/uv-java-test-pipe";
        }

        Files.deleteIfExists(FileSystems.getDefault().getPath(PIPE_NAME));
        Map<String, Object> bindings = new HashMap<>();
        bindings.put(SCRIPT_PIPE, PIPE_NAME);
        bindings.put(SCRIPT_ON_EXIT, false);
        Permissions permissions = new Permissions();

        //platform
        permissions.add(new PropertyPermission("os.name", "read"));
        permissions.add(new LibUVPermission("libuv.pipe.bind"));

        doFail(bindings, f, permissions);
    }

    @Test
    public void testModules() throws Throwable {
        File f = new File("src/test/js/security/modules.js");
        Map<String, Object> bindings = new HashMap<>();

        Permissions permissions = new Permissions();
        permissions.add(new RuntimePermission("avatar-js"));

        doSuccess(bindings, f, permissions);
    }

    @Test
    public void testModules2() throws Throwable {
        File f = new File("src/test/js/security/modules2.js");
        Map<String, Object> bindings = new HashMap<>();

        Permissions permissions = new Permissions();
        bindings.put(SCRIPT_PORT, getPort());
        doFail(bindings, f, permissions);
    }

    @Test
    public void testProcess() throws Throwable {
        File f = new File("src/test/js/security/process.js");
        Map<String, Object> bindings = new HashMap<>();
        String osname = System.getProperty("os.name").toLowerCase();
        if (osname.startsWith("windows")) {
            bindings.put("__test_windows", "true");
        }
        Permissions permissions = new Permissions();
        doSuccess(bindings, f, permissions);
    }

    private static void testFailure(Callable r) throws Exception {
        try {
            r.call();
            throw new RuntimeException("Should have failed");
        } catch (Throwable ex) {
            if (!(getCause(ex) instanceof AccessControlException)) {
                System.err.println("UNEXPECTED EXCEPTION " + ex);
                ex.printStackTrace();
                throw ex;
            } else {
                System.err.println("Catch expected exception " + ex);
            }
        }
    }

    private static Throwable getCause(Throwable ex) {
        if (ex instanceof AccessControlException || ex == null) {
            return ex;
        }
        return getCause(ex.getCause());
    }

    private static void testSuccess(Callable r) throws Exception {
        try {
            r.call();
        } catch (Exception ex) {
            System.err.println("UNEXPECTED EXCEPTION " + ex);
            ex.printStackTrace();
            throw ex;
        }
    }

    private static void doFail(Map<String, Object> bindings, File f) throws Throwable {
        doFail(bindings, f, new Permissions());
    }

    private static void doFail(Map<String, Object> bindings, File f, Permissions permissions) throws Throwable {
        // Capture caller
        System.err.println("Called by " + new Exception().getStackTrace()[1].getMethodName());
        URL location = f.toURI().toURL();
        final String[] args = {f.getAbsolutePath()};
        final ScriptEngine engine = newEngine(bindings);
        final Server server = newServer(engine);
        init(permissions, location);

        testFailure(new Callable() {
            @Override
            public Object call() throws Exception {
                try {
                    server.run(args);
                } catch (Exception ex) {
                    throw ex;
                } catch (Throwable th) {
                    throw new Exception(th);
                }
                return null;
            }
        });

        System.out.println(f + " NoAuth test passed");
    }

    private static void doSuccess(Map<String, Object> bindings, File f, Permissions permissions) throws Throwable {
        // Capture caller
        System.err.println("Called by " + new Exception().getStackTrace()[1].getMethodName());
        URL location = f.toURI().toURL();
        final String[] args = { f.getAbsolutePath() };
        final ScriptEngine engine = newEngine(bindings);
        final Server server = newServer(engine);
        init(permissions, location);

        testSuccess(new Callable() {
            @Override
            public Object call() throws Exception {
                try {
                    server.run(args);
                } catch (Exception ex) {
                    throw ex;
                } catch (Throwable th) {
                    throw new Exception(th);
                }
                Object obj = engine.get(SCRIPT_OK);
                if (obj == null) {
                    throw new Exception("TEST FAILURE");
                }
                return null;
            }
        });

        System.err.println(f + " Auth test passed");
    }

    private static void init(Permissions permissions, URL location) throws Exception {
        Policy.setPolicy(new SimplePolicy(permissions, location));
        System.setSecurityManager(new SecurityManager());
    }
}
