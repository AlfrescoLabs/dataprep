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
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.json.simple.JSONObject;

/**
 * Create user helper class, creates an Alfresco user using public API.
 * 
 * @author Michael Suzuki
 * @author Bocancea Bogdan
 */
public class UserService
{
    private static Log logger = LogFactory.getLog(UserService.class);

    /**
     * Create an Alfresco user on enterprise.
     * 
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
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(shareUrl) || StringUtils.isEmpty(adminUser)
                || StringUtils.isEmpty(adminPass) || StringUtils.isEmpty(email))
        {
            throw new IllegalArgumentException("User detail is required");
        }
        String firstName = userName;
        String defaultLastName = "lastName";
        JSONObject body = encode(userName, password, firstName, defaultLastName, email);
        AlfrescoHttpClient client = new AlfrescoHttpClient();

        String apiUrl = shareUrl.replaceFirst("share", AlfrescoHttpClient.ALFRESCO_API_PATH);
        String reqURL = apiUrl + "people?alf_ticket=" + client.getAlfTicket(apiUrl, adminUser, adminPass);
        if (logger.isTraceEnabled())
        {
            logger.trace("Using Url - " + reqURL);
        }
        HttpPost request = null;
        HttpResponse response = null;
        try
        {
            request = client.generatePostRequest(reqURL, body);
            response = client.executeRequest(request);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_OK:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("User created successfully: " + userName);
                    }
                    return true;
                case HttpStatus.SC_CONFLICT:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("User: " + userName + " alreary created");
                    }

                    break;
                default:
                    logger.error("Unable to create user: " + response.toString());
                    break;
            }
        }
        finally
        {
            request.releaseConnection();
            client.close();
        }
        return false;
    }

    /**
     * Builds a json object representing the user data.
     * 
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

    /**
     * Checks if user already exists.
     * 
     * @param shareUrl
     * @param username
     * @return true if user exists
     * @throws Exception
     */
    public static boolean userExists(final String shareUrl, 
                                     final String adminUser, 
                                     final String adminPass, 
                                     final String username) throws Exception
    {
        AlfrescoHttpClient client = new AlfrescoHttpClient();
        String ticket = client.getAlfTicket(shareUrl, adminUser, adminPass);
        String url = client.parsePath(shareUrl) + "people/" + username + "?alf_ticket=" + ticket;
        HttpGet get = new HttpGet(url);
        try
        {
            HttpResponse response = client.executeRequest(get);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                return true;
            }
        }
        finally
        {
            client.close();
        }
        return false;
    }

    /**
     * Delete a user from enterprise.
     * 
     * @param shareUrl
     * @param adminUser
     * @param adminPass
     * @param userName
     * @return true if successful
     * @throws Exception
     */
    public static boolean delete(final String shareUrl, 
                                 final String adminUser, 
                                 final String adminPass, 
                                 final String userName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(shareUrl) || StringUtils.isEmpty(adminUser) || StringUtils.isEmpty(adminPass))
        {
            throw new IllegalArgumentException("Null Parameters: Please correct");
        }
        AlfrescoHttpClient client = new AlfrescoHttpClient();
        String ticket = client.getAlfTicket(shareUrl, adminUser, adminPass);
        String url = client.parsePath(shareUrl) + "people/" + userName + "?alf_ticket=" + ticket;
        HttpDelete httpDelete = new HttpDelete(url);
        try
        {
            HttpResponse response = client.executeRequest(httpDelete);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_OK:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("User deleted successfully: " + userName);
                    }
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    throw new RuntimeException("User: " + userName + " doesn't exists");
                default:
                    logger.error("Unable to delete user: " + response.toString());
                    break;
            }
        }
        finally
        {
            httpDelete.releaseConnection();
            client.close();
        }
        return false;
    }
}
