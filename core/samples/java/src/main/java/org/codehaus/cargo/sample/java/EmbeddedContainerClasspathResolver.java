/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ========================================================================
 */
package org.codehaus.cargo.sample.java;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.cargo.container.internal.util.JdkUtils;
import org.codehaus.cargo.util.CargoException;

/**
 * Create a classloader to load container classes.
 * 
 * @version $Id$
 */
public class EmbeddedContainerClasspathResolver
{
    /**
     * Map of dependencies, with key the container identifier and value the list of dependencies.
     */
    private static final Map<String, List<String>> DEPENDENCIES =
        new HashMap<String, List<String>>();

    static
    {
        List<String> jetty4xDependencies = new ArrayList<String>();
        jetty4xDependencies.add("lib/*.jar");
        jetty4xDependencies.add("ext/*.jar");

        List<String> jetty5xDependencies = new ArrayList<String>();
        jetty5xDependencies.add("lib/*.jar");
        jetty5xDependencies.add("ext/*.jar");

        List<String> jetty6xDependencies = new ArrayList<String>();
        jetty6xDependencies.add("lib/*.jar");
        jetty6xDependencies.add("lib/jsp-2.0/*.jar");
        jetty6xDependencies.add("lib/management/*.jar");
        jetty6xDependencies.add("lib/naming/*.jar");
        jetty6xDependencies.add("lib/plus/*.jar");
        jetty6xDependencies.add("lib/xbean/*.jar");

        List<String> jetty7xDependencies = new ArrayList<String>();
        jetty7xDependencies.add("lib/*.jar");
        jetty7xDependencies.add("lib/jndi/*.jar");
        jetty7xDependencies.add("lib/jsp/*.jar");

        List<String> jetty8x9xDependencies = new ArrayList<String>();
        jetty8x9xDependencies.add("lib/*.jar");
        jetty8x9xDependencies.add("lib/annotations/*.jar");
        jetty8x9xDependencies.add("lib/jndi/*.jar");
        jetty8x9xDependencies.add("lib/jsp/*.jar");

        List<String> tomcat5xDependencies = new ArrayList<String>();
        tomcat5xDependencies.add("bin/*.jar");
        tomcat5xDependencies.add("common/lib/*.jar");
        tomcat5xDependencies.add("server/lib/*.jar");

        DEPENDENCIES.put("jetty4x", jetty4xDependencies);
        DEPENDENCIES.put("jetty5x", jetty5xDependencies);
        DEPENDENCIES.put("jetty6x", jetty6xDependencies);
        DEPENDENCIES.put("jetty7x", jetty7xDependencies);
        DEPENDENCIES.put("jetty8x", jetty8x9xDependencies);
        DEPENDENCIES.put("jetty9x", jetty8x9xDependencies);
        DEPENDENCIES.put("tomcat5x", tomcat5xDependencies);
    }

    /**
     * JDK utilities.
     */
    private JdkUtils jdkUtils = new JdkUtils();

    /**
     * Resolve dependencies for an embedded container and create classpath.
     * @param containerId Container identifier.
     * @param containerHome Container home.
     * @return {@link ClassLoader} with dependencies, <code>null</code> if the container is not
     * supported in the embedded mode.
     * @throws FileNotFoundException If some dependencies cannot be found.
     */
    public ClassLoader resolveDependencies(String containerId, String containerHome)
        throws FileNotFoundException
    {
        List<String> dependencies = DEPENDENCIES.get(containerId);
        if (dependencies == null)
        {
            return null;
        }

        ClassLoader classloader = null;

        try
        {
            List<URL> urls = new ArrayList<URL>();

            if (containerId.equals("jetty7x") || containerId.equals("jetty8x"))
            {
                String xerces = System.getProperty("cargo.testdata.xerces-jars");
                if (xerces == null)
                {
                    throw new IllegalArgumentException("cargo.testdata.xerces-jars not defined");
                }
                File[] xercesJARs = new File(xerces).listFiles();
                if (xercesJARs == null)
                {
                    throw new FileNotFoundException("Directory not found: " + xerces);
                }
                for (File xercesJAR : xercesJARs)
                {
                    urls.add(xercesJAR.toURI().toURL());
                }
            }

            for (String dependencyRelativePath : dependencies)
            {
                if (dependencyRelativePath.endsWith("*.jar"))
                {
                    // JAR folder - Add all JARs in this directory
                    File folder = new File(containerHome, dependencyRelativePath).getParentFile();
                    File[] jars = folder.listFiles(new FilenameFilter()
                    {
                        public boolean accept(File dir, String name)
                        {
                            return name.endsWith(".jar");
                        }
                    });
                    if (jars == null)
                    {
                        throw new FileNotFoundException("No files matched: " + folder.toString()
                            + "/*.jar");
                    }
                    for (File jar : jars)
                    {
                        urls.add(jar.toURI().toURL());
                    }
                }
                else
                {
                    // Single JAR file or directory
                    File dependencyPath = new File(containerHome, dependencyRelativePath);
                    urls.add(dependencyPath.toURI().toURL());
                }
            }

            // On OS X, the tools.jar classes are included in the classes.jar so there is no need
            // to include any tools.jar file to the classpath.
            if (!this.jdkUtils.isOSX())
            {
                urls.add(this.jdkUtils.getToolsJar().toURI().toURL());
            }

            if (containerId.equals("tomcat5x"))
            {
                /*
                 * Here is the problem this code is trying to solve.
                 * 
                 * When this is run inside forked JUnit or Surefire, the system class loader
                 * contains a bunch of classes, of which the servlet API; and we can't do anything
                 * about it.
                 * 
                 * The web application class loader that Tomcat uses has a non-standard delegation
                 * order; it tries to load classes from WAR before it delegates to ancestors. To
                 * avoid loading Java SE classes from WAR (the servlet spec describes some of
                 * those), this class loader checks the system class loader.
                 * 
                 * So when we load Tomcat classes (URLClassLoader below), this class loader needs
                 * to delegate to the system class loader, so that both the Tomcat implementation
                 * and web application load the servlet API from the same place. Otherwise you get
                 * a ClassCastException. So that's why we set getClass().getClassLoader() as the
                 * parent.
                 * 
                 * However, that causes another problem in a separate place: with this change,
                 * Tomcat will load ANT from the system class loader (because that's another JAR
                 * that JUnit / Surefire puts into the classpath), but the system class loader
                 * doesn't have tools.jar. So <javac> fails, and this breaks Jasper, which
                 * compiles JSP files through <javac> ANT task.
                 * 
                 * To avoid this problem, we want to load ANT from the class loader we create
                 * below, which will also contain tools.jar. To do this, we insert another class
                 * loader and cut the delegation chain for org.apache.tools.ant.
                 * 
                 * All this has been fixed with Tomcat 6 as it is shipped with its own Java
                 * compiler (Eclipse JDT) hence doesn't rely on tools.jar anymore.
                 */
                classloader = new URLClassLoader(new URL[0], getClass().getClassLoader())
                {
                    @Override
                    protected synchronized Class<?> loadClass(String name, boolean resolve)
                        throws ClassNotFoundException
                    {
                        if (name.startsWith("org.apache.tools.ant"))
                        {
                            throw new ClassNotFoundException();
                        }
                        return super.loadClass(name, resolve);
                    }
                };
            }

            // For all containers other than Tomcat, we pass null as the parent to ensure no JARs
            // outside of the ones we've added are added to the classpath.
            classloader = new URLClassLoader(urls.toArray(new URL[urls.size()]), classloader);
        }
        catch (MalformedURLException e)
        {
            throw new CargoException("Failed to resolve dependency", e);
        }

        return classloader;
    }
}
