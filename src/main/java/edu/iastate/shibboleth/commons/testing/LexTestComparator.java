package edu.iastate.shibboleth.commons.testing;

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
import org.pitest.testapi.TestUnit;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class LexTestComparator implements TestComparator {
    @Override
    public int compare(final TestUnit tu1, final TestUnit tu2) {
        final String n1 = NameUtils.sanitizeExtendedTestName(tu1.getDescription().getName());
        final String n2 = NameUtils.sanitizeExtendedTestName(tu2.getDescription().getName());
        return n1.compareTo(n2);
    }
}
