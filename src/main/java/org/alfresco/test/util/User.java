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
import org.json.simple.JSONObject;

/**
 * Create user helper class, creates an Alfresco user using public API.
 * 
 * @author Michael Suzuki
 * @author Bocancea Bogdan
 */
public class User
{
    private static Log logger = LogFactory.getLog(User.class);

    /**
     * Create an Alfresco user.
     * @param shareUrl
     * @param adminUser
     * @param adminPass
     * @param userName
     * @param password
     * @param email
     * @return true if successful
     * @throws Exception if error
     */
    public static boolean create(final String shareUrl,
                                 final String adminUser,
                                 final String adminPass,
                                 final String userName,
                                 final String password,
                                 final String email) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)
                || StringUtils.isEmpty(shareUrl) || StringUtils.isEmpty(adminUser)
                || StringUtils.isEmpty(adminPass) || StringUtils.isEmpty(email))
        {
            throw new IllegalArgumentException("User detail is required");
        }
        String firstName = userName;
        String defaultLastName = "lastName";
        JSONObject body = encode(userName, password, firstName, defaultLastName, email);

        String apiUrl = shareUrl.replaceFirst("share", AlfrescoHttpClient.ALFRESCO_API_PATH);
        String reqURL = apiUrl + "people?alf_ticket=" + AlfrescoHttpClient.getAlfTicket(apiUrl, adminUser, adminPass);
        if(logger.isTraceEnabled())
        {
            logger.trace("Using Url - " + reqURL);
        }
        // Create the http client 
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = null;
        HttpResponse response = null;
        try
        {
            request = AlfrescoHttpClient.generatePostRequest(reqURL, body);
            response = AlfrescoHttpClient.executeRequest(client, request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("User created successfully: " + userName);
                }
                return true;
            }
            else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CONFLICT)
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("User: " + userName + " alreary created");
                }
                return false;
            }
            else
            {
                logger.error("Unable to create user: " + response.toString());
                return false;
            }
        }
        finally
        {
            request.releaseConnection();
        }
    }
    /**
     * Builds a json object representing the user data.
     * @param userName
     * @param password
     * @param firstName
     * @param lastName
     * @param email
     * @return
     */
    @SuppressWarnings("unchecked")
    private static JSONObject encode(final String userName,
                              final String password,
                              final String firstName,
                              final String lastName,
                              final String email)
    {
        JSONObject body = new JSONObject();
        body.put("userName", userName);
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("password", password);
        body.put("email", email);
        return body;
    }
}
