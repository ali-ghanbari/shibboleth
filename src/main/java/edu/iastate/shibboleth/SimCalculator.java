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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class SimCalculator {
    public static Map<Patch, Double> doTokenAnalysis(final Collection<Patch> patches,
                                                     final File patchedSourceFilesDir) {
        Validate.isTrue(patchedSourceFilesDir.isDirectory());
        final Map<Patch, Double> res = new HashMap<>();
        final Collection<File> originalFiles = FileUtils.listFiles(new File(patchedSourceFilesDir, "original"),
                new String[] {"java"}, true);
        for (final Patch patch : patches) {
            final Collection<File> patchedFiles = FileUtils.listFiles(new File(patchedSourceFilesDir, patch.getId()),
                    new String[] {"java"}, true);
            final Set<File> targetOriginalFiles = new HashSet<>();
            final Set<File> targetPatchedFiles = new HashSet<>();
            for (final File originalFile : originalFiles) {
                for (final File patchedFile : patchedFiles) {
                    if (originalFile.getName().equals(patchedFile.getName())) {
                        targetOriginalFiles.add(originalFile);
                        targetPatchedFiles.add(patchedFile);
                    }
                }
            }
            final TokenAnalyzer ta = new TokenAnalyzer();
            final double dist = ta.calculateSimilarity(targetOriginalFiles,
                    targetPatchedFiles,
                    sanitizeMethodNames(patch.getPatchedMethods()),
                    new CosineSimilarityCalculator());
            res.put(patch, Math.min(dist, 1D));
        }
        return res;
    }

    private static List<String> sanitizeMethodNames(final Collection<String> methodNames) {
        return methodNames.stream()
                .map(SimCalculator::sanitizeMethodName)
                .map(mn -> mn.replace('$', '.'))
                .collect(Collectors.toList());
    }

    private static String sanitizeMethodName(final String methodName) {
        final int indexOfInit = methodName.indexOf(".<init>");
        if (indexOfInit >= 0) {
            final int indexOfLastDot = methodName.lastIndexOf('.', indexOfInit - 1);
            final String className = methodName.substring(1 + indexOfLastDot, indexOfInit);
            return methodName.replace("<init>", className);
        }
        return methodName;
    }
}
