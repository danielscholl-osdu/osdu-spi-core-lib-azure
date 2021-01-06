//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.azure.logging;

import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Util;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Checks if slf4j got bound with log4j2 correctly.
 */
@Component
@ConditionalOnProperty(value = "logging.checkSlf4jBinding.enabled", havingValue = "true", matchIfMissing = true)
public class Slf4JBindingChecker {

    private final String staticLoggerBinderPath = "org/slf4j/impl/StaticLoggerBinder.class";
    private Set<URL> staticLoggerBinderPathSet = new LinkedHashSet<URL>();

    /**
     * Since this methods is annotated with @PostConstruct, it is called only once.
     * Its job is to fail the application if slf4j is not correctly bound to log4j2.
     */
    @PostConstruct
    public void performCheck() {
        Set<URL> pathSet = findPossibleStaticLoggerBinderPathSet();
        if (!checkPathSet(pathSet)) {
            throw new RuntimeException("\nAPPLICATION FAILED TO START\nslf4j is not correctly bound to log4j2\n");
        }
    }

    /**
     * Find all the resources on classpath with the name "org/slf4j/impl/StaticLoggerBinder.class".
     * If there are multiple resources, that means there are multiple slf4j bindings present
     * @return set of resources on classpath with name "org/slf4j/impl/StaticLoggerBinder.class"
     */
    private Set<URL> findPossibleStaticLoggerBinderPathSet() {
        try {
            ClassLoader loggerFactoryClassLoader = LoggerFactory.class.getClassLoader();
            Enumeration<URL> paths;
            if (loggerFactoryClassLoader == null) {
                paths = ClassLoader.getSystemResources(staticLoggerBinderPath);
            } else {
                paths = loggerFactoryClassLoader.getResources(staticLoggerBinderPath);
            }
            while (paths.hasMoreElements()) {
                URL path = paths.nextElement();
                staticLoggerBinderPathSet.add(path);
            }
        } catch (IOException ioe) {
            Util.report("Error getting resources from path", ioe);
        }
        return staticLoggerBinderPathSet;
    }

    /**
     * If numbers of paths is greater than 1, then there are multiple bindings on classpath for slf4j
     * In this case, slf4j will not bind correctly with log4j2.
     * For binding to happen correctly, we need only one binding to be present, and that should of log4j2
     * @param binderPathSet set of resources on classpath with name "org/slf4j/impl/StaticLoggerBinder.class"
     * @return true if only one binding is found of log4j2 type, else return false
     */
    private boolean checkPathSet(final Set<URL> binderPathSet) {
        if (binderPathSet.size() == 1) {
            URL path = binderPathSet.iterator().next();
            if (path.toString().contains("log4j-slf4j-impl-")) {
                return true;
            }
        }
        return false;
    }
}
