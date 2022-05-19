package edu.iastate.shibboleth.commons.misc;

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

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.JSR;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public final class ProgramElementCounter {
    private Integer linesCount;

    private Integer branchesCount;

    public ProgramElementCounter() {
        this.linesCount = null;
        this.branchesCount = null;
    }

    public void processClassFiles(final File productionClassesBaseDirectory) throws IOException {
        final Collection<File> classFiles = FileUtils.listFiles(productionClassesBaseDirectory,
                new String[] {"class"}, true);
        this.linesCount = 0;
        this.branchesCount = 0;
        for (final File classFile : classFiles) {
            try (final InputStream is = new FileInputStream(classFile)) {
                final ClassReader classReader = new ClassReader(is);
                final CollectorClassVisitor cv = new CollectorClassVisitor();
                classReader.accept(cv, ClassReader.EXPAND_FRAMES);
                this.linesCount += cv.linesCount;
                this.branchesCount += cv.branchesCount;
            }
        }
    }

    public int getLinesCount() {
        return Objects.requireNonNull(this.linesCount);
    }

    public int getBranchesCount() {
        return Objects.requireNonNull(branchesCount);
    }

    static class CollectorClassVisitor extends ClassVisitor {
        int linesCount;

        int branchesCount;

        public CollectorClassVisitor() {
            super(ASM7);
            this.linesCount = 0;
            this.branchesCount = 0;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            final MethodVisitor defaultMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new CollectorMethodVisitor(defaultMethodVisitor);
        }

        class CollectorMethodVisitor extends MethodVisitor {
            private int currentLineNo;

            public CollectorMethodVisitor(final MethodVisitor methodVisitor) {
                super(ASM7, methodVisitor);
                this.currentLineNo = -1;
            }

            @Override
            public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                branchesCount += (1 + labels.length);
                visitLineNumber();
                super.visitLookupSwitchInsn(dflt, keys, labels);
            }

            @Override
            public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
                branchesCount += (1 + labels.length);
                visitLineNumber();
                super.visitTableSwitchInsn(min, max, dflt, labels);
            }

            @Override
            public void visitJumpInsn(int opcode, Label label) {
                if (opcode != GOTO && opcode != JSR) {
                    branchesCount += 2;
                }
                visitLineNumber();
                super.visitJumpInsn(opcode, label);
            }

            private void visitLineNumber() {
                if (this.currentLineNo > 0) {
                    linesCount++;
                    this.currentLineNo = -1;
                }
            }

            @Override
            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                visitLineNumber();
                super.visitTryCatchBlock(start, end, handler, type);
            }

            @Override
            public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                visitLineNumber();
                super.visitMultiANewArrayInsn(descriptor, numDimensions);
            }

            @Override
            public void visitIincInsn(int var, int increment) {
                visitLineNumber();
                super.visitIincInsn(var, increment);
            }

            @Override
            public void visitLdcInsn(Object value) {
                visitLineNumber();
                super.visitLdcInsn(value);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean iface) {
                visitLineNumber();
                super.visitMethodInsn(opcode, owner, name, desc, iface);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name,String descriptor) {
                visitLineNumber();
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                visitLineNumber();
                super.visitTypeInsn(opcode, type);
            }

            @Override
            public void visitVarInsn(int opcode, int var) {
                visitLineNumber();
                super.visitVarInsn(opcode, var);
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                visitLineNumber();
                super.visitIntInsn(opcode, operand);
            }

            @Override
            public void visitInsn(int opcode) {
                visitLineNumber();
                super.visitInsn(opcode);
            }

            @Override
            public void visitInvokeDynamicInsn(String name,
                                               String descriptor,
                                               Handle bootstrapMethodHandle,
                                               Object... bootstrapMethodArguments) {
                visitLineNumber();
                super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            }

            @Override
            public void visitLineNumber(int line, Label start) {
                this.currentLineNo = line;
                super.visitLineNumber(line, start);
            }
        }
    }
}
