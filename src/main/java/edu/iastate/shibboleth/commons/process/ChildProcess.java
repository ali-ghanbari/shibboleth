package edu.iastate.shibboleth.commons.process;

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

import org.pitest.process.WrappingProcess;
import org.pitest.util.CommunicationThread;
import org.pitest.util.ExitCode;

import java.io.IOException;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public abstract class ChildProcess {
    protected final WrappingProcess process;

    protected final CommunicationThread communicationThread;

    protected ChildProcess(final WrappingProcess process,
                           final CommunicationThread communicationThread) {
        this.process = process;
        this.communicationThread = communicationThread;
    }

    public void start() throws IOException, InterruptedException {
        this.communicationThread.start();
        this.process.start();
    }

    public ExitCode waitToDie() {
        try {
            return this.communicationThread.waitToFinish();
        } finally {
            this.process.destroy();
        }
    }
}
