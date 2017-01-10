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

import org.alfresco.dataprep.DashboardCustomization.Page;
import org.alfresco.dataprep.SitePagesService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
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
    private String user = "theUser" + System.currentTimeMillis();
    private String theSite = "theSite" + System.currentTimeMillis();
    List<String>tags = new ArrayList<String>();
    
    @BeforeClass(alwaysRun = true)
    public void setup()
    {
        userService.create(ADMIN, ADMIN, user, password, user + domain, "firstName", "lastName");
        site.create(user, password, "mydomain", theSite, theSite, Visibility.PUBLIC);
        tags.add("tag1");
        tags.add("tag2");
    }
    
    @Test
    public void addAndUpdateCalendarEvent()
    {
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(user, password, theSite, "what1", "where1", "description2", today, today,
                "6:00 PM", "8:00 PM", false, "tag1"));
        Assert.assertTrue(pageService.addCalendarEvent(user, password, theSite, "what2", "where2", "description2", today, today,
                "12:00", "13:00", false, "tag1"));
        String name1 = pageService.getEventName(user, password, theSite, "what1", "where1", today, today, "6:00 PM", "8:00 PM", false);
        String name2 = pageService.getEventName(user, password, theSite, "what2", "where2", today, today, "12:00", "13:00", false);
        Assert.assertNotNull(name1);
        Assert.assertNotNull(name2);
        Assert.assertFalse(name1.equals(name2));
        Assert.assertTrue(pageService.updateEvent(user, password, theSite,name1, "what1-new", "where1-new", today, today, "10:00 PM", "11:00 PM", false));
    }

    @Test
    public void addCalendarEvent24h()
    {
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(user, password, theSite, "what24", "where24", "description", today, today, 
                "12:00", "13:00", false, "tag1"));
        String nameEvent = pageService.getEventName(user, password, theSite, "what24", "where24", today, today, "12:00", "13:00", false);
        Assert.assertNotNull(nameEvent);
    }
    
    @Test
    public void addCalendarEventNullParams()
    {
        Assert.assertTrue(pageService.addCalendarEvent(user, password, theSite, "whatNull", "whereNull", "description", null, null,
                null, null, false, "tag1"));
    }
    
    @Test
    public void addCalendarEventAllDay()
    {
        Assert.assertTrue(pageService.addCalendarEvent(user, password, theSite, "whatAllDay", "whereAllDay", "description", null, null,
                null, null, true, null));
        Date today = new Date();
        String nameEvent = pageService.getEventName(user, password, theSite, "whatAllDay", "whereAllDay", today, today, null, null, true);
        Assert.assertNotNull(nameEvent);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addCalendarEventFakeSite()
    {
        Assert.assertTrue(pageService.addCalendarEvent(user, password, "fakeSite", "what", "where", "description", null, null,
                null, null, true, null));
    }
    
    @Test
    public void removeEvent()
    {
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(user, password, theSite, "what", "where", "description", today, today, 
                "10:00", "11:00", false, "tag1"));
        String nameEvent = pageService.getEventName(user, password, theSite, "what", "where", today, today, "10:00", "11:00", false);
        Assert.assertNotNull(nameEvent);
        Assert.assertTrue(pageService.removeEvent(user, password, theSite, "what", "where", today, today, "10:00", "11:00", false));
        nameEvent = pageService.getEventName(user, password, theSite, "what", "where", today, today, "10:00", "11:00", false);
        Assert.assertTrue(nameEvent.isEmpty());;
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeEventInvalidCredentials()
    {
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(user, password, theSite, "what", "where", "description", today, today,
                "12:00", "13:00", false, "tag1"));
        pageService.removeEvent(user, "fakePassword", theSite, "what", "where", today, today, "12:00", "13:00", false);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeEventInvalidSite()
    {
        Date today = new Date();
        Assert.assertTrue(pageService.addCalendarEvent(user, password, theSite, "what", "where", "description", today, today,
                "12:00", "13:00", false, "tag1"));
        pageService.removeEvent(user, password, "fakeSite", "what", "where", today, today, "12:00", "13:00", false);
    }
    
    @Test
    public void createAndUpdateWikiPage()
    {
        String wikiTitle1 = "wiki_1" + System.currentTimeMillis();
        String wikiTitle2 = "Wiki Title" + System.currentTimeMillis();
        site.addPageToSite(user, password, theSite, Page.WIKI, null);
        Assert.assertTrue(pageService.createWiki(user, password, theSite, wikiTitle1, wikiTitle1, tags));
        Assert.assertTrue(pageService.createWiki(user, password, theSite, wikiTitle2, wikiTitle2, tags));
        Assert.assertTrue(pageService.wikiExists(user, password, theSite, wikiTitle1));
        Assert.assertTrue(pageService.wikiExists(user, password, theSite, wikiTitle2));
        Assert.assertFalse(pageService.wikiExists(user, password, theSite, "fakeWiki"));
        Assert.assertTrue(pageService.updateWikiPage(user, password, theSite, wikiTitle2, "newwikititle1", "new content1", tags));
    }
    
    @Test
    public void removeWikiPage()
    {
        String wikiTitle1 = "wikiRemove" + System.currentTimeMillis();
        String wikiTitle2 = "Wiki Remove" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createWiki(user, password, theSite, wikiTitle1, wikiTitle1, null));
        Assert.assertTrue(pageService.createWiki(user, password, theSite, wikiTitle2, wikiTitle2, null));
        Assert.assertTrue(pageService.deleteWikiPage(user, password, theSite, wikiTitle1));
        Assert.assertFalse(pageService.wikiExists(user, password, theSite, wikiTitle1));
        Assert.assertTrue(pageService.deleteWikiPage(user, password, theSite, wikiTitle2));
        Assert.assertFalse(pageService.wikiExists(user, password, theSite, wikiTitle2));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeNonExistentWikiPage()
    {
        pageService.deleteWikiPage(user, password, theSite, "fakeWiki");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeWikiNonExistentSite()
    {
        String wikiTitle1 = "wikiNoSite" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createWiki(user, password, theSite, wikiTitle1, wikiTitle1, null));
        pageService.deleteWikiPage(user, password, "fakeSite", wikiTitle1);
    }
    
    @Test
    public void createAndUpdateBlog()
    {
        String draftBlog = "draftCreate" + System.currentTimeMillis(); 
        String publishBlog = "publishCreate" + System.currentTimeMillis();
        site.addPageToSite(user, password, theSite, Page.BLOG, null);
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, publishBlog, publishBlog, false, tags));
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, draftBlog, draftBlog, true, tags));
        Assert.assertTrue(pageService.blogExists(user, password, theSite, draftBlog, true));
        Assert.assertTrue(pageService.blogExists(user, password, theSite, publishBlog, false));
        Assert.assertTrue( pageService.updateBlogPost(user, password, theSite, draftBlog, draftBlog+"-ed", draftBlog+"new text", false));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createBlogInvalidSite()
    {
        String draftBlog = "draft" + System.currentTimeMillis();
        pageService.createBlogPost(user, password, "fakeSite", draftBlog, draftBlog, false, null);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createBlogInvalidUser()
    {
        String draftBlog = "draft" + System.currentTimeMillis(); 
        pageService.createBlogPost("fakeUser", "fakePass", theSite, draftBlog, draftBlog, false, null);
    }
    
    @Test
    public void deleteBlog()
    {
        String draftBlog = "draftDelete" + System.currentTimeMillis();
        site.addPageToSite(user, password, theSite, Page.BLOG, null);
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, draftBlog, draftBlog, true, null));
        Assert.assertTrue(pageService.blogExists(user, password, theSite, draftBlog, true));
        Assert.assertTrue(pageService.deleteBlogPost(user, password, theSite, draftBlog, true));
        Assert.assertFalse(pageService.blogExists(user, password, theSite, draftBlog, true));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteBlogInvalidBlog()
    {
        pageService.deleteBlogPost(user, user, theSite, "invalidblog", true);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteBlogInvalidSite()
    {
        String draftBlog = "draftSite" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, draftBlog, draftBlog, true, null));
        pageService.deleteBlogPost(user, password, "invalidBlog", draftBlog, true);
    }
    
    @Test
    public void createAndUpdateLink()
    {
        String linkTitle = "linkCreate" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createLink(user, password, theSite, linkTitle, linkTitle, linkTitle, true, tags));
        Assert.assertTrue(pageService.linkExists(user, password, theSite, linkTitle));
        Assert.assertTrue(pageService.updateLink(user, password, theSite, linkTitle, "new link title", "new url", "new description", true, tags));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createLinkInvalidSite()
    {
        String linkTitle = "link" + System.currentTimeMillis();
        pageService.createLink(user, password, "fakeSite", linkTitle, linkTitle, linkTitle, true, null);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createLinkInvalidUser()
    {
        String linkTitle = "link" + System.currentTimeMillis();
        pageService.createLink("fakeUser", "fakePass", theSite, linkTitle, linkTitle, linkTitle, true, null);
    }
    
    @Test
    public void deleteLink()
    {
        String linkTitle = "link-delete" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createLink(user, password, theSite, linkTitle, linkTitle, linkTitle, true, null));
        Assert.assertTrue(pageService.linkExists(user, password, theSite, linkTitle));
        Assert.assertTrue(pageService.deleteLink(user, password, theSite, linkTitle));
        Assert.assertFalse(pageService.linkExists(user, password, theSite, linkTitle));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteLinkInvalidLink()
    {
        String link = "linkSiteDelete" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createLink(user, password, theSite, link, link, link, true, null));
        pageService.deleteLink(user, password, theSite, "fakeLink");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteLinkInvalidSite()
    {
        String link = "linkInvalidSIte" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createLink(user, password, theSite, link, link, link, true, null));
        pageService.deleteLink(user, password, "fakeSite", link);
    }
    
    @Test
    public void createAndUpdateDiscussionTopic()
    {
        String topicTitle = "topic-" + System.currentTimeMillis();
        site.addPageToSite(user, password, theSite, Page.DISCUSSIONS, null);
        Assert.assertTrue(pageService.createDiscussion(user, password, theSite, topicTitle, topicTitle, tags));
        Assert.assertTrue(pageService.discussionExists(user, password, theSite, topicTitle));
        Assert.assertTrue(pageService.updateDiscussion(user, password, theSite, topicTitle, "new title1", "new desc", tags));
    }
    
    @Test
    public void deleteDiscussionTopic()
    {
        String topicTitle = "topicDelete-" + System.currentTimeMillis();
        String topic2 = "topicDelete-2" + System.currentTimeMillis();
        site.addPageToSite(user, password, theSite, Page.DISCUSSIONS, null);
        Assert.assertTrue(pageService.createDiscussion(user, password, theSite, topicTitle, topicTitle, null));
        pageService.replyToDiscussion(user, password, theSite, topicTitle, "firstReplay");
        Assert.assertTrue(pageService.discussionExists(user, password, theSite, topicTitle));
        Assert.assertTrue(pageService.deleteDiscussion(user, password, theSite, topicTitle));
        Assert.assertFalse(pageService.discussionExists(user, password, theSite, topicTitle));
        Assert.assertTrue(pageService.createDiscussion(user, password, theSite, topic2, topic2, null));
        Assert.assertTrue(pageService.discussionExists(user, password, theSite, topic2));
        Assert.assertTrue(pageService.deleteDiscussion(user, password, theSite, topic2));
        Assert.assertFalse(pageService.discussionExists(user, password, theSite, topic2));
    }
    
    @Test
    public void replyToTopic()
    {
        String topicTitle = "topic-reply" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createDiscussion(user, password, theSite, topicTitle, topicTitle, null));
        Assert.assertTrue(pageService.replyToDiscussion(user, password, theSite, topicTitle, "firstReply"));
        Assert.assertTrue(pageService.replyToDiscussion(user, password, theSite, topicTitle, "secondReply"));
        Assert.assertTrue(pageService.replyToDiscussion(user, password, theSite, topicTitle, "thirdReply"));
        List<String> replies = pageService.getDiscussionReplies(user, password, theSite, topicTitle);
        Assert.assertTrue(replies.contains("firstReply") && replies.contains("secondReply") && replies.contains("thirdReply"));
    }
    
    @Test
    public void commentBlog()
    {
        String draftBlog = "draftComment" + System.currentTimeMillis(); 
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, draftBlog, draftBlog, true, null));
        Assert.assertTrue(pageService.blogExists(user, password, theSite, draftBlog, true));
        Assert.assertTrue(pageService.commentBlog(user, password, theSite, draftBlog, true, "comment1"));
        Assert.assertTrue(pageService.commentBlog(user, password, theSite, draftBlog, true, "comment2"));
        Assert.assertTrue(pageService.commentBlog(user, password, theSite, draftBlog, true, "comment3"));
        Assert.assertTrue(pageService.commentBlog(user, password, theSite, draftBlog, true, "comment4"));
        List<String> comments = pageService.getBlogComments(user, password, theSite, draftBlog);
        Assert.assertTrue(comments.contains("comment1") && comments.contains("comment2") 
                && comments.contains("comment3") && comments.contains("comment4"));
        Assert.assertNotNull(pageService.getCommentNodeRef(user, password, theSite, Page.BLOG, draftBlog, "comment1"));
    }
    
    @Test
    public void commentLink()
    {
        String linkTitle = "link-comment" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createLink(user, password, theSite, linkTitle, linkTitle, linkTitle, true, null));
        Assert.assertTrue(pageService.linkExists(user, password, theSite, linkTitle));
        Assert.assertTrue(pageService.commentLink(user, password, theSite, linkTitle, "link comment 1"));
        Assert.assertTrue(pageService.commentLink(user, password, theSite, linkTitle, "link comment 2"));
        Assert.assertTrue(pageService.commentLink(user, password, theSite, linkTitle, "link comment 3"));
        Assert.assertTrue(pageService.commentLink(user, password, theSite, linkTitle, "link comment 4"));
        List<String> comments = pageService.getLinkComments(user, password, theSite, linkTitle);
        Assert.assertTrue(comments.contains("link comment 1") && comments.contains("link comment 2") 
                && comments.contains("link comment 3") && comments.contains("link comment 4"));
        Assert.assertTrue(pageService.deleteLinkComment(user, password, theSite, linkTitle, "link comment 1"));
        Assert.assertTrue(pageService.getCommentNodeRef(user, password, theSite, Page.LINKS, linkTitle, "link comment 1").isEmpty());
        comments = pageService.getLinkComments(user, password, theSite, linkTitle);
        Assert.assertFalse(comments.contains("link comment 1"));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void commentNonExistentBlog()
    {
        pageService.commentBlog(user, password, theSite, "fakeBlog", true, "comment1");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void commentLinkNonExistentSite()
    {
        String linkTitle = "link-CommNoSite" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createLink(user, password, theSite, linkTitle, linkTitle, linkTitle, true, null));
        pageService.commentLink(user, password, "fakeSite", linkTitle, "link comment 1");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void getCommentsFakeBlog()
    {
        pageService.getBlogComments(user, password, theSite, "fake-blog");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void getCommentsBlogsFakeSite()
    {
        String draftBlog = "draftFakeSiteComm" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, draftBlog, draftBlog, true, null));
        pageService.getBlogComments(user, password, "fakeSite", draftBlog);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void getTopicDiscussionFakeSite()
    {
        String topicTitle = "topicFakeSiteComm" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createDiscussion(user, password, theSite, topicTitle, topicTitle, null));
        Assert.assertTrue(pageService.discussionExists(user, password, theSite, topicTitle));
        pageService.getDiscussionReplies(user, password, "fakeSite", topicTitle);
    }
    
    @Test
    public void deleteCommentBlog()
    {
        String draftBlog = "draftDeleteComm" + System.currentTimeMillis();
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, draftBlog, draftBlog, true, null));
        Assert.assertTrue(pageService.blogExists(user, password, theSite, draftBlog, true));
        Assert.assertTrue(pageService.commentBlog(user, password, theSite, draftBlog, true, "comment1"));
        Assert.assertTrue(pageService.commentBlog(user, password, theSite, draftBlog, true, "comment2"));
        Assert.assertNotNull(pageService.getCommentNodeRef(user, password, theSite, Page.BLOG, draftBlog, "comment1"));
        Assert.assertTrue(pageService.deleteBlogComment(user, password, theSite, draftBlog, "comment1"));
        List<String> comments = pageService.getBlogComments(user, password, theSite, draftBlog);
        Assert.assertTrue(comments.contains("comment2"));
        Assert.assertFalse(comments.contains("comment1"));
    }
    
    @Test
    public void commentBlogConsumer()
    {
        String userConsumer = "userCons-" + System.currentTimeMillis();
        String draftBlog = "draftConsumer" + System.currentTimeMillis(); 
        userService.create(ADMIN, ADMIN, userConsumer, userConsumer, userConsumer + domain, "fistN", "lastN");
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(user, password, userConsumer, theSite, "SiteConsumer"));
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, draftBlog, draftBlog, false, null));
        Assert.assertTrue(pageService.blogExists(user, password, theSite, draftBlog, false));
        Assert.assertFalse(pageService.commentBlog(userConsumer, userConsumer, theSite, draftBlog, true, "comment1"));
    }
    
    @Test
    public void commentBlogContributor()
    {
        String userContributor = "userContrib-" + System.currentTimeMillis();
        String draftBlog = "draftContrib" + System.currentTimeMillis(); 
        userService.create(ADMIN, ADMIN, userContributor, userContributor, userContributor + "@test", "first", "last");
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(user, password, userContributor, theSite, "SiteContributor"));
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, draftBlog, draftBlog, false, null));
        Assert.assertTrue(pageService.blogExists(user, password, theSite, draftBlog, false));
        Assert.assertTrue(pageService.commentBlog(userContributor, userContributor, theSite, draftBlog, true, "comment1"));
    }
    
    @Test
    public void commentBlogCollaborator()
    {
        String userCollaborator = "userCollab" + System.currentTimeMillis();
        String draftBlog = "draftCollab" + System.currentTimeMillis(); 
        userService.create(ADMIN, ADMIN, userCollaborator, userCollaborator, userCollaborator + "@test", "ff", "ll");
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(user, password, userCollaborator, theSite, "SiteCollaborator"));
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, draftBlog, draftBlog, false, null));
        Assert.assertTrue(pageService.blogExists(user, password, theSite, draftBlog, false));
        Assert.assertTrue(pageService.commentBlog(userCollaborator, userCollaborator, theSite, draftBlog, true, "comment1"));
    }
    
    @Test
    public void deleteCommentContributor()
    {
        String userContributor = "userContrib-" + System.currentTimeMillis();
        String draftBlog = "draftContrib" + System.currentTimeMillis(); 
        userService.create(ADMIN, ADMIN, userContributor, userContributor, userContributor + "@test", "fff", "lll");
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(user, password, userContributor, theSite, "SiteContributor"));
        Assert.assertTrue(pageService.createBlogPost(user, password, theSite, draftBlog, draftBlog, false, null));
        Assert.assertTrue(pageService.blogExists(user, password, theSite, draftBlog, false));
        Assert.assertTrue(pageService.commentBlog(user, password, theSite, draftBlog, true, "comment1"));
        Assert.assertFalse(pageService.deleteBlogComment(userContributor, userContributor, theSite, draftBlog, "comment1"));
    }
}
