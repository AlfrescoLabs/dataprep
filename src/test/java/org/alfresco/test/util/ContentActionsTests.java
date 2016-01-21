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
import org.alfresco.dataprep.GroupService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
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
    @Autowired private GroupService groupService;
    private String document = "actionDoc";
    private String folder = "testFolder";
    private String commentDoc = "commentDoc";
    private String userName = "actionMan" + System.currentTimeMillis();
    private String siteName = "actionSite" + System.currentTimeMillis();
    private String userToInvite = "inviteUser" + System.currentTimeMillis();
    private String permissionUser = "permission" + System.currentTimeMillis();
    private String group = "group" + System.currentTimeMillis();
    private String permissionDoc = "permissionDoc" + System.currentTimeMillis();
    private String repoDoc = "actionRepoDoc" + System.currentTimeMillis();
    
    @BeforeClass(alwaysRun = true)
    public void userSetup()
    {
        userService.create(ADMIN, ADMIN, userName, password, userName + domain,"the","boss");
        site.create(userName, password, "mydomain", siteName,  "my site description", Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, document, document);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, permissionDoc, permissionDoc);
        content.createDocumentInRepository(userName, password, "Shared", DocumentType.PDF, repoDoc, repoDoc);
        content.createFolder(userName, password, folder, siteName);
        userToInvite = "inviteUser" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToInvite, password, userToInvite, "invited", "user");
        userService.create(ADMIN, ADMIN, permissionUser, password, permissionUser + "@test.com","permission","man");
        groupService.createGroup(ADMIN, ADMIN, group);
        userService.inviteUserToSiteAndAccept(userName, password, userToInvite, siteName, "SiteConsumer");
    }
    
    @Test
    public void addSingleTagDocument()
    {
        String tag1 = "tag1";
        Assert.assertTrue(contentAction.addSingleTag(userName, password, siteName, document, tag1));
        List<String> tag = contentAction.getTagNamesFromContent(userName, password, siteName, document);
        Assert.assertEquals(tag.get(0), tag1);
    }
    
    @Test
    public void addTagSymbols()
    {
        String tag1 = "!@#$%^&*()_+<>?:{}[]";
        String tagSymbolDoc = "tagDocSymb" + System.currentTimeMillis();
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, tagSymbolDoc, tagSymbolDoc);
        Assert.assertFalse(contentAction.addSingleTag(userName, password, siteName, tagSymbolDoc, tag1));
        List<String> tag = contentAction.getTagNamesFromContent(userName, password, siteName, tagSymbolDoc);
        Assert.assertTrue(tag.isEmpty());
    }
    
    @Test
    public void addMultipleTagsDocument()
    {
        String tag1 = "tag1" + System.currentTimeMillis();
        String tag2 = "tag2" + System.currentTimeMillis();
        String tag3 = "tag3" + System.currentTimeMillis();
        List<String> tags = new ArrayList<String>();
        tags.add(tag1);
        tags.add(tag2);
        tags.add(tag3);
        Assert.assertTrue(contentAction.addMultipleTags(userName, password, siteName, document, tags));
        List<String> returnTags = contentAction.getTagNamesFromContent(userName, password, siteName, document);
        Assert.assertEquals(returnTags.get(0), tag1);
        Assert.assertEquals(returnTags.get(1), tag2);
        Assert.assertEquals(returnTags.get(2), tag3);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addTagInvalidContent()
    {
        contentAction.addSingleTag(userName, password, siteName, "fakeDoc", "tag1");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addTagInvalidUser()
    {
        contentAction.addSingleTag("fakeUser", password, siteName, document, "tag1");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addTagInvalidSite()
    {
        contentAction.addSingleTag(userName, password, "fakeSite", document, "tag1");
    }
    
    @Test 
    public void addTagFolder()
    {
        String tagFolder = "tagFolder" + System.currentTimeMillis();
        content.createFolder(userName, password, tagFolder, siteName);
        contentAction.addSingleTag(userName, password, siteName, tagFolder, "tagfolder");
        List<String> returnTags = contentAction.getTagNamesFromContent(userName, password, siteName, tagFolder);
        Assert.assertTrue(returnTags.get(0).equals("tagfolder"));
    }
    
    @Test
    public void addMultipleTagsFolder()
    {
        String tag1 = "tag1" + System.currentTimeMillis();
        String tag2 = "tag2" + System.currentTimeMillis();
        String tag3 = "tag3" + System.currentTimeMillis();
        List<String> tags = new ArrayList<String>();
        tags.add(tag1);
        tags.add(tag2);
        tags.add(tag3);
        Assert.assertTrue(contentAction.addMultipleTags(userName, password, siteName, folder, tags));
        List<String> returnTags = contentAction.getTagNamesFromContent(userName, password, siteName, folder);
        Assert.assertEquals(returnTags.get(0), tag1);
        Assert.assertEquals(returnTags.get(1), tag2);
        Assert.assertEquals(returnTags.get(2), tag3);
    }
    
    @Test
    public void deleteTag()
    {
        String tag1 = "tag1";
        String docName = "removeTag" + System.currentTimeMillis();
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, docName, docName);
        Assert.assertTrue(contentAction.addSingleTag(userName, password, siteName, docName, tag1));
        contentAction.removeTag(userName, password, siteName, docName, tag1);
        List<String> tag = contentAction.getTagNamesFromContent(userName, password, siteName, docName);
        Assert.assertTrue(tag.isEmpty());
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteTagInvalidTag()
    {
        contentAction.removeTag(userName, password, siteName, document, "faketag");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteTagInvalidContent()
    {
        contentAction.removeTag(userName, password, siteName, "fakeDoc", "tag1");
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void deleteTagInvalidSite()
    {
        contentAction.removeTag(userName, password, "fakeSite", document, "tag1");
    }
    
    @Test
    public void addCommentForDocument()
    {
        String comment = "test comment";
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, commentDoc, comment));
        List<String> comments = contentAction.getComments(userName, password, siteName, commentDoc);
        Assert.assertEquals(comments.get(0), comment);
    }
    
    @Test
    public void addCommentForFolder()
    {
        String commentFolder = "comment on folder";
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, folder, commentFolder));
        List<String> comments = contentAction.getComments(userName, password, siteName, folder);
        Assert.assertEquals(comments.get(0), commentFolder);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addCommentInvalidContent()
    {
        contentAction.addComment(userName, password, siteName, "fakeDoc", "the comment");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addCommentInvalidSite()
    {
        contentAction.addComment(userName, password, "fakeSite", commentDoc, "fakeSiteComment");
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addEmptyComment()
    {
       contentAction.addComment(userName, password, siteName, folder, "");
    }
    
    @Test
    public void deleteCommentDocument()
    {
        String docName = "removeComment" + System.currentTimeMillis();
        String comment = "test remove comment";
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, docName, docName);
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, docName, comment));
        Assert.assertTrue(contentAction.removeComment(userName, password, siteName, docName, comment));
        List<String> comments = contentAction.getComments(userName, password, siteName, docName);
        Assert.assertTrue(comments.isEmpty());
    }
    
    @Test
    public void addMultipleCommentsDocument()
    {
        String comment1 = "comment1" + System.currentTimeMillis();
        String comment2 = "comment2" + System.currentTimeMillis();
        String comment3 = "comment3" + System.currentTimeMillis();
        List<String> comments = new ArrayList<String>();
        comments.add(comment1);
        comments.add(comment2);
        comments.add(comment3);
        Assert.assertTrue(contentAction.addMultipleComments(userName, password, siteName, commentDoc, comments));
        List<String> returnComments = contentAction.getComments(userName, password, siteName, commentDoc);
        Assert.assertEquals(returnComments.get(0), comment3);
        Assert.assertEquals(returnComments.get(1), comment2);
        Assert.assertEquals(returnComments.get(2), comment1);
    }
    
    @Test
    public void likeDocument()
    {
        Assert.assertTrue(contentAction.likeContent(userName, password, siteName, document));
        Assert.assertTrue(contentAction.likeContent(userToInvite, password, siteName, document));
        int likes = contentAction.countLikes(userName, password, siteName, document);
        Assert.assertEquals(likes, 2);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void likeInvalidDoc()
    {
        contentAction.likeContent(userName, password, siteName, "fakeDoc");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void likeInvalidUser()
    {
        contentAction.likeContent("fakeUser", password, siteName, document);
    }
    
    @Test(dependsOnMethods="likeDocument")
    public void removeLike()
    {
        Assert.assertTrue(contentAction.removeLike(userName, password, siteName, document));
        Assert.assertEquals(contentAction.countLikes(userName, password, siteName, document), 1);
        Assert.assertTrue(contentAction.removeLike(userToInvite, password, siteName, document));
        Assert.assertEquals(contentAction.countLikes(userName, password, siteName, document), 0);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeLikeInvalidDoc()
    {
        contentAction.removeLike(userName, password, siteName, "fakeDoc");
    }
    
    @Test
    public void likeFolder()
    {
        Assert.assertTrue(contentAction.likeContent(userName, password, siteName, folder));
        int likes = contentAction.countLikes(userName, password, siteName, folder);
        Assert.assertEquals(likes, 1);
    }
    
    @Test(dependsOnMethods="likeFolder")
    public void removeLikeFromFolder()
    {
        Assert.assertTrue(contentAction.removeLike(userName, password, siteName, folder));
        Assert.assertEquals(contentAction.countLikes(userName, password, siteName, folder), 0);
    }
    
    @Test
    public void setFavoriteDocument()
    {
        Assert.assertTrue(contentAction.setFileAsFavorite(userName, password, siteName, document));
        Assert.assertTrue(contentAction.isFavorite(userName, password, siteName, document));
    }
    
    @Test 
    public void setFavoriteFolder()
    {
        Assert.assertTrue(contentAction.setFolderAsFavorite(userName, password, siteName, folder));
        Assert.assertTrue(contentAction.isFavorite(userName, password, siteName, folder));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void favoriteFakeDocument()
    {
        contentAction.setFileAsFavorite(userName, password, siteName, "fakeDoc");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void favoriteFakeFolder()
    {
        contentAction.setFolderAsFavorite(userName, password, siteName, "fakeFolder");
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void favoriteFakeSite()
    {
        contentAction.setFolderAsFavorite(userName, password, "fakeSite", document);
    }
    
    @Test
    public void setFavoriteDocumentTwice()
    {
        String favDoc = "favDoc" + System.currentTimeMillis();
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, favDoc, favDoc);
        Assert.assertTrue(contentAction.setFileAsFavorite(userName, password, siteName, favDoc));
        Assert.assertTrue(contentAction.setFileAsFavorite(userName, password, siteName, favDoc));
    }
    
    @Test(dependsOnMethods="setFavoriteDocument")
    public void removeDocumentFavorite()
    {
        Assert.assertTrue(contentAction.removeFavorite(userName, password, siteName, document));
        Assert.assertFalse(contentAction.isFavorite(userName, password, siteName, document));
    }
    
    @Test(dependsOnMethods="setFavoriteFolder")
    public void removeFavoriteFolder()
    {
        Assert.assertTrue(contentAction.removeFavorite(userName, password, siteName, folder));
        Assert.assertFalse(contentAction.isFavorite(userName, password, siteName, folder));
    }
    
    @Test
    public void editOffline()
    {
        ObjectId idWpc = contentAction.checkOut(userName, password, siteName, document);
        Assert.assertTrue(!idWpc.getId().isEmpty());
        contentAction.cancelCheckOut(userName, password, siteName, document);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void cancelNotEditedFile()
    {
        String docName = "editDoc" + System.currentTimeMillis();
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, docName, docName);
        contentAction.cancelCheckOut(userName, password, siteName, docName);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void editOfflineFolder()
    {
        contentAction.checkOut(userName, password, siteName, folder);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void editOfflineTwice()
    {
        String docName = "editDoc" + System.currentTimeMillis();
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, docName, docName);
        contentAction.checkOut(userName, password, siteName, docName);
        contentAction.checkOut(userName, password, siteName, docName);
    }
    
    @Test
    public void checkIn()
    {
        String docName = "checkInDoc" + System.currentTimeMillis();
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
        String copyDoc = "copyFile" + System.currentTimeMillis();
        String targetFolder = "targeFolder";
        String sourceFolder = "sourceFolder";
        String subFolder = "subFolder";
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
        String moveDoc = "moveFile" + System.currentTimeMillis();
        String targetFolder = "targeFolder" + System.currentTimeMillis();
        String sourceFolder = "sourceFolder" + System.currentTimeMillis();
        String subFolder = "subFolder"  + System.currentTimeMillis();
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
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentConsumer = "comment consumer";
        userService.create(ADMIN, ADMIN, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteConsumer"));
        contentAction.addComment(inviteUser, password, siteName, commentDoc, commentConsumer);
    }
    
    //TODO: uncomment after ACE-4614 is fixed
    //@Test
    public void addCommentContributor()
    {
        String commentDoc = "contribDoc" + System.currentTimeMillis();
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentContributor = "comment contributor";
        userService.create(ADMIN, ADMIN, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, commentDoc, commentDoc);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteContributor"));
        Assert.assertTrue(contentAction.addComment(inviteUser, password, siteName, commentDoc, commentContributor));
        List<String> comments = contentAction.getComments(inviteUser, password, siteName, commentDoc);
        Assert.assertEquals(comments.get(0), commentContributor);
    }
    
    @Test
    public void addCommentCollaborator()
    {
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentCollaborator = "comment collaborator";
        userService.create(ADMIN, ADMIN, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteCollaborator"));
        Assert.assertTrue(contentAction.addComment(inviteUser, password, siteName, commentDoc, commentCollaborator));
        List<String> comments = contentAction.getComments(inviteUser, password, siteName, commentDoc);
        Assert.assertEquals(comments.get(0), commentCollaborator);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteCommentColaborator()
    {
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentCollaborator = "delete comment collaborator";
        userService.create(ADMIN, ADMIN, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteCollaborator"));
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, commentDoc, commentCollaborator));
        contentAction.removeComment(inviteUser, password, siteName, commentDoc, commentCollaborator);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteCommentContributor()
    {
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentContributor = "delete comment Contributor";
        userService.create(ADMIN, ADMIN, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteContributor"));
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, commentDoc, commentContributor));
        contentAction.removeComment(inviteUser, password, siteName, commentDoc, commentContributor);
    }
    
    @Test
    public void deleteCommentManager()
    {
        String inviteUser = "invite" + System.currentTimeMillis();
        String commentManager = "delete comment manager";
        userService.create(ADMIN, ADMIN, inviteUser, password, inviteUser + "@test.com","firstname","lastname");
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userName, password, inviteUser, siteName, "SiteManager"));
        Assert.assertTrue(contentAction.addComment(userName, password, siteName, commentDoc, commentManager));
        Assert.assertTrue(contentAction.removeComment(inviteUser, password, siteName, commentDoc, commentManager));
    }
    
    @Test
    public void setPermissionForDocument()
    {
        Assert.assertTrue(contentAction.setPermissionForUser(userName, password, siteName, permissionDoc, permissionUser, "SiteConsumer", false));
    }
    
    @Test
    public void setPermissionForFolder()
    {
        Assert.assertTrue(contentAction.setPermissionForUser(userName, password, siteName, folder, permissionUser, "SiteManager", true));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void setPermissionFakeContent()
    {
        contentAction.setPermissionForUser(userName, password, siteName, "fakeContent", permissionUser, "SiteManager", true);
    }
    
    @Test
    public void setPermissionForGroup()
    {
        Assert.assertTrue(contentAction.setPermissionForGroup(userName, password, siteName, permissionDoc, group, "SiteConsumer", false));
    }
    
    @Test(dependsOnMethods="setPermissionForGroup")
    public void removePermissionForGroup()
    {
        Assert.assertTrue(contentAction.removePermissionForGroup(userName, password, siteName, permissionDoc, group, "SiteConsumer", true));
    }
    
    @Test(dependsOnMethods="setPermissionForDocument")
    public void removePermissionForUser()
    {
        Assert.assertTrue(contentAction.removePermissionForUser(userName, password, siteName, permissionDoc, permissionUser, "SiteConsumer", false));
    }
    
    @Test
    public void copyToByPath()
    {
        String docToCopy = "copyDoc" + System.currentTimeMillis();
        content.createDocument(userName, password, siteName, DocumentType.HTML, docToCopy, docToCopy);
        CmisObject copiedObj = contentAction.copyTo(userName, password, "Sites/" + siteName + "/documentLibrary/" + docToCopy, "Shared");
        Assert.assertFalse(copiedObj.getId().isEmpty());
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void copyToBPathFakeFrom()
    {
        contentAction.copyTo(userName, password, "fakepath", "Shared");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void copyToBPathFakeTo()
    {
        contentAction.copyTo(userName, password, "Shared", "fakePath");
    }
    
    @Test
    public void moveToByPath()
    {
        String docToMove = "moveIt" + System.currentTimeMillis();
        content.createDocument(userName, password, siteName, DocumentType.HTML, docToMove, docToMove);
        CmisObject newObj = contentAction.moveTo(userName, password, "Sites/" + siteName + "/documentLibrary/" + docToMove, "Shared");
        Assert.assertFalse(newObj.getId().isEmpty());
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, docToMove).isEmpty());
        Assert.assertFalse(content.getNodeRefByPath(userName, password, "Shared/" + docToMove).isEmpty());
    }
    
    @Test
    public void addTagsInRepository()
    {
        String tag1 = "tag1-repo";
        String tag2 = "tag2-repo";
        String tag3 = "tag3-repo";
        List<String> tags = new ArrayList<String>();
        tags.add(tag1);
        tags.add(tag2);
        tags.add(tag3);
        Assert.assertTrue(contentAction.addSingleTag(userName, password, "Shared/" + repoDoc, "singleRepoTag"));
        Assert.assertTrue(contentAction.addMultipleTags(userName, password, "Shared/" + repoDoc, tags));
        List<String> getTags = contentAction.getTagNamesFromContent(userName, password, "Shared/" + repoDoc);
        Assert.assertTrue(getTags.get(0).equals("singleRepoTag".toLowerCase()));
        Assert.assertTrue(getTags.get(1).equals(tag1));
        Assert.assertTrue(getTags.get(2).equals(tag2));
        Assert.assertTrue(getTags.get(3).equals(tag3));
    }
    
    @Test(dependsOnMethods="addTagsInRepository")
    public void removeTagsFromRepository()
    {
        Assert.assertTrue(contentAction.removeTag(userName, password, "Shared/" + repoDoc, "singleRepoTag"));
        Assert.assertTrue(contentAction.removeTag(userName, password, "Shared/" + repoDoc, "tag1-repo"));
        List<String> getTags = contentAction.getTagNamesFromContent(userName, password, "Shared/" + repoDoc);
        Assert.assertEquals(getTags.size(), 2);
        Assert.assertTrue(getTags.get(0).equals("tag2-repo"));
        Assert.assertTrue(getTags.get(1).equals("tag3-repo"));
    }
}
