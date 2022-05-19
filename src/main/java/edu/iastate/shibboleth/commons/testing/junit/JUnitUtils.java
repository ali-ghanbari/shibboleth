package edu.iastate.shibboleth.commons.testing.junit;

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
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.pitest.classinfo.ClassName;
import org.pitest.functional.FCollection;
import org.pitest.functional.Option;
import org.pitest.junit.DescriptionFilter;
import org.pitest.junit.adapter.AdaptedJUnitTestUnit;
import org.pitest.testapi.TestUnit;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A versatile, less lame JUnit test case finder!
 * It is compatible with JUnit 3.XX and 4.YY.
 *
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class JUnitUtils {
    private JUnitUtils() { }

    private static final Map<Class<?>, Set<Method>> VISITED;

    static {
        VISITED = new HashMap<>();
    }

    public static List<TestUnit> discoverTestUnits(final Collection<String> classNames) {
        final List<TestUnit> testUnits = new LinkedList<>();
        final Collection<Class<?>> classes =
                FCollection.flatMap(FCollection.map(classNames, ClassName.stringToClassName()),
                        ClassName.nameToClass());
        // find JUnit 4.XX tests
        testUnits.addAll(findJUnit4YYTestUnits(classes));
        testUnits.addAll(findJUnit3XXTestUnits(classes));
        for (final Map.Entry<Class<?>, Set<Method>> entry : VISITED.entrySet()) {
            entry.getValue().clear();
        }
        VISITED.clear();
        testUnits.sort(createTestUnitComparator());
        return testUnits;
    }

    private static Comparator<TestUnit> createTestUnitComparator() {
        return new Comparator<TestUnit>() {
            @Override
            public int compare(final TestUnit tu1, final TestUnit tu2) {
                final String n1 = getTestName(tu1);
                final String n2 = getTestName(tu2);
                return n1.compareTo(n2);
            }

            private String getTestName(final TestUnit tu) {
                final String testName = tu.getDescription().getName();
                return NameUtils.sanitizeExtendedTestName(testName);
            }
        };
    }

    private static boolean shouldAdd(final Class<?> testClass, final Method testMethod) {
        Set<Method> methods = VISITED.computeIfAbsent(testClass, k -> new HashSet<>());
        return methods.add(testMethod);
    }

    private static Collection<? extends TestUnit> findJUnit3XXTestUnits(Collection<Class<?>> classes) {
        final List<TestUnit> testUnits = new LinkedList<>();
        for (final Class<?> clazz : classes) {
            if (isAbstract(clazz)) {
                continue;
            }
            if (isJUnit3XXTestSuite(clazz)) {
                testUnits.addAll(findJUnit3XXTestUnits(clazz));
            }
        }
        return testUnits;
    }

    private static boolean isJUnit3XXTestSuite(Class<?> clazz) {
        do {
            clazz = clazz.getSuperclass();
            if (clazz == TestCase.class) {
                return true;
            }
        } while (clazz != null);
        return false;
    }

    private static Collection<? extends TestUnit> findJUnit3XXTestUnits(final Class<?> testClass) {
        final List<TestUnit> testUnits = new LinkedList<>();
        for (final Method method : testClass.getMethods()) {
            final int mod = method.getModifiers();
            if (Modifier.isAbstract(mod) || Modifier.isNative(mod) || !Modifier.isPublic(mod)) {
                continue;
            }
            if (method.getReturnType() == Void.TYPE
                    && method.getName().startsWith("test")
                    && shouldAdd(testClass, method)) {
                testUnits.add(createTestUnit(testClass, method));
            }
        }
        return testUnits;
    }

    private static Collection<? extends TestUnit> findJUnit4YYTestUnits(Collection<Class<?>> classes) {
        final List<TestUnit> testUnits = new LinkedList<>();
        for (final Class<?> clazz : classes) {
            if (isAbstract(clazz)) {
                continue;
            }
            for (final Method method : clazz.getMethods()) {
                final int mod = method.getModifiers();
                if (Modifier.isAbstract(mod) || Modifier.isNative(mod) || !Modifier.isPublic(mod)) {
                    continue;
                }
                final Test annotation = method.getAnnotation(Test.class);
                if (annotation != null && shouldAdd(clazz, method)) {
                    testUnits.add(createTestUnit(clazz, method));
                }
            }
        }
        return testUnits;
    }

    private static TestUnit createTestUnit(final Class<?> testClass,
                                           final Method testMethod) {
        final Description testDescription = Description.createTestDescription(testClass,
                testMethod.getName(),
                testMethod.getDeclaredAnnotations());
        final Filter filter = DescriptionFilter.matchMethodDescription(testDescription);
        return new AdaptedJUnitTestUnit(testClass, Option.some(filter));
    }

    private static boolean isAbstract(final Class<?> clazz) {
        final int mod = clazz.getModifiers();
        return Modifier.isInterface(mod) || Modifier.isAbstract(mod);
    }
}
