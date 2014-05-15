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

package runner;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.oracle.avatar.js.Server;

public class TestRunner {

    private static boolean secure = false;
    private static long delay = 100;
    private static long timeout = 60 * 1000;
    private static String maxheap = "1g";
    private static boolean assertions = true;
    private static boolean deprecations = false;
    private static boolean redirect = true;
    private static boolean colorize = true;
    private static boolean fork = true;

    private static final PrintStream err = System.err;
    private static final String pwd = System.getProperty("user.dir");

    private static final List<String> testNames = new ArrayList<>();
    private static final List<String> jvmArgs = new ArrayList<>();
    private static final List<String> javaArgs = new ArrayList<>();
    private static final List<String> jarArgs = new ArrayList<>();
    private static final List<String> failedTests = new ArrayList<>();

    private static final String escape = "\033[";
    private static String red;
    private static String green;
    private static String magenta;
    private static String cyan;
    private static String bold;
    private static String reset;

    private static List<String> exclusions;
    private static List<Pattern> exclusionPatterns;

    private static String[] noforkExclusions = {
            "test-child-process-fork-and-spawn.js",
            "test-domain.js",
            "test-http-exceptions.js",
            "test-http-upgrade-server2.js"
    };

    private static String[] noforkExclusionExpressions = {
            "^test\\-cluster\\-.+.js$",
            "test\\-domain\\-.+.js$"
    };

    private static int testsRun = 0;
    private static int testsToRun = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) throws Throwable {
        if (args.length < 1) {
            err.print("Usage: [options] <args>\n");
            err.print(" where options may be\n");
            err.print("  -nofork to run each test in the same jvm (disabled by default)\n");
            err.print("  -nocolor to disable colorized output (for non-interactive runs)\n");
            err.print("  -noassert to disable assertions (enabled by default)\n");
            err.print("  -deprecation to throw on deprecations (disabled by default)\n");
            err.print("  -noredirect to disable stdout/stderr redirection to files\n");
            err.print("  -secure to run tests with a security manager\n");
            err.print("  -delay ms to pause between each test to give the OS time to cleanup (default: 100)\n");
            err.print("  -timeout s to specify the timeout for each test (default: 60s)\n");
            err.print("  -mx N to specify the maximum jvm heap size for each test (default: 1g)\n");
            err.print(" <args> is some combination of node tests and/or directories\n");
            err.print("directories are searched for tests matching \"test-*.js\"\n");
            System.exit(-1);
        }

        final Path exclusionsFile = Paths.get(pwd, platform() + "-test-exclusions.txt");
        if (exclusionsFile.toFile().exists()) {
            exclusions = Arrays.asList(new String(Files.readAllBytes(exclusionsFile), "utf-8")
                    .split("\n"))
                    .stream()
                    .map(e -> {
                        int comment = e.indexOf(" ");
                        if (comment > 0) {
                            return e.substring(0, comment).trim();
                        } else {
                            return e.trim();
                        }
                    })
                    .collect(Collectors.toList());
        } else {
            exclusions = new ArrayList<>(); // empty
        }

        final List<String> paths = new ArrayList<>();
        for (int i=0; i < args.length; i++) {
            final String root = args[i];
            if (root.matches("^-\\w*")) {
                switch (root) {
                    case "-nofork": fork = false; break;
                    case "-noassert": assertions = false; break;
                    case "-deprecation": deprecations = true; break;
                    case "-nocolor": colorize = false; break;
                    case "-noredirect": redirect = false; break;
                    case "-secure": secure = true; break;
                    case "-delay": delay = Long.parseLong(args[++i]); break;
                    case "-mx": maxheap = args[++i]; break;
                    case "-timeout": timeout = Long.parseLong(args[++i]) * 1000; break;
                    default: jvmArgs.add(root);
                }
            } else {
                paths.add(root);
            }
        }

        exclusionPatterns = new ArrayList<>(noforkExclusionExpressions.length);
        if (!fork) {
            exclusions.addAll(Arrays.asList(noforkExclusions));
            for (final String expression : noforkExclusionExpressions) {
                exclusionPatterns.add(Pattern.compile(expression));
            }
        }

        for (final String root : paths) {
            final Path rootPath = Paths.get(root);
            final File stats = rootPath.toFile();
            if (stats.isDirectory()) {
                scanDir(rootPath);
            } else if (stats.isFile()) {
                scanFile(root, rootPath);
            } else {
                final int names = rootPath.getNameCount();
                final String basePath = rootPath.getName(names - 1).toString();
                final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + basePath);
                final Path directory = rootPath.subpath(0, names - 1);
                Files.list(directory)
                        .filter(f -> f.toString().matches(".+test-.+\\.js$"))
                        .sorted()
                        .forEach(f -> {
                            final Path fp = f.getName(f.getNameCount() - 1);
                            if (matcher.matches(fp)) {
                                testNames.add(f.toString());
                            }
                        });
            }
        }

        red = colorize ? escape + Integer.toString(31) + "m" : "";
        green = colorize ? escape + Integer.toString(32) + "m" : "";
        magenta = colorize ? escape + Integer.toString(35) + "m" : "";
        cyan = colorize ? escape + Integer.toString(36) + "m" : "";
        bold = colorize ? escape + Integer.toString(1) + "m" : "";
        reset = colorize ? escape + Integer.toString(0) + "m" : "";

        Paths.get(pwd, "test", "tmp").toFile().mkdirs();
        final String target = Paths.get(pwd, "dist").toString();
        final String jar = Paths.get(target, "avatar-js.jar").toString();
        final String results = Paths.get(pwd, "test-output").toString();
        Paths.get(results).toFile().mkdirs();

        testsRun = 0;
        testsToRun = testNames.size();
        testsFailed = 0;

        javaArgs.add("-server");
        javaArgs.add(assertions ? "-ea" : "-da");
        javaArgs.add("-Djava.awt.headless=true");
        javaArgs.add("-Xmx" + maxheap);
        javaArgs.add("-Xcheck:jni");
        javaArgs.add("-Djava.library.path=" + target);

        jarArgs.add("-jar");
        jarArgs.add(jar);
        if (deprecations) {
            jarArgs.add("--throw-deprecation");
        }
        if (secure) {
            javaArgs.add("-Djava.security.manager");
            javaArgs.add("-Djava.security.policy=" + Paths.get(pwd, "dist/avatar-js.policy"));
        }

        final long totalStart = System.currentTimeMillis();
        while (testNames.size() > 0) {
            final String testName = testNames.remove(0);
            err.print(++testsRun + "/" + testsToRun + " " + testName + " ... ");
            if (fork) {
                final String outputDir = Paths.get(results, testName).toString().replaceAll("\\.js$", "");
                final Path outputDirPath = Paths.get(outputDir);
                outputDirPath.toFile().mkdirs();
                forkNextTest(testName, outputDir);
            } else {
                runNextTest(testName, results);
            }
            System.gc();
            Thread.sleep(delay);
        }

        final int completion = testsToRun == 0 ? 0 : (100 * (testsRun - testsFailed)) / testsToRun;
        err.print("\nTotal tests attempted: " + testsRun +
                ", failed: " + testsFailed +
                ", completion rate: " + completion + "%" +
                ", total time: " + ((System.currentTimeMillis() - totalStart) / 1000) + "s" +
                "\n");
        if (testsFailed > 0) {
            err.print("Failed tests: " + failedTests.size() + "\n");
            final AtomicInteger i = new AtomicInteger(0);
            failedTests.forEach(t -> err.print(i.incrementAndGet() + " " + t + "\n"));
        }
        err.print("\n");
        System.exit(testsFailed);
    }

    private static void runNextTest(final String testName,
                                    final String outputDir) throws Throwable {
        err.print("\n");
        System.setProperty("avatar-js.log.output.dir=", outputDir);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final AtomicBoolean passed = new AtomicBoolean(false);
        final long start = System.currentTimeMillis();
        final Thread runner = new Thread(() -> {
            Server server = null;
            try {
                server = new Server();
                server.run(testName);
                passed.set(true);
            } catch (Throwable throwable) {
                passed.set(false);
                failure.set(throwable);
            } finally {
                if (server != null) {
                    server.close();
                }
            }
        }, testName + " runner");
        runner.start();
        runner.join(timeout);
        err.print(testName + " " + ((System.currentTimeMillis() - start) / 1000) + "s ");
        if (passed.get()) {
            err.println(bold + green + "OK" + reset);
        } else {
            if (failure.get() != null) {
                err.println(bold + red + "FAILED" + reset);
                testsFailed++;
                failedTests.add(testName);
                failure.get().printStackTrace();
            } else {
                err.println(bold + cyan + "TIMEOUT" + red + " FAILED" + reset);
                testsFailed++;
                failedTests.add(testName);
            }
        }
        err.print("\n");
    }

    private static void forkNextTest(final String testName, final String outputDir) throws IOException {
        final List<String> spawnArgs = new ArrayList<>();
        spawnArgs.add("java");
        spawnArgs.addAll(jvmArgs);
        spawnArgs.addAll(javaArgs);
        spawnArgs.add("-Davatar-js.log.output.dir=" + outputDir);
        spawnArgs.addAll(jarArgs);
        spawnArgs.add(testName);

        final ProcessBuilder pb = new ProcessBuilder(spawnArgs);
        pb.directory(Paths.get(pwd).toFile());
        if (redirect) {
            pb.redirectOutput(Paths.get(outputDir, "stdout.txt").toFile());
            pb.redirectError(Paths.get(outputDir, "stderr.txt").toFile());
        } else {
            pb.inheritIO();
        }

        final long start = System.currentTimeMillis();
        final Process process = pb.start();
        boolean failed = false;

        boolean completed = false;
        try {
            completed = process.waitFor(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            err.print(magenta + "INTERRUPTED " + reset);
            failed = true;
        }
        err.print(((System.currentTimeMillis() - start) / 1000) + "s ");
        if (!completed) {
            err.print(bold + cyan + "TIMEOUT " + reset);
            failed = true;
        }
        final int code = process.exitValue();
        if (code != 0 || failed) {
            testsFailed++;
            err.print(bold + red + "FAILED");
            failedTests.add(testName);
        } else {
            err.print(bold + green + "OK");
        }
        err.print(reset + "\n");
    }

    private static void scanFile(String root, Path rootPath) {
        final String baseName = rootPath.getName(rootPath.getNameCount() - 1).toString();
        if (exclusions.contains(baseName)) {
            err.print("excluding " + baseName + "\n");
        } else {
            testNames.add(root);
        }
    }

    private static void scanDir(final Path rootPath) throws IOException {
        Files.list(rootPath)
                .filter(f -> f.toString().matches(".+test-.+\\.js$"))
                .sorted()
                .forEach(f -> {
                    final String test = f.toString();
                    final String basename = f.getName(f.getNameCount() - 1).toString();
                    boolean excluded = false;
                    if (exclusions.contains(basename)) {
                        excluded = true;
                    } else if (!fork) {
                        for (final Pattern p : exclusionPatterns) {
                            final Matcher matcher = p.matcher(basename);
                            if (matcher.matches()) {
                                excluded = true;
                                break;
                            }
                        }
                    }
                    if (excluded) {
                        err.print("excluding " + test + "\n");
                    } else {
                        testNames.add(test);
                    }
                });
    }

    private static String platform() {
        final String osname = System.getProperty("os.name").toLowerCase();
        if (osname.startsWith("windows")) {
            return "win32";
        } else {
            if (osname.startsWith("mac os x")) {
                return "darwin";
            }
        }
        return osname;
    }

}
