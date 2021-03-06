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
package org.codehaus.cargo.container.resin;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.tools.ant.types.FilterChain;
import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.configuration.builder.ConfigurationBuilder;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.LoggingLevel;
import org.codehaus.cargo.container.resin.internal.AbstractResinStandaloneLocalConfiguration;
import org.codehaus.cargo.container.resin.internal.Resin2xConfigurationBuilder;
import org.codehaus.cargo.container.resin.internal.Resin2xStandaloneLocalConfigurationCapability;

/**
 * Resin 2.x standalone
 * {@link org.codehaus.cargo.container.spi.configuration.ContainerConfiguration} implementation.
 * 
 */
public class Resin2xStandaloneLocalConfiguration extends
    AbstractResinStandaloneLocalConfiguration
{
    /**
     * Where elements for resources will be inserted.
     */
    public static final String XML_PARENT_OF_RESOURCES = "//caucho.com";

    /**
     * Capability of the Resin standalone configuration.
     * 
     * @see ResinStandaloneLocalConfigurationCapability
     */
    private static ConfigurationCapability capability =
        new Resin2xStandaloneLocalConfigurationCapability();

    /**
     * {@inheritDoc}
     * 
     * @see AbstractResinStandaloneLocalConfiguration#AbstractResinStandaloneLocalConfiguration(String)
     */
    public Resin2xStandaloneLocalConfiguration(String dir)
    {
        super(dir);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.cargo.container.spi.configuration.AbstractStandaloneLocalConfiguration#AbstractStandaloneLocalConfiguration(String)
     */
    public ConfigurationCapability getCapability()
    {
        return capability;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getXpathForResourcesParent()
    {
        return "//caucho.com";
    }

    /**
     * {@inheritDoc} Resin2x application servers currently use DTD, and therefore return and empty
     * map;
     */
    @Override
    protected Map<String, String> getNamespaces()
    {
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Resin2xConfigurationBuilder
     */
    @Override
    protected ConfigurationBuilder createConfigurationBuilder(
        LocalContainer container)
    {
        return new Resin2xConfigurationBuilder();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractResinStandaloneLocalConfiguration#prepareConfigurationDirectory(org.codehaus.cargo.container.Container, java.lang.String)
     */
    protected void prepareConfigurationDirectory(Container container, String confDir)
        throws IOException
    {
        FilterChain filterChain = getFilterChain();

        // Add expanded WAR support
        getAntUtils().addTokenToFilterChain(filterChain, "resin.expanded.webapps",
            createExpandedWarTokenValue("app-dir"));

        // Add token filters for adding users
        getAntUtils().addTokenToFilterChain(filterChain, "resin2x.users",
            getSecurityToken("<init-param user='", "'/>"));

        // Add logging property tokens

        // Note: The tokenValue value must never be an empty string as otherwise
        // the Ant filtering code fails.
        String tokenValue = " ";
        String logLevel = getPropertyValue(GeneralPropertySet.LOGGING);
        if (LoggingLevel.HIGH.equalsLevel(logLevel))
        {
            tokenValue = "<log id='/' href='stdout:' timestamp='[%H:%M:%S.%s]'/>";
        }
        getAntUtils().addTokenToFilterChain(filterChain, "resin2x.debuglog", tokenValue);

        // make sure you use this method, as it ensures the same filehandler
        // that created the directory will be used to copy the resource.
        // This is especially important for unit testing
        getResourceUtils().copyResource(RESOURCE_PATH + container.getId() + "/resin.conf",
            getFileHandler().append(confDir, "resin.conf"), getFileHandler(), getFilterChain(), 
            "UTF-8");
    }

}
