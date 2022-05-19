package edu.iastate.shibboleth.commons.collections;

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

import org.pitest.functional.F;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public final class Printer {
    private Printer() { }

    public static <T> String join(final Iterable<T> elements,
                                  final F<T, String> mapper,
                                  final String delimiter) {
        final StringBuilder sb = new StringBuilder();
        for (final T element : elements) {
            sb.append(delimiter).append(mapper.apply(element));
        }
        return sb.substring(delimiter.length());
    }

    public static <T> String join(final T[] elements,
                                  final F<T, String> mapper,
                                  final String delimiter) {
        final StringBuilder sb = new StringBuilder();
        final int iMax = elements.length - 1;
        if (iMax >= 0) {
            for (int i = 0; ; i++) {
                sb.append(mapper.apply(elements[i]));
                if (i == iMax) {
                    return sb.toString();
                }
                sb.append(delimiter);
            }
        }
        return "";
    }
}
