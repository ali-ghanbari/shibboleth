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
import org.pitest.testapi.ResultCollector;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class DefaultResultCollector implements ResultCollector {
    @Override
    public void notifyEnd(Description description, Throwable t) {
        System.out.flush();
        System.err.println();
        t.printStackTrace();
        System.err.println();
        System.err.flush();
    }

    @Override
    public void notifyEnd(Description description) {
        // nothing
    }

    @Override
    public void notifyStart(Description description) {
        final String testName = NameUtils.sanitizeExtendedTestName(description.getName());
        System.out.println("RUNNING: " + testName + "...");
    }

    @Override
    public void notifySkipped(Description description) {
        final String testName = NameUtils.sanitizeExtendedTestName(description.getName());
        System.out.println("SKIPPED: " + testName);
    }

    @Override
    public boolean shouldExit() {
        return false;
    }
}
