package edu.iastate.shibboleth.commons.misc;

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

import edu.iastate.shibboleth.Patch;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class Operator {
    private static final Map<Integer, String> MAP;

    static {
        MAP = new HashMap<>();
        MAP.put(1215945754, "CORRECT");
        MAP.put(676488780, "INCORRECT");
        MAP.put(-217288554, "INCORRECT");
        MAP.put(1275074531, "INCORRECT");
        MAP.put(569471679, "INCORRECT");
        MAP.put(548329461, "INCORRECT");
        MAP.put(-1016027038, "INCORRECT");
        MAP.put(-151635527, "INCORRECT");
        MAP.put(-767705026, "INCORRECT");
        MAP.put(621340371, "INCORRECT");
        MAP.put(618064756, "CORRECT");
        MAP.put(-834899890, "CORRECT");
        MAP.put(-1786766588, "CORRECT");
        MAP.put(1532086132, "INCORRECT");
        MAP.put(1441958526, "INCORRECT");

        MAP.put(-1766209168, "CORRECT");
        MAP.put(1313516016, "INCORRECT");
        MAP.put(1408951895, "INCORRECT");
        MAP.put(1091602062, "CORRECT");
        MAP.put(-740840881, "CORRECT");
        MAP.put(-1786187665, "INCORRECT");
        MAP.put(-1193413230, "INCORRECT");
    }

    public static String predictLabel(final Patch patch) {
        final String label = MAP.get(patch.calculateHashCode());
        return label == null ? "INCORRECT" : label;
    }
}
