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

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the AlfrescoHttpClient helper class.
 * 
 * @author Michael Suzuki
 */
public class AlfrescoHttpClientTest
{
    @Test
    public void getClientWithAuth()
    {
        HttpClient client = AlfrescoHttpClient.getHttpClientWithBasicAuth("localhost", "admin", "password");
        Assert.assertNotNull(client);
        client = AlfrescoHttpClient.getHttpClientWithBasicAuth("localhost", 442, "admin", "password");
        Assert.assertNotNull(client);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getClientWithInvalidParam()
    {
        AlfrescoHttpClient.getHttpClientWithBasicAuth("localhost", null, "pass");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getClientWithInvalidPassword()
    {
        AlfrescoHttpClient.getHttpClientWithBasicAuth("localhost", "michael", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getClientWithInvalidUrlParam()
    {
        AlfrescoHttpClient.getHttpClientWithBasicAuth(null, "test", "test");
    }

    @Test
    public void getAlfrescoTicket() throws IOException, JSONException
    {
        String apiUrl = "http://127.0.0.1:8080/alfresco/service/api/";
        String ticket = AlfrescoHttpClient.getAlfTicket(apiUrl, "admin", "admin");
        Assert.assertNotNull(ticket);
    }

    @Test
    public void getAlfTicketBadUrl() throws IOException, JSONException
    {
        String apiUrl = "http://127.0.0.1:8080/badUrl/";
        String ticket = AlfrescoHttpClient.getAlfTicket(apiUrl, "admin", "admin");
        Assert.assertEquals(ticket, "");
    }
    
    @Test
    public void getAlfTicketBadUser() throws IOException, JSONException
    {
        String apiUrl = "http://127.0.0.1:8080/alfresco/service/api/";
        String ticket = AlfrescoHttpClient.getAlfTicket(apiUrl, "someUser", "wrongPassword");
        Assert.assertEquals(ticket, "");
    }

}
