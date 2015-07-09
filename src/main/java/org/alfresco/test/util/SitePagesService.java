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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONObject;
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
     * @return true if user exists
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
        post.setEntity(client.setMessageBody(body));
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(post);
            if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Event created successfully");
                }
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
}
