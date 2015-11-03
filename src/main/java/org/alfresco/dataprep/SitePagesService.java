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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.alfresco.dataprep.DashboardCustomization.Page;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
/**
 *  Class that performs crud operation on calendar events, blogs, data lists, wiki, discussions and links.
 * 
 * @author Bogdan Bocancea
 */
public class SitePagesService
{
    private static Log logger = LogFactory.getLog(SitePagesService.class);
    @Autowired private AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    @Autowired private SiteService siteService;
    
    /**
     * Add calendar event
     * 
     * @param userName String user name
     * @param password String user password
     * @param siteName String site name
     * @param what String what
     * @param where String event location
     * @param description String event description
     * @param startDate Date event start date
     * @param endDate Date event end date
     * @param timeStart String event start time
     * @param timeEnd String event time finish
     * @param allDay boolean all day event
     * @param tag String tag the event
     * @return true if event is created
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean addCalendarEvent(final String userName,
                                    final String password,
                                    final String siteName,
                                    final String what,
                                    final String where,
                                    final String description,
                                    final Date startDate,
                                    final Date endDate,
                                    String timeStart,
                                    String timeEnd,
                                    final boolean allDay,
                                    String tag) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
            || StringUtils.isEmpty(what))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        if(!siteService.exists(siteName, userName, password))
        {
            throw new RuntimeException("Site doesn't exists " + siteName);
        }
        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getAlfrescoUrl() + "alfresco/s/calendar/create";
        // set default time if null
        if(StringUtils.isEmpty(timeStart))
        {
            timeStart = "12:00";
        }
        if(StringUtils.isEmpty(timeEnd))
        {
            timeEnd = "13:00";
        }
        Date currentDate = new Date();
        String pattern = "yyyy-MM-dd'T'Z";
        SimpleDateFormat fulldate = new SimpleDateFormat("EEEE, dd MMMM, yyyy");
        SimpleDateFormat fullFormat = new SimpleDateFormat(pattern);
        DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
        String fulldatefrom = "";
        String fulldateto = "";
        String timeStart24 = "";
        String startAt, endAt;
        if(timeStart.contains("AM") || timeStart.contains("PM"))
        {
            timeStart24 = convertTo24Hour(timeStart);
        }
        else
        {
            timeStart24 = timeStart;
        }
        String timeEnd24 = "";
        if(timeEnd.contains("AM") || timeEnd.contains("PM"))
        {
            timeEnd24 = convertTo24Hour(timeEnd);
        }
        else
        {
            timeEnd24 = timeEnd;
        }

        if(startDate == null)
        {
            // set the current date
            fulldatefrom = fulldate.format(currentDate);
            startAt = fullFormat.format(currentDate);
            DateTime dateTime = dtf.parseDateTime(startAt);
            startAt = dateTime.toString().replaceFirst("00:00", timeStart24);
        }
        else
        {
            fulldatefrom = fulldate.format(startDate); 
            startAt = fullFormat.format(startDate);
            DateTime dateTime = dtf.parseDateTime(startAt);
            startAt = dateTime.toString().replaceFirst("00:00", timeStart24);
        }
        if(endDate == null)
        {
            // set the current date
            fulldateto = fulldate.format(currentDate);
            endAt = fullFormat.format(currentDate);
            DateTime dateTime = dtf.parseDateTime(endAt);
            endAt = dateTime.toString().replaceFirst("00:00", timeEnd24);
        }
        else
        {
            fulldateto = fulldate.format(endDate); 
            endAt = fullFormat.format(endDate);
            DateTime dateTime = dtf.parseDateTime(endAt);
            endAt = dateTime.toString().replaceFirst("00:00", timeEnd24);
        }
        HttpPost post = new HttpPost(reqURL);
        JSONObject body = new JSONObject();
        body.put("fromdate", fulldatefrom);
        body.put("start", timeStart);
        body.put("todate", fulldateto);
        body.put("end", timeEnd);
        if(tag == null)
        {
            tag = "";
        }
        body.put("tags", tag);
        body.put("site", siteName);
        body.put("page", "calendar");
        body.put("docfolder", "");
        body.put("what", what);
        body.put("where", where);
        body.put("desc", description);
        body.put("startAt", startAt);
        body.put("endAt", endAt);
        if(allDay)
        {
            body.put("allday", "on");
        }
        HttpResponse response = client.executeRequest(client, userName, password, body, post);
        if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Event created successfully");
            }
            return true;
        }
        return false;
    }

    /**
     * Get the name (ID) of the event
     * 
     * @param userName String user name
     * @param password String user password
     * @param siteName String site name
     * @param what String what
     * @param where String event location
     * @param from Date event start date
     * @param to Date event end date
     * @param timeStart String event start time
     * @param timeEnd String event time finish
     * @param allDay boolean all day event
     * @return String name (id) of event
     * @throws Exception if error
     */
    public String getEventName(final String userName,
                               final String password,
                               final String siteName,
                               final String what,
                               final String where,
                               Date from,
                               Date to,
                               String timeStart,
                               String timeEnd,
                               final boolean allDay) throws Exception
    {
        String name = "";
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); 
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        to = calendar.getTime();
        String strFrom = dateFormat.format(from);
        String strTo = dateFormat.format(to);
        String reqURL = client.getAlfrescoUrl() + "alfresco/s/calendar/events/" + siteName + 
                "/user?from=" + strFrom + "&to" + strTo + "&repeating=all";
        HttpGet get = new HttpGet(reqURL);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(get);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_OK:
                    org.json.JSONObject items = new org.json.JSONObject(EntityUtils.toString(response.getEntity()));
                    org.json.JSONArray events = items.getJSONArray("events");
                    for (int i = 0; i < events.length(); i++) 
                    {
                        String itemWhat = items.getJSONArray("events").getJSONObject(i).getString("title");
                        String itemWhere = items.getJSONArray("events").getJSONObject(i).getString("where");
                        if(allDay)
                        {
                            if (itemWhat.equals(what) || itemWhere.equals(where))
                            {
                                name = items.getJSONArray("events").getJSONObject(i).getString("name");
                            }
                        }
                        else
                        {
                            String sTime = events.getJSONObject(i).getJSONObject("startAt").getString("iso8601");
                            String eTime = events.getJSONObject(i).getJSONObject("endAt").getString("iso8601");
                            DateTime st = new DateTime(sTime);
                            DateTime et = new DateTime(eTime);
                            DateTimeFormatter  sdf = DateTimeFormat.forPattern("HH:mm");
                            sTime = st.toString(sdf);
                            eTime = et.toString(sdf);
                            
                            
                            if(timeStart.contains("AM") || timeStart.contains("PM"))
                            {
                                timeStart = convertTo24Hour(timeStart);
                            }
                            if(timeEnd.contains("AM") || timeEnd.contains("PM"))
                            {
                                timeEnd = convertTo24Hour(timeEnd);
                            }
                            if(itemWhat.equals(what) && sTime.equals(timeStart) && eTime.equals(timeEnd))
                            {
                                name = items.getJSONArray("events").getJSONObject(i).getString("name");
                                break;
                            }
                        }
                    }
                    return name;
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new RuntimeException("Invalid credentials");
                default:
                    logger.error("Unable to find event " + response.toString());
                    break;
            }
        }
        finally
        {
            get.releaseConnection();
            client.close();
        }  
        return name;
    }

    /**
     * Remove an event
     * 
     * @param userName String user name
     * @param password String user password
     * @param siteName String site name
     * @param what String what
     * @param where String event location
     * @param from Date event start date
     * @param to Date event end date
     * @param timeStart String event start time
     * @param timeEnd String event time finish
     * @param allDay boolean all day event
     * @return boolean true if event is removed
     * @throws Exception if error
     */
    public boolean removeEvent(final String userName,
                               final String password,
                               final String siteName,
                               final String what,
                               final String where,
                               Date from,
                               Date to,
                               String timeStart,
                               String timeEnd,
                               final boolean allDay) throws Exception
    {
        if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName) || StringUtils.isEmpty(what)
                || StringUtils.isEmpty(from.toString()) || StringUtils.isEmpty(to.toString()))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        String eventName = getEventName(userName, password, siteName, what, where, from, to, timeStart, timeEnd, allDay);
        if(StringUtils.isEmpty(eventName))
        {
            throw new RuntimeException("Event not found");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqURL = client.getAlfrescoUrl() + "alfresco/s/calendar/event/" + siteName + "/" + eventName;
        HttpDelete request = new HttpDelete(reqURL);
        HttpResponse response = client.executeRequest(client, userName, password, request);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_NO_CONTENT:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site " + siteName);
            default:
                logger.error("Unable to delete event: " + response.toString());
                break;
        }
        return false;
    }

    /**
     * Convert time to 24 hour format
     * 
     * @param time String time
     * @throws ParseException if error
     * @return String converted hour
     */
    private String convertTo24Hour(String time) throws ParseException 
    {
        DateFormat f1 = new SimpleDateFormat("hh:mm a"); 
        Date d = null;
        d = f1.parse(time);
        DateFormat f2 = new SimpleDateFormat("HH:mm");
        String x = f2.format(d);
        return x;
    }

    @SuppressWarnings("unchecked")
    private JSONArray createTagsArray(List<String>tags)
    {
        JSONArray array = new JSONArray();
        if(tags != null)
        {
            for(int i = 0; i < tags.size(); i++)
            {
                array.add(tags.get(i));
            }
        }
        return array;
    }

    /**
     * Create a new wiki page
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param wikiTitle String wiki title
     * @param content String wiki content
     * @param tags List of tags
     * @return true if wiki page is created (200 Status)
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean createWiki(final String userName,
                              final String password,
                              final String siteName,
                              String wikiTitle,
                              final String content,
                              final List<String>tags) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(wikiTitle))
        {
            throw new IllegalArgumentException("Null Parameters: Please correct");
        }
        if (wikiTitle.contains(" ")) 
        {
            wikiTitle = wikiTitle.replace(" ", "%20");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getAlfrescoUrl() + "alfresco/s/slingshot/wiki/page/" + siteName + "/" + wikiTitle;
        HttpPut put = new HttpPut(url);    
        JSONObject body = new JSONObject();
        body.put("page", "wiki-page");
        body.put("pageTitle", wikiTitle);
        body.put("pagecontent", content);
        body.put("tags", createTagsArray(tags));
        HttpResponse response = client.executeRequest(client, userName, password, body, put);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Wiki page " + wikiTitle + " is created successfuly");
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site " + siteName);
            default:
                logger.error("Unable to create wiki page: " + response.toString());
                break;
        }
        return false;
    }

    /**
     * Verify if wiki exists
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param wikiTitle String wiki title
     * @return true if wiki exists (200 Status)
     * @throws Exception if error
     */
    public boolean wikiExists(final String userName,
                              final String password,
                              final String siteName,
                              String wikiTitle) throws Exception
    {
        if (wikiTitle.contains(" ")) 
        {
            wikiTitle = wikiTitle.replaceAll(" ", "_");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getAlfrescoUrl() + "alfresco/s/slingshot/wiki/page/" + siteName + "/" + wikiTitle;
        HttpGet get = new HttpGet(url);
        HttpResponse response = client.executeRequest(client, userName, password, get);
        if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            return true;
        }  
        return false;
    }

    /**
     * Delete wiki page
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param wikiTitle String wiki title
     * @return true if wiki is removed (204 Status)
     * @throws Exception if error
     */
    public boolean deleteWikiPage(final String userName,
                                  final String password,
                                  final String siteName,
                                  String wikiTitle) throws Exception
    {
        if (wikiTitle.contains(" ")) 
        {
            wikiTitle = wikiTitle.replaceAll(" ", "_");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getAlfrescoUrl() + "alfresco/s/slingshot/wiki/page/" + siteName + "/" + wikiTitle;
        HttpDelete delete = new HttpDelete(url);
        HttpResponse response = client.executeRequest(client, userName, password, delete);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_NO_CONTENT:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Wiki " + wikiTitle + " or site " + siteName + " doesn't exists");
            default:
                logger.error("Unable to delete wiki page: " + response.toString());
                break;
        }
        return false;
    }

    /**
     * Create a new blog post
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param blogTitle String blog title
     * @param content String blog content
     * @param draft boolean create blog as draft. If not it will be published
     * @param tags List of tags
     * @return true if blog post is created (200 Status)
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean createBlogPost(final String userName,
                                  final String password,
                                  final String siteName,
                                  final String blogTitle,
                                  final String content,
                                  final boolean draft,
                                  final List<String>tags) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(blogTitle))
        {
            throw new IllegalArgumentException("Null Parameters: Please correct");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getApiUrl() + "blog/site/" + siteName + "/blog/posts";
        HttpPost post = new HttpPost(url);
        JSONObject body = new JSONObject();
        body.put("title", blogTitle);
        body.put("content", content);
        body.put("draft", draft);
        body.put("tags", createTagsArray(tags));
        HttpResponse response = client.executeRequest(client, userName, password, body, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Blog " + blogTitle + " is created successfuly");
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site " + siteName);
            default:
                logger.error("Unable to create blog page: " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Get the name (id) of a blog post
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param blogTitle String blog title
     * @param isDraft boolean is draft
     * @return String name of blog post
     * @throws Exception if error
     */
    public String getBlogName(final String userName,
                              final String password,
                              final String siteName,
                              final String blogTitle,
                              final boolean draft) throws Exception
    {
        return getName(userName, password, siteName, blogTitle, draft, Page.BLOG);
    }

    /**
     * Verify if blog post exists
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param blogTitle String blog title
     * @param isDraft boolean is draft
     * @return boolean true if blog post exists
     * @throws Exception if error
     */
    public boolean blogExists(final String userName,
                              final String password,
                              final String siteName,
                              final String blogTitle,
                              final boolean draft) throws Exception
    {
        if(getBlogName(userName, password, siteName, blogTitle, draft).isEmpty())
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Delete blog post
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param blogTitle String blog title
     * @param isDraft boolean is draft
     * @return true if blog is removed (200 Status)
     * @throws Exception if error
     */
    public boolean deleteBlogPost(final String userName,
                                  final String password,
                                  final String siteName,
                                  final String blogTitle,
                                  final boolean isDraft) throws Exception
    {
        String blogName = getBlogName(userName, password, siteName, blogTitle, isDraft);
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getApiUrl() + "blog/post/site/" + siteName + "/blog/" + blogName + "?page=blog-postlist";
        HttpDelete delete = new HttpDelete(url);
        HttpResponse response = client.executeRequest(client, userName, password, delete);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Blog doesn't exists " + blogTitle);
            default:
                logger.error("Unable to delete blog post: " + response.toString());
                break;
        }
        return false;
    }

    /**
     * Create a new blog post
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param linkTitle String link title
     * @param url String link url
     * @param description String link description
     * @param internal boolean internal
     * @param tags List of tags
     * @return true if link is created (200 Status)
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean createLink(final String userName,
                              final String password,
                              final String siteName,
                              final String linkTitle,
                              final String url,
                              final String description,
                              final boolean internal,
                              final List<String>tags) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(linkTitle))
        {
            throw new IllegalArgumentException("Null Parameters: Please correct");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiUrl() + "links/site/" + siteName + "/links/posts";
        HttpPost post = new HttpPost(reqUrl);
        JSONObject body = new JSONObject();
        body.put("title", linkTitle);
        body.put("url", url);
        body.put("description", description);
        if(internal)
        {
            body.put("internal", internal);
        }
        body.put("tags", createTagsArray(tags));
        HttpResponse response = client.executeRequest(client, userName, password, body, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Link " + linkTitle + " is created successfuly");
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site " + siteName);
            default:
                logger.error("Unable to create link: " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Get the name (id) of a link
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param linkTitle String blog title
     * @return String name of blog post
     * @throws Exception if error
     */
    public String getLinkName(final String userName,
                              final String password,
                              final String siteName,
                              final String linkTitle) throws Exception
    {
        return getName(userName, password, siteName, linkTitle, false, Page.LINKS);
    }
    
    /**
     * Verify if link exists
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param linkTitle String blog title
     * @return boolean true if link exists
     * @throws Exception if error
     */
    public boolean linkExists(final String userName,
                              final String password,
                              final String siteName,
                              final String linkTitle) throws Exception
    {
        if(getLinkName(userName, password, siteName, linkTitle).isEmpty())
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Get the name (id) for blog post, link, discussion
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param title String blog title
     * @param draftBlog boolean is blog draft
     * @return String name (id)
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    private String getName(final String userName,
                           final String password,
                           final String siteName,
                           final String title,
                           boolean draftBlog,
                           final Page page) throws Exception
    {
        String name = "";
        String url = "";
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        switch(page)
        {
            case LINKS:
                url = client.getApiUrl() + "links/site/" + siteName + "/links?filter=all&contentLength=512&page=1&pageSize=10&startIndex=0";
                break;
            case BLOG:
                if(draftBlog)
                {
                    url = client.getApiUrl() + "blog/site/" + siteName + "/blog/posts/mydrafts";
                }
                else
                {
                    url = client.getApiUrl() + "blog/site/" + siteName + "/blog/posts/mypublished";
                }
                break;
            case DISCUSSIONS:
                url = client.getApiUrl() + "forum/site/" + siteName + "/discussions/posts";
                break;
            default:
                break;
        }
        HttpGet get = new HttpGet(url);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(get);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_OK:
                    String strResponse = EntityUtils.toString(response.getEntity());
                    JSONParser parser = new JSONParser();
                    Object obj = parser.parse(strResponse);
                    JSONObject jsonObject = (JSONObject) obj;
                    JSONArray jArray = (JSONArray) jsonObject.get("items");
                    Iterator<JSONObject> iterator = ((List<JSONObject>) jArray).iterator();
                    while (iterator.hasNext())
                    {
                        JSONObject factObj = (JSONObject) iterator.next();
                        String theTitle = (String) factObj.get("title");
                        if(title.toString().equalsIgnoreCase(theTitle))
                        {
                            return (String) factObj.get("name");
                        }
                    }
                    return "";
                case HttpStatus.SC_NOT_FOUND:
                    throw new RuntimeException("Invalid site " + siteName);
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new RuntimeException("Invalid credentials");
                default:
                    logger.error("Unable to find: " + title + " " + response.toString());
                    break;
            }
        }
        finally
        {
            get.releaseConnection();
            client.close();
        }
        return name;
    }

    /**
     * Delete link
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param linkTitle String blog title
     * @return true if link is removed (200 Status)
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean deleteLink(final String userName,
                              final String password,
                              final String siteName,
                              final String linkTitle) throws Exception
    {
        String linkName = getLinkName(userName, password, siteName, linkTitle);
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getApiUrl() + "links/delete/site/" + siteName + "/links";
        HttpPost post = new HttpPost(url);
        JSONObject body = new JSONObject();
        JSONArray array = new JSONArray();
        array.add(linkName);
        body.put("items", array);
        HttpResponse response = client.executeRequest(client, userName, password, body, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Link doesn't exists " + linkTitle);
            default:
                logger.error("Unable to delete link: " + response.toString());
                break;
        }
        return false;
    }

    /**
     * Create discussion topic
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param discussionTitle String topic title
     * @param text String topic content
     * @param tags List<String> tags
     * @return true if topic is created
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public boolean createDiscussion(final String userName,
                                    final String password,
                                    final String siteName,
                                    final String discussionTitle,
                                    final String text,
                                    final List<String>tags) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(discussionTitle))
        {
            throw new IllegalArgumentException("Null Parameters: Please correct");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiUrl() + "forum/site/" + siteName + "/discussions/posts";
        HttpPost post = new HttpPost(reqUrl);
        JSONObject body = new JSONObject();
        body.put("title", discussionTitle);
        body.put("content", text);
        body.put("tags", createTagsArray(tags));
        HttpResponse response = client.executeRequest(client, userName, password, body, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Discussion " + discussionTitle + " is created successfuly");
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site " + siteName);
            default:
                logger.error("Unable to create link: " + response.toString());
                break;
        }
        return false;
    }

    /**
     * Get the name(id) of a created topic
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param discussionTitle String discussion title
     * @return String name (id) of the topic
     * @throws Exception if error
     */
    public String getDiscussionName(final String userName,
                                    final String password,
                                    final String siteName,
                                    final String discussionTitle) throws Exception
    {
        return getName(userName, password, siteName, discussionTitle, false, Page.DISCUSSIONS);
    }

    /**
     * Verify if a discussion topic exists
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param discussionTitle String discussion title
     * @return true if topic exists
     * @throws Exception if error
     */
    public boolean discussionExists(final String userName,
                                    final String password,
                                    final String siteName,
                                    final String discussionTitle) throws Exception
    {
        if(getDiscussionName(userName, password, siteName, discussionTitle).isEmpty())
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Delete a discussion topic
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param discussionTitle String discussion title
     * @return true if deleted
     * @throws Exception if error
     */
    public boolean deleteDiscussion(final String userName,
                                    final String password,
                                    final String siteName,
                                    final String discussionTitle) throws Exception
    {
        String discussionName = getDiscussionName(userName, password, siteName, discussionTitle);
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getApiUrl() + "forum/post/site/" + siteName + "/discussions/" + discussionName + "?page=discussions-topicview";
        HttpDelete delete = new HttpDelete(url);
        HttpResponse response = client.executeRequest(client, userName, password, delete);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Topic doesn't exists " + discussionTitle);
            default:
                logger.error("Unable to delete topic post: " + response.toString());
                break;
        }
        return false;
    }
}
