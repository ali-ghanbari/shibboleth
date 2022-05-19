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

import edu.iastate.shibboleth.commons.asm.ComputeClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.functional.predicate.Predicate;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.pitest.bytecode.FrameOptions.pickFlags;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
class PrimaryProfilerTransformer implements ClassFileTransformer {
    private static final Map<String, String> CACHE = new HashMap<>();

    private final ClassByteArraySource byteArraySource;

    private final Predicate<String> appClassFilter;

    private final Set<String> patchedMethods;

    public PrimaryProfilerTransformer(final ClassByteArraySource byteArraySource,
                                      final Predicate<String> appClassFilter,
                                      final Set<String> patchedMethods) {
        this.byteArraySource = byteArraySource;
        this.appClassFilter = appClassFilter;
        this.patchedMethods = patchedMethods;
    }

    private boolean isAppClass(String className) {
        className = className.replace('/', '.');
        return this.appClassFilter.apply(className);
    }

    @Override
    public byte[] transform(final ClassLoader loader,
                            final String className,
                            final Class<?> classBeingRedefined,
                            final ProtectionDomain protectionDomain,
                            final byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null || !isAppClass(className)) {
            return null; // no transformation
        }
        try {
            return transformBC(transformIVRecorder(classfileBuffer));
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            return null;
        }
    }

    private byte[] transformBC(final byte[] classfileBuffer) {
        final ClassReader classReader = new ClassReader(classfileBuffer);
        final BCTransformer bcTransformer = new BCTransformer();
        classReader.accept(bcTransformer, ClassReader.EXPAND_FRAMES);
        bcTransformer.transform();
        final ClassWriter classWriter = new ComputeClassWriter(this.byteArraySource, CACHE, pickFlags(classfileBuffer));
        bcTransformer.accept(classWriter);
        return classWriter.toByteArray();
    }

    private byte[] transformIVRecorder(final byte[] classfileBuffer) {
        final ClassReader classReader = new ClassReader(classfileBuffer);
        final ClassWriter classWriter = new ComputeClassWriter(this.byteArraySource, CACHE, pickFlags(classfileBuffer));
        final ClassVisitor classVisitor = new IVRecorderTransformer(classWriter, this.patchedMethods);
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }
}
