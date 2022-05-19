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
import org.pitest.functional.predicate.Predicate;
import org.pitest.testapi.Description;
import org.pitest.testapi.ResultCollector;
import org.pitest.util.Log;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
class EarlyExitResultCollector implements ResultCollector {
    protected final ResultCollector child;

    protected boolean hadFailure;

    protected final Predicate<String> failingTestFilter;

    public EarlyExitResultCollector(final ResultCollector child,
                                    final Predicate<String> failingTestFilter) {
        this.child = child;
        this.failingTestFilter = failingTestFilter;
    }

    @Override
    public void notifyEnd(Description description, Throwable t) {
        this.child.notifyEnd(description, t);
        final String failingTestName = NameUtils.sanitizeExtendedTestName(description.getName());
        Log.getLogger().info("******************");
        Log.getLogger().info("" + this.hadFailure);
        Log.getLogger().info("******************");
        this.hadFailure = !this.failingTestFilter.apply(failingTestName);
    }

    @Override
    public void notifyEnd(Description description) {
        this.child.notifyEnd(description);
    }

    @Override
    public void notifyStart(Description description) {
        this.child.notifyStart(description);
    }

    @Override
    public void notifySkipped(Description description) {
        this.child.notifySkipped(description);
    }

    @Override
    public boolean shouldExit() {
        return this.hadFailure;
    }
}
