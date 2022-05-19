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

import edu.iastate.shibboleth.commons.collections.Printer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.pitest.functional.F;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.objectweb.asm.Opcodes.ASM7;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public final class NameUtils {
    private NameUtils() { }

    public static String constructFullFileName(String className, String sourceFileName) {
        className = className.replace('/', '.');
        return String.format("%s/%s",
                getPackageName(className).replace('.', '/'),
                sourceFileName);
    }

    public static String constructMethodFullName(final String className,
                                                 final String methodName,
                                                 final String descriptor) {
        return String.format("%s.%s(%s)",
                className.replace('/', '.'),
                methodName,
                Printer.join(Type.getArgumentTypes(descriptor), typeToClassName(), ","));
    }

    private static F<Type, String> typeToClassName() {
        return Type::getClassName;
    }

    public static String getPackageName(final String className) {
        int index = className.lastIndexOf('.');
        if (index >= 0) {
            return className.substring(0, index);
        }
        index = className.lastIndexOf('/');
        if (index < 0) {
            throw new IllegalArgumentException();
        }
        return className.substring(0, index);
    }

    public static String sanitizeExtendedTestName(final String extendedTestName) {
        final int indexOfSpace = extendedTestName.indexOf(' ');
        return sanitizeTestName(extendedTestName.substring(1 + indexOfSpace));
    }

    public static String sanitizeTestName(String name) {
        //SETLab style: test.class.name:test_name
        name = name.replace(':', '.');
        //Defects4J style: test.class.name::test_name
        name = name.replace("..", ".");
        int indexOfLP = name.indexOf('(');
        if (indexOfLP >= 0) {
            final String testCaseName = name.substring(0, indexOfLP);
            name = name.substring(1 + indexOfLP, name.length() - 1) + "." + testCaseName;
        }
        return name;
    }

    public static Pair<String, String> decomposeMethodName(final String qualifiedMethodName) {
        final int indexOfLastDot = qualifiedMethodName.lastIndexOf('.');
        final String className = qualifiedMethodName.substring(0, indexOfLastDot);
        final String methodName = qualifiedMethodName.substring(1 + indexOfLastDot);
        return new ImmutablePair<>(className, methodName);
    }

    /**
     * Given a class file, returns Java name of the class.
     *
     * @param classFile Class file on the file system
     * @return Java name of the class
     */
    public static String getClassName(final File classFile) {
        try (final InputStream fis = new FileInputStream(classFile)) {
            final NameExtractor extractor = new NameExtractor();
            final ClassReader cr = new ClassReader(fis);
            cr.accept(extractor, 0);
            return extractor.className;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class NameExtractor extends ClassVisitor {
        String className;

        public NameExtractor() {
            super(ASM7);
        }

        @Override
        public void visit(final int version,
                          final int access,
                          final String name,
                          final String signature,
                          final String superName,
                          final String[] interfaces) {
            this.className = name.replace('/', '.');
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }

}
