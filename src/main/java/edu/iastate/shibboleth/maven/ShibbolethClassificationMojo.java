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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
@Mojo(name = "classify", requiresDependencyResolution = ResolutionScope.TEST)
public class ShibbolethClassificationMojo extends AbstractShibbolethMojo {
    private static final CSVFormat CSV_FORMAT;

    static {
        CSV_FORMAT = CSVFormat.DEFAULT.withRecordSeparator(System.lineSeparator());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        checkAndSanitizeParameters();
        final ShibbolethEntryPoint entryPoint = new ShibbolethEntryPoint(this);
        final Map<Patch, PairedPrimaryProfilerResults> result = entryPoint.start();
        final File patchedSourceFilesBaseDir =
                new File(entryPoint.getProjectBaseDirectory(), "patched-source-files");
        final Map<Patch, Double> tsMap =
                SimCalculator.doTokenAnalysis(entryPoint.getPatches(), patchedSourceFilesBaseDir);
        final Map<Patch, PatchScoreTriple> scores = buildScores(tsMap, result);
        final String[] classes = classify(scores);
        final Iterator<Patch> ki = scores.keySet().iterator();
        int i = 0;
        getLog().info("------------------------------------------");
        while (ki.hasNext()) {
            System.out.println(ki.next().getId() + ": " + ("0".equals(classes[i++]) ? "INCORRECT" : "CORRECT"));
        }
        getLog().info("------------------------------------------");
        (new File("scores.csv")).delete();
    }

    public static String[] classify(final Map<Patch, PatchScoreTriple> scores) throws MojoExecutionException {
        try (final PrintWriter pw = new PrintWriter("scores.csv");
             final CSVPrinter printer = new CSVPrinter(pw, CSV_FORMAT)) {
            printer.printRecord("ID", "SCS", "TS", "BC");
            for (final Map.Entry<Patch, PatchScoreTriple> ent : scores.entrySet()) {
                final PatchScoreTriple score = ent.getValue();
                printer.printRecord(ent.getKey().getId(), score.getScs(), score.getTs(), score.getBc());
            }
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }
        String rawOutput;
        try {
            reifyResources();
            final Process classifier = Runtime.getRuntime().exec(new String[] {"python3.9", "shibboleth_classifier.py"});
            classifier.waitFor();
            rawOutput = IOUtils.toString(classifier.getInputStream(), Charset.defaultCharset()).trim();
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }
        if (!rawOutput.matches("\\[[01\\s]+\\]")) {
            throw new MojoExecutionException("Something went wrong! Please make sure you have installed Python 3.9 and all the dependencies mentioned in the project website");
        }
        rawOutput = rawOutput.substring(1, rawOutput.length() - 1);
        return rawOutput.split("\\s+");
    }

    private static void reifyResources() throws IOException {
        final InputStream[] resis = getResources();
        copy(resis[0], new File("rf.model"));
        copy(resis[1], new File("shibboleth_classifier.py"));
    }

    private static void copy(final InputStream is, final File outFile) throws IOException {
        outFile.deleteOnExit();
        try (final OutputStream os = new FileOutputStream(outFile)) {
            IOUtils.copy(is, os);
        }
    }

    private static InputStream[] getResources() {
        final InputStream[] res = new InputStream[2];
        InputStream is = ShibbolethClassificationMojo.class.getClassLoader().getResourceAsStream("rf.model");
        if (is == null) {
            throw new IllegalArgumentException("file not found!");
        } else {
            res[0] = is;
        }
        is = ShibbolethClassificationMojo.class.getClassLoader().getResourceAsStream("shibboleth_classifer.py");
        if (is == null) {
            throw new IllegalArgumentException("file not found!");
        } else {
            res[1] = is;
        }
        return res;
    }
}
