package edu.iastate.shibboleth.constants;

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

import edu.iastate.shibboleth.commons.misc.PropertyUtils;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class Params {
    public static final int BYTECODE_CLASS_CACHE_SIZE;

    public static final int UNIT_SIZE;

    public static final int TEST_UNIT_TIME_OUT;

    static {
        BYTECODE_CLASS_CACHE_SIZE = PropertyUtils.getIntProperty("dpp.def.cache.size", 500);
        UNIT_SIZE = PropertyUtils.getIntProperty("dpp.unit.size", 1024);
        TEST_UNIT_TIME_OUT = PropertyUtils.getIntProperty("dpp.testcase.timeout", 10); // unit is minutes
    }
}
