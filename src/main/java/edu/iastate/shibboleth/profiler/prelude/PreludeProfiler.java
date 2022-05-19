package edu.iastate.shibboleth.profiler.prelude;

/*-
 * #%L
 * shibboleth-maven-plugin
 * %%
 * Copyright (C) 2021 - 2022 Iowa State University
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import edu.iastate.shibboleth.commons.misc.NameUtils;
import edu.iastate.shibboleth.commons.process.ProcessUtils;
import edu.iastate.shibboleth.commons.relational.MethodsDom;
import edu.iastate.shibboleth.commons.testing.junit.runner.RecordingResultCollector;
import edu.iastate.shibboleth.commons.testing.junit.runner.JUnitRunner;
import org.pitest.boot.HotSwapAgent;
import org.pitest.classinfo.CachingByteArraySource;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.functional.predicate.Predicate;
import org.pitest.process.ProcessArgs;
import org.pitest.testapi.Description;
import org.pitest.testapi.ResultCollector;
import org.pitest.testapi.TestUnit;
import org.pitest.util.ExitCode;
import org.pitest.util.IsolationUtils;
import org.pitest.util.SafeDataInputStream;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entry point for Profiler.
 * Profiler is responsible for running test cases against original and patched versions
 * of the program and returning the system state at the exit point(s) of the specified
 * method.
 *
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public final class PreludeProfiler {
    private static final int CACHE_SIZE = 500;

    private PreludeProfiler() { }

    public static void main(String[] args) {
        System.out.println("Prelude Profiler is HERE!");
        final int port = Integer.parseInt(args[0]);
        Socket socket = null;
        try {
            socket = new Socket("localhost", port);

            final SafeDataInputStream dis = new SafeDataInputStream(socket.getInputStream());

            final PreludeProfilerArguments arguments = dis.read(PreludeProfilerArguments.class);

            final ClassLoader contextClassLoader = IsolationUtils.getContextClassLoader();
            ClassByteArraySource byteArraySource = new ClassloaderByteArraySource(contextClassLoader);
            byteArraySource = new CachingByteArraySource(byteArraySource, CACHE_SIZE);

            final MethodsDom methodsDom = new MethodsDom();
            final ClassFileTransformer transformer = new PreludeProfilerTransformer(byteArraySource,
                    arguments.appClassFilter, methodsDom);
            HotSwapAgent.addTransformer(transformer);

            final PreludeProfilerReporter reporter = new PreludeProfilerReporter(socket.getOutputStream());

            final RecordingResultCollector resultCollector = new RecordingResultCollector();
            final JUnitRunner runner = new JUnitRunner(arguments.testClassNames, resultCollector);
            runner.setTestUnits(decorateTestCases(runner.getTestUnits()));
            final long startTS = System.currentTimeMillis();
            runner.run();
            final long endTS = System.currentTimeMillis();
            System.out.println();
            System.out.printf("[Profiler] Ran %d tests in %d ms",
                    runner.getTestUnits().size(),
                    endTS - startTS);
            System.out.println();
            System.out.println();

            methodsDom.save(".", true);
            reporter.reportMethodCoverageMap(MethodCoverageRecorder.getCoverageMap());
            reporter.reportFailingTests(new HashSet<>(resultCollector.getFailingTestNames()));

            System.out.println("Prelude Profiler is DONE!");
            reporter.done(ExitCode.OK);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.out);
            System.out.println("WARNING: Error during profiling!");
        } finally {
            ProcessUtils.safelyCloseSocket(socket);
        }
    }

    private static List<TestUnit> decorateTestCases(final List<? extends TestUnit> testUnits) {
        final List<TestUnit> res = new LinkedList<>();
        for (final TestUnit testUnit : testUnits) {
            res.add(new TestUnit() {
                @Override
                public void execute(ResultCollector resultCollector) {
                    final String testName = NameUtils.sanitizeExtendedTestName(testUnit.getDescription().getName());
                    MethodCoverageRecorder.addTestUnit(testName);
                    testUnit.execute(resultCollector);
                }

                @Override
                public Description getDescription() {
                    return testUnit.getDescription();
                }
            });
        }
        return res;
    }

    public static PreludeProfilerResults runPrelude(final ProcessArgs defaultProcessArgs,
                                                    final Predicate<String> appClassFilter,
                                                    final Collection<String> testClassNames,
                                                    final Collection<String> patchedMethods) throws IOException, InterruptedException {
        final PreludeProfilerArguments arguments = new PreludeProfilerArguments(appClassFilter,
                testClassNames,
                patchedMethods);
        final PreludeProfilerProcess process = new PreludeProfilerProcess(defaultProcessArgs, arguments);
        process.start();
        process.waitToDie();
        final MethodsDom methodsDom = new MethodsDom(".");
        return new PreludeProfilerResults() {
            @Override
            public MethodsDom getMethodsDom() {
                return methodsDom;
            }

            @Override
            public Map<String, Set<Integer>> getMethodCoverageMap() {
                return process.getMethodCoverageMap();
            }

            @Override
            public Set<String> getFailingTests() {
                return process.getFailingTests();
            }
        };
    }
}
