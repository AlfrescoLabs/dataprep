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
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.social.alfresco.api.Alfresco;
import org.springframework.social.alfresco.api.entities.Site.Visibility;

/**
 * Site utility helper that performs crud operation on Site.
 * <ul>
 * <li> Creates an Alfresco site.
 * <li> Deletes an Alfresco site.
 * </ul>
 * @author Michael Suzuki
 *
 */
public class SiteService
{
    private final PublicApiFactory publicApiFactory;
    private final AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    
    public SiteService(PublicApiFactory publicApiFactory, AlfrescoHttpClientFactory alfrescoHttpClientFactory)
    {
        this.publicApiFactory = publicApiFactory;
        this.alfrescoHttpClientFactory = alfrescoHttpClientFactory;
    }
    
    /**
     * Create site using Alfresco public API.
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
     * @param siteId site identifier
     * @param username site user
     * @param password user password
     * @return true if exists
     * @throws Exception if error
     */
    public boolean exists(final String siteId, final String username,final String password) throws Exception
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
     * @param username site user
     * @param password user password
     * @return list of sites
     * @throws Exception if error
     */
    public List<String> getSites(final String username,final String password) throws Exception
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
}
