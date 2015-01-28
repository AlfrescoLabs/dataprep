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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Helper class that provides HttpClient.
 * @author Michael Suzuki
 */
public class AlfrescoHttpClient
{
    /**
     * Provides an http client with basic authentication based on port 443.
     * @param url API url location
     * @param username String user identifier 
     * @param password String user password
     * @return {@link HttpClient} client
     */
    public static HttpClient getHttpClientWithBasicAuth(String url, String username, String password)
    {
        return getHttpClientWithBasicAuth(url, 443, username, password);
    }
    /**
     * Provides an http client with basic authentication.
     * @param url API url location
     * @param port int port number
     * @param username String user identifier 
     * @param password String user password
     * @return {@link HttpClient} client
     */
    public static HttpClient getHttpClientWithBasicAuth(String url,int port, String username, String password)
    {
        if(StringUtils.isEmpty(username)||StringUtils.isEmpty(password))
        {
            throw new IllegalArgumentException(
                    String.format("Input required username was %s and password %s",username,password));
        }
        if(StringUtils.isEmpty(url))
        {
            
            throw new IllegalArgumentException("API URL is required");
        }
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(url, port),
                new UsernamePasswordCredentials(username, password));
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        return httpclient;
    }
}
