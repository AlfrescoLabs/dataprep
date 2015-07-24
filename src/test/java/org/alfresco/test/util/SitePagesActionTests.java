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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.dataprep.SitePagesService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.alfresco.dataprep.DashboardCustomization.Page;
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
    @Autowired private SiteService site;
    @Autowired private UserService userService;
    @Autowired private SitePagesService pageService;
    private String admin = "admin";

    @Test
    public void addCalendarEvent() throws Exception
    {
        String siteId = "calendar-site" + System.currentTimeMillis();
        site.create(ADMIN, ADMIN, "myDomain", siteId, "my site description", Visibility.PUBLIC);   
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(ADMIN, ADMIN, siteId, "what", "where", "description", today, today, 
                "6:00 PM", "8:00 PM", false, "tag1"));        
        Assert.assertTrue(pageService.addCalendarEvent(ADMIN, ADMIN, siteId, "what", "where", "description", today, today, 
                "12:00", "13:00", false, "tag1"));      
        String name1 = pageService.getEventName(ADMIN, ADMIN, siteId, "what", "where", today, today, "6:00 PM", "8:00 PM", false);
        String name2 = pageService.getEventName(ADMIN, ADMIN, siteId, "what", "where", today, today, "12:00", "13:00", false);
        Assert.assertNotNull(name1);
        Assert.assertNotNull(name2);
        Assert.assertFalse(name1.equals(name2));
    }
    
    @Test
    public void addCalendarEvent24h() throws Exception
    {
        String siteId = "calendar-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);     
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", today, today, 
                "12:00", "13:00", false, "tag1"));  
        String nameEvent = pageService.getEventName(userName, userName, siteId, "what", "where", today, today, "12:00", "13:00", false);
        Assert.assertNotNull(nameEvent);
    }
    
    @Test
    public void addCalendarEventNullParams() throws Exception
    {
        String siteId = "calendar-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", null, null, 
                null, null, false, "tag1"));  
    }
    
    @Test
    public void addCalendarEventAllDay() throws Exception
    {
        String siteId = "calendar-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", null, null, 
                null, null, true, null));
        Date today = new Date();
        String nameEvent = pageService.getEventName(userName, userName, siteId, "what", "where", today, today, null, null, true);
        Assert.assertNotNull(nameEvent);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addCalendarEventFakeSite() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, "fakeSite", "what", "where", "description", null, null, 
                null, null, true, null));  
    }
    
    @Test
    public void removeEvent() throws Exception
    {
        String siteId = "calendar-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);     
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", today, today, 
                "12:00", "13:00", false, "tag1"));  
        String nameEvent = pageService.getEventName(userName, userName, siteId, "what", "where", today, today, "12:00", "13:00", false);
        Assert.assertNotNull(nameEvent);       
        Assert.assertTrue(pageService.removeEvent(userName, userName, siteId, "what", "where", today, today, "12:00", "13:00", false));
        nameEvent = pageService.getEventName(userName, userName, siteId, "what", "where", today, today, "12:00", "13:00", false);
        Assert.assertTrue(nameEvent.isEmpty());;
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeEventInvalidCredentials() throws Exception
    {
        String siteId = "calendar-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);     
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", today, today, 
                "12:00", "13:00", false, "tag1"));  
        String nameEvent = pageService.getEventName(userName, userName, siteId, "what", "where", today, today, "12:00", "13:00", false);
        Assert.assertNotNull(nameEvent);       
        pageService.removeEvent(userName, "fakePassword", siteId, "what", "where", today, today, "12:00", "13:00", false);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeEventInvalidSite() throws Exception
    {
        String siteId = "calendar-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);     
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", today, today, 
                "12:00", "13:00", false, "tag1"));  
        String nameEvent = pageService.getEventName(userName, userName, siteId, "what", "where", today, today, "12:00", "13:00", false);
        Assert.assertNotNull(nameEvent);       
        pageService.removeEvent(userName, userName, "fakeSite", "what", "where", today, today, "12:00", "13:00", false);
    }
    
    @Test
    public void createWikiPage() throws Exception
    {
        String siteId = "wiki-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String wikiTitle1 = "wiki_1" + + System.currentTimeMillis();
        String wikiTitle2 = "Wiki Title" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.WIKI, null);
        List<String>tags = new ArrayList<String>();
        tags.add("tag1");
        tags.add("tag2");
        Assert.assertTrue(pageService.createWiki(userName, userName, siteId, wikiTitle1, wikiTitle1, tags));
        Assert.assertTrue(pageService.createWiki(userName, userName, siteId, wikiTitle2, wikiTitle2, tags));
        Assert.assertTrue(pageService.wikiExists(userName, userName, siteId, wikiTitle1));       
        Assert.assertTrue(pageService.wikiExists(userName, userName, siteId, wikiTitle2));       
        Assert.assertFalse(pageService.wikiExists(userName, userName, siteId, "fakeWiki"));
    }
    
    @Test
    public void removeWikiPage() throws Exception
    {
        String siteId = "wiki-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String wikiTitle1 = "wiki_1" + + System.currentTimeMillis();
        String wikiTitle2 = "Wiki Title" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.createWiki(userName, userName, siteId, wikiTitle1, wikiTitle1, null));
        Assert.assertTrue(pageService.createWiki(userName, userName, siteId, wikiTitle2, wikiTitle2, null));
        Assert.assertTrue(pageService.deleteWikiPage(userName, userName, siteId, wikiTitle1));
        Assert.assertFalse(pageService.wikiExists(userName, userName, siteId, wikiTitle1));        
        Assert.assertTrue(pageService.deleteWikiPage(userName, userName, siteId, wikiTitle2));
        Assert.assertFalse(pageService.wikiExists(userName, userName, siteId, wikiTitle2));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeNonExistentWikiPage() throws Exception
    {
        String siteId = "wiki-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        pageService.deleteWikiPage(userName, userName, siteId, "fakeWiki");      
    }
}
