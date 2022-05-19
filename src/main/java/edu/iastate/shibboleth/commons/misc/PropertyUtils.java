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

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class PropertyUtils {
    private PropertyUtils() { }

    public static int getIntProperty(final String property, final int defaultVal) {
        return Integer.parseInt(System.getProperty(property, String.valueOf(defaultVal)));
    }

    public static long getLongProperty(final String property, final long defaultVal) {
        return Long.parseLong(System.getProperty(property, String.valueOf(defaultVal)));
    }
}
