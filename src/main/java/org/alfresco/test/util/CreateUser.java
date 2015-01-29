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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Create user helper class, creates an Alfresco user using public API.
 * 
 * @author Michael Suzuki
 * @author Bocancea Bogdan
 */
public class CreateUser extends AlfrescoHttpClient
{
    private static Log logger = LogFactory.getLog(CreateUser.class);

    public static boolean createEnterpriseUser(String shareUrl, String adminUser, String adminPass, String userName, String password, String email)
            throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(shareUrl) || StringUtils.isEmpty(adminUser)
                || StringUtils.isEmpty(adminPass) || StringUtils.isEmpty(email))
        {
            throw new IllegalArgumentException("Null Parameters: Please correct");
        }

        String apiUrl = shareUrl.replaceFirst("share", apiContextEnt);
        String firstName = userName;
        String defaultLastName = "lastName";

        String reqURL = apiUrl + "people?alf_ticket=" + getAlfTicket(apiUrl, adminUser, adminPass);
        logger.info("Using Url - " + reqURL);
        String[] headers = getRequestHeaders(MIME_TYPE_JSON + ";charset=" + UTF_8_ENCODING);
        String[] body = { "userName", userName, "firstName", firstName, "lastName", defaultLastName, "password", password, "email", email };
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = null;
        HttpResponse response = null;

        try
        {
            request = generatePostRequest(reqURL, headers, body);
            response = executeRequest(client, request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
            {
                logger.info("User created successfully: " + userName);
                return true;
            }
            else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CONFLICT)
            {
                logger.info("User: " + userName + " alreary created");
                return false;
            }
            else
            {
                logger.error(response.toString());
                return false;
            }
        }
        finally
        {
            request.releaseConnection();
        }

    }
}
