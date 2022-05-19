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

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class PatchScoreTriple implements Comparable<PatchScoreTriple> {
    private final double ts;

    private final double scs;

    private final double bc;

    public PatchScoreTriple(double ts, double scs, double bc) {
        this.ts = ts;
        this.scs = scs;
        this.bc = bc;
    }

    @Override
    public int compareTo(final PatchScoreTriple that) {
        int r = Double.compare(that.bc, this.bc);
        if (r == 0) {
            r = Double.compare(that.ts, this.ts);
            if (r == 0) {
                return Double.compare(that.scs, this.scs);
            }
        }
        return r;
    }

    public double getTs() {
        return ts;
    }

    public double getScs() {
        return scs;
    }

    public double getBc() {
        return bc;
    }
}
