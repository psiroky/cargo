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
package org.codehaus.cargo.container.tomcat;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.configuration.builder.ConfigurationChecker;
import org.codehaus.cargo.container.configuration.entry.ResourceFixture;
import org.codehaus.cargo.container.tomcat.internal.AbstractCatalinaStandaloneLocalConfigurationTest;
import org.codehaus.cargo.container.tomcat.internal.Tomcat5And6xConfigurationChecker;
import org.codehaus.cargo.util.Dom4JUtil;
import org.custommonkey.xmlunit.XMLAssert;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

/**
 * Tests for the Tomcat 5 implementation of StandaloneLocalConfigurationTest
 * 
 * @version $Id$
 */
public class Tomcat5xStandaloneLocalConfigurationTest extends
    AbstractCatalinaStandaloneLocalConfigurationTest
{
    /**
     * Creates a {@link Tomcat5xStandaloneLocalConfiguration}. {@inheritdoc}
     * @param home Configuration home.
     * @return Local configuration for <code>home</code>.
     */
    @Override
    protected LocalConfiguration createLocalConfiguration(String home)
    {
        return new Tomcat5xStandaloneLocalConfiguration(home);
    }

    /**
     * Creates a {@link Tomcat5xInstalledLocalContainer}. {@inheritdoc}
     * @param configuration Container's configuration.
     * @return Local container for <code>configuration</code>.
     */
    @Override
    protected InstalledLocalContainer createLocalContainer(LocalConfiguration configuration)
    {
        return new Tomcat5xInstalledLocalContainer(configuration);
    }

    /**
     * @return {@link Tomcat5And6xConfigurationChecker}.
     */
    @Override
    protected ConfigurationChecker createConfigurationChecker()
    {
        return new Tomcat5And6xConfigurationChecker();
    }

    /**
     * {@inheritdoc}
     * @param fixture Resource fixture.
     * @return <code>conf/context.xml</code> in the configuration's home.
     */
    @Override
    protected String getResourceConfigurationFile(ResourceFixture fixture)
    {
        return configuration.getHome() + "/conf/context.xml";
    }

    /**
     * {@inheritdoc}
     * @throws Exception If anything goes wrong.
     */
    @Override
    protected void setUpResourceFile() throws Exception
    {
        Dom4JUtil xmlUtil = new Dom4JUtil(getFileHandler());
        String file = getResourceConfigurationFile(null);
        Document document = DocumentHelper.createDocument();
        document.addElement("Context");
        xmlUtil.saveXml(document, file);
    }

    /**
     * Test {@link
     * Tomcat5xStandaloneLocalConfiguration#configure(org.codehaus.cargo.container.LocalContainer)}
     * @throws Exception If anything goes wrong.
     */
    public void testConfigure() throws Exception
    {
        configuration.configure(container);
        assertTrue(configuration.getFileHandler().exists(
            configuration.getHome() + "/conf/catalina.properties"));
        assertTrue(configuration.getFileHandler().exists(
            configuration.getHome() + "/conf/context.xml"));
    }

    /**
     * {@inheritdoc}
     */
    @Override
    protected void setUpManager()
    {
        configuration.getFileHandler().mkdirs(container.getHome() + "/webapps");
        configuration.getFileHandler().mkdirs(container.getHome() + "/server/webapps/manager");
        configuration.getFileHandler().createFile(
            container.getHome() + "/conf/Catalina/localhost/manager.xml");
    }

    /**
     * Test {@link
     * Tomcat5xStandaloneLocalConfiguration#configure(org.codehaus.cargo.container.LocalContainer)}
     * and check manager.
     */
    public void testConfigureManager()
    {
        configuration.configure(container);
        assertTrue(configuration.getFileHandler().exists(
            configuration.getHome() + "/conf/context.xml"));
        assertTrue(configuration.getFileHandler().exists(
            configuration.getHome() + "/conf/catalina.properties"));
        assertTrue(configuration.getFileHandler().exists(
            configuration.getHome() + "/conf/Catalina/localhost/manager.xml"));
        assertTrue(configuration.getFileHandler().exists(
            configuration.getHome() + "/server/webapps/manager"));
    }

    /**
     * Test AJP configuration.
     * @throws Exception If anything goes wrong.
     */
    public void testConfigureSetsCorrectAJPConnectorIdentifier() throws Exception
    {
        // check protocol instead of classname, as class is not required.
        configuration.configure(container);
        String config =
            configuration.getFileHandler().readTextFile(
                configuration.getHome() + "/conf/server.xml", "UTF-8");
        XMLAssert.assertXpathEvaluatesTo("AJP/1.3", "//Connector[@port='8009']/@protocol", config);
    }

    /**
     * Test AJP configuration.
     * @throws Exception If anything goes wrong.
     */
    public void testConfigureSetsDefaultAJPPort() throws Exception
    {
        configuration.configure(container);
        String config =
            configuration.getFileHandler().readTextFile(
                configuration.getHome() + "/conf/server.xml", "UTF-8");
        XMLAssert.assertXpathEvaluatesTo(configuration
            .getPropertyValue(TomcatPropertySet.AJP_PORT),
            "//Connector[@protocol='AJP/1.3']/@port", config);
    }

    /**
     * Test AJP configuration.
     * @throws Exception If anything goes wrong.
     */
    public void testConfigureSetsAJPPort() throws Exception
    {
        configuration.setProperty(TomcatPropertySet.AJP_PORT, "1001");
        configuration.configure(container);
        String config =
            configuration.getFileHandler().readTextFile(
                configuration.getHome() + "/conf/server.xml", "UTF-8");
        XMLAssert.assertXpathEvaluatesTo("1001", "//Connector[@protocol='AJP/1.3']/@port", config);
    }

    /**
     * Test webapps directory configuration.
     * @throws Exception If anything goes wrong.
     */
    public void testConfigureDefaultWebappsDirectory() throws Exception
    {
        configuration.configure(container);
        String config =
            configuration.getFileHandler().readTextFile(
                configuration.getHome() + "/conf/server.xml", "UTF-8");
        XMLAssert.assertXpathEvaluatesTo("webapps", "//Host/@appBase", config);
    }

    /**
     * Test webapps directory configuration.
     * @throws Exception If anything goes wrong.
     */
    public void testConfigureSetsWebappsDirectory() throws Exception
    {
        configuration.setProperty(TomcatPropertySet.WEBAPPS_DIRECTORY, "some_directory");
        configuration.configure(container);
        String config =
            configuration.getFileHandler().readTextFile(
                configuration.getHome() + "/conf/server.xml", "UTF-8");
        XMLAssert.assertXpathEvaluatesTo("some_directory", "//Host/@appBase", config);
    }

}
