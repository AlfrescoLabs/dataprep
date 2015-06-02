/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.test.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.test.util.DashboardCustomization.DashletLayout;
import org.alfresco.test.util.DashboardCustomization.Page;
import org.alfresco.test.util.DashboardCustomization.SiteDashlet;
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
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.springframework.social.alfresco.api.Alfresco;
import org.springframework.social.alfresco.api.entities.Site.Visibility;

/**
 * Site utility helper that performs crud operation on Site.
 * <ul>
 * <li> Creates an Alfresco site.
 * <li> Deletes an Alfresco site.
 * <li> Mark as favorite
 * <li> Remove favorite
 * </ul>
 * @author Michael Suzuki
 * @author Bogdan Bocancea
 *
 */
public class SiteService
{
    private final PublicApiFactory publicApiFactory;
    private final AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    private static Log logger = LogFactory.getLog(SiteService.class);
    
    public SiteService(PublicApiFactory publicApiFactory, AlfrescoHttpClientFactory alfrescoHttpClientFactory)
    {
        this.publicApiFactory = publicApiFactory;
        this.alfrescoHttpClientFactory = alfrescoHttpClientFactory;
    }
    
    /**
     * Create site using Alfresco public API.
     * 
     * @param username identifier
     * @param password user password
     * @param domain the comany or org id
     * @param siteId site identifier
     * @param description site description
     * @param visability site visability type
     * @throws IOException io error
     */
    public void create(final String username,
                       final String password,
                       final String domain, 
                       final String siteId,
                       final String description,
                       final Visibility visability) throws IOException
    {
        Alfresco publicApi = publicApiFactory.getPublicApi(username,password);
        publicApi.createSite(domain, 
                             siteId, 
                             "site-dashboard", 
                             siteId,
                             description, 
                             visability);
    }
    /**
     * Checks if site exists
     * 
     * @param siteId site identifier
     * @param username site user
     * @param password user password
     * @return true if exists
     * @throws Exception if error
     */
    public boolean exists(final String siteId, 
                          final String username,
                          final String password) throws Exception
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        try
        {
            String ticket = client.getAlfTicket(username, password);
            String apiUrl = client.getApiUrl();
            String url = String.format("%ssites/%s?alf_ticket=%s",apiUrl, siteId, ticket);
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.executeRequest(get);
            if( 200 == response.getStatusLine().getStatusCode())
            {
                return true;
            }
            return false;
        } 
        finally
        {
            client.close();
        }
    }
    /**
     * Delete an alfresco site.
     * 
     * @param username user details
     * @param password user details
     * @param domain user details 
     * @param siteId site identifier
     */
    public void delete(final String username,
                       final String password,
                       final String domain, 
                       final String siteId)
    {
        Alfresco publicApi = publicApiFactory.getPublicApi(username,password);
        publicApi.removeSite(domain, siteId);
    }
    
    /**
     * Gets all existing sites
     * 
     * @param username site user
     * @param password user password
     * @return list of sites
     * @throws Exception if error
     */
    public List<String> getSites(final String username,
                                 final String password) throws Exception
    {
        List<String> mySitesList=new ArrayList<String>() ;
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        try
        {
            String ticket = client.getAlfTicket(username, password);
            String apiUrl = client.getApiUrl();
            String url = String.format("%ssites?alf_ticket=%s",apiUrl, ticket);
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.executeRequest(get);
            if( 200 == response.getStatusLine().getStatusCode())
            {
                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity , "UTF-8"); 
                Object obj=JSONValue.parse(responseString);
                JSONArray jarray=(JSONArray)obj;           
                for (Object item:jarray)
                {
                    JSONObject jobject=(JSONObject) item;
                    mySitesList.add(jobject.get("title").toString());
                    System.out.println("----"+jobject.get("title").toString());
                }      
            }
            return mySitesList;
        } 
        finally
        {
            client.close();
        }
    }
    
    /**
     * Get site node ref 
     * 
     * @param userName
     * @param password
     * @param siteName
     * @return String
     * @throws Exception if error
     */
    public String getSiteNodeRef(final String userName,
                                 final String password,
                                 final String siteName) throws Exception
    {
        String siteNodeRef = "";
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }            
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiVersionUrl() + "sites/" + siteName;        
        try
        {
            HttpGet get = new HttpGet(reqUrl);
            HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
            HttpResponse response = clientWithAuth.execute(get);        
            if( HttpStatus.SC_OK  == response.getStatusLine().getStatusCode())
            {
                String result = client.readStream(response.getEntity()).toJSONString();
                if(!StringUtils.isEmpty(result))
                {
                    JSONParser parser = new JSONParser();  
                    Object obj = parser.parse(result);
                    JSONObject jsonObject = (JSONObject) obj;  
                    JSONObject sites = (JSONObject) jsonObject.get("entry");
                    return siteNodeRef = (String) sites.get("guid");
                }
            }
        }
        finally
        {
            client.close();
        }    
        return siteNodeRef;
    }
    
    /**
     * Set site as favorite
     * 
     * @param userName
     * @param password
     * @param siteName
     * @return true if marked as favorite
     * @throws Exception if error
     */
    public boolean setFavorite(final String userName,
                               final String password,
                               final String siteName) throws Exception
    {
        if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef = getSiteNodeRef(userName, password, siteName);
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites";
        HttpPost post  = new HttpPost(reqUrl);
        String jsonInput;
        jsonInput = "{\"target\": {\"" + "site" + "\" : {\"guid\" : \"" + nodeRef + "\"}}}";
        StringEntity se = new StringEntity(jsonInput.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        post.setEntity(se);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(post);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_CREATED:        
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    throw new RuntimeException("Site doesn't exists " + siteName);
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new RuntimeException("Invalid user name or password");
                default:
                    logger.error("Unable to mark as favorite: " + response.toString());
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
     * Verify if a document or folder is marked as favorite
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @return true if marked as favorite
     * @throws Exception if error
     */
    public boolean isFavorite(final String userName,
                              final String password,
                              final String siteName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }     
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef = getSiteNodeRef(userName, password, siteName);      
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites/" + nodeRef;     
        HttpGet get = new HttpGet(reqUrl);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(get);  
            if( HttpStatus.SC_OK  == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace( "Site " + siteName + "is marked as favorite");
                }
                return true;
            }
            else
            {
                return false;
            }
        }
        finally
        {
            get.releaseConnection();
            client.close();
        }    
    }
    
    /**
     * Remove favorite site
     * 
     * @param userName
     * @param password
     * @param siteName
     * @return true if favorite is removed
     * @throws Exception if error
     */
    public boolean removeFavorite(final String userName,
                                  final String password,
                                  final String siteName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }       
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String siteNodeRef = getSiteNodeRef(userName, password, siteName); 
        if(StringUtils.isEmpty(siteNodeRef))
        {
            throw new RuntimeException("Site doesn't exists " + siteName);
        }
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites/" + siteNodeRef; 
        HttpDelete delete = new HttpDelete(reqUrl);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {          
            HttpResponse response = clientWithAuth.execute(delete);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_NO_CONTENT:

                    if(logger.isTraceEnabled())
                    {
                        logger.trace( "Site " + siteName + "is removed from favorite");
                    }
                    return true;    
                default:
                    logger.error("Unable to remove favorite site: " + response.toString());
                    break;
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
     * Add pages to site dashboard
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param multiplePages
     * @param page - single page to be added
     * @param pages - list of pages to be added
     * @return true if the page is added
     * @throws Exception if error
     */
    private boolean addPages(final String userName,
                             final String password,
                             final String siteName,
                             final boolean multiplePages,
                             final Page page,
                             final List<Page> pages) throws Exception
    {    
        if(!exists(siteName, userName, password))
        {
            throw new RuntimeException("Site doesn't exists " + siteName);
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getAlfrescoUrl() + DashboardCustomization.SITE_PAGES_URL;        
        org.json.JSONObject body = new org.json.JSONObject();
        org.json.JSONArray array = new org.json.JSONArray();      
        body.put("siteId", siteName);
        // set the default page (Document Library)
        array.put(new org.json.JSONObject().put("pageId", Page.DOCLIB.pageId));
        
        if(pages != null)
        {
            for(int i = 0; i < pages.size(); i++)
            {
                if(!Page.DOCLIB.pageId.equals(pages.get(i).pageId))
                {
                    array.put(new org.json.JSONObject().put("pageId", pages.get(i).pageId));
                }
            }
        }       
        // add the new page
        if(!multiplePages)
        {
            array.put(new org.json.JSONObject().put("pageId", page.pageId));
        }       
        body.put("pages", array);
        body.put("themeId", "");   
        HttpPost post  = new HttpPost(url);
        StringEntity se = new StringEntity(body.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        post.setEntity(se);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(post);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Page " + page.pageId + " was added to site " + siteName);
                }
                return true;
            }
            else
            {
                logger.error("Unable to add page to site " + siteName);
                return false;
            }
            }
            finally
            {
                post.releaseConnection();
                client.close();
            }  
    }            
    
    /**
     * Add a single page to site dashboard
     * If there are pages added previously add them to 'oldPages' list in order 
     * to keep them on the site dashboard.
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param page
     * @param oldPages - pages that were added previously
     * @return true if the page is added
     * @throws Exception if error
     */
    public boolean addPageToSite(final String userName,
                                 final String password,
                                 final String siteName,
                                 final Page page,
                                 final List<Page> oldPages) throws Exception
    {        
        return addPages(userName, password, siteName, false, page, oldPages);
    }
    
    /**
     * Add pages to site dashboard
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param pages
     * @return true if pages are added
     * @throws Exception if error
     */
    public boolean addPagesToSite(final String userName,
                                 final String password,
                                 final String siteName,
                                 final List<Page> pages) throws Exception
    {       
        return addPages(userName, password, siteName, true, null, pages);
    }
    
    /**
     * Add dashlet to site dashboard
     * 
     * @param userName
     * @param password
     * @return true if the dashlet is added
     * @throws Exception if error
     */
    public boolean addDashlet(final String userName,
                              final String password,
                              final String siteName,
                              final SiteDashlet dashlet,
                              final DashletLayout layout,
                              final int column,
                              final int position) throws Exception
    {        
        if(!exists(siteName, userName, password))
        {
            throw new RuntimeException("Site doesn't exists " + siteName);
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getAlfrescoUrl() + DashboardCustomization.ADD_DASHLET_URL;        
        org.json.JSONObject body = new org.json.JSONObject();
        org.json.JSONArray array = new org.json.JSONArray();     
        body.put("dashboardPage", "site/" + siteName + "/dashboard");
        body.put("templateId", layout.id);
        
        Hashtable<String, String> defaultDashlets = new Hashtable<String, String>();
        defaultDashlets.put(SiteDashlet.SITE_MEMBERS.id, "component-1-1");
        defaultDashlets.put(SiteDashlet.SITE_CONNTENT.id, "component-2-1");
        defaultDashlets.put(SiteDashlet.SITE_ACTIVITIES.id, "component-2-2");
        
        Iterator<Map.Entry<String, String>> entries = defaultDashlets.entrySet().iterator();
        while (entries.hasNext()) 
        {
          Map.Entry<String, String> entry = entries.next();
          org.json.JSONObject jDashlet = new org.json.JSONObject();
          jDashlet.put("url", entry.getKey());
          jDashlet.put("regionId", entry.getValue());
          jDashlet.put("originalRegionId", entry.getValue());
          array.put(jDashlet);
        }    
        
        org.json.JSONObject newDashlet = new org.json.JSONObject();
        newDashlet.put("url", dashlet.id);
        String region = "component-" + column + "-" + position;
        newDashlet.put("regionId", region);
        array.put(newDashlet);
        body.put("dashlets", array);
        
        HttpPost post  = new HttpPost(url);
        StringEntity se = new StringEntity(body.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        post.setEntity(se);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(post);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Dashlet " + dashlet.name + " was added to site " + siteName);
                }
                return true;
            }
            else
            {
                logger.error("Unable to add dashlet to site " + siteName);
            }
            }
            finally
            {
                post.releaseConnection();
                client.close();
            }
        return false;
    }
}
