package edu.iastate.shibboleth.commons.asm;

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
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.reflect.Modifier;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public final class AdviceAdapterUtils {
    private AdviceAdapterUtils() { }

    public static void loadArgArray(final AdviceAdapter adapter) {
        final Type objectType = Type.getObjectType("java/lang/Object");
        final Type[] argumentTypes = adapter.getArgumentTypes();
        final boolean isStatic = Modifier.isStatic(adapter.getAccess());
        adapter.push(isStatic ? argumentTypes.length : 1 + argumentTypes.length);
        adapter.newArray(objectType);
        int arrayIndex = 0;
        if (!isStatic) {
            adapter.dup();
            adapter.push(arrayIndex++);
            adapter.loadThis();
            adapter.arrayStore(objectType);
        }
        for (int i = 0; i < argumentTypes.length; i++) {
            adapter.dup();
            adapter.push(arrayIndex++);
            adapter.loadArg(i);
            adapter.box(argumentTypes[i]);
            adapter.arrayStore(objectType);
        }
    }
}
