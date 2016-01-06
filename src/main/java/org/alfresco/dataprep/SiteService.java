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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.dataprep.DashboardCustomization.DashletLayout;
import org.alfresco.dataprep.DashboardCustomization.Page;
import org.alfresco.dataprep.DashboardCustomization.SiteDashlet;
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
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.Alfresco;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.springframework.stereotype.Service;
@Service
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
public class SiteService
{
    private static Log logger = LogFactory.getLog(SiteService.class);
    @Autowired private PublicApiFactory publicApiFactory;
    @Autowired private AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    
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
       Alfresco publicApi = publicApiFactory.getPublicApi(username,password);
       try
       {
           publicApi.createSite(domain,
                                siteId,
                                "site-dashboard", 
                                title,
                                description, 
                                visibility);
       }
       catch (IOException e)
       {
           throw new RuntimeException("Failed to create site:" + siteId);
       }
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
        try
        {
            //String ticket = client.getAlfTicket(username, password);
            String apiUrl = client.getApiUrl();
            String url = String.format("%ssites/%s",apiUrl, siteId);
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(username, password, get);
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
                String result = client.readStream(response.getEntity()).toJSONString();
                if(!StringUtils.isEmpty(result))
                {
                    JSONParser parser = new JSONParser();
                    Object obj = null;
                    try
                    {
                        obj = parser.parse(result);
                    }
                    catch (ParseException e)
                    {
                        logger.error("Failed to parse response");
                    }
                    JSONObject jsonObject = (JSONObject) obj;
                    JSONObject sites = (JSONObject) jsonObject.get("entry");
                    return siteNodeRef = (String) sites.get("guid");
                }
            }
            else
            {
                logger.error("Unable to get node ref of " + siteName + " " + response.getStatusLine());
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
        HttpResponse response = client.executeRequest(userName, password, post);
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
        HttpResponse response = client.executeRequest(userName, password, get);
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
        HttpResponse response = client.executeRequest(userName, password, delete);
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
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getAlfrescoUrl() + DashboardCustomization.SITE_PAGES_URL;
        JSONObject body = new JSONObject();
        JSONArray array = new JSONArray();
        body.put("siteId", siteName);
        // set the default page (Document Library)
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
        HttpResponse response = client.executeRequest(userName, password, body, post);
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
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getAlfrescoUrl() + DashboardCustomization.ADD_DASHLET_URL;
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
        HttpResponse response = client.executeRequest(userName, password, body, post);
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
        if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(title))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
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
                    StringEntity xmlEntity = new StringEntity(readContentRmSite(), "UTF-8");
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
    
    private String readContentRmSite()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream input = classLoader.getResourceAsStream("contentRMSite.xml");
        try 
        {
            return IOUtils.toString(input);
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
}
