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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

/**
 * Create user helper class, creates an Alfresco user using public API.
 * 
 * @author Michael Suzuki
 * @author Bocancea Bogdan
 */
public class UserService
{
    private static Log logger = LogFactory.getLog(UserService.class);
    private AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    public static String DEFAULT_LAST_NAME = "lastName";
    public static String PAGE_ACCEPT_URL = "page/accept-invite";
    public static String PAGE_REJECT_URL = "page/reject-invite";
    
    public UserService(AlfrescoHttpClientFactory alfrescoHttpClientFactory)
    {
        this.alfrescoHttpClientFactory = alfrescoHttpClientFactory;
    }

    /**
     * Create an Alfresco user on enterprise.
     * 
     * @param adminUser admin username
     * @param adminPass password
     * @param userName new user
     * @param password new user password
     * @param email new user email
     * @return true if successful
     * @throws Exception if error
     */
    public boolean create(final String adminUser, 
                          final String adminPass, 
                          final String userName, 
                          final String password, 
                          final String email) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(adminUser)
                || StringUtils.isEmpty(adminPass) || StringUtils.isEmpty(email))
        {
            throw new IllegalArgumentException("User detail is required");
        }
        String firstName = userName;
        JSONObject body = encode(userName, password, firstName, DEFAULT_LAST_NAME, email);
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "people?alf_ticket=" + client.getAlfTicket(adminUser, adminPass);
        if (logger.isTraceEnabled())
        {
            logger.trace("Create user using Url - " + reqURL);
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
     * @param userName user identifier
     * @param password user password
     * @param firstName first name
     * @param lastName last name
     * @param email email
     * @return {@link JSONObject} of user entity
     */
    @SuppressWarnings("unchecked")
    private JSONObject encode(final String userName, 
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
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param username user identifier 
     * @return true if user exists
     * @throws Exception if error
     */
    public boolean userExists(final String adminUser, 
                              final String adminPass, 
                              final String username) throws Exception
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String ticket = client.getAlfTicket(adminUser, adminPass);
        String url = client.getApiUrl() + "people/" + username + "?alf_ticket=" + ticket;
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
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param userName user identifier
     * @return true if successful 
     * @throws Exception if error
     */
    public boolean delete(final String adminUser, 
                          final String adminPass, 
                          final String userName) throws Exception
    {
        if (StringUtils.isEmpty(userName) ||  StringUtils.isEmpty(adminUser) || StringUtils.isEmpty(adminPass))
        {
            throw new IllegalArgumentException("Null Parameters: Please correct");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String ticket = client.getAlfTicket(adminUser, adminPass);
        String url = client.getApiUrl() + "people/" + userName + "?alf_ticket=" + ticket;
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
    
    /**
     * Utility to invite a enterprise user to Site and accept the invitation
     * 
     * @param invitingUserName user identifier
     * @param invitingUserPassword user password
     * @param userToInvite user label
     * @param siteName site identifier which invite user.
     * @param role type
     * @return true if invite is successful
     * @throws Exception if error
     */
    public boolean inviteUserToSiteAndAccept(final String invitingUserName, 
                                             final String invitingUserPassword, 
                                             final String userToInvite,
                                             final String siteName,
                                             final String role) throws Exception
    {      
        if (StringUtils.isEmpty(invitingUserName) ||  StringUtils.isEmpty(invitingUserPassword) || StringUtils.isEmpty(userToInvite) ||
                StringUtils.isEmpty(siteName) || StringUtils.isEmpty(role) )
        {
            throw new IllegalArgumentException("Null Parameters: Please correct");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getApiUrl() + "invite/start?inviteeFirstName=" + userToInvite + 
                     "&inviteeLastName=" + DEFAULT_LAST_NAME + 
                     "&inviteeEmail=" + userToInvite + 
                     "&inviteeUserName=" + userToInvite +         
                     "&siteShortName=" + siteName +
                     "&inviteeSiteRole=" + role + 
                     "&serverPath="  + client.getHost()+
                     "&acceptUrl="  + PAGE_ACCEPT_URL +
                     "&rejectUrl="  + PAGE_REJECT_URL;

        if (logger.isTraceEnabled())
        {
            logger.trace("Invite user: " + userToInvite + " using Url - " + url);
        }    
        HttpGet get = new HttpGet(url);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(invitingUserName, invitingUserPassword);
        try
        {
            HttpResponse response = clientWithAuth.execute(get);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_OK:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("User successfully invited: " + userToInvite);
                    }
                    String result = client.readStream(response.getEntity()).toJSONString();
                    String inviteId = client.getParameterFromJSON(result, "inviteId");
                    String inviteTicket = client.getParameterFromJSON(result, "inviteTicket");
                    return acceptSiteInvitation(inviteId, inviteTicket);
                default:
                    logger.error("Unable to invite user: " + response.toString());
                    break;
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
     * Accept invitation to site.
     * 
     * @param inviteId identifier
     * @param inviteTicket authentication ticket
     * @return true if invite is successful
     * @throws Exception if error
     */
    private boolean acceptSiteInvitation(final String inviteId, 
                                         final String inviteTicket) throws Exception
    {
        if (StringUtils.isEmpty(inviteId) ||  StringUtils.isEmpty(inviteTicket))
        {
            throw new IllegalArgumentException("Null Parameters: Please correct");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getApiUrl() + "invite/" + inviteId + "/" + inviteTicket + "/accept";
        HttpPut put = new HttpPut(url);      
        try
        {
            HttpResponse response = client.executeRequest(put);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_OK:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("User accepted the invitation successfully");
                    }
                    return true;
                default:
                    logger.error("Unable to accept the invite: " + response.toString());
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
     * Method to request to join a site
     * 
     * @param userName
     * @param password
     * @param domain
     * @param siteId
     * @return true if request is successful
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean requestSiteMembership(final String userName,
                                         final String password, 
                                         final String siteId) throws Exception 
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteId))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String api = client.getApiUrl().replace("/service", "");
        String reqUrl = api + "-default-/public/alfresco/versions/1/people/" + userName + "/site-membership-requests";
        HttpPost post  = new HttpPost(reqUrl);
        JSONObject body = new JSONObject();
        body.put("id", siteId);
        post.setEntity(client.setMessageBody(body));
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(post);
            if(201 == response.getStatusLine().getStatusCode())
            {
                return true;
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
     * Delete a pending request for a Moderated Site
     * 
     * @param siteManager
     * @param passManager
     * @param userName - user that made the request to the Site
     * @param siteId
     * @return true if request is deleted (204 Status)
     * @throws Exception if error
     */
    public boolean removePendingSiteRequest(final String siteManager,
                                            final String passwordManager, 
                                            final String userName,
                                            final String siteId) throws Exception 
    {
        if (StringUtils.isEmpty(siteManager) || StringUtils.isEmpty(passwordManager) || StringUtils.isEmpty(siteId) 
                || StringUtils.isEmpty(userName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String api = client.getApiUrl().replace("/service", "");
        String reqUrl = api + "-default-/public/alfresco/versions/1/people/" + userName + "/site-membership-requests/" + siteId;
        HttpDelete delete  = new HttpDelete(reqUrl);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(siteManager, passwordManager);
        try
        {
            HttpResponse response = clientWithAuth.execute(delete);
            if(204 == response.getStatusLine().getStatusCode())
            {
                return true;
            }
        }
        finally
        {
            delete.releaseConnection();
            client.close();
        }
        return false;
    }
    
    /**
     * Remove a user from site
     * 
     * @param siteManager
     * @param passManager
     * @param userName - user that made the request to the Site
     * @param siteId
     * @return true if request is deleted (204 Status)
     * @throws Exception if error
     */
    public boolean removeSiteMembership(final String siteManager,
                                        final String passwordManager, 
                                        final String userName,
                                        final String siteId) throws Exception 
    {
        if (StringUtils.isEmpty(siteManager) || StringUtils.isEmpty(passwordManager) || StringUtils.isEmpty(siteId) 
                || StringUtils.isEmpty(userName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getApiUrl() + "sites/" + siteId.toLowerCase() + "/memberships/" + userName;
        HttpDelete delete  = new HttpDelete(url);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(siteManager, passwordManager);
        try
        {
            HttpResponse response = clientWithAuth.execute(delete);
            if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                return true;
            }
        }
        finally
        {
            delete.releaseConnection();
            client.close();
        }
        return false;
    }
    
    /**
     * Checks if group exists.
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName
     * @return true if user exists
     * @throws Exception if error
     */
    public boolean groupExists(final String adminUser,
                               final String adminPass,
                               final String groupName) throws Exception
    {
        if (StringUtils .isEmpty(adminUser) || StringUtils .isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "groups/" + groupName + "?alf_ticket=" + client.getAlfTicket(adminUser, adminPass);
        HttpGet request = new HttpGet(reqURL);
        try
        {
            HttpResponse response = client.executeRequest(request);
            if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                return true;
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
     * Create a new Group.
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName 
     * @return true if user exists
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean createGroup(final String adminUser,
                               final String adminPass,
                               final String groupName) throws Exception
    {
        if (StringUtils .isEmpty( adminUser) || StringUtils .isEmpty( adminPass) || StringUtils.isEmpty (groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "rootgroups/" + groupName + "?alf_ticket=" + client.getAlfTicket(adminUser, adminPass);
        if (logger.isTraceEnabled())
        {
            logger.trace ("Create group using url - " + reqURL);
        }
        HttpPost request = new HttpPost(reqURL);
        JSONObject body = new JSONObject();
        body.put("displayName", groupName);
        request.setEntity(client.setMessageBody(body));
        try
        {
            HttpResponse response = client.executeRequest(request);
            if(HttpStatus.SC_CREATED == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("Group: " + groupName + " is created successfully");
                }
                return true;
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
     * Add user to group.
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName 
     * @param userName
     * @return true if user exists
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean addUserToGroup(final String adminUser,
                                  final String adminPass,
                                  final String groupName,
                                  final String userName) throws Exception
    {
        if (StringUtils .isEmpty(adminUser) || StringUtils .isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "groups/" + groupName.toLowerCase() + "/children/" + userName + 
                "?alf_ticket=" + client.getAlfTicket(adminUser, adminPass);
        HttpPost request = new HttpPost(reqURL);
        JSONObject body = new JSONObject();
        body.put("", "");
        request.setEntity(client.setMessageBody(body));
        try
        {
            HttpResponse response = client.executeRequest(request);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_OK:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("User " + userName + " was added to " + groupName);
                    }
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Root group " + groupName + " not found");
                    }
                default:
                    logger.error("Unable to add user to group: " + response.toString());
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
     * Add sub group. If sub group doesn't exists it will be created.
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName 
     * @param userName
     * @return true if user exists
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean addSubGroup(final String adminUser,
                               final String adminPass,
                               final String groupName,
                               final String subGroup) throws Exception
    {
        if (StringUtils .isEmpty(adminUser) || StringUtils .isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "groups/" + groupName.toLowerCase() + "/children/GROUP_" + subGroup + 
                "?alf_ticket=" + client.getAlfTicket(adminUser, adminPass);
        HttpPost request = new HttpPost(reqURL);
        JSONObject body = new JSONObject();
        body.put("", "");
        request.setEntity(client.setMessageBody(body));
        try
        {
            HttpResponse response = client.executeRequest(request);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_CREATED:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Sub group " + subGroup + " was added to " + groupName);
                    }
                    return true;
                case HttpStatus.SC_OK:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Sub group " + subGroup + " was added to " + groupName);
                    }
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Root group " + groupName + " not found");
                    }
                default:
                    logger.error("Unable to add sub group to group: " + response.toString());
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
     * Remove user from group
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName 
     * @param userName
     * @return true if user exists
     * @throws Exception if error
     */
    public boolean removeUserFromGroup(final String adminUser,
                                       final String adminPass,
                                       final String groupName,
                                       final String userName) throws Exception
    {
        if (StringUtils .isEmpty(adminUser) || StringUtils .isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "groups/" + groupName.toLowerCase() + "/children/" + userName + 
                "?alf_ticket=" + client.getAlfTicket(adminUser, adminPass);
        HttpDelete request = new HttpDelete(reqURL);
        try
        {
            HttpResponse response = client.executeRequest(request);
            if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("User: " + userName + " is removed from " + groupName);
                }
                return true;
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
     * Remove subgroup from group
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName 
     * @param subGroup
     * @return true if user exists
     * @throws Exception if error
     */
    public boolean removeSubgroupFromGroup(final String adminUser,
                                           final String adminPass,
                                           final String groupName,
                                           final String subGroup) throws Exception
    {
        if (StringUtils .isEmpty(adminUser) || StringUtils .isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "groups/" + groupName.toLowerCase() + "/children/GROUP_" + subGroup + 
                "?alf_ticket=" + client.getAlfTicket(adminUser, adminPass);
        HttpDelete request = new HttpDelete(reqURL);
        try
        {
            HttpResponse response = client.executeRequest(request);
            if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("Sub group: " + subGroup + " is removed from " + groupName);
                }
                return true;
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
     * Remove a root group
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName 
     * @return true if user exists
     * @throws Exception if error
     */
    public boolean removeGroup(final String adminUser,
                               final String adminPass,
                               final String groupName) throws Exception
    {
        if (StringUtils .isEmpty(adminUser) || StringUtils .isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "rootgroups/" + groupName + "?alf_ticket=" + client.getAlfTicket(adminUser, adminPass);
        HttpDelete request = new HttpDelete(reqURL);
        try
        {
            HttpResponse response = client.executeRequest(request);
            if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("Group: " + groupName + " is removed successfully");
                }
                return true;
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
     * Count users and groups added in root group
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName
     * @return true if user exists
     * @throws Exception if error
     */
    public int countAuthoritiesFromGroup(final String adminUser,
                                         final String adminPass,
                                         final String groupName) throws Exception
    {
        if (StringUtils .isEmpty(adminUser) || StringUtils .isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "groups/" + groupName + "/children?alf_ticket=" + client.getAlfTicket(adminUser, adminPass);
        HttpGet request = new HttpGet(reqURL);
        try
        {
            HttpResponse response = client.executeRequest(request);
            if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                HttpEntity entity = response. getEntity();
                String responseString = EntityUtils.toString(entity , "UTF-8");        
                JSONParser parser = new JSONParser();
                JSONObject obj = (JSONObject) parser.parse(responseString);       
                JSONObject data = (JSONObject) obj.get("paging");
                long count = (Long) data.get("totalItems");
                Integer i = (int) (long) count;
                return i;
            }
        }
        finally
        {
            request.releaseConnection();
            client.close();
        }
        return 0;
    }
    
    /**
     * Count members of a site
     * 
     * @param userName user name that owns the site
     * @param userPass user password
     * @param siteName
     * @return total number of site members
     * @throws Exception if error
     */
    public int countSiteMembers(final String userName,
                                final String userPass,
                                final String siteName) throws Exception
    { 
           int count=0;
           if (StringUtils .isEmpty(userName) || StringUtils .isEmpty(userPass) || StringUtils.isEmpty(siteName))
           {
               throw new IllegalArgumentException("Parameter missing");
           }
           
           AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
           String reqURL = client.getApiUrl() + "sites/" + siteName + "/memberships?alf_ticket=" + client.getAlfTicket(userName, userPass);
           HttpGet request = new HttpGet(reqURL);
           
           try
           {
               HttpResponse response = client.executeRequest(request);
               if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
               {
                   HttpEntity entity = response.getEntity();
                   String responseString = EntityUtils.toString(entity , "UTF-8"); 
                   Object obj = JSONValue.parse(responseString);
                   JSONArray array = (JSONArray)obj;
                   count = array.size();
               }
           }
           finally
           {
               request.releaseConnection();
               client.close();
           }
           
           return count;
    }
    
}
