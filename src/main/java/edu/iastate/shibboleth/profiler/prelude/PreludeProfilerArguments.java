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

import org.apache.commons.lang3.Validate;
import org.pitest.functional.predicate.Predicate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Arguments passed to the "prelude" profiler process which is responsible for
 * recording field accesses of the program.
 * !Internal use only!
 *
 * @author Ali Ghanbari (alig@iastate.edu)
 */
class PreludeProfilerArguments implements Serializable {
    private static final long serialVersionUID = 1L;

    final Predicate<String> appClassFilter;

    final Set<String> testClassNames;

    final Set<String> patchedMethods;

    public PreludeProfilerArguments(final Predicate<String> appClassFilter,
                                    final Collection<String> testClassNames,
                                    final Collection<String> patchedMethods) {
        if (appClassFilter != null) {
            Validate.isInstanceOf(Serializable.class, appClassFilter);
        }
        this.appClassFilter = appClassFilter;
        this.testClassNames = new HashSet<>(testClassNames);
        if (patchedMethods != null) {
            this.patchedMethods = new HashSet<>(patchedMethods);
        } else {
            this.patchedMethods = Collections.emptySet();
        }
    }
}
