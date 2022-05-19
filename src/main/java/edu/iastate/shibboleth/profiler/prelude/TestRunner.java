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

import edu.iastate.shibboleth.commons.process.ProcessUtils;
import edu.iastate.shibboleth.commons.testing.junit.runner.DefaultResultCollector;
import edu.iastate.shibboleth.commons.testing.junit.runner.JUnitRunner;
import org.pitest.process.ProcessArgs;
import org.pitest.util.ExitCode;
import org.pitest.util.SafeDataInputStream;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class TestRunner {
    private TestRunner() { }

    public static void main(String[] args) {
        System.out.println("Test runner is HERE!");
        final int port = Integer.parseInt(args[0]);
        Socket socket = null;
        try {
            socket = new Socket("localhost", port);

            final SafeDataInputStream dis = new SafeDataInputStream(socket.getInputStream());

            final PreludeProfilerArguments arguments = dis.read(PreludeProfilerArguments.class);

            final PreludeProfilerReporter reporter = new PreludeProfilerReporter(socket.getOutputStream());

            final DefaultResultCollector resultCollector = new DefaultResultCollector();
            final JUnitRunner runner = new JUnitRunner(arguments.testClassNames, resultCollector);
            final long startTS = System.currentTimeMillis();
            runner.run();
            final long endTS = System.currentTimeMillis();
            System.out.println();
            System.out.printf("[Test Runner] Ran %d tests in %d ms",
                    runner.getTestUnits().size(),
                    endTS - startTS);
            System.out.println();
            System.out.println();

            System.out.println("Test Runner is DONE!");
            reporter.done(ExitCode.OK);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.out);
            System.out.println("WARNING: Error while running tests!");
        } finally {
            ProcessUtils.safelyCloseSocket(socket);
        }
    }

    public static void runTests(final ProcessArgs defaultProcessArgs, final Collection<String> testClassNames) throws IOException, InterruptedException {
        final PreludeProfilerArguments arguments =
                new PreludeProfilerArguments(null, testClassNames, null);
        final TestRunnerProcess process = new TestRunnerProcess(defaultProcessArgs, arguments);
        process.start();
        process.waitToDie();
    }
}
