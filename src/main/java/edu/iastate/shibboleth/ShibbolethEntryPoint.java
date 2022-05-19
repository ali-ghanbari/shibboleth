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

import edu.iastate.shibboleth.commons.classpath.ClassPathUtils;
import edu.iastate.shibboleth.commons.functional.SerializablePredicate;
import edu.iastate.shibboleth.commons.misc.NameUtils;
import edu.iastate.shibboleth.commons.process.LoggerUtils;
import edu.iastate.shibboleth.commons.relational.MethodsDom;
import edu.iastate.shibboleth.maven.AbstractShibbolethMojo;
import edu.iastate.shibboleth.profiler.prelude.PreludeProfiler;
import edu.iastate.shibboleth.profiler.prelude.PreludeProfilerResults;
import edu.iastate.shibboleth.profiler.prelude.TestRunner;
import edu.iastate.shibboleth.profiler.primary.PairedPrimaryProfilerResults;
import edu.iastate.shibboleth.profiler.primary.PrimaryProfiler;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassInfo;
import org.pitest.classpath.ClassFilter;
import org.pitest.classpath.ClassPath;
import org.pitest.classpath.CodeSource;
import org.pitest.classpath.PathFilter;
import org.pitest.classpath.ProjectClassPaths;
import org.pitest.functional.predicate.Predicate;
import org.pitest.functional.prelude.Prelude;
import org.pitest.mutationtest.config.DefaultCodePathPredicate;
import org.pitest.mutationtest.config.DefaultDependencyPathPredicate;
import org.pitest.mutationtest.tooling.JarCreatingJarFinder;
import org.pitest.mutationtest.tooling.KnownLocationJavaAgentFinder;
import org.pitest.process.JavaAgent;
import org.pitest.process.JavaExecutableLocator;
import org.pitest.process.KnownLocationJavaExecutableLocator;
import org.pitest.process.LaunchOptions;
import org.pitest.process.ProcessArgs;
import org.pitest.testapi.TestUnit;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class ShibbolethEntryPoint {
    private static final CSVFormat CSV_FORMAT;

    static {
        CSV_FORMAT = CSVFormat.DEFAULT.withRecordSeparator(System.lineSeparator());
    }

    private final Log log;

    private final File compatibleJREHome;

    private final ClassPath classPath;

    private final ClassByteArraySource byteArraySource;

    private final Predicate<String> appClassFilter;

    private final Predicate<String> testClassFilter;

    private final Set<Patch> patches;

    private final List<String> childJVMArgs;

    private final File projectBaseDirectory;

    private final File buildDirectory;

    private final boolean testsDryRun;

    public ShibbolethEntryPoint(final File compatibleJREHome,
                                final ClassPath classPath,
                                final Predicate<String> appClassFilter,
                                final Predicate<String> testClassFilter,
                                final Collection<String> childJVMArgs,
                                final File inputCSVFile,
                                final File projectBaseDirectory,
                                final File buildDirectory,
                                final boolean testsDryRun) {
        this.log = new SystemStreamLog();
        this.compatibleJREHome = compatibleJREHome;
        this.classPath = classPath;
        this.byteArraySource = ClassPathUtils.createClassByteArraySource(this.classPath);
        this.appClassFilter = appClassFilter;
        this.testClassFilter = testClassFilter;
        this.patches = loadPatchesFromFile(inputCSVFile);
        this.childJVMArgs = new ArrayList<>(childJVMArgs);
        this.projectBaseDirectory = projectBaseDirectory;
        this.buildDirectory = buildDirectory;
        this.testsDryRun = testsDryRun;
    }

    public ShibbolethEntryPoint(final AbstractShibbolethMojo mojo) {
        this.log = mojo.getLog();
        this.patches = loadPatchesFromFile(mojo.getInputFile());
        this.compatibleJREHome = mojo.getCompatibleJREHome();
        this.classPath = mojo.createClassPath();
        this.byteArraySource = ClassPathUtils.createClassByteArraySource(this.classPath);
        this.appClassFilter = mojo.getAppClassFilter();
        this.testClassFilter = mojo.getTestClassFilter();
        this.childJVMArgs = new ArrayList<>(mojo.getChildJVMArgs());
        this.projectBaseDirectory = mojo.getProject().getBasedir();
        this.buildDirectory = new File(mojo.getProject().getBuild().getOutputDirectory());
        this.testsDryRun = mojo.isTestDryRun();
    }

    private Set<Patch> loadPatchesFromFile(final File patchesListFile) {
        final Set<Patch> patches = new HashSet<>();
        try (final Reader csvReader = new FileReader(patchesListFile);
             final CSVParser csvParser = new CSVParser(csvReader, CSV_FORMAT)) {
            for (final CSVRecord record : csvParser.getRecords()) {
                final String patchId = record.get(0);
                final String[] patchedMethods = record.get(2).split(";");
                final String[] patchedClassFileNames = record.get(3).split(";");
                patches.add(new Patch(patchId, patchedClassFileNames, patchedMethods));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage(), e.getCause());
        }
        return patches;
    }

    public Map<Patch, PairedPrimaryProfilerResults> start() throws MojoExecutionException {
        try {
            return start0();
        } catch (final Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }
    }

    private Map<Patch, PairedPrimaryProfilerResults> start0() throws Exception {
        final ProcessArgs defaultProcessArgs = getDefaultProcessArgs();

        final CoverageInformation covInfo;
        final File iflResultsDir = new File(this.projectBaseDirectory, "intelliFL");
        if (iflResultsDir.isDirectory()) {
            this.log.info("Loading intelliFL Results...");
            covInfo = IntelliFLResult.load(iflResultsDir);
            this.log.info("Loaded intelliFL Results.");
        } else {
            if (this.testsDryRun) {
                TestRunner.runTests(getDefaultProcessArgs(), retrieveTestClassNames());
            }
            final PreludeProfilerResults profilerResults =
                PreludeProfiler.runPrelude(getDefaultProcessArgs(),
                        this.appClassFilter,
                        retrieveTestClassNames(),
                        this.getPatchedMethods());

            final MethodsDom methodsDom = profilerResults.getMethodsDom();

            final Set<String> passingTests = new HashSet<>();
            final Map<String, Set<String>> coverageMapInverse = new HashMap<>();

            for (final Map.Entry<String, Set<Integer>> ent : profilerResults.getMethodCoverageMap().entrySet()) {
                final String testName = ent.getKey();
                if (!profilerResults.getFailingTests().contains(testName)) {
                    passingTests.add(testName);
                }
                for (final int methodIndex : ent.getValue()) {
                    final String methodName = methodsDom.get(methodIndex);
                    coverageMapInverse.computeIfAbsent(methodName, __ -> new HashSet<>()).add((testName));
                }
            }

            covInfo = new CoverageInformation() {

                @Override
                public Set<String> getCoveringPassingTests(String methodName) {
                    if (!coverageMapInverse.containsKey(methodName)) {
                        return Collections.emptySet();
                    }
                    final Set<String> coveringTests = new HashSet<>(coverageMapInverse.get(methodName));
                    coveringTests.removeIf(s -> !passingTests.contains(s));
                    return coveringTests;
                }

                @Override
                public Set<String> getCoveringPassingTests(Collection<String> methods) {
                    final Set<String> coveringTests = new HashSet<>();
                    for (final String methodName : methods) {
                        final Set<String> set = coverageMapInverse.get(methodName);
                        if (set == null) {
                            continue;
                        }
                        coveringTests.addAll(set);
                    }
                    coveringTests.removeIf(s -> !passingTests.contains(s));
                    return coveringTests;
                }

                @Override
                public Set<String> getCoveringFailingTests(String methodName) {
                    if (!coverageMapInverse.containsKey(methodName)) {
                        return Collections.emptySet();
                    }
                    final Set<String> coveringTests = new HashSet<>(coverageMapInverse.get(methodName));
                    coveringTests.removeIf(s -> !profilerResults.getFailingTests().contains(s));
                    return coveringTests;
                }

                @Override
                public Set<String> getCoveringFailingTests(Collection<String> methods) {
                    final Set<String> coveringTests = new HashSet<>();
                    for (final String methodName : methods) {
                        final Set<String> set = coverageMapInverse.get(methodName);
                        if (set == null) {
                            continue;
                        }
                        coveringTests.addAll(set);
                    }
                    coveringTests.removeIf(s -> !profilerResults.getFailingTests().contains(s));
                    return coveringTests;
                }
            };
        }

        final Map<Patch, PairedPrimaryProfilerResults> result = new HashMap<>();

        for (final Patch patch : this.patches) {
            final Set<String> coveringPassingTest = covInfo.getCoveringPassingTests(patch.getPatchedMethods());
            final PairedPrimaryProfilerResults resultsPair = PrimaryProfiler.run(defaultProcessArgs,
                    this.appClassFilter,
                    coveringPassingTest,
                    this.buildDirectory,
                    this.projectBaseDirectory,
                    patch);
            result.put(patch, resultsPair);
        }

        return result;
    }

    private static Predicate<TestUnit> testNameSetToTestUnitPredicate(final Set<String> tests) {
        return new SerializablePredicate<TestUnit>() {
            @Override
            public Boolean apply(final TestUnit testUnit) {
                String testName = testUnit.getDescription().getName();
                testName = NameUtils.sanitizeExtendedTestName(testName);
                return tests.contains(testName);
            }
        };
    }

    private static class CodeCoverage implements Comparable<CodeCoverage> {
        private final double lcAbsDiff;

        private final double bcAbsDiff;

        public CodeCoverage(final int coveredLinesBefore,
                            final int totalLinesBefore,
                            final int coveredBranchesBefore,
                            final int totalBranchesBefore,
                            final int coveredLinesAfter,
                            final int totalLinesAfter,
                            final int coveredBranchesAfter,
                            final int totalBranchesAfter) {
            final double lcBefore = getRatio(coveredLinesBefore, totalLinesBefore);
            final double lcAfter = getRatio(coveredLinesAfter, totalLinesAfter);
            final double bcBefore = getRatio(coveredBranchesBefore, totalBranchesBefore);
            final double bcAfter = getRatio(coveredBranchesAfter, totalBranchesAfter);
            this.lcAbsDiff = Math.abs(lcAfter - lcBefore);
            this.bcAbsDiff = Math.abs(bcAfter - bcBefore);
        }

        private double getRatio(final int numerator,
                                final int denominator) {
            if (denominator == 0) {
                return 0D;
            }
            if (numerator >= denominator) {
                return 1D;
            }
            return ((double) numerator) / ((double) denominator);
        }

        @Override
        public int compareTo(final CodeCoverage that) {
            final int r = Double.compare(that.bcAbsDiff, this.bcAbsDiff);
            if (r == 0) {
                return Double.compare(that.lcAbsDiff, this.lcAbsDiff);
            }
            return r;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CodeCoverage)) {
                return false;
            }
            final CodeCoverage that = (CodeCoverage) o;
            return Double.compare(that.lcAbsDiff, this.lcAbsDiff) == 0
                    && Double.compare(that.bcAbsDiff, this.bcAbsDiff) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.lcAbsDiff, this.bcAbsDiff);
        }
    }

    private Set<String> getPatchedMethods() {
        final Set<String> patchedMethods = new HashSet<>();
        for (final Patch patch : this.patches) {
            patchedMethods.addAll(patch.getPatchedMethods());
        }
        return patchedMethods;
    }

    private CoveringTests getCoveringTests(final Map<String, Set<Integer>> map,
                                           final Set<String> failingTests,
                                           final int methodIndex) {
        final CoveringTests coveringTests = new CoveringTests();
        for (final Map.Entry<String, Set<Integer>> entry : map.entrySet()) {
            if (entry.getValue().contains(methodIndex)) {
                final String testName = entry.getKey();
                if (failingTests.contains(testName)) {
                    coveringTests.failingTests.add(testName);
                } else {
                    coveringTests.passingTests.add(testName);
                }
            }
        }
        return coveringTests;
    }

    private static class CoveringTests {
        final Set<String> passingTests;

        final Set<String> failingTests;

        public CoveringTests() {
            this.passingTests = new HashSet<>();
            this.failingTests = new HashSet<>();
        }
    }

    private Set<String> retrieveTestClassNames() {
        final ProjectClassPaths pcp = new ProjectClassPaths(this.classPath, defaultClassFilter(), defaultPathFilter());
        final CodeSource codeSource = new CodeSource(pcp);
        final Set<String> testClassNames = new HashSet<>();
        for (final ClassInfo classInfo : codeSource.getTests()) {
            testClassNames.add(classInfo.getName().asJavaName());
        }
        return testClassNames;
    }

    private static PathFilter defaultPathFilter() {
        return new PathFilter(new DefaultCodePathPredicate(),
                Prelude.not(new DefaultDependencyPathPredicate()));
    }

    private ClassFilter defaultClassFilter() {
        return new ClassFilter(this.testClassFilter, this.appClassFilter);
    }

    private ProcessArgs getDefaultProcessArgs() {
        final LaunchOptions defaultLaunchOptions = new LaunchOptions(getJavaAgent(),
                getDefaultJavaExecutableLocator(),
                this.childJVMArgs,
                Collections.<String, String>emptyMap());
        return ProcessArgs.withClassPath(this.classPath)
                .andLaunchOptions(defaultLaunchOptions)
                .andStderr(LoggerUtils.err())
                .andStdout(LoggerUtils.out());
    }

    private JavaExecutableLocator getDefaultJavaExecutableLocator() {
        final File javaFile = FileUtils.getFile(this.compatibleJREHome, "bin", "java");
        return new KnownLocationJavaExecutableLocator(javaFile.getAbsolutePath());
    }

    private JavaAgent getJavaAgent() {
        final String jarLocation = (new JarCreatingJarFinder(this.byteArraySource))
                .getJarLocation()
                .value();
        return new KnownLocationJavaAgentFinder(jarLocation);
    }

    public Set<Patch> getPatches() {
        return this.patches;
    }

    public File getProjectBaseDirectory() {
        return this.projectBaseDirectory;
    }

    public Log getLog() {
        return this.log;
    }
}
