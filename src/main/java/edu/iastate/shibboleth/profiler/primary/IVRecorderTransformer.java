package edu.iastate.shibboleth.profiler.primary;

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

import edu.iastate.shibboleth.commons.asm.FinallyBlockAdviceAdapter;
import edu.iastate.shibboleth.commons.misc.NameUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.lang.reflect.Modifier;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM7;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
class IVRecorderTransformer extends ClassVisitor {
    final Set<String> patchedMethods;

    String className;

    boolean isInterface;

    public IVRecorderTransformer(final ClassVisitor classVisitor,
                                 final Set<String> patchedMethods) {
        super(ASM7, classVisitor);
        this.patchedMethods = patchedMethods;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.isInterface = Modifier.isInterface(access);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        final MethodVisitor defaultMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (this.isInterface || Modifier.isNative(access) || Modifier.isAbstract(access)) {
            return defaultMethodVisitor;
        }
        final String methodFullName = NameUtils.constructMethodFullName(this.className, name, descriptor);
        final boolean isPatchedMethod = this.patchedMethods.contains(methodFullName);
        return new IVRecorderMethodVisitor(defaultMethodVisitor, access, name, descriptor, isPatchedMethod);
    }

    static class IVRecorderMethodVisitor extends FinallyBlockAdviceAdapter {
        static final Type IV_RECORDER = Type.getType(IVRecorder.class);

        final boolean isPatchedMethod;

        public IVRecorderMethodVisitor(final MethodVisitor methodVisitor,
                                       final int access,
                                       final String name,
                                       final String descriptor,
                                       final boolean isPatchedMethod) {
            super(ASM7, methodVisitor, access, name, descriptor);
            this.isPatchedMethod = isPatchedMethod;
        }

        @Override
        protected void insertPrelude() {
            if (this.isPatchedMethod) {
                invokeStatic(IV_RECORDER, Method.getMethod("void enterPatchedMethod()"));
            }
        }

        @Override
        protected void insertSequel(boolean normalExit) {
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            push(MULTIANEWARRAY);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitMultiANewArrayInsn(descriptor, numDimensions);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            push(LOOKUPSWITCH);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            push(TABLESWITCH);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            push(IINC);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitIincInsn(var, increment);
        }

        @Override
        public void visitLdcInsn(Object value) {
            push(LDC);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitLdcInsn(value);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            push(opcode);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void invokeDynamic(String name,
                                  String descriptor,
                                  Handle bootstrapMethodHandle,
                                  Object... bootstrapMethodArguments) {
            push(INVOKEDYNAMIC);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.invokeDynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean iface) {
            push(opcode);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitMethodInsn(opcode, owner, name, desc, iface);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            push(opcode);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            push(opcode);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            push(opcode);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            push(opcode);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitInsn(int opcode) {
            push(opcode);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitInsn(opcode);
        }

        @Override
        public void visitInvokeDynamicInsn(String name,
                                           String descriptor,
                                           Handle bootstrapMethodHandle,
                                           Object... bootstrapMethodArguments) {
            push(INVOKEDYNAMIC);
            invokeStatic(IV_RECORDER, Method.getMethod("void recordOpcode(int)"));
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }
    }
}
