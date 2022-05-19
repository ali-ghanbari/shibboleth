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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static edu.iastate.shibboleth.constants.Params.UNIT_SIZE;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class BCRecorder {
    public static final Map<String, Set<Long>> COVERAGE_MAP; // test suite/case name --> set of covered branches

    public static Set<Long> coveredBranches;

    static {
        COVERAGE_MAP = new HashMap<>();
    }

    private BCRecorder() { }

    static void addTestUnit(final String testUnitName) {
        coveredBranches = new HashSet<>(UNIT_SIZE);
        COVERAGE_MAP.put(testUnitName, coveredBranches);
    }

    public static void recordCoveredBranch(final long uid) {
        if (coveredBranches == null) {
            /*some of the programs like JacksonDatabind from Defects4J use user-defined annotations from
            production code in their test classes. therefore, "recordCoveredBranch" gets called before any
            "addTestUnit" and we get NPE in this method! this is to circumvent such a situation.*/
            return;
        }
        coveredBranches.add(uid);
    }

    private static Set<Long> flattenValues(final Map<String, Set<Long>> map) {
        final Set<Long> values = new HashSet<>();
        for (final Map.Entry<String, Set<Long>> entry : map.entrySet()) {
            values.addAll(entry.getValue());
        }
        return values;
    }

    static int getCoveredBranchesCount() {
        final Set<Long> coveredBranches = flattenValues(COVERAGE_MAP);
        return coveredBranches.size();
    }
}
