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
        String wikiTitle1 = "wiki_1" + System.currentTimeMillis();
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
        String wikiTitle1 = "wiki_1" + System.currentTimeMillis();
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
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeWikiNonExistentSite() throws Exception
    {
        String siteId = "wiki-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String wikiTitle1 = "wiki_1" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.createWiki(userName, userName, siteId, wikiTitle1, wikiTitle1, null));
        pageService.deleteWikiPage(userName, userName, "fakeSite", wikiTitle1);
    }
    
    @Test
    public void createBlog() throws Exception
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        String publishBlog = "publish" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.BLOG, null);
        List<String>tags = new ArrayList<String>();
        tags.add("tag1");
        tags.add("tag2");
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, publishBlog, publishBlog, false, tags));
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, draftBlog, draftBlog, true, tags));
        Assert.assertTrue(pageService.blogExists(userName, userName, siteId, draftBlog, true));
        Assert.assertTrue(pageService.blogExists(userName, userName, siteId, publishBlog, false));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createBlogInvalidSite() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        pageService.createBlogPost(userName, userName, "fakeSite", draftBlog, draftBlog, false, null);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createBlogInvalidUser() throws Exception
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        pageService.createBlogPost("fakeUser", "fakePass", siteId, draftBlog, draftBlog, false, null);
    }
    
    @Test
    public void deleteBlog() throws Exception
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.BLOG, null);
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, draftBlog, draftBlog, true, null));
        Assert.assertTrue(pageService.blogExists(userName, userName, siteId, draftBlog, true));
        Assert.assertTrue(pageService.deleteBlogPost(userName, userName, siteId, draftBlog, true));
        Assert.assertFalse(pageService.blogExists(userName, userName, siteId, draftBlog, true));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteBlogInvalidBlog() throws Exception
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, draftBlog, draftBlog, true, null));
        pageService.deleteBlogPost(userName, userName, siteId, "invalidblog", true);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteBlogInvalidSite() throws Exception
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, draftBlog, draftBlog, true, null));
        pageService.deleteBlogPost(userName, userName, "invalidBlog", draftBlog, true);
    }
    
    @Test
    public void createLink() throws Exception
    {
        String siteId = "link-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String linkTitle = "link-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.LINKS, null);
        List<String>tags = new ArrayList<String>();
        tags.add("tag1");
        tags.add("tag2");
        Assert.assertTrue(pageService.createLink(userName, userName, siteId, linkTitle, linkTitle, linkTitle, true, tags));
        Assert.assertTrue(pageService.linkExists(userName, userName, siteId, linkTitle));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createLinkInvalidSite() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        String linkTitle = "link" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        pageService.createLink(userName, userName, "fakeSite", linkTitle, linkTitle, linkTitle, true, null);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createLinkInvalidUser() throws Exception
    {
        String siteId = "link-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String linkTitle = "link" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        pageService.createLink("fakeUser", "fakePass", siteId, linkTitle, linkTitle, linkTitle, true, null);
    }
    
    @Test
    public void deleteLink() throws Exception
    {
        String siteId = "link-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String linkTitle = "link-" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.LINKS, null);
        Assert.assertTrue(pageService.createLink(userName, userName, siteId, linkTitle, linkTitle, linkTitle, true, null));
        Assert.assertTrue(pageService.linkExists(userName, userName, siteId, linkTitle));
        Assert.assertTrue(pageService.deleteLink(userName, userName, siteId, linkTitle));
        Assert.assertFalse(pageService.linkExists(userName, userName, siteId, linkTitle));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteLinkInvalidLink() throws Exception
    {
        String siteId = "link-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String link = "link" + + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.createLink(userName, userName, siteId, link, link, link, true, null));
        pageService.deleteLink(userName, userName, siteId, "fakeLink");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteLinkInvalidSite() throws Exception
    {
        String siteId = "link-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String link = "link" + + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.createLink(userName, userName, siteId, link, link, link, true, null));
        pageService.deleteLink(userName, userName, "fakeSite", link);
    }
    
    @Test
    public void createDiscussionTopic() throws Exception
    {
        String siteId = "topic-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String topicTitle = "topic-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.DISCUSSIONS, null);
        List<String>tags = new ArrayList<String>();
        tags.add("tag1");
        tags.add("tag2");
        Assert.assertTrue(pageService.createDiscussion(userName, userName, siteId, topicTitle, topicTitle, tags));
        Assert.assertTrue(pageService.discussionExists(userName, userName, siteId, topicTitle));
    }
    
    @Test
    public void deleteDiscussionTopic() throws Exception
    {
        String siteId = "topic-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String topicTitle = "topic-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.DISCUSSIONS, null);
        List<String>tags = new ArrayList<String>();
        tags.add("tag1");
        tags.add("tag2");
        Assert.assertTrue(pageService.createDiscussion(userName, userName, siteId, topicTitle, topicTitle, tags));
        Assert.assertTrue(pageService.discussionExists(userName, userName, siteId, topicTitle));
        Assert.assertTrue(pageService.deleteDiscussion(userName, userName, siteId, topicTitle));
        Assert.assertFalse(pageService.discussionExists(userName, userName, siteId, topicTitle));
    }
}
