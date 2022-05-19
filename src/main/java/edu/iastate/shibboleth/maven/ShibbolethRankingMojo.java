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

import edu.iastate.shibboleth.Patch;
import edu.iastate.shibboleth.PatchScoreTriple;
import edu.iastate.shibboleth.ShibbolethEntryPoint;
import edu.iastate.shibboleth.SimCalculator;
import edu.iastate.shibboleth.profiler.primary.PairedPrimaryProfilerResults;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
@Mojo(name = "rank", requiresDependencyResolution = ResolutionScope.TEST)
public class ShibbolethRankingMojo extends AbstractShibbolethMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        checkAndSanitizeParameters();
        final ShibbolethEntryPoint entryPoint = new ShibbolethEntryPoint(this);
        final Map<Patch, PairedPrimaryProfilerResults> result = entryPoint.start();
        final File patchedSourceFilesBaseDir =
                new File(entryPoint.getProjectBaseDirectory(), "patched-source-files");
        final Map<Patch, Double> tsMap =
                SimCalculator.doTokenAnalysis(entryPoint.getPatches(), patchedSourceFilesBaseDir);
        final Map<Patch, Integer> ranks = rank(buildScores(tsMap, result));
        getLog().info("------------------------------------------");
        ranks.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    System.out.println(entry.getValue() + " " + entry.getKey().getId());
                });
        getLog().info("------------------------------------------");
    }

    public static Map<Patch, Integer> rank(final Map<Patch, PatchScoreTriple> scores) {
        final Map<PatchScoreTriple, List<Patch>> map = new TreeMap<>();
        for (final Map.Entry<Patch, PatchScoreTriple> ent : scores.entrySet()) {
            map.computeIfAbsent(ent.getValue(), __ -> new ArrayList<>()).add(ent.getKey());
        }
        final Map<Patch, Integer> result = new HashMap<>();
        int rank = 0;
        for (final List<Patch> tie : map.values()) {
            rank += tie.size();
            for (final Patch patch : tie) {
                result.put(patch, rank);
            }
        }
        return result;
    }
}
