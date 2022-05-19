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

import org.pitest.process.ProcessArgs;
import org.pitest.process.WrappingProcess;
import org.pitest.util.ExitCode;
import org.pitest.util.SocketFinder;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class TestRunnerProcess {
    private final WrappingProcess process;

    private final PreludeProfilerCommunicationThread communicationThread;

    TestRunnerProcess(final ProcessArgs processArgs,
                      final PreludeProfilerArguments arguments) {
        this((new SocketFinder()).getNextAvailableServerSocket(), processArgs, arguments);
    }

    private TestRunnerProcess(final ServerSocket socket,
                              final ProcessArgs processArgs,
                              final PreludeProfilerArguments arguments) {
        this.process = new WrappingProcess(socket.getLocalPort(), processArgs, TestRunner.class);
        this.communicationThread = new PreludeProfilerCommunicationThread(socket, arguments);
    }

    void start() throws IOException, InterruptedException {
        this.communicationThread.start();
        this.process.start();
    }

    ExitCode waitToDie() {
        try {
            return this.communicationThread.waitToFinish();
        } finally {
            this.process.destroy();
        }
    }
}
