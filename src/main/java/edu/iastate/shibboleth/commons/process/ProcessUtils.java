package edu.iastate.shibboleth.commons.process;

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

import java.io.IOException;
import java.net.Socket;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public final class ProcessUtils {
    private ProcessUtils() { }

    public static void panic() {
        while (true);
    }

    // credit: copied from PIT source code
    public static void safelyCloseSocket(final Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (final IOException e) {
                e.printStackTrace();
                System.out.println("Couldn't close socket");
            }
        }
    }
}
