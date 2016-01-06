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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.ContentActions;
import org.alfresco.dataprep.ContentAspects;
import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test content actions (tags, comments, likes, favorites).
 * 
 * @author Bogdan Bocancea
 */
public class ContentActionsTests extends AbstractTest
{   
    @Autowired private UserService userService;
    @Autowired private SiteService site;
    @Autowired private ContentService content;
    @Autowired private ContentActions contentAction;
    @Autowired private ContentAspects contentAspect;
    String admin = "admin";
    String password = "password";
    String tagDoc = "tagDoc";
    String folder = "testFolder"; 
    String commentDoc = "commentDoc";
    
    @Test
    public void addSingleTagDocument()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        Document doc1 = content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, tagDoc, tagDoc);
        Assert.assertFalse(doc1.getId().isEmpty());
        Assert.assertTrue(contentAction.addSingleTag(userName, password, siteName, tagDoc, tag1));
        List<String> tag = contentAction.getTagNamesFromContent(userName, password, siteName, tagDoc);
        Assert.assertEquals(tag.get(0), tag1);
    }
    
    @Test
    public void addTagSymbols()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "!@#$%^&*()_+<>?:{}[]";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, tagDoc, tagDoc);
        Assert.assertFalse(contentAction.addSingleTag(userName, password, siteName, tagDoc, tag1));
        List<String> tag = contentAction.getTagNamesFromContent(userName, password, siteName, tagDoc);
        Assert.assertTrue(tag.isEmpty());
    }
    
    @Test
    public void addMultipleTagsDocument()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1";
        String tag2 = "tag2";
        String tag3 = "tag3";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        List<String> tags = new ArrayList<String>();
        tags.add(tag1);
        tags.add(tag2);
        tags.add(tag3);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, tagDoc, tagDoc);
        Assert.assertTrue(contentAction.addMultipleTags(userName, password, siteName, tagDoc, tags));
        List<String> returnTags = contentAction.getTagNamesFromContent(userName, password, siteName, tagDoc);
        Assert.assertEquals(returnTags.get(0), tag1);
        Assert.assertEquals(returnTags.get(1), tag2);
        Assert.assertEquals(returnTags.get(2), tag3);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addTagInvalidContent()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        contentAction.addSingleTag(userName, password, siteName, "fakeDoc", tag1);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addTagInvalidUser()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        String tagDoc = "tagDoc";
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, tagDoc, tagDoc);
        contentAction.addSingleTag("fakeUser", password, siteName, "document", tag1);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addTagInvalidSite()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, tagDoc, tagDoc);
        contentAction.addSingleTag(userName, password, "fakeSite", tagDoc, tag1);
    }
    
    @Test 
    public void addTagFolder()
    {
        String siteName = "siteTag-" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        Folder newFolder = content.createFolder(userName, password, folder, siteName);
        Assert.assertFalse(newFolder.getId().isEmpty());
        contentAction.addSingleTag(userName, password, siteName, folder, tag1);
        List<String> returnTags = contentAction.getTagNamesFromContent(userName, password, siteName, folder);
        Assert.assertEquals(returnTags.get(0), tag1);
    }
    
    @Test
    public void addMultipleTagsFolder()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1";
        String tag2 = "tag2";
        String tag3 = "tag3";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        List<String> tags = new ArrayList<String>();
        tags.add(tag1);
        tags.add(tag2);
        tags.add(tag3);
        content.createFolder(userName, password, folder, siteName);
        Assert.assertTrue(contentAction.addMultipleTags(userName, password, siteName, folder, tags));
        List<String> returnTags = contentAction.getTagNamesFromContent(userName, password, siteName, folder);
        Assert.assertEquals(returnTags.get(0), tag1);
        Assert.assertEquals(returnTags.get(1), tag2);
        Assert.assertEquals(returnTags.get(2), tag3);
    }
    
    @Test
    public void deleteTag()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description",
                    Visibility.PUBLIC);
        Document doc1 = content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, tagDoc, tagDoc);
        Assert.assertFalse(doc1.getId().isEmpty());
        Assert.assertTrue(contentAction.addSingleTag(userName, password, siteName, tagDoc, tag1));
        contentAction.removeTag(userName, password, siteName, tagDoc, tag1);
        List<String> tag = contentAction.getTagNamesFromContent(userName, password, siteName, tagDoc);
        Assert.assertTrue(tag.isEmpty());
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteTagInvalidTag()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        Document doc1 = content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, tagDoc, tagDoc);
        Assert.assertFalse(doc1.getId().isEmpty());
        Assert.assertTrue(contentAction.addSingleTag(userName, password, siteName, tagDoc, tag1));
        contentAction.removeTag(userName, password, siteName, tagDoc, "faketag");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteTagInvalidContent()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        Document doc1 = content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, tagDoc, tagDoc);
        Assert.assertFalse(doc1.getId().isEmpty());
        Assert.assertTrue(contentAction.addSingleTag(userName, password, siteName, tagDoc, tag1));     
        contentAction.removeTag(userName, password, siteName, "fakeDoc", tag1);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void deleteTagInvalidSite()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String tag1 = "tag1";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        Document doc1 = content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, tagDoc, tagDoc);
        Assert.assertFalse(doc1.getId().isEmpty());
        Assert.assertTrue(contentAction.addSingleTag(userName, password, siteName, tagDoc, tag1));     
        contentAction.removeTag(userName, password, "fakeSite", tagDoc, tag1);
    }
    
    @Test
    public void addCommentForDocument()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String comment = "test comment";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, commentDoc, comment));
        List<String> comments = contentAction.getComments(userName, password, siteName, commentDoc);
        Assert.assertEquals(comments.get(0), comment);
    }
    
    @Test
    public void addCommentForFolder()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String commentFolder = "comment on folder";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, folder, commentFolder));
        List<String> comments = contentAction.getComments(userName, password, siteName, folder);
        Assert.assertEquals(comments.get(0), commentFolder);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addCommentInvalidContent()
    {
        String siteName = "siteTag" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String comment = "Comment";
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        contentAction.addComment(userName, password, siteName, "fakeDoc", comment);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addCommentInvalidSite()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "tagUser" + System.currentTimeMillis();
        String comment = "Comment" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        contentAction.addComment(userName, password, "fakeSite", commentDoc, comment);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addEmptyComment()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String folder = "commentFolder";
       contentAction.addComment(userName, password, siteName, folder, "");
    }
    
    @Test
    public void deleteCommentDocument()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String comment = "test comment";
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description",
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, commentDoc, comment));
        List<String> comments = contentAction.getComments(userName, password, siteName, commentDoc);
        Assert.assertEquals(comments.get(0), comment);
        Assert.assertTrue(contentAction.removeComment(userName, password, siteName, commentDoc, comment));
        comments = contentAction.getComments(userName, password, siteName, commentDoc);
        Assert.assertTrue(comments.isEmpty());
    }
    
    @Test
    public void addMultipleCommentsDocument()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String comment1 = "comment1";
        String comment2 = "comment2";
        String comment3 = "comment3";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        List<String> comments = new ArrayList<String>();
        comments.add(comment1);
        comments.add(comment2);
        comments.add(comment3);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        Assert.assertTrue(contentAction.addMultipleComments(userName, password, siteName, commentDoc, comments));
        List<String> returnComments = contentAction.getComments(userName, password, siteName, commentDoc);
        Assert.assertEquals(returnComments.get(0), comment3);
        Assert.assertEquals(returnComments.get(1), comment2);
        Assert.assertEquals(returnComments.get(2), comment1);  
    }
    
    @Test
    public void likeDocument()
    {
        String siteName = "siteLike" + System.currentTimeMillis();
        String userName = "likeUser" + System.currentTimeMillis();
        String userToInvite = "inviteUser" + System.currentTimeMillis();
        String likeDoc = "likeDoc";
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        userService.create(admin, admin, userToInvite, password, userToInvite, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, likeDoc, likeDoc);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, userToInvite, siteName, "SiteConsumer"));
        Assert.assertTrue(contentAction.likeContent(userName, password, siteName, likeDoc));
        Assert.assertTrue(contentAction.likeContent(userToInvite, password, siteName, likeDoc));
        int likes = contentAction.countLikes(userName, password, siteName, likeDoc);
        Assert.assertEquals(likes, 2);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void likeInvalidDoc()
    {
        String siteName = "siteLike" + System.currentTimeMillis();
        String userName = "likeUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        contentAction.likeContent(userName, password, siteName, "fakeDoc");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void likeInvalidUser()
    {
        String siteName = "siteLike" + System.currentTimeMillis();
        String userName = "likeUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description",
                    Visibility.PUBLIC);
        String likeDoc = "likeDoc";
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, likeDoc, likeDoc);
        contentAction.likeContent("fakeUser", password, siteName, likeDoc);
    }
    
    @Test
    public void removeLike()
    {
        String siteName = "siteLike" + System.currentTimeMillis();
        String userName = "likeUser" + System.currentTimeMillis();
        String userToInvite = "inviteUser" + System.currentTimeMillis();
        String likeDoc = "likeDoc";
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        userService.create(admin, admin, userToInvite, password, userToInvite, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description",
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, likeDoc, likeDoc);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, userToInvite, siteName, "SiteConsumer"));
        Assert.assertTrue(contentAction.likeContent(userName, password, siteName, likeDoc));
        Assert.assertTrue(contentAction.likeContent(userToInvite, password, siteName, likeDoc));
        Assert.assertTrue(contentAction.removeLike(userName, password, siteName, likeDoc));
        Assert.assertEquals(contentAction.countLikes(userName, password, siteName, likeDoc), 1);
        Assert.assertTrue(contentAction.removeLike(userToInvite, password, siteName, likeDoc));
        Assert.assertEquals(contentAction.countLikes(userName, password, siteName, likeDoc), 0);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteLikeInvalidDoc()
    {
        String siteName = "siteLike" + System.currentTimeMillis();
        String userName = "likeUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        contentAction.removeLike(userName, password, siteName, "fakeDoc");
    }
    
    @Test
    public void likeFolder()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis(); 
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description",
                    Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        Assert.assertTrue(contentAction.likeContent(userName, password, siteName, folder));
        int likes = contentAction.countLikes(userName, password, siteName, folder);
        Assert.assertEquals(likes, 1);
    }
    
    @Test
    public void removeLikeFromFolder()
    {
        String siteName = "siteLike" + System.currentTimeMillis();
        String userName = "likeUser" + System.currentTimeMillis();
        String userToInvite = "inviteUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        userService.create(admin, admin, userToInvite, password, userToInvite, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, userToInvite, siteName, "SiteConsumer"));
        Assert.assertTrue(contentAction.likeContent(userName, password, siteName, folder));
        Assert.assertTrue(contentAction.likeContent(userToInvite, password, siteName, folder));
        Assert.assertTrue(contentAction.removeLike(userName, password, siteName, folder));
        Assert.assertEquals(contentAction.countLikes(userName, password, siteName, folder), 1);
        Assert.assertTrue(contentAction.removeLike(userToInvite, password, siteName, folder));
        Assert.assertEquals(contentAction.countLikes(userName, password, siteName, folder), 0);
    }
    
    @Test
    public void setFavoriteDocument()
    {
        String siteName = "siteFav" + System.currentTimeMillis();
        String userName = "favUser" + System.currentTimeMillis();
        String favDoc = "favDoc";
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, favDoc, favDoc);
        Assert.assertTrue(contentAction.setFileAsFavorite(userName, password, siteName, favDoc));
        Assert.assertTrue(contentAction.isFavorite(userName, password, siteName, favDoc));
    }
    
    @Test 
    public void setFavoriteFolder()
    {
        String siteName = "siteFav" + System.currentTimeMillis();
        String userName = "favUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        Assert.assertTrue(contentAction.setFolderAsFavorite(userName, password, siteName, folder));
        Assert.assertTrue(contentAction.isFavorite(userName, password, siteName, folder));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void favoriteFakeDocument()
    {
        String siteName = "siteFav" + System.currentTimeMillis();
        String userName = "favUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                   Visibility.PUBLIC);
        contentAction.setFileAsFavorite(userName, password, siteName, "fakeDoc");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void favoriteFakeFolder()
    {
        String siteName = "siteFav" + System.currentTimeMillis();
        String userName = "favUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                   Visibility.PUBLIC);
        contentAction.setFolderAsFavorite(userName, password, siteName, "fakeFolder");
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void favoriteFakeSite()
    {
        String userName = "favUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        contentAction.setFolderAsFavorite(userName, password, "fakeSite", "doc");
    }
    
    @Test
    public void setFavoriteDocumentTwice()
    {
        String siteName = "siteFav" + System.currentTimeMillis();
        String userName = "favUser" + System.currentTimeMillis();
        String favDoc = "favDoc";
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, favDoc, favDoc);
        Assert.assertTrue(contentAction.setFileAsFavorite(userName, password, siteName, favDoc));
        Assert.assertTrue(contentAction.setFileAsFavorite(userName, password, siteName, favDoc));
    }
    
    @Test
    public void removeDocumentFavorite()
    {
        String siteName = "siteFav" + System.currentTimeMillis();
        String userName = "favUser" + System.currentTimeMillis();
        String favDoc = "favDoc";
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, favDoc, favDoc);
        Assert.assertTrue(contentAction.setFileAsFavorite(userName, password, siteName, favDoc));
        Assert.assertTrue(contentAction.removeFavorite(userName, password, siteName, favDoc));
        Assert.assertFalse(contentAction.isFavorite(userName, password, siteName, favDoc));
    }
    
    @Test 
    public void removeFavoriteFolder()
    {
        String siteName = "siteFav" + System.currentTimeMillis();
        String userName = "favUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        Assert.assertTrue(contentAction.setFolderAsFavorite(userName, password, siteName, folder));
        Assert.assertTrue(contentAction.removeFavorite(userName, password, siteName, folder));
        Assert.assertFalse(contentAction.isFavorite(userName, password, siteName, folder));
    }
    
    @Test
    public void editOffline()
    {
        String siteName = "editSite" + System.currentTimeMillis();
        String userName = "editUser" + System.currentTimeMillis();
        String docName = "editDoc";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, docName, docName);
        ObjectId idWpc = contentAction.checkOut(userName, password, siteName, docName);
        Assert.assertTrue(!idWpc.getId().isEmpty());
        contentAction.cancelCheckOut(userName, password, siteName, docName);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void cancelNotEditedFile()
    {
        String siteName = "editSite" + System.currentTimeMillis();
        String docName = "editDoc";
        site.create(ADMIN,
                    ADMIN,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(ADMIN, ADMIN, siteName, DocumentType.TEXT_PLAIN, docName, docName);
        contentAction.cancelCheckOut(ADMIN, ADMIN, siteName, docName);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void editOfflineFolder()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String folder = "folderTest";
        site.create(ADMIN,
                    ADMIN,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createFolder(ADMIN, ADMIN, folder, siteName);
        contentAction.checkOut(ADMIN, ADMIN, siteName, folder);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void editOfflineTwice()
    {
        String siteName = "editSite" + System.currentTimeMillis();
        String userName = "editUser" + System.currentTimeMillis();
        String docName = "editDoc";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, docName, docName);
        contentAction.checkOut(userName, password, siteName, docName);
        contentAction.checkOut(userName, password, siteName, docName);
    }
    
    @Test
    public void checkIn()
    {
        String siteName = "checkInSite" + System.currentTimeMillis();
        String userName = "checkInUser" + System.currentTimeMillis();
        String docName = "checkInDoc" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, docName, docName);
        ObjectId id = contentAction.checkIn(userName, password, siteName, docName, DocumentType.TEXT_PLAIN, "new content", true, "comment");
        Assert.assertFalse(id.getId().isEmpty());
        contentAction.checkIn(userName, password, siteName, docName, DocumentType.TEXT_PLAIN, "third edit", false, "third comment");
        String version = contentAction.getVersion(userName, password, siteName, docName);
        Assert.assertEquals(version, "2.1");
        Assert.assertTrue(content.getDocumentContent(userName, password, siteName, docName).equals("third edit"));
    }
    
    @Test
    public void folderCopyTo()
    {
        String siteName = "copyToSite" + System.currentTimeMillis();
        String userName = "copytoUser" + System.currentTimeMillis();
        String copyDoc = "copyFile" + System.currentTimeMillis();
        String targetFolder = "targeFolder";
        String sourceFolder = "sourceFolder";
        String subFolder = "subFolder";
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description",
                    Visibility.PUBLIC);
        content.createFolder(userName, password, targetFolder, siteName);
        Folder f1 = content.createFolder(userName, password, sourceFolder, siteName);
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        properties.put(PropertyIds.NAME, subFolder);
        f1.createFolder(properties);
        content.createDocumentInFolder(userName, password, siteName, sourceFolder, DocumentType.TEXT_PLAIN, copyDoc, copyDoc);
        content.createDocumentInFolder(userName, password, siteName, subFolder, DocumentType.TEXT_PLAIN, copyDoc + "R1", copyDoc + "R1");
        Folder objectCopied = (Folder) contentAction.copyTo(userName, password, siteName, sourceFolder, siteName, targetFolder);
        Assert.assertTrue(objectCopied.getFolderParent().getName().equals(targetFolder));
    }
    
    @Test
    public void copyFileAnotherSite()
    {
        String siteName1 = "sourceSite" + System.currentTimeMillis();
        String siteName2 = "targetSite" + System.currentTimeMillis();
        String fileToCopy = "fileToCopy";
        site.create(ADMIN, ADMIN, "mydomain", siteName1,  "my site description", Visibility.PUBLIC);
        site.create(ADMIN, ADMIN, "mydomain", siteName2,  "my site description", Visibility.PUBLIC);
        content.createDocument(ADMIN, ADMIN, siteName1, DocumentType.MSWORD, fileToCopy, fileToCopy);
        contentAction.copyTo(ADMIN, ADMIN, siteName1, fileToCopy, siteName2, null);
        Assert.assertNotNull(content.getNodeRef(ADMIN, ADMIN, siteName2, fileToCopy));
        Assert.assertNotNull(content.getNodeRef(ADMIN, ADMIN, siteName1, fileToCopy));
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void copyFileNonExistentTarget()
    {
        String siteName1 = "sourceSite" + System.currentTimeMillis();
        String fileToCopy = "fileToCopy";
        site.create(ADMIN, ADMIN, "mydomain", siteName1,  "my site description", Visibility.PUBLIC);
        content.createDocument(ADMIN, ADMIN, siteName1, DocumentType.MSWORD, fileToCopy, fileToCopy);
        contentAction.copyTo(ADMIN, ADMIN, siteName1, fileToCopy, "fakeSite", null);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void copyFileNonExistentSource()
    {
        String siteName1 = "sourceSite" + System.currentTimeMillis();
        site.create(ADMIN, ADMIN, "mydomain", siteName1,  "my site description", Visibility.PUBLIC);
        contentAction.copyTo(ADMIN, ADMIN, siteName1, "fakeFile", siteName1, null);
    }
    
    @Test
    public void moveTo()
    {
        String siteName = "moveToSite" + System.currentTimeMillis();
        String userName = "moveUser" + System.currentTimeMillis();
        String moveDoc = "moveFile" + System.currentTimeMillis();
        String targetFolder = "targeFolder";
        String sourceFolder = "sourceFolder";
        String subFolder = "subFolder";
        userService.create(admin, admin, userName, password, userName,"firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName,
                    "my site description",
                    Visibility.PUBLIC);
        content.createFolder(userName, password, targetFolder, siteName);
        Folder f1 = content.createFolder(userName, password, sourceFolder, siteName);
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        properties.put(PropertyIds.NAME, subFolder);
        f1.createFolder(properties);
        content.createDocumentInFolder(userName, password, siteName, sourceFolder, DocumentType.TEXT_PLAIN, moveDoc, moveDoc);
        content.createDocumentInFolder(userName, password, siteName, subFolder, DocumentType.TEXT_PLAIN, moveDoc + "R1", moveDoc + "R1");
        Folder objectMoved = (Folder) contentAction.moveTo(userName, password, siteName, subFolder, siteName, targetFolder);
        
        Assert.assertTrue(objectMoved.getFolderParent().getName().equals(targetFolder));
        Assert.assertFalse(content.getNodeRefByPath(userName, password,
                "Sites/" + siteName + "/documentLibrary/" + sourceFolder + "/" + moveDoc).isEmpty());
        Assert.assertTrue(content.getNodeRefByPath(userName, password, 
                "Sites/" + siteName + "/documentLibrary/" + sourceFolder + "/" + subFolder).isEmpty());
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void moveFileNonExistentTarget()
    {
        String siteName1 = "sourceSite" + System.currentTimeMillis();
        String fileToMove = "fileToCopy";
        site.create(ADMIN, ADMIN, "mydomain", siteName1,  "my site description", Visibility.PUBLIC);
        content.createDocument(ADMIN, ADMIN, siteName1, DocumentType.MSWORD, fileToMove, fileToMove);
        contentAction.moveTo(ADMIN, ADMIN, siteName1, fileToMove, "fakeSite", null);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void moveFileNonExistentSource()
    {
        String siteName1 = "sourceSite" + System.currentTimeMillis();
        site.create(ADMIN, ADMIN, "mydomain", siteName1,  "my site description", Visibility.PUBLIC);
        contentAction.copyTo(ADMIN, ADMIN, siteName1, "fakeFile", siteName1, null);
    }
    
    @Test
    public void moveFileAnotherSite()
    {
        String siteName1 = "sourceSite" + System.currentTimeMillis();
        String siteName2 = "targetSite" + System.currentTimeMillis();
        String fileToCopy = "fileToMove";
        site.create(ADMIN, ADMIN, "mydomain", siteName1,  "my site description", Visibility.PUBLIC);
        site.create(ADMIN, ADMIN, "mydomain", siteName2,  "my site description", Visibility.PUBLIC);
        content.createDocument(ADMIN, ADMIN, siteName1, DocumentType.MSWORD, fileToCopy, fileToCopy);
        contentAction.moveTo(ADMIN, ADMIN, siteName1, fileToCopy, siteName2, null);
        Assert.assertNotNull(content.getNodeRef(ADMIN, ADMIN, siteName2, fileToCopy));
        Assert.assertTrue(content.getNodeRef(ADMIN, ADMIN, siteName1, fileToCopy).isEmpty());
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addCommentConsumer()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentConsumer = "comment consumer";
        userService.create(admin, admin, userName, password, userName +  "@test.com","firstname","lastname");
        userService.create(admin, admin, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteConsumer"));
        contentAction.addComment(inviteUser, password, siteName, commentDoc, commentConsumer);
    }
    
    //TODO: uncomment after ACE-4614 is fixed
    //@Test
    public void addCommentContributor()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentContributor = "comment contributor";
        userService.create(admin, admin, userName, password, userName +  "@test.com","firstname","lastname");
        userService.create(admin, admin, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteContributor"));
        Assert.assertTrue(contentAction.addComment(inviteUser, password, siteName, commentDoc, commentContributor));
        List<String> comments = contentAction.getComments(inviteUser, password, siteName, commentDoc);
        Assert.assertEquals(comments.get(0), commentContributor);
    }
    
    @Test
    public void addCommentCollaborator()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentCollaborator = "comment collaborator";
        userService.create(admin, admin, userName, password, userName +  "@test.com","firstname","lastname");
        userService.create(admin, admin, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteCollaborator"));
        Assert.assertTrue(contentAction.addComment(inviteUser, password, siteName, commentDoc, commentCollaborator));
        List<String> comments = contentAction.getComments(inviteUser, password, siteName, commentDoc);
        Assert.assertEquals(comments.get(0), commentCollaborator);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteCommentColaborator()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentCollaborator = "comment collaborator";
        userService.create(admin, admin, userName, password, userName +  "@test.com","firstname","lastname");
        userService.create(admin, admin, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteCollaborator"));
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, commentDoc, commentCollaborator));
        contentAction.removeComment(inviteUser, password, siteName, commentDoc, commentCollaborator);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteCommentContributor()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentContributor = "comment Contributor";
        userService.create(admin, admin, userName, password, userName +  "@test.com","firstname","lastname");
        userService.create(admin, admin, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteContributor"));
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, commentDoc, commentContributor));
        contentAction.removeComment(inviteUser, password, siteName, commentDoc, commentContributor);
    }
    
    @Test
    public void deleteCommentManager()
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentManager = "comment manager";
        userService.create(admin, admin, userName, password, userName +  "@test.com","firstname","lastname");
        userService.create(admin, admin, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteManager"));
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, commentDoc, commentManager));
        Assert.assertTrue(contentAction.removeComment(inviteUser, password, siteName, commentDoc, commentManager));
    }
}
