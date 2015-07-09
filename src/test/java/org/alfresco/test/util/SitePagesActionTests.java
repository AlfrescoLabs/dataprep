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

import java.util.Date;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.Test;

/**
 * Test site pages (Calendar, Blog, Wiki, Links, Discussions, Data List)
 * data load api crud operations.
 * 
 * @author Bogdan Bocancea
 */

public class SitePagesActionTests extends AbstractTest
{
    @Autowired
    private SiteService site;
    @Autowired
    private UserService userService;
    @Autowired
    private SitePagesService pageService;
    private String admin = "admin";

    @Test
    public void addCalendarEvent() throws Exception
    {
        String siteId = "calendar-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", today, today, 
                "6:00 PM", "8:00 PM", false, "tag1"));
        
    }
    
    @Test
    public void addCalendarEvent24h() throws Exception
    {
        String siteId = "calendar-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", today, today, 
                "12:00", "13:00", false, "tag1"));  
    }
    
    @Test
    public void addCalendarEventNullParams() throws Exception
    {
        String siteId = "calendar-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", null, null, 
                null, null, false, "tag1"));  
    }
    
    @Test
    public void addCalendarEventAllDay() throws Exception
    {
        String siteId = "calendar-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", null, null, 
                null, null, true, null));  
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addCalendarEventFakeSite() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName);
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, "fakeSite", "what", "where", "description", null, null, 
                null, null, true, null));  
    }  
}
