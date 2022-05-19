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

import edu.iastate.shibboleth.commons.misc.NameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class Patch implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;

    private final Set<File> patchedClassFiles;

    private final Set<String> patchedMethods;

    public Patch(final String id,
                 final String[] patchedClassFileNames,
                 final String[] patchedMethods) {
        this.id = id;
        this.patchedClassFiles = new HashSet<>();
        for (final String patchedFileName : patchedClassFileNames) {
            this.patchedClassFiles.add(new File(patchedFileName));
        }
        this.patchedMethods = new HashSet<>();
        Collections.addAll(this.patchedMethods, patchedMethods);
    }

    private int getFileHashCode(final File file) throws IOException {
        return Arrays.hashCode(FileUtils.readFileToByteArray(file));
    }

    public int calculateHashCode() {
        int hashCode = 1;
        try {
            for (final File file : this.patchedClassFiles) {
                hashCode = 31 * hashCode + getFileHashCode(file);
            }
        } catch (final IOException ignored) { }
        return hashCode;
    }

    private File getOriginalClassFile(final File buildDirectory, final String patchedClassName) {
        return new File(buildDirectory, patchedClassName.replace('.', File.separatorChar) + ".class");
    }

    private File getBackupClassFile(final File backupBaseDirectory, final String patchedClassName) {
        return new File(backupBaseDirectory, patchedClassName + ".class");
    }

    public void install(final File buildDirectory, final File backupBaseDirectory) throws IOException {
        Validate.isTrue(buildDirectory.isDirectory());
        Validate.isTrue(backupBaseDirectory.isDirectory());
        for (final File patchedClassFile : this.patchedClassFiles) {
            final String patchedClassName = NameUtils.getClassName(patchedClassFile);
            final File originalClassFile = getOriginalClassFile(buildDirectory, patchedClassName);
            if (!originalClassFile.isFile()) {
                throw new IllegalStateException("Original file '" + originalClassFile.getAbsolutePath() + "' does not exist.");
            }
            final File backupFile = getBackupClassFile(backupBaseDirectory, patchedClassName);
            FileUtils.copyFile(originalClassFile, backupFile);
            FileUtils.copyFile(patchedClassFile, originalClassFile);
        }
    }

    public void uninstall(final File buildDirectory, final File backupBaseDirectory) throws IOException {
        Validate.isTrue(buildDirectory.isDirectory());
        Validate.isTrue(backupBaseDirectory.isDirectory());
        for (final File patchedClassFile : this.patchedClassFiles) {
            final String patchedClassName = NameUtils.getClassName(patchedClassFile);
            final File originalClassFile = getOriginalClassFile(buildDirectory, patchedClassName);
            if (!originalClassFile.isFile()) {
                throw new IllegalStateException("Original file '" + originalClassFile.getAbsolutePath() + "' does not exist.");
            }
            final File backupFile = getBackupClassFile(backupBaseDirectory, patchedClassName);
            if (!backupFile.isFile()) {
                throw new IllegalStateException("Backup file '" + backupFile.getAbsolutePath() + "' does not exist.");
            }
            FileUtils.copyFile(backupFile, originalClassFile);
            FileUtils.forceDelete(backupFile);
        }
    }

    public String getId() {
        return this.id;
    }

    public Set<File> getPatchedClassFiles() {
        return this.patchedClassFiles;
    }

    public Set<String> getPatchedMethods() {
        return this.patchedMethods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Patch)) {
            return false;
        }
        final Patch that = (Patch) o;
        return this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
