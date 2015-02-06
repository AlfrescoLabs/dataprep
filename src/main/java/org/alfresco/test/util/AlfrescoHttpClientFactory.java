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
package org.alfresco.test.util;

import org.springframework.beans.factory.FactoryBean;

/**
 * Alfresco HttpClient factory.
 * 
 * @author Michael Suzuki
 */
public class AlfrescoHttpClientFactory
{

    private String host;
    private String scheme;
    private int port;
    AlfrescoHttpClientFactory(final String scheme, final String host, int port)
    {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
    }
    public AlfrescoHttpClient getObject() throws Exception
    {
        return new AlfrescoHttpClient(scheme, host, port);
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

}
