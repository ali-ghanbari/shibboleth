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
import org.pitest.testapi.TestUnit;

import java.util.Collection;

/**
 * Test unit filter allows us to selectively run a subset of test cases.
 *
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class TestUnitFilter {
    public static Predicate<TestUnit> all() {
        return testUnit -> Boolean.TRUE;
    }

    public static Predicate<TestUnit> some(final Collection<String> testUnitNames) {
        return testUnit -> {
            final String testName = NameUtils.sanitizeExtendedTestName(testUnit.getDescription().getName());
            return testUnitNames.contains(testName);
        };
    }
}
