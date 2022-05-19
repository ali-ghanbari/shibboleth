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

import edu.iastate.shibboleth.commons.misc.NameUtils;
import edu.iastate.shibboleth.commons.relational.MethodsDom;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ASM7;

/**
 * A class transformer to instrument class files so that accessed fields are recorded.
 * This is intended to be used to minimize the overhead of obj-utils library.
 * !Internal use only!
 *
 * @author Ali Ghanbari (alig@iastate.edu)
 */
class TransformerClassVisitor extends ClassVisitor {
    private final MethodsDom methodsDom;

    private String owner;

    public TransformerClassVisitor(final ClassVisitor classVisitor, final MethodsDom methodsDom) {
        super(ASM7, classVisitor);
        this.methodsDom = methodsDom;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.owner = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        final String methodFullName = NameUtils.constructMethodFullName(this.owner, name, descriptor);
        final int methodIndex = this.methodsDom.getOrAdd(methodFullName);
        return new MethodCoverageTransformer(methodVisitor, access, name, descriptor, methodIndex);
    }
}
