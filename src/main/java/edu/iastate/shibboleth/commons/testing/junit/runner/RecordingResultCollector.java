package edu.iastate.shibboleth.commons.testing.junit.runner;

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
import org.pitest.testapi.Description;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class RecordingResultCollector extends DefaultResultCollector {
    private final List<String> failingTestNames;

    public RecordingResultCollector(final List<String> failingTestNames) {
        this.failingTestNames = failingTestNames;
    }

    public RecordingResultCollector() {
        this(new LinkedList<>());
    }

    @Override
    public void notifyEnd(Description description, Throwable t) {
        final String failingTestName = NameUtils.sanitizeExtendedTestName(description.getName());
        this.failingTestNames.add(failingTestName);
        super.notifyEnd(description, t);
    }

    public List<String> getFailingTestNames() {
        return failingTestNames;
    }
}
