package edu.iastate.shibboleth.profiler.primary;

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

import edu.iastate.shibboleth.Patch;
import edu.iastate.shibboleth.commons.misc.NameUtils;
import edu.iastate.shibboleth.commons.process.ProcessUtils;
import edu.iastate.shibboleth.commons.testing.junit.runner.JUnitRunner;
import edu.iastate.shibboleth.commons.testing.junit.runner.RecordingResultCollector;
import edu.iastate.shibboleth.constants.Params;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.runner.manipulation.Filter;
import org.pitest.boot.HotSwapAgent;
import org.pitest.classinfo.CachingByteArraySource;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.functional.Option;
import org.pitest.functional.predicate.Predicate;
import org.pitest.junit.DescriptionFilter;
import org.pitest.junit.adapter.AdaptedJUnitTestUnit;
import org.pitest.process.ProcessArgs;
import org.pitest.testapi.Description;
import org.pitest.testapi.ResultCollector;
import org.pitest.testapi.TestUnit;
import org.pitest.util.ExitCode;
import org.pitest.util.IsolationUtils;
import org.pitest.util.SafeDataInputStream;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.runner.Description.createTestDescription;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class PrimaryProfiler {
    public static void main(String[] args) {
        System.out.println("Primary Profiler is HERE!");
        Socket socket = null;
        try {
            final int port = Integer.parseInt(args[0]);
            socket= new Socket("localhost", port);

            final SafeDataInputStream dis = new SafeDataInputStream(socket.getInputStream());

            final PrimaryProfilerArguments arguments = dis.read(PrimaryProfilerArguments.class);

            final ClassLoader contextClassLoader = IsolationUtils.getContextClassLoader();
            ClassByteArraySource byteArraySource = new ClassloaderByteArraySource(contextClassLoader);
            byteArraySource = new CachingByteArraySource(byteArraySource, Params.BYTECODE_CLASS_CACHE_SIZE);

            final ClassFileTransformer transformer =
                    new PrimaryProfilerTransformer(byteArraySource, arguments.appClassFilter, arguments.patchedMethods);
            HotSwapAgent.addTransformer(transformer);
            final RecordingResultCollector resultCollector = new RecordingResultCollector();
            final List<TestUnit> coveringTestUnits = testNameToTestUnit(arguments.coveringTests);
            createDecoratedRunner(coveringTestUnits, resultCollector).run();

            // finalizing & reporting the results
            final PrimaryProfilerReporter reporter = new PrimaryProfilerReporter(socket.getOutputStream());
            reporter.reportInsnVectorMap(IVRecorder.getInstructionVectorMap());
            reporter.reportCoveredBranchesCount(BCRecorder.getCoveredBranchesCount());
            System.out.println("Primary Profiling is DONE!");
            reporter.done(ExitCode.OK);
        } catch (final Throwable t) {
            t.printStackTrace(System.out);
            System.out.println("Error in profiler");
        } finally {
            ProcessUtils.safelyCloseSocket(socket);
        }
    }

    private static JUnitRunner createDecoratedRunner(final Collection<TestUnit> testUnits,
                                                     final ResultCollector resultCollector) {
        final List<TestUnit> decoratedTestUnits = new LinkedList<>();
        for (final TestUnit testUnit : testUnits) {
            decoratedTestUnits.add(new TestUnit() {
                @Override
                public void execute(ResultCollector rc) {
                    String testName = testUnit.getDescription().getName();
                    testName = NameUtils.sanitizeExtendedTestName(testName);
                    IVRecorder.setCurrentTest(testName);
                    BCRecorder.addTestUnit(testName);
                    testUnit.execute(rc);
                }

                @Override
                public Description getDescription() {
                    return testUnit.getDescription();
                }
            });
        }
        return new JUnitRunner(decoratedTestUnits, resultCollector);
    }

    private static List<TestUnit> testNameToTestUnit(final Collection<String> testCaseNames) throws Exception {
        final List<TestUnit> res = new LinkedList<>();
        for (final String testCaseName : testCaseNames) {
            final Pair<String, String> methodNameParts = NameUtils.decomposeMethodName(NameUtils.sanitizeExtendedTestName(testCaseName));
            final Class<?> testSuite = Class.forName(methodNameParts.getLeft());
            Method testCase = null;
            for (final Method method : testSuite.getMethods()) {
                final int mod = method.getModifiers();
                if (Modifier.isAbstract(mod) || Modifier.isNative(mod) || !Modifier.isPublic(mod)) {
                    continue;
                }
                if (method.getName().equals(methodNameParts.getRight())) {
                    testCase = method;
                    break;
                }
            }
            if (testCase == null) {
                throw new IllegalStateException("not found test method " + methodNameParts.getRight());
            }
            final Filter filter = DescriptionFilter.matchMethodDescription(createTestDescription(testSuite,
                    testCase.getName(),
                    testCase.getDeclaredAnnotations()));
            res.add(new AdaptedJUnitTestUnit(testSuite, Option.some(filter)));
        }
        return res;
    }

    private static PrimaryProfilerResults runProfiler(final ProcessArgs processArgs,
                                                      final Predicate<String> appClassFilter,
                                                      final Collection<String> patchedMethods,
                                                      final Collection<String> coveringTests) throws Exception {
        final PrimaryProfilerArguments arguments =
                new PrimaryProfilerArguments(appClassFilter, coveringTests, patchedMethods);
        final PrimaryProfilerProcess process = new PrimaryProfilerProcess(processArgs, arguments);
        process.start();
        process.waitToDie();
        return new PrimaryProfilerResults() {
            @Override
            public Map<String, IV> getInstructionVectorMap() {
                return process.getIVMap();
            }

            @Override
            public int getCoveredBranches() {
                return process.getCoveredBranchesCount();
            }
        };
    }

    public static PairedPrimaryProfilerResults run(final ProcessArgs processArgs,
                                                   final Predicate<String> appClassFilter,
                                                   final Collection<String> coveringTests,
                                                   final File buildDirectory,
                                                   final File backupDirectory,
                                                   final Patch patch) throws Exception {
        final PrimaryProfilerResults beforePatching = runProfiler(processArgs,
                appClassFilter,
                patch.getPatchedMethods(),
                coveringTests);
        patch.install(buildDirectory, backupDirectory);
        final PrimaryProfilerResults afterPatching = runProfiler(processArgs,
                appClassFilter,
                patch.getPatchedMethods(),
                coveringTests);
        patch.uninstall(buildDirectory, backupDirectory);
        return new PairedPrimaryProfilerResults(beforePatching, afterPatching);
    }
}
