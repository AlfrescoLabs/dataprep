package org.alfresco.dataprep;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
/**
 * Create group helper class, creates an Alfresco group using public API.
 * 
 * @author Bocancea Bogdan
 */

public class GroupService
{
    private static Log logger = LogFactory.getLog(GroupService.class);
    @Autowired private  AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    
    /**
     * Method to request to join a site
     * 
     * @param siteManager String site manager
     * @param passwordManager String password
     * @param siteId site identifier
     * @param groupName group to be invited
     * @param role applied for the group
     * @return true if request is successful
     * @throws RuntimeException if invalid site or user role
     */
    @SuppressWarnings("unchecked")
    public boolean inviteGroupToSite(final String siteManager,
                                     final String passwordManager, 
                                     final String siteId,
                                     final String groupName,
                                     final String role)
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
        HttpResponse response = client.executeRequest(siteManager, passwordManager, body, post);
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
     * Checks if group exists.
     * 
     * @param adminUser admin username
     * @param adminPass admin credential
     * @param groupName String group name
     * @return true if user exists
     */
    public boolean groupExists(final String adminUser,
                               final String adminPass,
                               final String groupName)
    {
        if (StringUtils .isEmpty(adminUser) || StringUtils .isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "groups/" + groupName;
        HttpGet request = new HttpGet(reqURL);
        HttpResponse response = client.executeRequest(adminUser, adminPass, request);
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
     */
    @SuppressWarnings("unchecked")
    public boolean createGroup(final String adminUser,
                               final String adminPass,
                               final String groupName)
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
        HttpResponse response = client.executeRequest(adminUser, adminPass, body, request);
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
     */
    @SuppressWarnings("unchecked")
    public boolean addUserToGroup(final String adminUser,
                                  final String adminPass,
                                  final String groupName,
                                  final String userName)
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
        HttpResponse response = client.executeRequest(adminUser, adminPass, body, request);
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
     */
    @SuppressWarnings("unchecked")
    public boolean addSubGroup(final String adminUser,
                               final String adminPass,
                               final String groupName,
                               final String subGroup)
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
        HttpResponse response = client.executeRequest(adminUser, adminPass, body, request);
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
     */
    public boolean removeUserFromGroup(final String adminUser,
                                       final String adminPass,
                                       final String groupName,
                                       final String userName)
    {
        if (StringUtils .isEmpty(adminUser) || StringUtils .isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "groups/" + groupName + "/children/" + userName;
        HttpDelete request = new HttpDelete(reqURL);
        HttpResponse response = client.executeRequest(adminUser, adminPass, request);
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
     */
    public boolean removeSubgroupFromGroup(final String adminUser,
                                           final String adminPass,
                                           final String groupName,
                                           final String subGroup)
    {
        if (StringUtils .isEmpty(adminUser) || StringUtils .isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "groups/" + groupName + "/children/GROUP_" + subGroup;
        HttpDelete request = new HttpDelete(reqURL);
        HttpResponse response = client.executeRequest(adminUser, adminPass, request);
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
     */
    public boolean removeGroup(final String adminUser,
                               final String adminPass,
                               final String groupName)
    {
        if (StringUtils.isEmpty(adminUser) || StringUtils.isEmpty(adminPass) || StringUtils.isEmpty(groupName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getApiUrl() + "rootgroups/" + groupName;
        HttpDelete request = new HttpDelete(reqURL);
        HttpResponse response = client.executeRequest(adminUser, adminPass, request);
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
     */
    private HttpResponse getGroupDetails(final String adminUser,
                                         final String adminPass,
                                         final String groupName)
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
     */
    public int countAuthoritiesFromGroup(final String adminUser,
                                         final String adminPass,
                                         final String groupName)
    {
        HttpResponse response = getGroupDetails(adminUser, adminPass, groupName);
        if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            HttpEntity entity = response. getEntity();
            JSONObject obj = null;
            try
            {
                String responseString = EntityUtils.toString(entity , "UTF-8");
                JSONParser parser = new JSONParser();
                obj = (JSONObject) parser.parse(responseString);
            }
            catch (IOException | ParseException e)
            {
                logger.error("Failed to parse response", e);
            }
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
     */
    public boolean isUserAddedToGroup(final String adminUser,
                                      final String adminPass,
                                      final String groupName,
                                      final String userName)
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
     * Change the role for a group 
     * 
     * @param siteManager String site manager
     * @param passwordManager String password
     * @param siteName String site id
     * @param groupName String identifier group name
     * @param role String role
     * @return true if request is successful (Status: 200)
     * @throws RuntimeException if invalid site or if groupName is 
     * not member of the site
     */
    @SuppressWarnings("unchecked")
    public boolean changeGroupRole(final String siteManager,
                                   final String passwordManager,
                                   final String siteName,
                                   final String groupName,
                                   final String role)
    {
        String reqUrl;
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        JSONObject grpBody = new JSONObject();
        reqUrl = client.getApiUrl() + "sites/" + siteName.toLowerCase() + "/memberships";
        JSONObject group = new JSONObject();
        group.put("fullName", "GROUP_" + groupName);
        grpBody.put("role", role);
        grpBody.put("group", group);
        HttpPut put = new HttpPut(reqUrl);
        HttpResponse response = null;
        put.setEntity(client.setMessageBody(grpBody));
        response = client.executeRequest(siteManager, passwordManager, grpBody, put);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Role " + role + " successfully updated for " + groupName);
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site " + siteName);
            case HttpStatus.SC_BAD_REQUEST:
                throw new RuntimeException(groupName + " is not a member of site " + siteName);
            default:
                logger.error("Unable to change the role for: " + groupName + " " + response.toString());
                break;
        }
        return false;
    }
}
