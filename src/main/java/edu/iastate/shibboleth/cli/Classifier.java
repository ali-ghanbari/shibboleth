package edu.iastate.shibboleth.cli;

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
import edu.iastate.shibboleth.maven.ShibbolethClassificationMojo;

import java.io.File;
import java.util.Iterator;

/**
 * Command-line interface for Shibboleth classifier.
 *
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class Classifier extends Shibboleth {
    private Classifier() { }

    public static void main(final String[] args) throws Exception {
        (new Classifier()).runClassifier(args);
    }

    private void runClassifier(final String[] args) throws Exception {
        super.run(args);
        final String[] classes = ShibbolethClassificationMojo.classify(this.scores);
        final Iterator<Patch> ki = scores.keySet().iterator();
        int i = 0;
        System.out.println("------------------------------------------");
        while (ki.hasNext()) {
            System.out.println(ki.next().getId() + ": " + ("0".equals(classes[i++]) ? "INCORRECT" : "CORRECT"));
        }
        System.out.println("------------------------------------------");
        (new File("scores.csv")).delete();
    }
}
