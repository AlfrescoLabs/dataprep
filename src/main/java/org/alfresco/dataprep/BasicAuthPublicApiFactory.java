/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
 * This file is part of Alfresco
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.dataprep;

import org.apache.commons.lang3.StringUtils;
import org.springframework.social.alfresco.api.Alfresco;
import org.springframework.social.alfresco.api.impl.ConnectionDetails;
import org.springframework.social.alfresco.connect.BasicAuthAlfrescoConnectionFactory;
import org.springframework.social.connect.Connection;
/**
 * A public api factory that uses basic authentication to communicate with a repository.
 * 
 * @author steveglover
 * @author Michael Suzuki
 *
 */
public class BasicAuthPublicApiFactory implements PublicApiFactory
{
    private String scheme;
    private String host;
    private int port;
    private int maxNumberOfConnections;
    private int connectionTimeoutMs;
    private int socketTimeoutMs;
    private int socketTtlMs;
    private String context;
    private String publicApiServletName;;
    private String serviceServletName;
    private boolean ignoreServletName;
    private final static String ADMIN_DEFAULT = "admin";
    
    public String getContext()
    {
        return context;
    }

    public boolean isIgnoreServletName()
    {
        return ignoreServletName;
    }

    public String getScheme()
    {
        return scheme;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public Alfresco getPublicApi(String username, String password)
    {
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            throw new IllegalArgumentException("Valid username and password are required");
        }
        Alfresco alfresco = null;
        ConnectionDetails connectionDetails = new ConnectionDetails(scheme, host, port, username, password, context,
                    publicApiServletName, serviceServletName, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs, socketTtlMs);
        BasicAuthAlfrescoConnectionFactory basicAuthConnectionFactory = new BasicAuthAlfrescoConnectionFactory(connectionDetails, null);
        Connection<Alfresco> connection = basicAuthConnectionFactory.createConnection();
        alfresco = connection.getApi();
        if (alfresco == null)
        {
            throw new RuntimeException("Unable to retrieve API connection to Alfresco.");
        }

        return alfresco;
    }
    public Alfresco getTenantAdminPublicApi(String domain)
    {
        if(StringUtils.isEmpty(domain))
        {
            throw new IllegalArgumentException("domain value is required");
        }
        ConnectionDetails connectionDetails = new ConnectionDetails(scheme, host, port, "admin@" + domain, ADMIN_DEFAULT, context,
                publicApiServletName, serviceServletName, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs, socketTtlMs);
        BasicAuthAlfrescoConnectionFactory connectionFactory = new BasicAuthAlfrescoConnectionFactory(connectionDetails, null);
        Connection<Alfresco> connection = connectionFactory.createConnection();
        Alfresco alfresco = connection.getApi();
        return alfresco;
    }
    
    public Alfresco getAdminPublicApi()
    {
        ConnectionDetails connectionDetails = new ConnectionDetails(scheme, host, port, ADMIN_DEFAULT, ADMIN_DEFAULT, context,
                publicApiServletName, serviceServletName, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs, socketTtlMs);
        BasicAuthAlfrescoConnectionFactory connectionFactory = new BasicAuthAlfrescoConnectionFactory(connectionDetails, null);
        Connection<Alfresco> connection = connectionFactory.createConnection();
        Alfresco alfresco = connection.getApi();
        return alfresco;
    }
    public int getMaxNumberOfConnections()
    {
        return maxNumberOfConnections;
    }
    public void setMaxNumberOfConnections(int maxNumberOfConnections)
    {
        this.maxNumberOfConnections = maxNumberOfConnections;
    }
    public int getConnectionTimeoutMs()
    {
        return connectionTimeoutMs;
    }
    public void setConnectionTimeoutMs(int connectionTimeoutMs)
    {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }
    public int getSocketTimeoutMs()
    {
        return socketTimeoutMs;
    }
    public void setSocketTimeoutMs(int socketTimeoutMs)
    {
        this.socketTimeoutMs = socketTimeoutMs;
    }
    public int getSocketTtlMs()
    {
        return socketTtlMs;
    }
    public void setSocketTtlMs(int socketTtlMs)
    {
        this.socketTtlMs = socketTtlMs;
    }
    public String getPublicApiServletName()
    {
        return publicApiServletName;
    }
    public void setPublicApiServletName(String publicApiServletName)
    {
        this.publicApiServletName = publicApiServletName;
    }
    public String getServiceServletName()
    {
        return serviceServletName;
    }
    public void setServiceServletName(String serviceServletName)
    {
        this.serviceServletName = serviceServletName;
    }
    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }
    public void setHost(String host)
    {
        this.host = host;
    }
    public void setPort(int port)
    {
        this.port = port;
    }
    public void setContext(String context)
    {
        this.context = context;
    }
    public void setIgnoreServletName(boolean ignoreServletName)
    {
        this.ignoreServletName = ignoreServletName;
    }
}
