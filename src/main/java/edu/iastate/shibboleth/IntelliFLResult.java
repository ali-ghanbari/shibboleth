package edu.iastate.shibboleth;

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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class IntelliFLResult implements CoverageInformation {
    private final Set<String> passingTests;

    private final Set<String> failingTests;

    private final Map<String, Set<String>> coverageMapInverse; // method name -> covering tests

    IntelliFLResult() {
        this.passingTests = new HashSet<>();
        this.failingTests = new HashSet<>();
        this.coverageMapInverse = new HashMap<>();
    }

    public Set<String> getPassingTests() {
        return this.passingTests;
    }

    public Set<String> getFailingTests() {
        return this.failingTests;
    }

    private static Collection<File> retrieveFiles(File dir) {
        if (!dir.isDirectory() || !dir.getName().equals("intelliFL")) {
            throw new IllegalArgumentException("Missing intelliFL");
        }
        dir = new File(dir, "intelliFL-meth-cov");
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Missing intelliFL-meth-cov");
        }
        return FileUtils.listFiles(dir, new String[] {"gz"}, false);
    }

    public static IntelliFLResult load(final File baseDir) throws IOException {
        final IntelliFLResult result = new IntelliFLResult();
        for (final File coverageFile : retrieveFiles(baseDir)) {
            final Iterator<String> linesIt = FileUtils.readLines(coverageFile, Charset.defaultCharset()).iterator();
            if (!linesIt.hasNext()) {
                System.out.println("Skipping " + coverageFile.getAbsolutePath());
                continue;
            }
            final String firstLine = linesIt.next().trim();
            final int indexOfSpace = firstLine.indexOf(' ');
            final String testName = firstLine.substring(0, indexOfSpace);
            final boolean isFailing = !Boolean.parseBoolean(firstLine.substring(1 + indexOfSpace));
            (isFailing ? result.failingTests : result.passingTests).add(testName);
            while (linesIt.hasNext()) {
                String line = linesIt.next().trim();
                int indexOfColon = line.indexOf(':');
                final String ownerClass = line.substring(0, indexOfColon).replace('/', '.');
                final String clinitMethodFullName = ownerClass + "<clinit>()";
                line = line.substring(1 + indexOfColon);
                indexOfColon = line.indexOf(':');
                final String methodName = line.substring(0, indexOfColon);
                final String descriptor = line.substring(1 + indexOfColon);
                final String methodFullName = NameUtils.constructMethodFullName(ownerClass, methodName, descriptor);
                result.addCoverageFact(methodFullName, testName);
                result.addCoverageFact(clinitMethodFullName, testName);
            }
        }
        return result;
    }

    private void addCoverageFact(final String methodName, final String testName) {
        Set<String> set = this.coverageMapInverse.computeIfAbsent(methodName, k -> new HashSet<>());
        set.add(testName);
    }

    @Override
    public Set<String> getCoveringFailingTests(final String methodName) {
        if (!this.coverageMapInverse.containsKey(methodName)) {
            return Collections.emptySet();
        }
        final Set<String> coveringTests = new HashSet<>(this.coverageMapInverse.get(methodName));
        coveringTests.removeIf(s -> !this.failingTests.contains(s));
        return coveringTests;
    }

    @Override
    public Set<String> getCoveringFailingTests(final Collection<String> methods) {
        final Set<String> coveringTests = new HashSet<>();
        for (final String methodName : methods) {
            final Set<String> set = this.coverageMapInverse.get(methodName);
            if (set == null) {
                continue;
            }
            coveringTests.addAll(set);
        }
        coveringTests.removeIf(s -> !this.failingTests.contains(s));
        return coveringTests;
    }

    @Override
    public Set<String> getCoveringPassingTests(final String methodName) {
        if (!this.coverageMapInverse.containsKey(methodName)) {
            return Collections.emptySet();
        }
        final Set<String> coveringTests = new HashSet<>(this.coverageMapInverse.get(methodName));
        coveringTests.removeIf(s -> !this.passingTests.contains(s));
        return coveringTests;
    }

    @Override
    public Set<String> getCoveringPassingTests(final Collection<String> methods) {
        final Set<String> coveringTests = new HashSet<>();
        for (final String methodName : methods) {
            final Set<String> set = this.coverageMapInverse.get(methodName);
            if (set == null) {
                continue;
            }
            coveringTests.addAll(set);
        }
        coveringTests.removeIf(s -> !this.passingTests.contains(s));
        return coveringTests;
    }

    public Set<String> getCoveringTests(final String methodName) {
        if (!this.coverageMapInverse.containsKey(methodName)) {
            return Collections.emptySet();
        }
        return new HashSet<>(this.coverageMapInverse.get(methodName));
    }

    public double getOchiai(final String methodName) {
        final double ep = getCoveringPassingTests(methodName).size();
        final double ef = getCoveringFailingTests(methodName).size();
        final double nf = this.coverageMapInverse.get(methodName).size() - ef;
        final double denum = Math.sqrt((ef + ep) * (ef + nf));
        return denum > 0D ? ef / denum : 0D;
    }

    public double getOchiai(final Collection<String> methods) {
        if (methods.isEmpty()) {
            return 0D;
        }
        final double n = methods.size();
        double sum = 0D;
        for (final String methodName : methods) {
            sum += getOchiai(methodName);
        }
        return sum / n;
    }
}
