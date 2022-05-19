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
import edu.iastate.shibboleth.maven.ShibbolethRankingMojo;

import java.util.Map;

/**
 * Command-line interface for Shibboleth ranker.
 *
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class Ranker extends Shibboleth {
    private Ranker() { }

    public static void main(final String[] args) {
        (new Ranker()).runRanker(args);
    }

    private void runRanker(final String[] args) {
        super.run(args);
        final Map<Patch, Integer> ranks = ShibbolethRankingMojo.rank(super.scores);
        System.out.println("-------------------------------------");
        ranks.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    System.out.println(entry.getValue() + " " + entry.getKey().getId());
                });
        System.out.println("-------------------------------------");
    }
}
