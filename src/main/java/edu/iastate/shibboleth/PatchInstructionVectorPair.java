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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class PatchInstructionVectorPair {
    final double[] beforeIV;

    final double[] afterIV;

    PatchInstructionVectorPair(final double[] beforeIV, final double[] afterIV) {
        this.beforeIV = beforeIV;
        this.afterIV = afterIV;
    }

    PatchInstructionVectorPair(final int[] beforeIV, final int[] afterIV) {
        final int n = beforeIV.length;
        this.beforeIV = new double[n];
        this.afterIV = new double[n];
        for (int i = 0; i < n; i++) {
            this.beforeIV[i] = beforeIV[i];
            this.afterIV[i] = afterIV[i];
        }
    }

    List<Number> getBeforeIVList() {
        final List<Number> ivList = new ArrayList<>(this.beforeIV.length);
        for (final double val : this.beforeIV) {
            ivList.add(val);
        }
        return ivList;
    }

    List<Number> getAfterIVList() {
        final List<Number> ivList = new ArrayList<>(this.afterIV.length);
        for (final double val : this.afterIV) {
            ivList.add(val);
        }
        return ivList;
    }
}
