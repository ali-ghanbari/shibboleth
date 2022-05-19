package edu.iastate.shibboleth.maven;

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

import edu.iastate.shibboleth.CosineSimilarityCalculator;
import edu.iastate.shibboleth.Patch;
import edu.iastate.shibboleth.PatchScoreTriple;
import edu.iastate.shibboleth.commons.functional.PredicateFactory;
import edu.iastate.shibboleth.commons.misc.NameUtils;
import edu.iastate.shibboleth.profiler.primary.IV;
import edu.iastate.shibboleth.profiler.primary.PairedPrimaryProfilerResults;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.pitest.classpath.ClassPath;
import org.pitest.functional.predicate.Predicate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public abstract class AbstractShibbolethMojo extends AbstractMojo {
    protected File compatibleJREHome;

    protected Predicate<String> appClassFilter;

    protected Predicate<String> testClassFilter;

    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(property = "plugin.artifactMap", readonly = true, required = true)
    private Map<String, Artifact> pluginArtifactMap;

    // -----------------------
    // ---- plugin params ----
    // -----------------------

    @Parameter(property = "targetClasses")
    protected Set<String> targetClasses;

    @Parameter(property = "excludedClasses")
    protected Set<String> excludedClasses;

    @Parameter(property = "excludeTestClasses", defaultValue = "true")
    protected boolean excludeTestClasses;

    @Parameter(property = "includeProductionClasses", defaultValue = "true")
    protected boolean includeProductionClasses;

    @Parameter(property = "targetTests")
    protected Set<String> targetTests;

    @Parameter(property = "excludedTests")
    protected Set<String> excludedTests;

    @Parameter(property = "inputFile", defaultValue = "./input-file.csv")
    protected File inputFile;

    @Parameter(property = "childJVMArgs")
    protected Set<String> childJVMArgs;

    @Parameter(property = "testDryRun", defaultValue = "false")
    protected boolean testDryRun;

    protected void checkAndSanitizeParameters() throws MojoFailureException {
        final String jreHome = System.getProperty("java.home");
        if (jreHome == null) {
            throw new MojoFailureException("JAVA_HOME is not set");
        }
        this.compatibleJREHome = new File(jreHome);
        if (!this.compatibleJREHome.isDirectory()) {
            throw new MojoFailureException("Invalid JAVA_HOME");
        }

        final String groupId = this.project.getGroupId();

        if (this.excludedTests == null) {
            this.excludedTests = Collections.emptySet();
        }
        final Predicate<String> excludedTestFilter = PredicateFactory.orGlobs(this.excludedTests);
        if (this.targetTests == null) {
            this.targetTests = new HashSet<>();
        }
        if (this.targetTests.isEmpty()) {
            this.targetTests.add(String.format("%s*Test", groupId));
            this.targetTests.add(String.format("%s*Tests", groupId));
            this.targetTests.add(String.format("%s*TestCase*", groupId));
        }
        this.testClassFilter = PredicateFactory.orGlobs(this.targetTests);
        this.testClassFilter = PredicateFactory.and(this.testClassFilter, PredicateFactory.not(excludedTestFilter));

        if (this.excludedClasses == null) {
            this.excludedClasses = new HashSet<>();
        }
        if (this.targetClasses == null) {
            this.targetClasses = new HashSet<>();
        }
        Predicate<String> excludedClassesFilter = PredicateFactory.orGlobs(this.excludedClasses);
        if (this.excludeTestClasses) {
            final File testClassesBaseDirectory = new File(this.project.getBuild().getTestOutputDirectory());
            excludedClassesFilter = PredicateFactory.or(excludedClassesFilter, classFileFilter(testClassesBaseDirectory));
        }
        Predicate<String> p = PredicateFactory.orGlobs(this.targetClasses);
        if (this.includeProductionClasses) {
            final File classesBaseDirectory = new File(this.project.getBuild().getOutputDirectory());
            p = PredicateFactory.or(p, classFileFilter(classesBaseDirectory));
        }
        this.appClassFilter = PredicateFactory.and(p, PredicateFactory.not(excludedClassesFilter));

        if (this.childJVMArgs == null) {
            this.childJVMArgs = new HashSet<>();
        }
        if (this.childJVMArgs.isEmpty()) {
            this.childJVMArgs.add("-Xmx16g");
        }
    }

    public static Predicate<String> classFileFilter(final File classesBaseDirectory) {
        final Collection<File> classFiles = FileUtils.listFiles(classesBaseDirectory, new String[] {"class"}, true);
        final Set<String> classes = new HashSet<>();
        for (final File classFile : classFiles) {
            classes.add(NameUtils.getClassName(classFile));
        }
        return PredicateFactory.fromCollection(classes);
    }

    private List<File> getProjectClassPath() {
        final List<File> classPath = new ArrayList<>();
        try {
            for (final Object cpElement : this.project.getTestClasspathElements()) {
                classPath.add(new File((String) cpElement));
            }
        } catch (DependencyResolutionRequiredException e) {
            getLog().warn(e);
        }
        return classPath;
    }

    private List<File> getPluginClassPath() {
        final List<File> classPath = new ArrayList<>();
        for (final Artifact dependency : this.pluginArtifactMap.values()) {
            if (isRelevantDep(dependency)) {
                classPath.add(dependency.getFile());
            }
        }
        return classPath;
    }

    private static boolean isRelevantDep(final Artifact dependency) {
        return dependency.getGroupId().equals("edu.iastate")
                && dependency.getArtifactId().equals("shibboleth-maven-plugin");
    }

    public ClassPath createClassPath() {
        final List<File> classPathElements = new ArrayList<>();
        classPathElements.addAll(getProjectClassPath());
        classPathElements.addAll(getPluginClassPath());
        return new ClassPath(classPathElements);
    }

    public File getCompatibleJREHome() {
        return this.compatibleJREHome;
    }

    public Predicate<String> getAppClassFilter() {
        return this.appClassFilter;
    }

    public Predicate<String> getTestClassFilter() {
        return this.testClassFilter;
    }

    public MavenProject getProject() {
        return this.project;
    }

    public Map<String, Artifact> getPluginArtifactMap() {
        return this.pluginArtifactMap;
    }

    public Set<String> getTargetClasses() {
        return this.targetClasses;
    }

    public Set<String> getExcludedClasses() {
        return this.excludedClasses;
    }

    public boolean isExcludeTestClasses() {
        return this.excludeTestClasses;
    }

    public boolean isIncludeProductionClasses() {
        return this.includeProductionClasses;
    }

    public Set<String> getTargetTests() {
        return this.targetTests;
    }

    public Set<String> getExcludedTests() {
        return this.excludedTests;
    }

    public File getInputFile() {
        return this.inputFile;
    }

    public Set<String> getChildJVMArgs() {
        return this.childJVMArgs;
    }

    public boolean isTestDryRun() {
        return testDryRun;
    }

    public static LinkedHashMap<Patch, PatchScoreTriple> buildScores(final Map<Patch, Double> staticInfo,
                                                                     final Map<Patch, PairedPrimaryProfilerResults> dynamicInfo) {
        final LinkedHashMap<Patch, PatchScoreTriple> res = new LinkedHashMap<>();
        for (final Map.Entry<Patch, Double> entry : staticInfo.entrySet()) {
            final Patch patch = entry.getKey();
            final double ts = entry.getValue();
            final PairedPrimaryProfilerResults profilerResults = dynamicInfo.get(patch);
            final double bc = profilerResults.afterPatching.getCoveredBranches()
                    - profilerResults.beforePatching.getCoveredBranches();
            final double scs = takeAvgSCS(profilerResults.beforePatching.getInstructionVectorMap(),
                    profilerResults.afterPatching.getInstructionVectorMap());
            res.put(patch, new PatchScoreTriple(ts, scs, bc));
        }
        return res;
    }

    private static double takeAvgSCS(final Map<String, IV> profilerResultsBefore,
                                     final Map<String, IV> profilerResultsAfter) {
        if (profilerResultsBefore == null || profilerResultsAfter == null) {
            return 0D;
        }
        if (profilerResultsBefore.isEmpty() || profilerResultsAfter.isEmpty()) {
            return 1D;
        }
        double n = 0D;
        double cosineSummation = 0D;
        final CosineSimilarityCalculator calculator = new CosineSimilarityCalculator();
        for (final Map.Entry<String, IV> entry : profilerResultsBefore.entrySet()) {
            final String testName = entry.getKey();
            cosineSummation += calculator.calculateSimDist(entry.getValue().getVector(),
                    profilerResultsAfter.get(testName).getVector());
            n = n + 1D;
        }
        return cosineSummation / n;
    }
}
