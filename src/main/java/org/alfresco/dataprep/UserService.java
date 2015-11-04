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
package org.alfresco.dataprep;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.dataprep.DashboardCustomization.DashletLayout;
import org.alfresco.dataprep.DashboardCustomization.UserDashlet;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
/**
 * Create user helper class, creates an Alfresco user using public API.
 * 
 * @author Michael Suzuki
 * @author Bocancea Bogdan
 */
public class UserService
{
    private static Log logger = LogFactory.getLog(UserService.class);
    @Autowired private  AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    public static String DEFAULT_LAST_NAME = "lastName";
    public static String PAGE_ACCEPT_URL = "page/accept-invite";
    public static String PAGE_REJECT_URL = "page/reject-invite";

    /**
     * Create an Alfresco user on enterprise.
     * 
     * @param adminUser admin username
     * @param adminPass password
     * @param userName String identifier new user
     * @param password new user password
     * @param email new user email
     * @param firstName first name
     * @param lastName last name
     * @return true if successful
     * @throws Exception 
     */
    public boolean create(final String adminUser, 
                          final String adminPass, 
                          final String userName, 
                          final String password, 
                          final String email,
                          final String firstName,
                          final String lastName) throws Exception 
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) ||
            StringUtils.isEmpty(adminUser) || StringUtils.isEmpty(adminPass) || 
            StringUtils.isEmpty(email) || StringUtils.isEmpty(firstName) ||
            StringUtils.isEmpty(lastName))
        {
            throw new IllegalArgumentException("User detail is required");
        }

        JSONObject body = encode(userName, password, firstName, lastName, email);
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "people";
        if (logger.isTraceEnabled())
        {
            logger.trace("Create user using Url - " + reqURL);
        }
        HttpPost post = new HttpPost(reqURL);
        HttpResponse response = client.executeRequest(client, adminUser, adminPass, body, post);
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
        return false;
    }

    /**
     * Builds a json object representing the user data.
     * 
     * @param userName String identifier user identifier
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
        String url = client.getApiUrl() + "people/" + URIUtil.encodeWithinPath(username);
        HttpGet get = new HttpGet(url);
        HttpResponse response = client.executeRequest(client, adminUser, adminPass, get);
        if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            return true;
        }
        return false;
    }
    /**
     * Delete a user from enterprise.
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param userName String identifier user identifier
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
        String url = client.getApiUrl() + "people/" + URIUtil.encodeWithinPath(userName);
        HttpDelete httpDelete = new HttpDelete(url);
        HttpResponse response = client.executeRequest(client, adminUser, adminPass, httpDelete);
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
        return false;
    }
    
    /**
     * Utility to invite a enterprise user to Site and accept the invitation
     * 
     * @param invitingUserName user identifier
     * @param invitingUserPassword user password
     * @param userToInvite user label
     * @param siteName site identifier which invite user.
     * @param role user role
     * @return true if invite is successful
     * @exception if error
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
                    String alfVersion = client.getAlfrescoVersion();
                    if(alfVersion.contains("5.1"))
                    {
                        return true;
                    }
                    else
                    {
                        String result = client.readStream(response.getEntity()).toJSONString();
                        String inviteId = client.getParameterFromJSON(result, "inviteId");
                        String inviteTicket = client.getParameterFromJSON(result, "inviteTicket");
                        return acceptSiteInvitation(inviteId, inviteTicket);
                    }
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
                                         final String inviteTicket)
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
     * @param siteManager String site manager
     * @param passwordManager String password
     * @param siteId site identifier
     * @param groupName group to be invited
     * @param role applied for the group
     * @return true if request is successful
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean inviteGroupToSite(final String siteManager,
                                     final String passwordManager, 
                                     final String siteId,
                                     final String groupName,
                                     final String role) throws Exception 
    {
        if (StringUtils.isEmpty(siteManager) || StringUtils.isEmpty(siteManager) || StringUtils.isEmpty(siteId)
                || StringUtils.isEmpty(groupName) || StringUtils.isEmpty(role))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiUrl() + "sites/" + siteId.toLowerCase() + "/memberships";
        HttpPost post  = new HttpPost(reqUrl);    
        JSONObject body = new JSONObject();
        JSONObject group = new JSONObject();
        group.put("fullName", "GROUP_" + groupName);
        body.put("role", role);
        body.put("group", group);
        HttpResponse response = client.executeRequest(client, siteManager, passwordManager, body, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Group " + groupName + " successfuly invited to site " + siteId);
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site, user or role");
            default:
                logger.error("Unable to invite group " + groupName + " " +  response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Method to request to join a site
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteId site identifier
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
        HttpResponse response = client.executeRequest(client, userName, password, body, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_CREATED:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Successfuly requested membership to site " + siteId);
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site " + siteId);
            case HttpStatus.SC_BAD_REQUEST:
                logger.error("Request to site: " + siteId + " has been already done");
            default:
                logger.error("Unable to  request membership to " + siteId + " " +  response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Method to add a member to a site (public, moderated or private)
     * 
     * @param siteManager String manager of the site
     * @param passwordManager String password
     * @param userName String user to added
     * @param siteId  site id
     * @param role String role to be applied
     * @return true if request is successful
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean createSiteMember(final String siteManager,
                                    final String passwordManager, 
                                    final String userName,
                                    final String siteId,
                                    final String role) throws Exception
    {
        if (StringUtils.isEmpty(siteManager) || StringUtils.isEmpty(passwordManager) || StringUtils.isEmpty(siteId) 
                || StringUtils.isEmpty(userName) || StringUtils.isEmpty(role))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiUrl().replace("/service", "") +
                "-default-/public/alfresco/versions/1/sites/" + siteId + "/members";
        HttpPost post  = new HttpPost(reqUrl);
        JSONObject body = new JSONObject();
        body.put("id", userName);
        body.put("role", role);
        HttpResponse response = client.executeRequest(client, siteManager, passwordManager, body, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_CREATED:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Successfuly added member to site " + siteId);
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site " + siteId);
            case HttpStatus.SC_BAD_REQUEST:
                throw new RuntimeException("Invalid role " + role);
            case HttpStatus.SC_CONFLICT:
                logger.error("User " + userName + " is already member of site " + siteId);
                break;
            default:
                logger.error("Unable to  request membership to " + siteId + " " +  response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Delete a pending request for a Moderated or Private Site
     * 
     * @param siteManager site manager id
     * @param passwordManager password
     * @param userName String identifier - user that made the request to the Site
     * @param siteId site identifier
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
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/site-membership-requests/" + siteId;
        HttpDelete delete  = new HttpDelete(reqUrl);
        HttpResponse response = client.executeRequest(client, siteManager, passwordManager, delete);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_NO_CONTENT:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Site doesn't exists or the site is PUBLIC " + siteId);
            default:
                logger.error("Unable to remove pending request: " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Remove a user from site
     * 
     * @param siteManager String site manager
     * @param passwordManager String password
     * @param userName String identifier - user that made the request to the Site
     * @param siteId String site id
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
        HttpResponse response = client.executeRequest(client, siteManager, passwordManager, delete);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Site doesn't exists " + siteId);
            default:
                logger.error("Unable to remove pending request: " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Change the role for a user or group 
     * 
     * @param siteManager String site manager
     * @param passwordManager String password
     * @param siteName String site id
     * @param entity String identifier - user name or group
     * @param role String role
     * @param isGroup boolean user or group
     * @return true if request is successful (Status: 200)
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    private boolean changeUserRole(final String siteManager,
                                   final String passwordManager,
                                   final String siteName,
                                   final String entity,
                                   final String role,
                                   final boolean isGroup) throws Exception
    {
        String reqUrl;
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String api = client.getApiUrl().replace("/service", "");
        JSONObject userBody = new JSONObject();
        JSONObject grpBody = new JSONObject();
        if(!isGroup)
        {
            reqUrl = api + "-default-/public/alfresco/versions/1/sites/" + siteName + "/members/" + entity;
            userBody.put("role", role);
        }
        else
        {
            reqUrl = client.getApiUrl() + "sites/" + siteName.toLowerCase() + "/memberships";
            JSONObject group = new JSONObject();
            group.put("fullName", "GROUP_" + entity);
            grpBody.put("role", role);
            grpBody.put("group", group);
        }       
        HttpPut put = new HttpPut(reqUrl);
        HttpResponse response = null;
        if(!isGroup)
        {
            put.setEntity(client.setMessageBody(userBody));
            response = client.executeRequest(client, siteManager, passwordManager, userBody, put);
        }
        else
        {
            put.setEntity(client.setMessageBody(grpBody));
            response = client.executeRequest(client, siteManager, passwordManager, grpBody, put);
        }
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Role " + role + " successfully updated for " + entity);
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site " + siteName);
            case HttpStatus.SC_BAD_REQUEST:
                throw new RuntimeException(entity + "is not a member of site " + siteName);
            default:
                logger.error("Unable to change the role for: " + entity + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Change the role for a user 
     * 
     * @param siteManager String site manager
     * @param passwordManager String password
     * @param siteName String site id
     * @param userName String identifier - user name
     * @param role String role
     * @return true if request is successful (Status: 200)
     * @throws Exception if error
     */
    public boolean changeUserRole(final String siteManager,
                                  final String passwordManager,
                                  final String siteName,
                                  final String userName,
                                  final String role) throws Exception
    {
        if (StringUtils.isEmpty(siteManager) || StringUtils.isEmpty(passwordManager) || StringUtils.isEmpty(siteName) 
                || StringUtils.isEmpty(userName) || StringUtils.isEmpty(role))
        {
            throw new IllegalArgumentException("Parameter missing");
        }         
        return changeUserRole(siteManager, passwordManager, siteName, userName, role, false);
    }
    
    /**
     * Change the role for a group 
     * 
     * @param siteManager String site manager
     * @param passwordManager String password
     * @param siteName String site id
     * @param groupName String identifier - group name   
     * @param role String role
     * @return true if request is successful (Status: 200)
     * @throws Exception if error
     */
    public boolean changeGroupRole(final String siteManager,
                                   final String passwordManager,
                                   final String siteName,
                                   final String groupName,
                                   final String role) throws Exception
    {
        if (StringUtils.isEmpty(siteManager) || StringUtils.isEmpty(passwordManager) || StringUtils.isEmpty(siteName) 
                || StringUtils.isEmpty(groupName) || StringUtils.isEmpty(role))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        return changeUserRole(siteManager, passwordManager, siteName, groupName, role, true);
    }
    
    /**
     * Checks if group exists.
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName String group name
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
        String reqURL = client.getApiUrl() + "groups/" + groupName;
        HttpGet request = new HttpGet(reqURL);
        HttpResponse response = client.executeRequest(client, adminUser, adminPass, request);
        if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            return true;
        }
        return false;
    }

    /**
     * Create a new Group.
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName group name
     * @return true if user exists
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean createGroup(final String adminUser,
                               final String adminPass,
                               final String groupName) throws Exception
    {
        if (StringUtils.isEmpty( adminUser) || StringUtils.isEmpty( adminPass) || StringUtils.isEmpty (groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "rootgroups/" + groupName;
        if (logger.isTraceEnabled())
        {
            logger.trace ("Create group using url - " + reqURL);
        }
        HttpPost request = new HttpPost(reqURL);
        JSONObject body = new JSONObject();
        body.put("displayName", groupName);
        HttpResponse response = client.executeRequest(client, adminUser, adminPass, body, request);
        if(HttpStatus.SC_CREATED == response.getStatusLine().getStatusCode())
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Group: " + groupName + " is created successfully");
            }
            return true;
        }      
        return false;
    }
    
    /**
     * Add user to group.
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName group name 
     * @param userName String identifier
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
        String reqURL = client.getApiUrl() + "groups/" + groupName + "/children/" + userName;
        HttpPost request = new HttpPost(reqURL);
        JSONObject body = new JSONObject();
        body.put("", "");
        HttpResponse response = client.executeRequest(client, adminUser, adminPass, body, request);
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
        return false;
    }

    /**
     * Add sub group. If sub group doesn't exists it will be created.
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName group name 
     * @param subGroup sub group name
     * @return true if subgroup is created
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
        String reqURL = client.getApiUrl() + "groups/" + groupName + "/children/GROUP_" + subGroup;
        HttpPost request = new HttpPost(reqURL);
        JSONObject body = new JSONObject();
        body.put("", "");
        HttpResponse response = client.executeRequest(client, adminUser, adminPass, body, request);
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
        return false;
    }

    /**
     * Remove user from group
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName group name
     * @param userName String identifier
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
        String reqURL = client.getApiUrl() + "groups/" + groupName + "/children/" + userName;
        HttpDelete request = new HttpDelete(reqURL);
        HttpResponse response = client.executeRequest(client, adminUser, adminPass, request);
        if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("User: " + userName + " is removed from " + groupName);
            }
            return true;
        }
        return false;
    }

    /**
     * Remove subgroup from group
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName group name 
     * @param subGroup sub group name
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
        String reqURL = client.getApiUrl() + "groups/" + groupName + "/children/GROUP_" + subGroup;
        HttpDelete request = new HttpDelete(reqURL);
        HttpResponse response = client.executeRequest(client, adminUser, adminPass, request);
        if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Sub group: " + subGroup + " is removed from " + groupName);
            }
            return true;
        }
        return false;
    }

    /**
     * Remove a root group
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName String group name
     * @return true if user exists
     * @throws Exception if error
     */
    public boolean removeGroup(final String adminUser,
                               final String adminPass,
                               final String groupName) throws Exception
    {
        if (StringUtils.isEmpty(adminUser) || StringUtils.isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "rootgroups/" + groupName;
        HttpDelete request = new HttpDelete(reqURL);
        HttpResponse response = client.executeRequest(client, adminUser, adminPass, request);
        if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Group: " + groupName + " is removed successfully");
            }
            return true;
        }
        return false;
    }
    
    /**
     * Get group details
     * 
     * @param adminUser String admin user
     * @param adminPass String admin password
     * @param groupName String group
     * @return HttpReponse 200 if ok
     * @throws Exception
     */
    private HttpResponse getGroupDetails(final String adminUser,
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
        return client.executeRequest(request);
    }
    
    /**
     * Count users and groups added in root group
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName String group name
     * @return true if user exists
     * @throws Exception if error
     */
    public int countAuthoritiesFromGroup(final String adminUser,
                                         final String adminPass,
                                         final String groupName) throws Exception
    {
        HttpResponse response = getGroupDetails(adminUser, adminPass, groupName);
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
        return 0;
    }
    
    /**
     * Verify if a user is member of a group.
     * 
     * @param adminUser String admin user
     * @param adminPass String admin password
     * @param groupName String group
     * @param userName String user to be searched
     * @return boolean true if user is found
     * @throws Exception
     */
    public boolean isUserAddedToGroup(final String adminUser,
                                      final String adminPass,
                                      final String groupName,
                                      final String userName) throws Exception
    {
        HttpResponse response = getGroupDetails(adminUser, adminPass, groupName);
        if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
            List<String> users = client.getElementsFromJsonArray(response, "data", "shortName");
            for(String user: users)
            {
                if(userName.toString().equalsIgnoreCase(user))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Count members of a site
     * 
     * @param userName login user that owns the site
     * @param userPass user password
     * @param siteName String site name
     * @return total number of site members
     * @throws Exception if error
     */
    public int countSiteMembers(final String userName,
                                final String userPass,
                                final String siteName) throws Exception
    { 
        int count=0;
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(userPass) || StringUtils.isEmpty(siteName))
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
                String responseString = EntityUtils.toString(entity, "UTF-8"); 
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
    
    /**
     * Login in alfresco share
     * 
     * @param userName login user name
     * @param userPass login user password
     * @return true for successful user login
     * @throws Exception if error
     */
    public HttpState login(final String userName,
                           final String userPass) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(userPass))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        HttpState state = null;
        org.apache.commons.httpclient.HttpClient theClient = new org.apache.commons.httpclient.HttpClient();
        String reqURL = client.getAlfrescoUrl() + "share/page/dologin";
        org.apache.commons.httpclient.methods.PostMethod post = new org.apache.commons.httpclient.methods.PostMethod(reqURL);
        NameValuePair[] formParams;
        CookieStore cookieStore = new BasicCookieStore();
        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCookieStore(cookieStore);
        formParams = (new NameValuePair[]{
                      new NameValuePair("username", userName),
                      new NameValuePair("password", userPass),
                      new NameValuePair("success", "/share/page/user/" + userName + "/dashboard"),
                      new NameValuePair("failure", "/share/page/type/login?error=true")});
        post.setRequestBody(formParams);
        int postStatus = theClient.executeMethod(post);
        if(302 == postStatus)
        {
            state = theClient.getState();
            post.releaseConnection();
            org.apache.commons.httpclient.methods.GetMethod get = new org.apache.commons.httpclient.methods.GetMethod(
                    client.getAlfrescoUrl() + "share/page/user/" + userName + "/dashboard");
            theClient.setState(state);
            theClient.executeMethod(get);
            get.releaseConnection();
        }
        return state;
    }
    
    /**
     * Add dashlet to user dashboard
     * 
     * @param userName String identifier
     * @param password String password
     * @param dashlet Dashlet dashlet
     * @param layout dashlet layout 
     * @param column int column index
     * @param position position in column
     * @return true if the dashlet is added
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean addDashlet(final String userName,
                              final String password,
                              final UserDashlet dashlet,
                              final DashletLayout layout,
                              final int column,
                              final int position) throws Exception
    {
        login(userName, password);
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getAlfrescoUrl() + DashboardCustomization.ADD_DASHLET_URL;
        JSONObject body = new JSONObject();
        JSONArray array = new JSONArray();
        body.put("dashboardPage", "user/" + userName + "/dashboard");
        body.put("templateId", layout.id);
        
        // keep default dashlets
        Hashtable<String, String> defaultDashlets = new Hashtable<String, String>();
        defaultDashlets.put(UserDashlet.MY_SITES.id, "component-1-1");
        defaultDashlets.put(UserDashlet.MY_TASKS.id, "component-1-2");
        defaultDashlets.put(UserDashlet.MY_ACTIVITIES.id, "component-2-1");
        defaultDashlets.put(UserDashlet.MY_DOCUMENTS.id, "component-2-2");
        
        Iterator<Map.Entry<String, String>> entries = defaultDashlets.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<String, String> entry = entries.next();
            JSONObject jDashlet = new JSONObject();
            jDashlet.put("url", entry.getKey());
            jDashlet.put("regionId", entry.getValue());
            jDashlet.put("originalRegionId", entry.getValue());
            array.add(jDashlet);
        }
        JSONObject newDashlet = new JSONObject();
        newDashlet.put("url", dashlet.id);
        String region = "component-" + column + "-" + position;
        newDashlet.put("regionId", region);
        array.add(newDashlet);
        body.put("dashlets", array);
        HttpPost post  = new HttpPost(url);
        post.setEntity(client.setMessageBody(body));
        try
        {
            HttpResponse response = client.executeRequest(post);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("Dashlet " + dashlet.name + " was added on user: " + userName + " dashboard");
                }
                return true;
            }
            else
            {
                logger.error("Unable to add dashlet to user dashboard " + userName);
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
     * Logout the current user from share.
     * 
     * @return HttpState 
     * @throws Exception if error
     */
    public HttpState logout() throws Exception
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        HttpState state = null;
        org.apache.commons.httpclient.HttpClient theClient = new org.apache.commons.httpclient.HttpClient();
        String reqURL = client.getAlfrescoUrl() + "share/page/dologout";
        org.apache.commons.httpclient.methods.PostMethod post = new org.apache.commons.httpclient.methods.PostMethod(reqURL);
        theClient.executeMethod(post);
        state = theClient.getState();
        return state;
    }
    
    /**
     * Follow or unfollow a user
     * 
     * @param userName String user name
     * @param password String password
     * @param userToFollow String user to be followed
     * @return true if user is followed successfully
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private boolean toFollowOrUnfollow(final String userName,
                                       final String password,
                                       final String userToFollowOrNot, 
                                       final boolean follow) throws Exception
    {
        String url;
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(userToFollowOrNot))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        if(follow)
        {
            url = client.getApiUrl() + "subscriptions/" + userName + "/follow";
        }
        else
        {
            url = client.getApiUrl() + "subscriptions/" + userName + "/unfollow";
        }
        HttpPost request = new HttpPost(url);
        JSONArray requestBody = new JSONArray();
        requestBody.add(userToFollowOrNot);
        request.setEntity(new StringEntity(requestBody.toString()));
        HttpResponse response = client.executeRequest(client, userName, password, request);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_NO_CONTENT:
                if (logger.isTraceEnabled())
                {
                    if(follow)
                    {
                        logger.trace("User " + userName + " is now following user " + userToFollowOrNot);
                    }
                    else
                    {
                        logger.trace("User " + userName + " is not following " + userToFollowOrNot);
                    }
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                if (logger.isTraceEnabled())
                {
                    logger.trace("User " + userToFollowOrNot + " does not exist");
                }
            default:
                logger.error("Unable to follow user: " + userToFollowOrNot + " - " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Follow a user
     * 
     * @param userName String user name
     * @param password String password
     * @param userToFollow String user to be followed
     * @return true if user is followed successfully
     * @throws Exception
     */
    public boolean followUser(final String userName,
                              final String password,
                              final String userToFollow) throws Exception
    {
        return toFollowOrUnfollow(userName, password, userToFollow, true);
    }
    
    /**
     * Unfollow a user
     * 
     * @param userName String user name
     * @param password String password
     * @param userToFollow String user to be followed
     * @return true if user is followed successfully
     * @throws Exception
     */
    public boolean unfollowUser(final String userName,
                              final String password,
                              final String userToFollow) throws Exception
    {
        return toFollowOrUnfollow(userName, password, userToFollow, false);
    }
    
    /**
     * Get a list of followers
     * 
     * @param userName
     * @param password
     * @return List<String> list of followers
     * @throws Exception
     */
    private List<String> getFollowUsers(final String userName,
                                        final String password,
                                        final boolean followers) throws Exception
    {
        String reqURL;
        List<String> userFollowers = new ArrayList<String>();
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        if(followers)
        {
            reqURL = client.getApiUrl() + "subscriptions/" + userName + "/followers";
        }
        else
        {
            reqURL = client.getApiUrl() + "subscriptions/" + userName + "/following";
        }
        HttpGet get = new HttpGet(reqURL);
        try
        {
            HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
            HttpResponse response = clientWithAuth.execute(get);
            if(200 == response.getStatusLine().getStatusCode())
            {
                userFollowers = client.getElementsFromJsonArray(response, "people", "userName");
            }
            return userFollowers;
        } 
        finally
        {
            get.releaseConnection();
            client.close();
        }
    }
    
    /**
     * Get a list of followers
     * 
     * @param userName
     * @param password
     * @return List<String> list of followers
     * @throws Exception
     */
    public List<String> getFollowers(final String userName,
                                     final String password) throws Exception
    {
        return getFollowUsers(userName, password, true);
    }
    
    /**
     * Get a list of followers
     * 
     * @param userName
     * @param password
     * @return List<String> list of followers
     * @throws Exception
     */
    public List<String> getFollowingUsers(final String userName,
                                          final String password) throws Exception
    {
        return getFollowUsers(userName, password, false);
    }
}
