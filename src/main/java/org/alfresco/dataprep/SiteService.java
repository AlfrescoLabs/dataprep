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
package org.alfresco.dataprep;

import org.alfresco.dataprep.DashboardCustomization.DashletLayout;
import org.alfresco.dataprep.DashboardCustomization.Page;
import org.alfresco.dataprep.DashboardCustomization.SiteDashlet;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.io.IOUtils;
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
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Site utility helper that performs crud operation on Site.
 * <ul>
 * <li> Creates an Alfresco site.
 * <li> Deletes an Alfresco site.
 * <li> Mark as favorite
 * <li> Remove favorite
 * <li> Add pages and dashlets to site
 * <li> Create Record Management site 
 * </ul>
 * @author Michael Suzuki
 * @author Bogdan Bocancea
 *
 */
@Service
public class SiteService
{
    private static Log logger = LogFactory.getLog(SiteService.class);
    
    @Autowired 
    private AlfrescoHttpClientFactory alfrescoHttpClientFactory;

    @Autowired
    private UserService userService;
    
    public enum RMSiteCompliance
    {
        STANDARD("{http://www.alfresco.org/model/recordsmanagement/1.0}rmsite"),
        DOD_5015_2_STD("{http://www.alfresco.org/model/dod5015/1.0}site");
        public final String compliance;
        RMSiteCompliance(String compliance)
        {
            this.compliance = compliance;
        }
    }
    
    public enum Visibility
    {
        PRIVATE, PUBLIC, MODERATED
    };
    
    /**
     * Create site using Alfresco public API.
     * 
     * @param username identifier
     * @param password user password
     * @param domain the company or org id
     * @param siteId site identifier
     * @param description site description
     * @param visibility site visibility type
     */
    public void create(final String username,
                       final String password,
                       final String domain,
                       final String siteId,
                       final String description,
                       final Visibility visibility)
    {
        create(username, password, domain, siteId, siteId, description, visibility);
    }
    
    @SuppressWarnings("unchecked")
    private boolean createSiteV1Api(final String username,
                                    final String password,
                                    final String siteId,
                                    final String title,
                                    final String description,
                                    final Visibility visibility)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiVersionUrl() + "sites";
        HttpPost post  = new HttpPost(reqUrl);
        JSONObject body = new JSONObject();;
        body.put("id", siteId);
        body.put("title", title);
        body.put("description", description);
        body.put("visibility", visibility.toString());
        
        HttpResponse response = client.executeAndRelease(username, password, body, post);
        if(HttpStatus.SC_CREATED == response.getStatusLine().getStatusCode())
        {
            logger.info(String.format("Successfuly created site with id '%s' ", siteId));
            return true;
        }
        else
        {
            logger.error(client.getParameterFromJSON(response,"briefSummary", "error"));
        }
        return false;
    }
    
    /**
    * Create site using Alfresco public API.
    * 
    * @param username identifier
    * @param password user password
    * @param domain the company or org id
    * @param siteId site identifier
    * @param title SiteName
    * @param description site description
    * @param visibility site visibility
    */
   public void create(final String username,
                      final String password,
                      final String domain, 
                      final String siteId,
                      final String title,
                      final String description,
                      final Visibility visibility)
   {
       AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
       if(client.getAlfVersion() >= 5.2)
       {
           createSiteV1Api(username, password, siteId, siteId, description, visibility);
       }
       else
       {
           createSiteOldApi(username, password, siteId, title, description, visibility);
       }
   }
   
   @SuppressWarnings("unchecked")
   private boolean createSiteOldApi(final String userName,
                                    final String password,
                                    final String siteId,
                                    final String title,
                                    final String description,
                                    final Visibility visibility)
   {
       AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
       String reqUrl = client.getApiUrl() + "sites";
       HttpPost post  = new HttpPost(reqUrl);
       JSONObject body = new JSONObject();
       body.put("visibility", visibility);
       body.put("title", title);
       body.put("shortName", siteId);
       body.put("description", description);
       body.put("sitePreset", "site-dashboard");
       post.setEntity(client.setMessageBody(body));
       HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
       try
       {
           HttpResponse response = clientWithAuth.execute(post);
           switch (response.getStatusLine().getStatusCode())
           {
               case HttpStatus.SC_OK:
                   String secondPostUrl = client.getAlfrescoUrl() + "alfresco/service/remoteadm/createmulti?s=sitestore";
                   HttpPost secondPost  = new HttpPost(secondPostUrl);
                   secondPost.setHeader("Content-Type", "application/xml;charset=UTF-8");
                   String xmlSiteContent = readSitePageContent("site-page-content.xml").replaceAll("&lt;shortName&gt;", siteId);
                   StringEntity xmlEntity = new StringEntity(xmlSiteContent, "UTF-8");
                   xmlEntity.setContentType("application/xml");
                   secondPost.setEntity(xmlEntity);
                   response = clientWithAuth.execute(secondPost);
                   secondPost.releaseConnection();
                   String url = String.format(client.getAlfrescoUrl() + "alfresco/service/slingshot/doclib2/doclist/all/site/%s/documentLibrary/", siteId);
                   HttpGet get = new HttpGet(url);
                   response = clientWithAuth.execute(get); 
                   if(200 == response.getStatusLine().getStatusCode())
                   {
                       logger.info(String.format("Successfully created %s site", siteId));
                       return true;
                   }
                   else
                   {
                       logger.error(String.format("Failed to open %s site", siteId));
                       return false;
                   }
               case HttpStatus.SC_BAD_REQUEST:
                   throw new RuntimeException(String.format("%s site already created", siteId));
               case HttpStatus.SC_UNAUTHORIZED:
                   throw new RuntimeException("Invalid credentials");
               default:
                   logger.error(String.format("Unable to create %s site. Reason: %s", siteId, response.toString()));
                   break;
           }
       }
       catch (IOException e)
       {
           throw new RuntimeException("Failed to execute create site POST request");
       }
       finally
       {
           post.releaseConnection();
           client.close();
       } 
       return false;
   }
   
    /**
     * Checks if site exists
     * 
     * @param siteId site identifier
     * @param username site user
     * @param password user password
     * @return true if exists
     */
    public boolean exists(final String siteId,
                          final String username,
                          final String password)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String apiUrl = client.getApiUrl();
        String url = String.format("%ssites/%s",apiUrl, siteId);
        HttpGet get = new HttpGet(url);
        try
        {
            HttpResponse response = client.execute(username, password, get);
            if( 200 == response.getStatusLine().getStatusCode())
            {
                return true;
            }
            return false;
        } 
        finally
        {
            get.releaseConnection();
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
    @Deprecated
    public void delete(final String username,
                       final String password,
                       final String domain,
                       final String siteId)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = String.format(client.getApiUrl() + "sites/%s", siteId);
        HttpDelete httpDelete = new HttpDelete(url);
        HttpResponse response = client.executeAndRelease(username, password, httpDelete);
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        {
            logger.info(String.format("Site deleted successfully: ", siteId));
        }
        else
        {
            logger.error(String.format("Unable to delete site %s. Reason: %s ", siteId, response.toString()));
        }
    }
    
    /**
     * Delete site.
     * 
     * @param username user details
     * @param password user details
     * @param siteId site identifier
     */
    public boolean delete(final String username,
                          final String password,
                          final String siteId)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = String.format(client.getApiUrl() + "sites/%s", siteId);
        HttpDelete httpDelete = new HttpDelete(url);
        HttpResponse response = client.executeAndRelease(username, password, httpDelete);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                logger.info(String.format("Site deleted successfully %s", siteId));
                return true;
            case HttpStatus.SC_NOT_FOUND:
                logger.error(String.format("Site %s does not exist", siteId));
                return false;
            default:
                logger.error(String.format("Unable to delete site %s. Reason: %s ", siteId, response.toString()));
                break;
        }
        return false;
    }
    
    /**
     * Gets all existing sites
     * 
     * @param userName site user
     * @param password user password
     * @return List<String> list of sites
     */
    public List<String> getSites(final String userName,
                                 final String password)
    {
        List<String> mySitesList = new ArrayList<String>();
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String apiUrl = client.getApiUrl() + "people/" + userName + "/sites";
        HttpGet get = new HttpGet(apiUrl);
        try
        {
            HttpResponse response = client.execute(userName, password, get);
            if(200 == response.getStatusLine().getStatusCode())
            {
                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity , "UTF-8");
                Object obj=JSONValue.parse(responseString);
                JSONArray jarray=(JSONArray)obj;
                for (Object item:jarray)
                {
                    JSONObject jobject=(JSONObject) item;
                    mySitesList.add(jobject.get("title").toString());
                }
            }
            return mySitesList;
        }
        catch (IOException e)
        {
            logger.error("Failed to execute request:" + get);
        } 
        finally
        {
            get.releaseConnection();
            client.close();
        }
        return mySitesList;
    }
    
    /**
     * Get site node ref 
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteName String site name
     * @return String site node ref
     */
    public String getSiteNodeRef(final String userName,
                                 final String password,
                                 final String siteName)
    {
        String siteNodeRef = "";
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiVersionUrl() + "sites/" + siteName;
        HttpGet get = new HttpGet(reqUrl);
        try
        {
            HttpResponse response = client.execute(userName, password, get);
            if( HttpStatus.SC_OK  == response.getStatusLine().getStatusCode())
            {
                return client.getParameterFromJSON(response,"guid", "entry");
            }
        }
        finally
        {
            get.releaseConnection();
            client.close();
        }
        return siteNodeRef;
    }
    
    /**
     * Set site as favorite
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteName String site name
     * @return true if marked as favorite
     * @throws RuntimeException if site doesn't exists
     */
    public boolean setFavorite(final String userName,
                               final String password,
                               final String siteName)
    {
        if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        String nodeRef = getSiteNodeRef(userName, password, siteName);
        if(StringUtils.isEmpty(nodeRef))
        {
            throw new RuntimeException("Site doesn't exists " + siteName);
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites";
        HttpPost post  = new HttpPost(reqUrl);
        String jsonInput;
        jsonInput = "{\"target\": {\"" + "site" + "\" : {\"guid\" : \"" + nodeRef + "\"}}}";
        StringEntity se = new StringEntity(jsonInput.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        post.setEntity(se);
        HttpResponse response = client.executeAndRelease(userName, password, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_CREATED:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Site doesn't exists " + siteName);
            default:
                logger.error("Unable to mark as favorite: " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Verify if a document or folder is marked as favorite
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteName String site name
     * @return true if marked as favorite
     */
    public boolean isFavorite(final String userName,
                              final String password,
                              final String siteName)
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }     
        String nodeRef = getSiteNodeRef(userName, password, siteName);
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites/" + nodeRef;
        HttpGet get = new HttpGet(reqUrl);
        HttpResponse response = client.executeAndRelease(userName, password, get);
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
    
    /**
     * Get a list of favorite sites for a user
     * @param userName String user name
     * @param password String password
     * @return List<String> of favorite sites
     */
    public List<String> getFavoriteSites(final String userName,
                                         final String password)
    {
        List<String> favorites = new ArrayList<String>();
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorite-sites/";
        HttpGet get = new HttpGet(reqUrl);
        HttpResponse response = client.execute(userName, password, get);
        if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            JSONArray jArray = client.getJSONArray(response, "list", "entries");
            for (Object item:jArray)
            {
                JSONObject jobject = (JSONObject) item;
                JSONObject entry = (JSONObject) jobject.get("entry");
                favorites.add((String) entry.get("id"));
            }
        }
        return favorites;
    }
    
    /**
     * Remove favorite site
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteName String site name
     * @return true if favorite is removed
     */
    public boolean removeFavorite(final String userName,
                                  final String password,
                                  final String siteName)
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }       
        String siteNodeRef = getSiteNodeRef(userName, password, siteName); 
        if(StringUtils.isEmpty(siteNodeRef))
        {
            throw new RuntimeException("Site doesn't exists " + siteName);
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites/" + siteNodeRef;
        HttpDelete delete = new HttpDelete(reqUrl);
        HttpResponse response = client.executeAndRelease(userName, password, delete);
        if( HttpStatus.SC_NO_CONTENT  == response.getStatusLine().getStatusCode())
        {
            if(logger.isTraceEnabled())
            {
                logger.trace( "Site " + siteName + "is removed from favorite");
            }
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Add pages to site dashboard
     * 
     * @param userName String identifier
     * @param password
     * @param siteName
     * @param multiplePages
     * @param page - single page to be added
     * @param pages - list of pages to be added
     * @return true if the page is added
     */
    @SuppressWarnings("unchecked")
    private boolean addPages(final String userName,
                             final String password,
                             final String siteName,
                             final boolean multiplePages,
                             final Page page,
                             final List<Page> pages)
    {
        if(!exists(siteName, userName, password))
        {
            throw new RuntimeException("Site doesn't exists " + siteName);
        }

        HttpState httpState = userService.login(userName, password);

        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getShareUrl() + DashboardCustomization.SITE_PAGES_URL;
        JSONObject body = new JSONObject();
        JSONArray array = new JSONArray();
        body.put("siteId", siteName);
        // set the default page (Document Library and Site Dashboard)
        array.add(new org.json.JSONObject().put("pageId", Page.DASHBOARD.pageId));
        array.add(new org.json.JSONObject().put("pageId", Page.DOCLIB.pageId));
        if(pages != null)
        {
            for(int i = 0; i < pages.size(); i++)
            {
                if(!Page.DOCLIB.pageId.equals(pages.get(i).pageId))
                {
                    array.add(new org.json.JSONObject().put("pageId", pages.get(i).pageId));
                }
            }
        }
        // add the new page
        if(!multiplePages)
        {
            array.add(new org.json.JSONObject().put("pageId", page.pageId));
        }
        body.put("pages", array);
        body.put("themeId", "");
        HttpPost post  = new HttpPost(url);
        post.setEntity(client.setMessageBody(body));
        client.setRequestWithCSRFToken(post, httpState);
        HttpResponse response = client.executeAndReleaseWithoutBasicAuthHeader(userName, password, post);
        if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            if(!multiplePages)
            {
                logger.info("Successfully added page "+ page.name() +" to site " + siteName);
            }
            else
            {
                logger.info("Successfully added "+ pages.size() + " pages to site " + siteName);
            }
            return true;
        }
        else
        {
            logger.error("Unable to add page to site " + siteName);
            return false;
        }
    }
    
    /**
     * Add a single page to site dashboard
     * If there are pages added previously add them to 'oldPages' list in order 
     * to keep them on the site dashboard.
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteName String site name
     * @param page to add
     * @param oldPages - pages that were added previously
     * @return true if the page is added
     */
    public boolean addPageToSite(final String userName,
                                 final String password,
                                 final String siteName,
                                 final Page page,
                                 final List<Page> oldPages)
    {
        return addPages(userName, password, siteName, false, page, oldPages);
    }

    public boolean addPageToSite(final String userName,
                                 final String password,
                                 final String siteName,
                                 final Page page)
    {
        return addPages(userName, password, siteName, false, page, null);
    }
    
    /**
     * Add pages to site dashboard
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteName String site name
     * @param pages to add
     * @return true if pages are added
     */
    public boolean addPagesToSite(final String userName,
                                 final String password,
                                 final String siteName,
                                 final List<Page> pages)
    {
        return addPages(userName, password, siteName, true, null, pages);
    }
    
    /**
     * Add dashlet to site dashboard
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteName String site name
     * @param dashlet Site dashlet
     * @param layout Dashlet layout
     * @param column int index of columns
     * @param position int position in column
     * @return true if the dashlet is added
     */
    @SuppressWarnings("unchecked")
    public boolean addDashlet(final String userName,
                              final String password,
                              final String siteName,
                              final SiteDashlet dashlet,
                              final DashletLayout layout,
                              final int column,
                              final int position)
    {        
        if(!exists(siteName, userName, password))
        {
            throw new RuntimeException("Site doesn't exists " + siteName);
        }

        HttpState httpState = userService.login(userName, password);

        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getShareUrl() + DashboardCustomization.ADD_DASHLET_URL;
        JSONObject body = new JSONObject();
        JSONArray array = new JSONArray();
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
        client.setRequestWithCSRFToken(post, httpState);
        HttpResponse response = client.executeAndReleaseWithoutBasicAuthHeader(userName, password, post);
        if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            logger.trace("Dashlet " + dashlet.name + " was added to site " + siteName);
            return true;
        }
        else
        {
            logger.error("Unable to add dashlet to site " + siteName);
        }
        return false;
    }
    
    /**
     * Create Record Management site
     * 
     * @param userName String identifier
     * @param password String password
     * @param title String site title
     * @param description String site description
     * @param compliance RMSiteCompliance site compliance
     * @return true if site is created
     */
    @SuppressWarnings("unchecked")
    public boolean createRMSite(final String userName,
                                final String password,
                                final String title,
                                final String description,
                                final RMSiteCompliance compliance)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiUrl() + "sites";
        HttpPost post  = new HttpPost(reqUrl);
        JSONObject body = new JSONObject();
        body.put("visibility", "PUBLIC");
        body.put("title", title);
        body.put("shortName", "rm");
        body.put("description", description);
        body.put("sitePreset", "rm-site-dashboard");
        body.put("compliance", compliance.compliance);
        body.put("type", compliance.compliance);
        post.setEntity(client.setMessageBody(body));
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(post);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_OK:
                    String secondPostUrl = client.getAlfrescoUrl() + "alfresco/service/remoteadm/createmulti?s=sitestore";
                    HttpPost secondPost  = new HttpPost(secondPostUrl);
                    secondPost.setHeader("Content-Type", "application/xml;charset=UTF-8");
                    StringEntity xmlEntity = new StringEntity(readSitePageContent("rm-site-page-content.xml"), "UTF-8");
                    xmlEntity.setContentType("application/xml");
                    secondPost.setEntity(xmlEntity);
                    response = clientWithAuth.execute(secondPost);
                    secondPost.releaseConnection();
                    String url = client.getAlfrescoUrl() + "alfresco/service/slingshot/doclib2/doclist/all/site/rm/documentLibrary/";
                    HttpGet get = new HttpGet(url);
                    response = clientWithAuth.execute(get); 
                    if(200 == response.getStatusLine().getStatusCode())
                    {
                        if (logger.isTraceEnabled())
                        {
                            logger.info("Successfully created RM site");
                        }
                        return true;
                    }
                    else
                    {
                        if (logger.isTraceEnabled())
                        {
                            logger.error("Failed to open RM site");
                        }
                        return false;
                    }
                case HttpStatus.SC_BAD_REQUEST:
                    throw new RuntimeException("RM Site already created");
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new RuntimeException("Invalid credentials");
                default:
                    logger.error("Unable to create RM site: " + response.toString());
                    break;
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to execute the request");
        }
        finally
        {
            post.releaseConnection();
            client.close();
        } 
        return false;
    }
    
    private String readSitePageContent(String xmlResourceName)
    {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream input = classLoader.getResourceAsStream(xmlResourceName);
        try 
        {
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        } 
        catch (IOException e) 
        {
            throw new RuntimeException("Unable to read contentRMSite.xml ", e);
        }
        finally
        {
            IOUtils.closeQuietly(input);
        }
    }
    
    /**
     * Set site as IMAP favorites
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteName String site name
     * @return true if marked as IMAP favorite
     * @throws RuntimeException if site doesn't exists
     */
    public boolean setIMAPFavorite(final String userName,
                                   final String password,
                                   final String siteName)
    {
        if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiUrl() + "people/" + userName + "/preferences";
        HttpPost post  = new HttpPost(reqUrl);
        String jsonInput;
        jsonInput = "{\"org\": {\"alfresco\":{\"share\":{\"sites\":{\"imapFavourites\":{\"" + siteName + "\":true}}}}}}";
        StringEntity se = new StringEntity(jsonInput.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        post.setEntity(se);
        HttpResponse response = client.executeAndRelease(userName, password, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Site doesn't exists " + siteName);
            default:
                logger.error("Unable to mark as IMAP favorite: " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Remove site from IMAP favorites
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteName String site name
     * @return true if removed from IMAP favorites
     * @throws RuntimeException if site doesn't exists
     */
    public boolean removeIMAPFavorite(final String userName,
                                      final String password,
                                      final String siteName)
    {
        if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiUrl() + "people/" + userName + "/preferences";
        HttpPost post  = new HttpPost(reqUrl);
        String jsonInput;
        jsonInput = "{\"org\": {\"alfresco\":{\"share\":{\"sites\":{\"imapFavourites\":{\"" + siteName + "\":false}}}}}}";
        StringEntity se = new StringEntity(jsonInput.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        post.setEntity(se);
        HttpResponse response = client.executeAndRelease(userName, password, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Site doesn't exists " + siteName);
            default:
                logger.error("Unable to mark as IMAP favorite: " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Update site visibility
     * 
     * @param userName user name
     * @param password user password
     * @param siteName site id
     * @param newVisibility {@link Visibility} new visibility
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean updateSiteVisibility(final String userName,
                                        final String password,
                                        final String siteName,
                                        final Visibility newVisibility)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiUrl() + "sites/" + siteName;
        HttpPut put = new HttpPut(reqUrl);
        JSONObject body = new JSONObject();
        body.put("visibility", newVisibility.toString());
        HttpResponse response = client.executeAndRelease(userName, password, body, put);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Site doesn't exists " + siteName);
            default:
                logger.error("Unable to update site visibility: " + response.toString());
                break;
        }
        return false;
    }
}
