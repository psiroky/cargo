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
package org.codehaus.cargo.maven2.configuration;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Properties;

/**
 * Holds configuration data for the <code>&lt;proxy&gt;</code> tag used to configure the plugin in
 * the <code>pom.xml</code> file.
 * 
 */
public class Proxy
{
    /**
     * Proxy host.
     */
    private String host;

    /**
     * Proxy port. Default to 80 if not set.
     */
    private int port = 80;

    /**
     * Username for authenticating proxies.
     */
    private String user;

    /**
     * Password for authenticating proxies.
     */
    private String password;

    /**
     * List of hosts that do not go through the proxy server. The value can be a list of hosts,
     * each seperated by a <code>|</code>, and in addition a wildcard character <code>(*)</code>
     * can be used for matching. For example: <code>*.foo.com|localhost</code>
     */
    private String excludeHosts = "";

    /**
     * @param host Proxy host.
     */
    public void setHost(String host)
    {
        this.host = host;
    }

    /**
     * @return Proxy host.
     */
    public String getHost()
    {
        return this.host;
    }

    /**
     * @param port Proxy port.
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * @return Proxy port. Default to 80 if not set.
     */
    public int getPort()
    {
        return this.port;
    }

    /**
     * @param user Username for authenticating proxies.
     */
    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * @return Username for authenticating proxies.
     */
    public String getUser()
    {
        return this.user;
    }

    /**
     * @param password Password for authenticating proxies.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * @return Password for authenticating proxies.
     */
    public String getPassword()
    {
        return this.password;
    }

    /**
     * @param proxyExcludeHosts List of hosts that do not go through the proxy server. The value
     * can be a list of hosts, each seperated by a <code>|</code>, and in addition a wildcard
     * character <code>(*)</code> can be used for matching. For example:
     * <code>*.foo.com|localhost</code>
     */
    public void setExcludeHosts(String proxyExcludeHosts)
    {
        this.excludeHosts = proxyExcludeHosts;
    }

    /**
     * @return List of hosts that do not go through the proxy server. The value can be a list of
     * hosts, each seperated by a <code>|</code>, and in addition a wildcard character
     * <code>(*)</code> can be used for matching. For example: <code>*.foo.com|localhost</code>
     */
    public String getExcludeHosts()
    {
        return this.excludeHosts;
    }

    /**
     * Set the Java system properties related to proxies.
     */
    public void configure()
    {
        if (getHost() != null && getHost().trim().length() > 0)
        {
            Properties sysprops = System.getProperties();
            String portString = Integer.toString(getPort());

            sysprops.put("http.proxyHost", getHost());
            sysprops.put("http.proxyPort", portString);
            sysprops.put("https.proxyHost", getHost());
            sysprops.put("https.proxyPort", portString);
            sysprops.put("ftp.proxyHost", getHost());
            sysprops.put("ftp.proxyPort", portString);

            if (getExcludeHosts() != null)
            {
                sysprops.put("http.nonProxyHosts", getExcludeHosts());
                sysprops.put("https.nonProxyHosts", getExcludeHosts());
                sysprops.put("ftp.nonProxyHosts", getExcludeHosts());
            }
            if (getUser() != null)
            {
                sysprops.put("http.proxyUser", getUser());
                sysprops.put("http.proxyPassword", getPassword());
                Authenticator.setDefault(new ProxyAuthenticator(getUser(), getPassword()));
            }
        }
    }

    /**
     * Clear all proxy settings.
     */
    public void clear()
    {
        Properties sysprops = System.getProperties();
        sysprops.remove("http.proxyHost");
        sysprops.remove("http.proxyPort");
        sysprops.remove("http.proxyUser");
        sysprops.remove("http.proxyPassword");
        sysprops.remove("https.proxyHost");
        sysprops.remove("https.proxyPort");
        sysprops.remove("ftp.proxyHost");
        sysprops.remove("ftp.proxyPort");
        Authenticator.setDefault(new ProxyAuthenticator("", ""));
    }

    /**
     * Authenticator for the Proxy.
     */
    private static final class ProxyAuthenticator extends Authenticator
    {
        /**
         * Authentication using a username/password.
         */
        private PasswordAuthentication authentication;

        /**
         * @param user the username
         * @param pass the password
         */
        private ProxyAuthenticator(String user, String pass)
        {
            this.authentication = new PasswordAuthentication(user, pass.toCharArray());
        }

        /**
         * @return The {@link PasswordAuthentication} object associated with this authenticator.
         */
        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
            return this.authentication;
        }
    }
}
