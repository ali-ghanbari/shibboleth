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

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public final class ControlId {
    public static final byte REPORT_COVERED_BRANCHES_COUNT = 1;

    public static final byte REPORT_RECORDED_INSN_VECTOR_MAP = 2;

    public static final byte REPORT_METHOD_COVERAGE_MAP = 4;

    public static final byte REPORT_FAILING_TESTS = 8;

    public static final byte DONE = 64;

    private ControlId() { }
}
