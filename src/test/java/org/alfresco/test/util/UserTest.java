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

import java.util.List;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.DashboardCustomization.DashletLayout;
import org.alfresco.dataprep.DashboardCustomization.UserDashlet;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test user data load api crud operations.
 * 
 * @author Michael Suzuki, Bogdan Bocancea
 */
public class UserTest extends AbstractTest
{
    @Autowired private SiteService site;
    @Autowired private UserService userService;
    @Autowired private ContentService contentService;
    String userName = "userm-" + System.currentTimeMillis();
    String firstName = "fname-" + System.currentTimeMillis();
    String lastName = "lname-" + System.currentTimeMillis();
    String email = userName;
    String globalUser = "global" + System.currentTimeMillis();
    String globalSite = "gSite" + System.currentTimeMillis();

    @BeforeClass(alwaysRun = true)
    public void userSetup()
    {
        userService.create(ADMIN, ADMIN, globalUser, password, globalUser + domain, firstName, lastName);
        site.create(globalUser, password, "mydomain", globalSite, globalSite, Visibility.PUBLIC);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserInvalidUserName()
    {
        userService.create(ADMIN, ADMIN, null, password, email, firstName, lastName);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserInvalidPassword()
    {
        userService.create(ADMIN, ADMIN, userName, null, email, firstName, lastName);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserNullADMIN()
    {
        userService.create(null, ADMIN, userName, password, email, firstName, lastName);
    }

    @Test
    public void createEnterpriseUser()
    {
        boolean result = userService.create(ADMIN, ADMIN, userName, password, email, firstName, lastName);
        Assert.assertTrue(result);
        Assert.assertTrue(userService.userExists(ADMIN, ADMIN, userName));
    }

    @Test
    public void checkUserExistsWhenHeDoesnt()
    {
        Assert.assertFalse(userService.userExists(ADMIN, ADMIN, "booo"));
    }

    @Test
    public void createSameEnterpriseUser()
    {
        String userName = "sameUserR1";
        String password = "password";
        userService.create(ADMIN, ADMIN, userName, password, email, firstName, lastName);
        boolean result = userService.create(ADMIN, ADMIN, userName, password, email, firstName, lastName);
        Assert.assertFalse(result);
    }

    @Test
    public void deleteUser()
    {
        String userName = "deleteUser";
        userService.create(ADMIN, ADMIN, userName, password, email, firstName, lastName);
        Assert.assertTrue(userService.delete(ADMIN, ADMIN, userName));
        Assert.assertFalse(userService.userExists(ADMIN, ADMIN, userName));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void deleteNonExistent()
    {
        String userName = "booo";
        userService.delete(ADMIN, ADMIN, userName);
    }

    @Test
    public void deleteUserContainingSpecialCharacters()
    {
        String userNameWithSpecialCharacters = "delete \"#user;<=>with?[]^special`{|}characters";
        userService.create(ADMIN, ADMIN, userNameWithSpecialCharacters, password + "@test", email, firstName, lastName);
        Assert.assertTrue(userService.delete(ADMIN, ADMIN, userNameWithSpecialCharacters));
        Assert.assertFalse(userService.userExists(ADMIN, ADMIN, userNameWithSpecialCharacters));
    }
  
    @Test
    public void requestSiteMembership()
    {
        Assert.assertTrue(userService.requestSiteMembership(ADMIN, ADMIN, globalSite));
    }
    
    @Test(dependsOnMethods="requestSiteMembership")
    public void requestSiteMembershipTwice()
    {
        Assert.assertFalse(userService.requestSiteMembership(ADMIN, ADMIN, globalSite));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void requestSiteMembershipNoExistentSite()
    {
        userService.requestSiteMembership(ADMIN, ADMIN, "fakeSite");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void requestSiteMembershipNoExistentUser()
    {
        userService.requestSiteMembership("fakeUser", "fakePass", globalSite);
    }
    
    @Test
    public void removePendingRequestModeratedSite()
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(ADMIN,
                    ADMIN,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.MODERATED);
        userService.create(ADMIN, ADMIN, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        Assert.assertTrue(userService.removePendingSiteRequest(ADMIN, ADMIN, userName, siteId));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removePendingRequestPublicSite()
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, globalSite));
        Assert.assertFalse(userService.removePendingSiteRequest(globalUser, password, userName, globalSite));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removePendingReqNonExistentSite()
    {
        userService.removePendingSiteRequest(ADMIN, ADMIN, userName, "fakeSite");
    }
    
    @Test
    public void removeSiteMembership()
    {
        String userName = "removeMember-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, globalSite));
        Assert.assertTrue(userService.removeSiteMembership(ADMIN, ADMIN, userName, globalSite));
    }
    
    @Test
    public void removeSiteMembershipByUser()
    {
        String userName = "userRemove-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, globalSite));
        Assert.assertTrue(userService.removeSiteMembership(globalUser, password, userName, globalSite));
    }
    
    @Test
    public void removeSiteMembershipNonMemberUser()
    {
        String userName = "userm-" + System.currentTimeMillis();
        String nonMemberUser = "nonMember-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userName, password, userName, firstName, lastName);
        userService.create(ADMIN, ADMIN, nonMemberUser, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, globalSite));
        Assert.assertFalse(userService.removeSiteMembership(nonMemberUser, password, userName, globalSite));
    }
    
    @Test
    public void removeSiteMembershiNoExistentUser()
    {
        Assert.assertFalse(userService.removeSiteMembership(ADMIN, ADMIN, "fakeUser", globalSite));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeSiteMembFakeSiteManager()
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, globalSite));
        userService.removeSiteMembership("fakeSiteManager", "fakePass", userName, globalSite);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeSiteMembershipNoExistentSite()
    {
        userService.removeSiteMembership(ADMIN, ADMIN, globalUser, "fakeSite");
    }

    @Test
    public void countSiteMembers()
    {
        String userManager = "user1-" + System.currentTimeMillis();
        String userToInvite = "user2-" + System.currentTimeMillis();
        String siteName = "site-" + System.currentTimeMillis();
        String emailUserManager = userManager + "@test.com";
        String emailUserToInvite = userToInvite + "@test.com";
        userService.create(ADMIN, ADMIN, userManager, password, emailUserManager, firstName, lastName);
        userService.create(ADMIN, ADMIN, userToInvite, password, emailUserToInvite, firstName, lastName);
        site.create(userManager, password, "mydomain", siteName, siteName, Visibility.PUBLIC);
        Assert.assertTrue(userService.createSiteMember(userManager, password, userToInvite, siteName, "SiteConsumer"));
        int noOfSiteMembers=userService.countSiteMembers(userManager, password, siteName);
        Assert.assertEquals(noOfSiteMembers, 2);
    }
    
    @Test
    public void countSiteMembersWithNoInvitee()
    {
        String userName = "user-" + System.currentTimeMillis();
        String siteName = "site-" + System.currentTimeMillis();
        String userEmail = userName + "@test.com";
        userService.create(ADMIN, ADMIN, userName, password, userEmail, firstName, lastName);
        site.create(userName, password, "mydomain", siteName, siteName, Visibility.PUBLIC);
        int noOfSiteMembers=userService.countSiteMembers(userName, password, siteName);
        Assert.assertEquals(noOfSiteMembers,1);
    }
   
    @Test
    public void addDashlets()
    {
        String theUser = "alfrescouser" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, theUser, password, theUser + domain, firstName, lastName);
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.WEB_VIEW, DashletLayout.THREE_COLUMNS, 3, 1));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.MY_SITES, DashletLayout.THREE_COLUMNS, 3, 2));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.WEB_VIEW, DashletLayout.FOUR_COLUMNS, 4, 1));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.MY_DOC_WORKSPACES, DashletLayout.FOUR_COLUMNS, 4, 2));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.RSS_FEED, DashletLayout.FOUR_COLUMNS, 4, 3));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.SITE_SEARCH, DashletLayout.FOUR_COLUMNS, 4, 4));
    }
    
    @Test
    public void addDashletToInvalidUser()
    {   
        Assert.assertFalse(userService.addDashlet("fakeUser", "fakePass", UserDashlet.WEB_VIEW, DashletLayout.THREE_COLUMNS, 3, 1));
    }
    
    @Test
    public void changeUserRole()
    {
        String userName = "userRole-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userName, password, userName + domain, firstName, lastName);
        Assert.assertTrue(userService.createSiteMember(globalUser, password, userName, globalSite, "SiteConsumer"));
        Assert.assertTrue(userService.changeUserRole(globalUser, password, globalSite, userName, "SiteCollaborator"));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidMember()
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userName, password, userName, firstName, lastName);
        userService.changeUserRole(ADMIN, ADMIN, globalSite, userName, "SiteCollaborator");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidSite()
    {
        userService.changeUserRole(ADMIN, ADMIN, "fakeSite", globalUser, "SiteCollaborator");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidRole()
    {
        userService.changeUserRole(ADMIN, ADMIN, globalSite, userName, "InvalidRole");
    }
    
    @Test
    public void addMemberToModeratedSite()
    {
        String userToAdd = "member" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToAdd, password, userToAdd + domain, firstName, lastName);
        Assert.assertTrue(userService.createSiteMember(globalUser, password, userToAdd, globalSite, "SiteContributor"));
    }
    
    @Test
    public void addMemberToPrivateSite()
    { 
        String userToAdd = "member" + System.currentTimeMillis();
        String privateSite = "site" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToAdd, password, userToAdd, firstName, lastName);
        site.create(globalUser, password, "mydomain", privateSite, privateSite, Visibility.PRIVATE);
        Assert.assertTrue(userService.createSiteMember(globalUser, password, userToAdd, privateSite, "SiteContributor"));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addMemberToInvalidSite()
    { 
        userService.createSiteMember(globalUser, password, ADMIN, "fakeSite", "SiteContributor");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addMemberInvalidRole()
    {
        String user = "userInvalidRole" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, user, password, user, firstName, lastName);
        userService.createSiteMember(globalUser, password, user, globalSite, "fakeRole");
    }

    @Test
    public void addMemberToSiteTwice()
    {
        String userToAdd = "member" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToAdd, password, userToAdd, firstName, lastName);
        Assert.assertTrue(userService.createSiteMember(globalUser, password, userToAdd, globalSite, "SiteContributor"));
        Assert.assertFalse(userService.createSiteMember(globalUser, password, userToAdd, globalSite, "SiteContributor"));
    }
    
    @Test
    public void followUser()
    {
        String user = "user" + System.currentTimeMillis();
        String userToFollow1 = "userFollow-1" + System.currentTimeMillis();
        String userToFollow2 = "userFollow-2" + System.currentTimeMillis();
        String userToFollow3 = "userFollow-3" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, user, password, user, firstName, lastName);
        userService.create(ADMIN, ADMIN, userToFollow1, password, userToFollow1, firstName, lastName);
        userService.create(ADMIN, ADMIN, userToFollow2, password, userToFollow2, firstName, lastName);
        userService.create(ADMIN, ADMIN, userToFollow3, password, userToFollow3, firstName, lastName);
        Assert.assertTrue(userService.followUser(user, password, userToFollow1));
        Assert.assertTrue(userService.followUser(user, password, userToFollow2));
        Assert.assertTrue(userService.followUser(user, password, userToFollow3));
        List<String> following = userService.getFollowingUsers(user, password);
        Assert.assertTrue(following.size()==3);
        Assert.assertTrue(following.contains(userToFollow1));
        Assert.assertTrue(following.contains(userToFollow2));
        Assert.assertTrue(following.contains(userToFollow3));
    }
    
    @Test
    public void followInvalidUser()
    {
        Assert.assertFalse(userService.followUser(ADMIN, ADMIN, "fakeUser"));
    }
    
    @Test
    public void getFollowers()
    {
        String userToFollow = "user" + System.currentTimeMillis();
        String user1 = "userFollow-1" + System.currentTimeMillis();
        String user2 = "userFollow-2" + System.currentTimeMillis();
        String user3 = "userFollow-3" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToFollow, password, userToFollow, firstName, lastName);
        userService.create(ADMIN, ADMIN, user1, password, user1, firstName, lastName);
        userService.create(ADMIN, ADMIN, user2, password, user2, firstName, lastName);
        userService.create(ADMIN, ADMIN, user3, password, user3, firstName, lastName);
        Assert.assertTrue(userService.followUser(user1, password, userToFollow));
        Assert.assertTrue(userService.followUser(user2, password, userToFollow));
        Assert.assertTrue(userService.followUser(user3, password, userToFollow));
        List<String> followers = userService.getFollowers(userToFollow, password);
        Assert.assertTrue(followers.size() == 3);
        Assert.assertTrue(followers.contains(user1));
        Assert.assertTrue(followers.contains(user2));
        Assert.assertTrue(followers.contains(user3));
    }
    
    @Test
    public void unfollowUser()
    {
        String user = "user" + System.currentTimeMillis();
        String userToFollow1 = "userFollow-1" + System.currentTimeMillis();
        String userToFollow2 = "userFollow-2" + System.currentTimeMillis();
        String userToFollow3 = "userFollow-3" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, user, password, user, firstName, lastName);
        userService.create(ADMIN, ADMIN, userToFollow1, password, userToFollow1, firstName, lastName);
        userService.create(ADMIN, ADMIN, userToFollow2, password, userToFollow2, firstName, lastName);
        userService.create(ADMIN, ADMIN, userToFollow3, password, userToFollow3, firstName, lastName);
        Assert.assertTrue(userService.followUser(user, password, userToFollow1));
        Assert.assertTrue(userService.followUser(user, password, userToFollow2));
        Assert.assertTrue(userService.followUser(user, password, userToFollow3));
        List<String> following = userService.getFollowingUsers(user, password);
        Assert.assertTrue(following.size() == 3);
        Assert.assertTrue(userService.unfollowUser(user, password, userToFollow1));
        Assert.assertTrue(userService.unfollowUser(user, password, userToFollow2));
        following = userService.getFollowingUsers(user, password);
        Assert.assertTrue(following.size() == 1);
    }
    
    @Test
    public void createRootCategory()
    {
        String rootCateg = "firstCateg" + System.currentTimeMillis();
        Assert.assertTrue(userService.createRootCategory(ADMIN, ADMIN, rootCateg));
        Assert.assertTrue(userService.categoryExists(ADMIN, ADMIN, rootCateg));
    }
    
    @Test
    public void createRootCategoryTwice()
    {
        String rootCateg = "firstCateg" + System.currentTimeMillis();
        Assert.assertTrue(userService.createRootCategory(ADMIN, ADMIN, rootCateg));
        Assert.assertFalse(userService.createRootCategory(ADMIN, ADMIN, rootCateg));
    }
    
    @Test
    public void createSubCategory()
    {
        String rootCateg = "rootCateg" + System.currentTimeMillis();
        String subCateg1 = "sub1" + System.currentTimeMillis();
        Assert.assertTrue(userService.createRootCategory(ADMIN, ADMIN, rootCateg));
        Assert.assertTrue(userService.createSubCategory(ADMIN, ADMIN, rootCateg, subCateg1));
    }
    
    @Test
    public void deleteRootCategory()
    {
        String rootCateg = "firstCateg" + System.currentTimeMillis();
        Assert.assertTrue(userService.createRootCategory(ADMIN, ADMIN, rootCateg));
        Assert.assertTrue(userService.deleteCategory(ADMIN, ADMIN, rootCateg));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteInvalidCategory()
    {
        userService.deleteCategory(ADMIN, ADMIN, "fakeCategory");
    }
    
    @Test
    public void deleteSubCategory()
    {
        String rootCateg = "rootCateg" + System.currentTimeMillis();
        String subCateg1 = "sub1" + System.currentTimeMillis();
        String subCateg2 = "sub2" + System.currentTimeMillis();
        String subCateg3 = "sub3" + System.currentTimeMillis();
        Assert.assertTrue(userService.createRootCategory(ADMIN, ADMIN, rootCateg));
        Assert.assertTrue(userService.createSubCategory(ADMIN, ADMIN, rootCateg, subCateg1));
        Assert.assertTrue(userService.createSubCategory(ADMIN, ADMIN, subCateg1, subCateg2));
        Assert.assertTrue(userService.createSubCategory(ADMIN, ADMIN, subCateg2, subCateg3));
        Assert.assertTrue(userService.categoryExists(ADMIN, ADMIN, rootCateg));
        Assert.assertTrue(userService.deleteCategory(ADMIN, ADMIN, subCateg3));
        Assert.assertTrue(userService.deleteCategory(ADMIN, ADMIN, rootCateg));
        Assert.assertFalse(userService.categoryExists(ADMIN, ADMIN, rootCateg));
    }
    
    @Test
    public void emptyTrashcan()
    {
        String folder = "deleteFold" + System.currentTimeMillis();
        String doc = "deleteDoc" + System.currentTimeMillis();
        contentService.createFolder(globalUser, password, folder, globalSite);
        contentService.createDocument(globalUser, password, globalSite, DocumentType.MSPOWERPOINT, doc, doc);
        contentService.deleteFolder(globalUser, password, globalSite, folder);
        contentService.deleteDocument(globalUser, password, globalSite, doc);
        Assert.assertTrue(userService.emptyTrashcan(globalUser, password));
        Assert.assertTrue(userService.getItemsNameFromTrashcan(globalUser, password).isEmpty());
    }
    
    @Test
    public void deleteTrashcanItem()
    {
        String folder = "deleteFold" + System.currentTimeMillis();
        String doc = "deleteDoc" + System.currentTimeMillis();
        Folder cmisFolder = contentService.createFolder(globalUser, password, folder, globalSite);
        Document cmisDocument = contentService.createDocument(globalUser, password, globalSite, DocumentType.MSPOWERPOINT, doc, doc);

        contentService.deleteFolder(globalUser, password, globalSite, folder);
        contentService.deleteDocument(globalUser, password, globalSite, doc);
        
        Assert.assertTrue(userService.deleteItemFromTranshcan(globalUser, password, cmisDocument.getId()));
        Assert.assertTrue(userService.getItemsNameFromTrashcan(globalUser, password).get(0).equalsIgnoreCase(folder));
        Assert.assertTrue(userService.getItemsNodeRefFromTrashcan(globalUser, password).get(0).contains(cmisFolder.getId()));
        Assert.assertTrue(userService.deleteItemFromTranshcan(globalUser, password, cmisFolder.getId()));
        Assert.assertTrue(userService.getItemsNameFromTrashcan(globalUser, password).isEmpty());
    }
    
    @Test
    public void recoverTrashcanItem()
    {
        String doc = "recoverDoc" + System.currentTimeMillis();
        contentService.createDocument(globalUser, password, globalSite, DocumentType.MSPOWERPOINT, doc, doc);
        String nodeRef = contentService.getNodeRef(globalUser, password, globalSite, doc);
        contentService.deleteDocument(globalUser, password, globalSite, doc);
        Assert.assertTrue(userService.recoverItemFromTranshcan(globalUser, password, nodeRef));
        Assert.assertFalse(contentService.getNodeRef(globalUser, password, globalSite, doc).isEmpty());
    }
    
    @Test
    public void deleteTrashcanFakeItem()
    {
        Assert.assertFalse(userService.deleteItemFromTranshcan(globalUser, password, "34242-3242wed-3652"));
    }
    
    @Test
    public void disableUser()
    {
        String userToDisable = "disableMe" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToDisable, password, userToDisable + domain, firstName, lastName);
        Assert.assertTrue(userService.disableUser(ADMIN, ADMIN, userToDisable, true));
    }
    
    @Test
    public void disableInxistentUser()
    {
        Assert.assertFalse(userService.disableUser(ADMIN, ADMIN, "user-" + System.currentTimeMillis(), true));
    }
    
    @Test
    public void disableEnableUser()
    {
        String userToDisable = "disableMe" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToDisable, password, userToDisable + domain, firstName, lastName);
        Assert.assertTrue(userService.disableUser(ADMIN, ADMIN, userToDisable, true));
        Assert.assertTrue(userService.disableUser(ADMIN, ADMIN, userToDisable, false));
    }
    
    @Test
    public void setUserQuota()
    {
        String userToUpdate = "quotaUser" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToUpdate, password, userToUpdate + domain, firstName, lastName);
        Assert.assertTrue(userService.setUserQuota(ADMIN, ADMIN, userToUpdate, 2));
    }
    
    @Test
    public void setUserQuotaInexistentUser()
    {
        String userToUpdate = "quotaUser" + System.currentTimeMillis();
        Assert.assertFalse(userService.setUserQuota(ADMIN, ADMIN, userToUpdate, 2));
    }
    
    @Test
    public void isUserAuthorized()
    {
        String userToVerify = "disableMe" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToVerify, password, userToVerify + domain, firstName, lastName);
        Assert.assertFalse(userService.isUserAuthorized(ADMIN, ADMIN, userToVerify));
        site.create(userToVerify,
                    password,
                    "myDomain",
                    "site" + System.currentTimeMillis(), 
                    "my site description", 
                    Visibility.MODERATED);
        Assert.assertTrue(userService.isUserAuthorized(ADMIN, ADMIN, userToVerify));
    }
    
    @Test
    public void changeUserPassword()
    {
        String userToUpdate = "passwordUser" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToUpdate, password, userToUpdate + domain, firstName, lastName);
        Assert.assertTrue(userService.changeUserPasswordByAdmin(ADMIN, ADMIN, userToUpdate, "new-password"));
        // check if new password works
        Assert.assertTrue(userService.followUser(userToUpdate, "new-password", ADMIN));
    }
}
