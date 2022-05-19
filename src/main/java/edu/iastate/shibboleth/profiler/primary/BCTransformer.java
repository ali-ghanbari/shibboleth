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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.JSR;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
class BCTransformer extends ClassNode {
    private static final String BRANCH_COVERAGE_RECORDER_NAME = Type.getType(BCRecorder.class).getInternalName();

    private static final Set<Long> BRANCH_ID_POOL = new HashSet<>();

    public BCTransformer() {
        super(ASM7);
    }

    void transform() {
        for (final MethodNode methodNode : this.methods) {
            final InsnList insnList = methodNode.instructions;
            if (insnList.size() == 0) {
                continue;
            }
            final Iterator<AbstractInsnNode> nit = insnList.iterator();
            while (nit.hasNext()) {
                AbstractInsnNode node = nit.next();
                if (node instanceof JumpInsnNode) {
                    if (node.getOpcode() == GOTO || node.getOpcode() == JSR) {
                        continue;
                    }
                    final LabelNode destLabel = ((JumpInsnNode) node).label;
                    insnList.insert(node, invoke()); // "fall through" branch
                    insnList.insert(destLabel, invoke()); // "true" branch
                } else if (node instanceof TableSwitchInsnNode) {
                    fillUpCases(insnList, ((TableSwitchInsnNode) node).dflt,
                            ((TableSwitchInsnNode) node).labels);
                } else if (node instanceof LookupSwitchInsnNode) {
                    fillUpCases(insnList, ((LookupSwitchInsnNode) node).dflt,
                            ((LookupSwitchInsnNode) node).labels);
                }
            }
        }
    }

    private void fillUpCases(final InsnList insnList,
                             final LabelNode dfltLabel,
                             final Collection<LabelNode> labels) {
        insnList.insert(dfltLabel, invoke());
        for (final LabelNode labelNode : labels) {
            insnList.insert(labelNode, invoke());
        }
    }

    private InsnList invoke() {
        final InsnList il = new InsnList();
        final long branchId = generateUID();
        il.add(new LdcInsnNode(branchId));
        il.add(new MethodInsnNode(INVOKESTATIC, BRANCH_COVERAGE_RECORDER_NAME, "recordCoveredBranch", "(J)V", false));
        return il;
    }

    private long generateUID() {
        long uid;
        do {
            uid = System.nanoTime();
        } while (BRANCH_ID_POOL.contains(uid));
        BRANCH_ID_POOL.add(uid);
        return uid;
    }
}
