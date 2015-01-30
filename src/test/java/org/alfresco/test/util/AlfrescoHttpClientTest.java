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

import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test the AlfrescoHttpClient helper class.
 * 
 * @author Michael Suzuki
 */
public class AlfrescoHttpClientTest
{
    AlfrescoHttpClient client;
    @BeforeMethod
    public void init()
    {
        client = new AlfrescoHttpClient();
    }
    @AfterMethod
    public void tearDown() throws IOException
    {
        client.close();
    }
    @Test
    public void getAlfTicketBadUrl() throws IOException, ParseException
    {
        String apiUrl = "http://127.0.0.1:8080/badUrl/";
        String ticket = client.getAlfTicket(apiUrl, "admin", "admin");
        Assert.assertEquals(ticket, "");
    }
    
    @Test
    public void getAlfTicketBadUser() throws IOException, ParseException
    {
        String apiUrl = "http://127.0.0.1:8080/alfresco/service/api/";
        String ticket = client.getAlfTicket(apiUrl, "someUser", "wrongPassword");
        Assert.assertEquals(ticket, "");
    }

}
