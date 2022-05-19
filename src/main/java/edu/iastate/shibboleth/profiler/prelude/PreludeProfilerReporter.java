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

import edu.iastate.shibboleth.constants.ControlId;
import org.pitest.util.ExitCode;
import org.pitest.util.SafeDataOutputStream;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A set of methods used for sending information (including status codes) to/from
 * child profiler process.
 * !Internal use only!
 *
 * @author Ali Ghanbari (alig@iastate.edu)
 */
class PreludeProfilerReporter {
    protected final SafeDataOutputStream dos;

    protected PreludeProfilerReporter(OutputStream os) {
        this.dos = new SafeDataOutputStream(os);
    }

    public synchronized void done(final ExitCode exitCode) {
        this.dos.writeByte(ControlId.DONE);
        this.dos.writeInt(exitCode.getCode());
        this.dos.flush();
    }

    public synchronized void reportMethodCoverageMap(final HashMap<String, Set<Integer>> methodCoverageMap) {
        this.dos.writeByte(ControlId.REPORT_METHOD_COVERAGE_MAP);
        this.dos.write(methodCoverageMap);
        this.dos.flush();
    }

    public synchronized void reportFailingTests(final HashSet<String> failingTestNames) {
        this.dos.writeByte(ControlId.REPORT_FAILING_TESTS);
        this.dos.write(failingTestNames);
        this.dos.flush();
    }
}
