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

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class CosineSimilarityCalculator implements VectorAnalyzer {
    @Override
    public double calculateSimDist(List<? extends Number> vector1,
                                   List<? extends Number> vector2) {
        final int n = vector1.size();
        if (n != vector2.size()) {
            throw new IllegalArgumentException();
        }
        double num = 0D;
        for (int i = 0; i < n; i++) {
            num += vector1.get(i).doubleValue() * vector2.get(i).doubleValue();
        }
        if (num != 0D) {
            num /= Math.sqrt(getSqSum(vector1));
            num /= Math.sqrt(getSqSum(vector2));
            return num;
        }
        return 0D;
    }

    private double getSqSum(List<? extends Number> vector) {
        double sqSum = 0D;
        for (final Number number : vector) {
            final double val = number.doubleValue();
            sqSum += val * val;
        }
        return sqSum;
    }

    @Override
    public double calculateSimDist(Number[] vector1, Number[] vector2) {
        return calculateSimDist(Arrays.asList(vector1), Arrays.asList(vector2));
    }

    public double calculateSimDist(int[] vector1, int[] vector2) {
        return calculateSimDist(ArrayUtils.toObject(vector1), ArrayUtils.toObject(vector2));
    }
}
