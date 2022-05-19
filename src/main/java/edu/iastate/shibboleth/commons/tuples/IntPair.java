package edu.iastate.shibboleth.commons.tuples;

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

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public final class IntPair {
    private IntPair() { }

    public static long pair(final int left, final int right) {
        return (((long) left) << 32) | (right & 0xFFFFFFFFL);
    }

    public static int left(final long pair) {
        return (int) (pair >> 32);
    }

    public static int right(final long pair) {
        return (int) pair;
    }
}
