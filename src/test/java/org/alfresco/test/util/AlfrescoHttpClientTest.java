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

import org.apache.http.client.HttpClient;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the AlfrescoHttpClient helper class.
 * @author Michael Suzuki
 *
 */
public class AlfrescoHttpClientTest
{
    @Test
    public void getClientWithAuth()
    {
        HttpClient client = AlfrescoHttpClient.getHttpClientWithBasicAuth("admin", "password");
        Assert.assertNotNull(client);
    }
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getClientWithInvalidParam()
    {
        HttpClient client = AlfrescoHttpClient.getHttpClientWithBasicAuth(null, null);
        Assert.assertNotNull(client);
    }
}
