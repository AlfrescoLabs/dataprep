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
import java.util.List;

import org.alfresco.dataprep.ContentActions;
import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
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
    String admin = "admin";
    String password = "password";
    String tagDoc = "tagDoc";
    String folder = "testFolder"; 
    String commentDoc = "commentDoc";
    
    @Test
    public void addSingleTagDocument() throws Exception
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
    public void addTagSymbols() throws Exception
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
    public void addMultipleTagsDocument() throws Exception
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
    public void addTagInvalidContent() throws Exception
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
    public void addTagInvalidUser() throws Exception
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
    public void addTagInvalidSite() throws Exception
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
    public void addTagFolder() throws Exception
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
    public void addMultipleTagsFolder() throws Exception
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
    public void deleteTag() throws Exception
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
    public void deleteTagInvalidTag() throws Exception
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
    public void deleteTagInvalidContent() throws Exception
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
    public void deleteTagInvalidSite() throws Exception
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
    public void addCommentForDocument() throws Exception
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
    public void addCommentForFolder() throws Exception
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
    public void addCommentInvalidContent() throws Exception
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
    public void addCommentInvalidSite() throws Exception
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
    public void addEmptyComment() throws Exception
    {
        String siteName = "siteComment" + System.currentTimeMillis();
        String userName = "commentUser" + System.currentTimeMillis();
        String folder = "commentFolder";     
       contentAction.addComment(userName, password, siteName, folder, "");
    }
    
    @Test
    public void deleteCommentDocument() throws Exception
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
    public void addMultipleCommentsDocument() throws Exception
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
    public void likeDocument() throws Exception
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
    public void likeInvalidDoc() throws Exception
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
    public void likeInvalidUser() throws Exception
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
    public void removeLike() throws Exception
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
    public void deleteLikeInvalidDoc() throws Exception
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
    public void likeFolder() throws Exception
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
    public void removeLikeFromFolder() throws Exception
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
    public void setFavoriteDocument() throws Exception
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
    public void setFavoriteFolder() throws Exception
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
    public void favoriteFakeDocument() throws Exception
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
    public void favoriteFakeFolder() throws Exception
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
    public void favoriteFakeSite() throws Exception
    {
        String userName = "favUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, "fname", "lname");
        contentAction.setFolderAsFavorite(userName, password, "fakeSite", "doc");
    }
    
    @Test
    public void setFavoriteDocumentTwice() throws Exception
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
    public void removeDocumentFavorite() throws Exception
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
    public void removeFavoriteFolder() throws Exception
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
}
