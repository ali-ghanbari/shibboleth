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

import edu.iastate.shibboleth.constants.Params;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class MethodCoverageRecorder {
    private static final long[] METHODS_BITMAP;

    public static final Map<String, long[]> COVERAGE_MAP; // test suite/case name --> set of covered methods

    public static long[] coveredMethods;

    static {
        METHODS_BITMAP = new long[Params.UNIT_SIZE];
        Arrays.fill(METHODS_BITMAP, 0L);
        COVERAGE_MAP = new HashMap<>();
    }

    private MethodCoverageRecorder() { }

    static void addTestUnit(final String testUnitName) {
        coveredMethods = METHODS_BITMAP.clone();
        COVERAGE_MAP.put(testUnitName, coveredMethods);
    }

    public static void recordMethod(final int methodIndex) {
        if (coveredMethods == null) {
            /*some programs, e.g., JacksonDatabind from Defects4J, use user-defined annotations from
            production code in their test classes, therefore "recordMethod" gets called before any
            "addTestUnit" and we get NPE in "recordMethod"! this is to circumvent such situation.*/
            return;
        }
        final int unitIndex = methodIndex / Long.SIZE;
        final int bitIndex = methodIndex % Long.SIZE;
        coveredMethods[unitIndex] |= 1L << bitIndex;
    }

    static HashMap<String, Set<Integer>> getCoverageMap() {
        final HashMap<String, Set<Integer>> coverageMap = new HashMap<>();
        for (final Map.Entry<String, long[]> entry : COVERAGE_MAP.entrySet()) {
            final String testUnitName = entry.getKey();
            final Set<Integer> set = new HashSet<>();
            final long[] methodsBitmap = entry.getValue();
            int methodIndex = 0;
            for (long unit : methodsBitmap) {
                for (int bitIndex = 0; bitIndex < Long.SIZE; bitIndex++) {
                    if ((unit & 1L) != 0L) {
                        set.add(methodIndex);
                    }
                    unit >>>= 1;
                    methodIndex++;
                }
            }
            coverageMap.put(testUnitName, set);
        }
        return coverageMap;
    }
}
