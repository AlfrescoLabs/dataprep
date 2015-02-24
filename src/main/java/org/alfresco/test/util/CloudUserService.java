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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Create user helper class, creates an Alfresco user on Cloud using API.
 * 
 * @author Bocancea Bogdan
 */
public class CloudUserService
{
    private static Log logger = LogFactory.getLog(CloudUserService.class);
    private AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    public static String DEFAULT_LAST_NAME = "lastName";
    
    public CloudUserService(AlfrescoHttpClientFactory alfrescoHttpClientFactory)
    {
        this.alfrescoHttpClientFactory = alfrescoHttpClientFactory;
    }
    
    /**
     * Create an Alfresco user on cloud.
     * 
     * @param userName
     * @param password
     * @param email
     * @return true if the user is created successfully
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean create(final String email, 
                          final String password) throws Exception
    {
        if (StringUtils.isEmpty(password) || StringUtils.isEmpty(email))
        {
            throw new IllegalArgumentException("User detail is required");
        }       
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getCloudObject();
        String reqURL = client.getCloudApiUrl() + AlfrescoHttpClient.CONTEXT_CLOUD_INTERNAL + "accounts/signupqueue";
        if (logger.isTraceEnabled())
        {
            logger.trace("Using Url - " + reqURL);
        }
        HttpPost request = null;
        HttpResponse response = null;
        JSONObject body = new JSONObject();
        body.put("source", "test-rest-client-script");
        body.put("email", email);
        try
        {
            request = client.generatePostRequest(reqURL, body);
            response = client.executeRequest(request);
            String result = client.readStream(response.getEntity()).toJSONString();
            String regKey = client.getParameterFromJSON(result, "registration", "key");
            String regId = client.getParameterFromJSON(result, "registration", "id");
            return activateUser(email, password, regKey, regId);
        }
        finally
        {
            request.releaseConnection();
            client.close();
        }      
    }
    
    /**
     * Method to create user with a network-admin role. 
     * It is required to upgrade the Account Type from free to Enterprise (1000).
     *   
     * @throws Exception
     */
    public boolean createUserAsTenantAdmin(final String authUser,
                                           final String authUserPassword,
                                           final String email, 
                                           final String password) throws Exception
    {
        boolean result = false;
        result = create(email, password);
        if(result)
        {
            String domain = getUserDomain(email);
            upgradeCloudAccount(domain, authUser, authUserPassword, "1000");
            return promoteUserAsAdmin(authUser, authUserPassword, email, domain);
        }
        return false;
        
    }
    
    /**
     * Activate the sign up request for a cloud user.
     * 
     * @param userName
     * @param password
     * @param email
     * @return true if the user is activated successfully
     * @throws Exception
     */
    private boolean activateUser(final String email, 
                                 final String password, 
                                 final String regKey, 
                                 final String regId) throws Exception
    {
        String firstName = email;
        JSONObject body = encode(password, firstName, DEFAULT_LAST_NAME, regKey, regId);
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getCloudObject();
        String reqURL = client.getCloudApiUrl() + AlfrescoHttpClient.CONTEXT_CLOUD_INTERNAL + "account-activations";
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
                        logger.trace("User created successfully: " + email);
                    }
                    return true;
                case HttpStatus.SC_CONFLICT:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("User: " + email + " alreary created");
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
     * Utility to upgrade the account type (for the given domain)
     * 
     * @param domain Domain Name to be upgraded
     * @param authUser authenticating user
     * @param authUserPass
     * @param accountTypeID AccountType ID e.g. 1000 if enterprise, 0 if free, 101 for partner
     * @return true if account upgrade succeeds
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public boolean upgradeCloudAccount(final String domain, 
                                       final String authUser,
                                       final String authUserPass,
                                       final String accountTypeID) throws Exception
    {
        if (StringUtils.isEmpty(domain) || StringUtils.isEmpty(accountTypeID))
        {
            throw new IllegalArgumentException("User detail is required");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getCloudObject();
        String reqURL = client.getCloudApiUrl() + AlfrescoHttpClient.CONTEXT_CLOUD_INTERNAL + "domains/" + domain.toLowerCase() + "/account";
        if (logger.isTraceEnabled())
        {
            logger.trace("Using Url - " + reqURL);
        }
        HttpPut put = new HttpPut(reqURL);
        HttpResponse response = null;      
        put.addHeader("key", client.getCloudKey());   
        JSONObject body = new JSONObject();
        body.put("accountTypeId", accountTypeID);
        put.setEntity(client.setMessageBody(body));
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(authUser, authUserPass);
        try
        {
            response = clientWithAuth.execute(put);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_OK:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Domain: " + domain + " was upgraded successfully");
                    }
                    return true;
                default:
                    logger.error("Unable to upgrade domain: " + domain + response.toString());
                    break;
            }
        }
        finally
        {
            put.releaseConnection();
            client.close();
        }                
        return false;
    }
    
    /**
     * Utility to verify if a user exists
     * 
     * @param email
     * @return true if user exists
     */
    public boolean userExists(final String email) throws Exception
    {
        
        if (StringUtils.isEmpty(email))
        {
            throw new IllegalArgumentException("User detail is required");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getCloudObject();
        String reqURL = client.getCloudApiUrl() + AlfrescoHttpClient.CONTEXT_CLOUD_INTERNAL + "user/" + email + "/accounts";
        if (logger.isTraceEnabled())
        {
            logger.trace("Using Url - " + reqURL);
        }
        HttpGet get = new HttpGet(reqURL);
        get.addHeader("key", client.getCloudKey());
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth("admin", "admin");
        try
        {
            HttpResponse response = clientWithAuth.execute(get);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                return true;
            }
        }
        finally
        {
            get.releaseConnection();
            client.close();
        }
        return false;
    }
      
    /**
     * Utility to invite a cloud user to Site using invite-To-Site- And
     * Activate-Invitation via rest API request
     * 
     * @param invitingUsername The email of inviting user
     * @param invitingUserpass The password of inviting user
     * @param emailUserToInvite The email of user that is being invited
     * @param userToInvitePass The password of user that is being invited
     * @param siteShortname Name of the site to which the user is being invited
     * @param role Role to be assigned to the invited user
     * @param message String message to be sent with the invitation
     * @return true is user invitation-acceptance succeeds
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public boolean inviteUserToSiteAndAccept(final String invitingUsername,
                                             final String invitingUserpass,
                                             final String emailUserToInvite,
                                             final String userToInvitePass,
                                             final String siteName, 
                                             final String role, 
                                             final String message) throws Exception
    {
        String domainName = getUserDomain(emailUserToInvite);

        if (StringUtils.isEmpty(invitingUsername) || StringUtils.isEmpty(invitingUserpass) || StringUtils.isEmpty(emailUserToInvite) || 
                StringUtils.isEmpty(siteName) || StringUtils.isEmpty(role))
        {
            throw new IllegalArgumentException("User detail is required");
        }
       
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getCloudObject();
        String reqURL = client.getCloudApiUrl() + AlfrescoHttpClient.CONTEXT_CLOUD_INTERNAL + "sites/" + siteName.toLowerCase() + "/invitations";
        if (logger.isTraceEnabled())
        {
            logger.trace("Request URL - " + reqURL + " for Site Invitation");
        }       
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(invitingUsername, invitingUserpass);
        HttpPost post = new HttpPost(reqURL);
        JSONObject body = new JSONObject();
        JSONArray list = new JSONArray();
        list.add(emailUserToInvite);
        body.put("inviterEmail", invitingUsername);
        body.put("inviteeEmails", list);
        body.put("role", role);
        body.put("inviterMessage", message);  
        post.setEntity(client.setMessageBody(body));
        HttpResponse response = null;
        post.addHeader("key", client.getCloudKey());        
        try
        {
            response = clientWithAuth.execute(post);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_OK:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("User successfully invited: " + emailUserToInvite);
                    }
                    String result = client.readStream(response.getEntity()).toJSONString();
                    String regKey = client.getParameterFromJSON(result, "invitations", "key");
                    String regId = client.getParameterFromJSON(result, "invitations", "id");
                    return userAcceptsSiteInvite(emailUserToInvite, userToInvitePass, domainName, regKey, regId);
                default:
                    logger.error("Unable to invite user: " + response.toString());
                    break;
            }
        }
        finally
        {
            post.releaseConnection();
            client.close();
        }   
        return false;
    }
    
    /**
     * Utility to Accept invitation to site on Cloud via rest API request
     * 
     * @param invitedUserEmail String email of user that is invited
     * @param invitedUserPass String password of user that is invited
     * @param invitedToDomain String Domain name of the user that is being invited
     * @param regKey
     * @param regid
     * @return true if user invitation-acceptance succeeds
     * @throws Exception
     */
    public boolean userAcceptsSiteInvite(final String invitedUserEmail, 
                                         final String invitedUserPass,
                                         final String invitedToDomain, 
                                         final String regKey, 
                                         final String regId) throws Exception
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getCloudObject();
        String reqURL = client.getCloudUrl() + invitedToDomain + "/page/invitation?key=" + regKey + "&id=" + regId;
        if (logger.isTraceEnabled())
        {
            logger.trace("Request URL - " + reqURL + " for accepting the site invitation");
        }       
        HttpGet get = new HttpGet(reqURL);
        get.addHeader("key", client.getCloudKey());
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(invitedUserEmail, invitedUserPass);
        try
        {
            HttpResponse response = clientWithAuth.execute(get);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("User accepted the invitation successfully");
                }
                return true;
            }
            else
            {
                logger.error("Unable to accept the invite: " + response.toString());
            }
        }
        finally
        {
            get.releaseConnection();
            client.close();
        } 
        return false;
    }
    
    /**
     * Utility to promote the user as network admin (for the given domain)
     * 
     * @param authUser String authenticating user
     * @param authUserPass String authenticating user password
     * @param userToBePromoted String userName to be promoted as network admin
     * @param domain String domain for which the user is being upgraded.
     * @return true if succeeds
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public boolean promoteUserAsAdmin(final String authUser,
                                      final String authUserPass,
                                      final String userToBePromoted,
                                      final String domain) throws Exception
    {
        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getCloudObject();
        String reqURL = client.getCloudApiUrl() + AlfrescoHttpClient.CONTEXT_CLOUD_INTERNAL + "domains/" + domain.toLowerCase() + "/account/networkadmins";
        if (logger.isTraceEnabled())
        {
            logger.trace("Request URL - " + reqURL + " for accepting the site invitation");
        }
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(authUser, authUserPass);
        HttpPost post = new HttpPost(reqURL);
        JSONObject body = new JSONObject();
        body.put("username", userToBePromoted);
        post.setEntity(client.setMessageBody(body));
        HttpResponse response = null;
        post.addHeader("key", client.getCloudKey());     
        try
        {
            response = clientWithAuth.execute(post);
            
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("User successfully promoted as Admin: " + userToBePromoted);
                }
                return true;
            }
            else
            {
                logger.error("Unable to promote user: " + response.toString());
            }
        }
        finally
        {
            post.releaseConnection();
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
     * @param regKey
     * @param regId
     * @return JSONObject
     */
    @SuppressWarnings("unchecked")
    private JSONObject encode(final String password, 
                              final String firstName, 
                              final String lastName, 
                              final String regKey,
                              final String regId)
    {
        JSONObject body = new JSONObject();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("password", password);
        body.put("key", regKey);
        body.put("id", regId);
        return body;
    }
    
    /**
     * This method is used to get the userDomain from the username value.
     * 
     * @param cloudUser
     * @return String
     */
    public String getUserDomain(String cloudUser)
    {
        if (StringUtils.isEmpty(cloudUser))
        {
            throw new IllegalArgumentException("Null Parameters: Please correct");
        }     
        return cloudUser.substring(cloudUser.lastIndexOf("@") + 1, cloudUser.length());
    }
    
    
}
