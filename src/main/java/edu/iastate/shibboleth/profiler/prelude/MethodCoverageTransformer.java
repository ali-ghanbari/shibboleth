package edu.iastate.shibboleth.profiler.prelude;

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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
class MethodCoverageTransformer extends AdviceAdapter {
    private static final Type METHOD_COVERAGE_RECORDER = Type.getType(MethodCoverageRecorder.class);

    private final int methodIndex;

    public MethodCoverageTransformer(final MethodVisitor methodVisitor,
                                     final int access,
                                     final String name,
                                     final String descriptor,
                                     final int methodIndex) {
        super(ASM7, methodVisitor, access, name, descriptor);
        this.methodIndex = methodIndex;
    }

    @Override
    protected void onMethodEnter() {
        push(this.methodIndex);
        invokeStatic(METHOD_COVERAGE_RECORDER, Method.getMethod("void recordMethod(int)"));
    }
}
