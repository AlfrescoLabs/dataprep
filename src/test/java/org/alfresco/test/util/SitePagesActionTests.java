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

import org.alfresco.dataprep.ContentService;
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
    @Autowired private ContentService contentService;
    private String admin = "admin";

    @Test
    public void addCalendarEvent()
    {
        String siteId = "calendar-site" + System.currentTimeMillis();
        site.create(ADMIN, ADMIN, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(ADMIN, ADMIN, siteId, "what1", "where1", "description2", today, today,
                "6:00 PM", "8:00 PM", false, "tag1"));
        Assert.assertTrue(pageService.addCalendarEvent(ADMIN, ADMIN, siteId, "what2", "where2", "description2", today, today,
                "12:00", "13:00", false, "tag1"));
        String name1 = pageService.getEventName(ADMIN, ADMIN, siteId, "what1", "where1", today, today, "6:00 PM", "8:00 PM", false);
        String name2 = pageService.getEventName(ADMIN, ADMIN, siteId, "what2", "where2", today, today, "12:00", "13:00", false);
        Assert.assertNotNull(name1);
        Assert.assertNotNull(name2);
        Assert.assertFalse(name1.equals(name2));
    }

    @Test
    public void addCalendarEvent24h()
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
    public void addCalendarEventNullParams()
    {
        String siteId = "calendar-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, siteId, "what", "where", "description", null, null,
                null, null, false, "tag1"));
    }
    
    @Test
    public void addCalendarEventAllDay()
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
    public void addCalendarEventFakeSite()
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        Assert.assertTrue(pageService.addCalendarEvent(userName, userName, "fakeSite", "what", "where", "description", null, null,
                null, null, true, null));
    }
    
    @Test
    public void removeEvent()
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
    public void removeEventInvalidCredentials()
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
    public void removeEventInvalidSite()
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
    public void createWikiPage()
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
    public void removeWikiPage()
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
    public void removeNonExistentWikiPage()
    {
        String siteId = "wiki-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        pageService.deleteWikiPage(userName, userName, siteId, "fakeWiki");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeWikiNonExistentSite()
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
    public void createBlog()
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
    public void createBlogInvalidSite()
    {
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        pageService.createBlogPost(userName, userName, "fakeSite", draftBlog, draftBlog, false, null);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createBlogInvalidUser()
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        pageService.createBlogPost("fakeUser", "fakePass", siteId, draftBlog, draftBlog, false, null);
    }
    
    @Test
    public void deleteBlog()
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
    public void deleteBlogInvalidBlog()
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
    public void deleteBlogInvalidSite()
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
    public void createLink()
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
    public void createLinkInvalidSite()
    {
        String userName = "userm-" + System.currentTimeMillis();
        String linkTitle = "link" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        pageService.createLink(userName, userName, "fakeSite", linkTitle, linkTitle, linkTitle, true, null);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createLinkInvalidUser()
    {
        String siteId = "link-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String linkTitle = "link" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        pageService.createLink("fakeUser", "fakePass", siteId, linkTitle, linkTitle, linkTitle, true, null);
    }
    
    @Test
    public void deleteLink()
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
    public void deleteLinkInvalidLink()
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
    public void deleteLinkInvalidSite()
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
    public void createDiscussionTopic()
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
    public void deleteDiscussionTopic()
    {
        String siteId = "topic-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String topicTitle = "topic-" + System.currentTimeMillis();
        String topic2 = "second topic";
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.DISCUSSIONS, null);
        Assert.assertTrue(pageService.createDiscussion(userName, userName, siteId, topicTitle, topicTitle, null));
        pageService.replyToDiscussion(userName, userName, siteId, topicTitle, "firstReplay");
        Assert.assertTrue(pageService.discussionExists(userName, userName, siteId, topicTitle));
        Assert.assertTrue(pageService.deleteDiscussion(userName, userName, siteId, topicTitle));
        Assert.assertFalse(pageService.discussionExists(userName, userName, siteId, topicTitle));
        Assert.assertTrue(pageService.createDiscussion(userName, userName, siteId, topic2, topic2, null));
        Assert.assertTrue(pageService.discussionExists(userName, userName, siteId, topic2));
        Assert.assertTrue(pageService.deleteDiscussion(userName, userName, siteId, topic2));
        Assert.assertFalse(pageService.discussionExists(userName, userName, siteId, topic2));
    }
    
    @Test
    public void replyToTopic()
    {
        String siteId = "topic-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String topicTitle = "topic-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.DISCUSSIONS, null);
        Assert.assertTrue(pageService.createDiscussion(userName, userName, siteId, topicTitle, topicTitle, null));
        Assert.assertTrue(pageService.replyToDiscussion(userName, userName, siteId, topicTitle, "firstReply"));
        Assert.assertTrue(pageService.replyToDiscussion(userName, userName, siteId, topicTitle, "secondReply"));
        Assert.assertTrue(pageService.replyToDiscussion(userName, userName, siteId, topicTitle, "thirdReply"));
        List<String> replies = pageService.getDiscussionReplies(userName, userName, siteId, topicTitle);
        Assert.assertTrue(replies.contains("firstReply") && replies.contains("secondReply") && replies.contains("thirdReply"));
    }
    
    @Test
    public void commentBlog()
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.BLOG, null);
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, draftBlog, draftBlog, true, null));
        Assert.assertTrue(pageService.blogExists(userName, userName, siteId, draftBlog, true));
        Assert.assertTrue(pageService.commentBlog(userName, userName, siteId, draftBlog, true, "comment1"));
        Assert.assertTrue(pageService.commentBlog(userName, userName, siteId, draftBlog, true, "comment2"));
        Assert.assertTrue(pageService.commentBlog(userName, userName, siteId, draftBlog, true, "comment3"));
        Assert.assertTrue(pageService.commentBlog(userName, userName, siteId, draftBlog, true, "comment4"));
        List<String> comments = pageService.getBlogComments(userName, userName, siteId, draftBlog);
        Assert.assertTrue(comments.contains("comment1") && comments.contains("comment2") 
                && comments.contains("comment3") && comments.contains("comment4"));
        Assert.assertNotNull(pageService.getCommentNodeRef(userName, userName, siteId, Page.BLOG, draftBlog, "comment1"));
    }
    
    @Test
    public void commentLink()
    {
        String siteId = "link-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String linkTitle = "link-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.LINKS, null);
        Assert.assertTrue(pageService.createLink(userName, userName, siteId, linkTitle, linkTitle, linkTitle, true, null));
        Assert.assertTrue(pageService.linkExists(userName, userName, siteId, linkTitle));
        Assert.assertTrue(pageService.commentLink(userName, userName, siteId, linkTitle, "link comment 1"));
        Assert.assertTrue(pageService.commentLink(userName, userName, siteId, linkTitle, "link comment 2"));
        Assert.assertTrue(pageService.commentLink(userName, userName, siteId, linkTitle, "link comment 3"));
        Assert.assertTrue(pageService.commentLink(userName, userName, siteId, linkTitle, "link comment 4"));
        List<String> comments = pageService.getLinkComments(userName, userName, siteId, linkTitle);
        Assert.assertTrue(comments.contains("link comment 1") && comments.contains("link comment 2") 
                && comments.contains("link comment 3") && comments.contains("link comment 4"));
        Assert.assertTrue(pageService.deleteLinkComment(userName, userName, siteId, linkTitle, "link comment 1"));
        Assert.assertTrue(pageService.getCommentNodeRef(userName, userName, siteId, Page.LINKS, linkTitle, "link comment 1").isEmpty());
        comments = pageService.getLinkComments(userName, userName, siteId, linkTitle);
        Assert.assertFalse(comments.contains("link comment 1"));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void commentNonExistentBlog()
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        pageService.commentBlog(userName, userName, siteId, "fakeBlog", true, "comment1");
        pageService.commentBlog(userName, userName, siteId, "fakeBlog", true, "comment1");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void commentLinkNonExistentSite()
    {
        String siteId = "link-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String linkTitle = "link-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.LINKS, null);
        Assert.assertTrue(pageService.createLink(userName, userName, siteId, linkTitle, linkTitle, linkTitle, true, null));
        pageService.commentLink(userName, userName, "fakeSite", linkTitle, "link comment 1");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void getCommentsFakeBlog()
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        pageService.getBlogComments(userName, userName, siteId, draftBlog);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void getCommentsBlogsFakeSite()
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        pageService.getBlogComments(userName, userName, "fakeSite", draftBlog);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void getTopicDiscussionFakeSite()
    {
        String siteId = "topic-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String topicTitle = "topic-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(pageService.createDiscussion(userName, userName, siteId, topicTitle, topicTitle, null));
        Assert.assertTrue(pageService.discussionExists(userName, userName, siteId, topicTitle));
        pageService.getDiscussionReplies(userName, userName, "fakeSite", topicTitle);
    }
    
    @Test
    public void deleteCommentBlog()
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName, userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        site.addPageToSite(userName, userName, siteId, Page.BLOG, null);
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, draftBlog, draftBlog, true, null));
        Assert.assertTrue(pageService.blogExists(userName, userName, siteId, draftBlog, true));
        Assert.assertTrue(pageService.commentBlog(userName, userName, siteId, draftBlog, true, "comment1"));
        Assert.assertTrue(pageService.commentBlog(userName, userName, siteId, draftBlog, true, "comment2"));
        Assert.assertNotNull(pageService.getCommentNodeRef(userName, userName, siteId, Page.BLOG, draftBlog, "comment1"));
        Assert.assertTrue(pageService.deleteBlogComment(userName, userName, siteId, draftBlog, "comment1"));
        List<String> comments = pageService.getBlogComments(userName, userName, siteId, draftBlog);
        Assert.assertTrue(comments.contains("comment2"));
        Assert.assertFalse(comments.contains("comment1"));
    }
    
    @Test
    public void commentBlogConsumer()
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String userConsumer = "userCons-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName + "@test", userName, userName);
        userService.create(admin, admin, userConsumer, userConsumer, userConsumer + "@test", userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, userName, userConsumer, siteId, "SiteConsumer"));
        site.addPageToSite(userConsumer, userConsumer, siteId, Page.BLOG, null);
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, draftBlog, draftBlog, false, null));
        Assert.assertTrue(pageService.blogExists(userName, userName, siteId, draftBlog, false));
        Assert.assertFalse(pageService.commentBlog(userConsumer, userConsumer, siteId, draftBlog, true, "comment1"));
    }
    
    @Test
    public void commentBlogContributor()
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String userContributor = "userContrib-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName + "@test", userName, userName);
        userService.create(admin, admin, userContributor, userContributor, userContributor + "@test", userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, userName, userContributor, siteId, "SiteContributor"));
        site.addPageToSite(userContributor, userContributor, siteId, Page.BLOG, null);
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, draftBlog, draftBlog, false, null));
        Assert.assertTrue(pageService.blogExists(userName, userName, siteId, draftBlog, false));
        Assert.assertTrue(pageService.commentBlog(userContributor, userContributor, siteId, draftBlog, true, "comment1"));
    }
    
    @Test
    public void commentBlogCollaborator()
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String userCollaborator = "userCollab" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName + "@test", userName, userName);
        userService.create(admin, admin, userCollaborator, userCollaborator, userCollaborator + "@test", userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, userName, userCollaborator, siteId, "SiteCollaborator"));
        site.addPageToSite(userCollaborator, userCollaborator, siteId, Page.BLOG, null);
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, draftBlog, draftBlog, false, null));
        Assert.assertTrue(pageService.blogExists(userName, userName, siteId, draftBlog, false));
        Assert.assertTrue(pageService.commentBlog(userCollaborator, userCollaborator, siteId, draftBlog, true, "comment1"));
    }
    
    @Test
    public void deleteCommentContributor()
    {
        String siteId = "blog-site" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String userContributor = "userContrib-" + System.currentTimeMillis();
        String draftBlog = "draft" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, userName, userName + "@test", userName, userName);
        userService.create(admin, admin, userContributor, userContributor, userContributor + "@test", userName, userName);
        site.create(userName, userName, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, userName, userContributor, siteId, "SiteContributor"));
        site.addPageToSite(userContributor, userContributor, siteId, Page.BLOG, null);
        Assert.assertTrue(pageService.createBlogPost(userName, userName, siteId, draftBlog, draftBlog, false, null));
        Assert.assertTrue(pageService.blogExists(userName, userName, siteId, draftBlog, false));
        Assert.assertTrue(pageService.commentBlog(userName, userName, siteId, draftBlog, true, "comment1"));
        Assert.assertFalse(pageService.deleteBlogComment(userContributor, userContributor, siteId, draftBlog, "comment1"));
    }
}
