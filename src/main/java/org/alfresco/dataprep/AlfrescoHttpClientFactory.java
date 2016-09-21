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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Scope;

/**
 * Alfresco HttpClient factory.
 * 
 * @author Michael Suzuki
 */
public class AlfrescoHttpClientFactory implements FactoryBean<AlfrescoHttpClient>
{
    private String host;
    private String scheme;
    private int port;
    private int sharePort;
    
    public int getSharePort()
    {
        return sharePort;
    }

    public void setSharePort(int sharePort)
    {
        this.sharePort = sharePort;
    }

    @Scope("prototype")
    public AlfrescoHttpClient getObject()
    {
        return new AlfrescoHttpClient(scheme, host, port, sharePort);
    }

    public Class<?> getObjectType()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isSingleton()
    {
        return false;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getScheme()
    {
        return scheme;
    }

    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

}
