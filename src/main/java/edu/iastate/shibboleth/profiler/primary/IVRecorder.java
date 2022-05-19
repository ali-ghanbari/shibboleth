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

import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public final class IVRecorder {
    private static final int[] IV_TEMPLATE;

    private static final HashMap<String, IV> IV_MAP;

    private static int[] unlimitedIV;

    private static final Object LOCK;

    private static boolean enteredPatchedMethodAtLestOnce;

    static {
        IV_TEMPLATE = new int[256];
        Arrays.fill(IV_TEMPLATE, 0);
        IV_MAP = new HashMap<>();
        LOCK = new Object();
        enteredPatchedMethodAtLestOnce = false; // by default we use limited coverage collection
    }

    private IVRecorder() { }

    public static void setCurrentTest(final String testName) {
        unlimitedIV =  IV_TEMPLATE.clone();
        IV_MAP.put(testName, new IV(unlimitedIV));
    }

    public static void enterPatchedMethod() {
        enteredPatchedMethodAtLestOnce = true;
    }

    public static void recordOpcode(final int opcode) {
        if (enteredPatchedMethodAtLestOnce) {
            synchronized (LOCK) {
                unlimitedIV[opcode]++;
            }
        }
    }

    static HashMap<String, IV> getInstructionVectorMap() {
        return IV_MAP;
    }
}
