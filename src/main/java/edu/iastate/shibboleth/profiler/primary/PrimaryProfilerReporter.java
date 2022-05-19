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

import edu.iastate.shibboleth.constants.ControlId;
import edu.iastate.shibboleth.commons.process.ChildProcessReporter;

import java.io.OutputStream;
import java.util.HashMap;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
class PrimaryProfilerReporter extends ChildProcessReporter {
    public PrimaryProfilerReporter(OutputStream os) {
        super(os);
    }

    public synchronized void reportInsnVectorMap(final HashMap<String, IV> insnVectorMap) {
        this.dos.writeByte(ControlId.REPORT_RECORDED_INSN_VECTOR_MAP);
        this.dos.write(insnVectorMap);
        this.dos.flush();
    }

    public synchronized void reportCoveredBranchesCount(final int coveredBranchesCount) {
        this.dos.writeByte(ControlId.REPORT_COVERED_BRANCHES_COUNT);
        this.dos.writeInt(coveredBranchesCount);
        this.dos.flush();
    }
}
