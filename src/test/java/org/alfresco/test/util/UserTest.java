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

import org.alfresco.dataprep.DashboardCustomization.DashletLayout;
import org.alfresco.dataprep.DashboardCustomization.UserDashlet;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
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
    String userName = "userm-" + System.currentTimeMillis();
    String firstName = "fname-" + System.currentTimeMillis();
    String lastName = "lname-" + System.currentTimeMillis();
    String email = userName;
    String admin = "admin";
    String globalUser = "global" + System.currentTimeMillis();
    String globalSite = "gSite" + System.currentTimeMillis();

    @BeforeClass(alwaysRun = true)
    public void userSetup()
    {
        userService.create(admin, admin, globalUser, password, globalUser + domain, firstName, lastName);
        site.create(globalUser, password, "mydomain", globalSite, globalSite, Visibility.PUBLIC);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserInvalidUserName()
    {
        userService.create(admin, admin, null, password, email, firstName, lastName);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserInvalidPassword()
    {
        userService.create(admin, admin, userName, null, email, firstName, lastName);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserNullAdmin()
    {
        userService.create(null, admin, userName, password, email, firstName, lastName);
    }

    @Test
    public void creatEnterpriseUser()
    {
        boolean result = userService.create(admin, admin, userName, password, email, firstName, lastName);
        Assert.assertTrue(result);
        Assert.assertTrue(userService.userExists(admin, admin, userName));
    }

    @Test
    public void checkUserExistsWhenHeDoesnt()
    {
        Assert.assertFalse(userService.userExists(admin, admin, "booo"));
    }

    @Test
    public void createSameEnterpriseUser()
    {
        String userName = "sameUserR1";
        String password = "password";
        userService.create(admin, admin, userName, password, email, firstName, lastName);
        boolean result = userService.create(admin, admin, userName, password, email, firstName, lastName);
        Assert.assertFalse(result);
    }

    @Test
    public void deleteUser()
    {
        String userName = "deleteUser";
        userService.create(admin, admin, userName, password, email, firstName, lastName);
        Assert.assertTrue(userService.delete(admin, admin, userName));
        Assert.assertFalse(userService.userExists(admin, admin, userName));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void deleteNonExistent()
    {
        String userName = "booo";
        userService.delete(admin, admin, userName);
    }

    @Test
    public void deleteUserContainingSpecialCharacters()
    {
        String userNameWithSpecialCharacters = "delete \"#user;<=>with?[]^special`{|}characters";
        userService.create(admin, admin, userNameWithSpecialCharacters, password, email, firstName, lastName);
        Assert.assertTrue(userService.delete(admin, admin, userNameWithSpecialCharacters));
        Assert.assertFalse(userService.userExists(admin, admin, userNameWithSpecialCharacters));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inviteUserInvalidinvitingUser()
    {
        userService.inviteUserToSiteAndAccept(null, password, "userToInvite", "site", "SiteConsumer");
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inviteUserInvalidSite()
    {
        userService.inviteUserToSiteAndAccept("userSiteManager", password, "userToInvite", null , "SiteConsumer");
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inviteUserInvalidRole()
    {
        userService.inviteUserToSiteAndAccept("userSiteManager", password, "userToInvite", "site", null);
    }

    @Test
    public void inviteUserToSiteAndAccept()
    {
        String userToInvite = "inviteUser" + System.currentTimeMillis();
        String emailUserInvited = userToInvite + "@test.com";
        userService.create(admin, admin, userToInvite, password, emailUserInvited, "z" + firstName, lastName);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(globalUser, password, userToInvite, globalSite, "SiteConsumer"));
    }
    
    @Test
    public void inviteUserToNonExistentSite()
    {
        String userToInvite = "inviteUser" + System.currentTimeMillis();
        Assert.assertFalse(userService.inviteUserToSiteAndAccept(globalUser, password, userToInvite, "whatSite", "SiteConsumer"));
    }
  
    @Test
    public void requestSiteMembership()
    {
        Assert.assertTrue(userService.requestSiteMembership(admin, admin, globalSite));
    }
    
    @Test(dependsOnMethods="requestSiteMembership")
    public void requestSiteMembershipTwice()
    {
        Assert.assertFalse(userService.requestSiteMembership(admin, admin, globalSite));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void requestSiteMembershipNoExistentSite()
    {
        userService.requestSiteMembership(admin, admin, "fakeSite");
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
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.MODERATED);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        Assert.assertTrue(userService.removePendingSiteRequest(admin, admin, userName, siteId));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removePendingRequestPublicSite()
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, globalSite));
        Assert.assertFalse(userService.removePendingSiteRequest(globalUser, password, userName, globalSite));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removePendingReqNonExistentSite()
    {
        userService.removePendingSiteRequest(admin, admin, userName, "fakeSite");
    }
    
    @Test
    public void removeSiteMembership()
    {
        String userName = "removeMember-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, globalSite));
        Assert.assertTrue(userService.removeSiteMembership(admin, admin, userName, globalSite));
    }
    
    @Test
    public void removeSiteMembershipByUser()
    {
        String userName = "userRemove-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, globalSite));
        Assert.assertTrue(userService.removeSiteMembership(globalUser, password, userName, globalSite));
    }
    
    @Test
    public void removeSiteMembershipNonMemberUser()
    {
        String userName = "userm-" + System.currentTimeMillis();
        String nonMemberUser = "nonMember-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.create(admin, admin, nonMemberUser, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, globalSite));
        Assert.assertFalse(userService.removeSiteMembership(nonMemberUser, password, userName, globalSite));
    }
    
    @Test
    public void removeSiteMembershiNoExistentUser()
    {
        Assert.assertFalse(userService.removeSiteMembership(admin, admin, "fakeUser", globalSite));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeSiteMembFakeSiteManager()
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, globalSite));
        userService.removeSiteMembership("fakeSiteManager", "fakePass", userName, globalSite);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeSiteMembershipNoExistentSite()
    {
        userService.removeSiteMembership(admin, admin, globalUser, "fakeSite");
    }

    @Test
    public void countSiteMembers()
    {
        String userManager = "user1-" + System.currentTimeMillis();
        String userToInvite = "user2-" + System.currentTimeMillis();
        String siteName = "site-" + System.currentTimeMillis();
        String emailUserManager = userManager + "@test.com";
        String emailUserToInvite = userToInvite + "@test.com";
        userService.create(admin, admin, userManager, password, emailUserManager, firstName, lastName);
        userService.create(admin, admin, userToInvite, password, emailUserToInvite, firstName, lastName);
        site.create(userManager, password, "mydomain", siteName, siteName, Visibility.PUBLIC);
        userService.inviteUserToSiteAndAccept(userManager, password, userToInvite, siteName, "SiteConsumer");
        int noOfSiteMembers=userService.countSiteMembers(userManager, password, siteName);
        Assert.assertEquals(noOfSiteMembers, 2);
    }
    
    @Test
    public void countSiteMembersWithNoInvitee()
    {
        String userName = "user-" + System.currentTimeMillis();
        String siteName = "site-" + System.currentTimeMillis();
        String userEmail = userName + "@test.com";
        userService.create(admin, admin, userName, password, userEmail, firstName, lastName);
        site.create(userName, password, "mydomain", siteName, siteName, Visibility.PUBLIC);
        int noOfSiteMembers=userService.countSiteMembers(userName, password, siteName);
        Assert.assertEquals(noOfSiteMembers,1);
    }
   
    @Test
    public void addDashlets()
    {
        String theUser = "alfrescouser" + System.currentTimeMillis();
        userService.create(admin, admin, theUser, password, theUser + domain, firstName, lastName);
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.MY_MEETING_WORKSPACES, DashletLayout.THREE_COLUMNS, 3, 1));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.MY_DISCUSSIONS, DashletLayout.THREE_COLUMNS, 3, 2));
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
        userService.create(admin, admin, userName, password, userName + domain, firstName, lastName);
        userService.inviteUserToSiteAndAccept(globalUser, password, userName, globalSite, "SiteConsumer");
        Assert.assertTrue(userService.changeUserRole(globalUser, password, globalSite, userName, "SiteCollaborator"));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidMember()
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.changeUserRole(admin, admin, globalSite, userName, "SiteCollaborator");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidSite()
    {
        userService.changeUserRole(admin, admin, "fakeSite", globalUser, "SiteCollaborator");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidRole()
    {
        userService.changeUserRole(admin, admin, globalSite, userName, "InvalidRole");
    }
    
    @Test
    public void addMemberToModeratedSite()
    {
        String userToAdd = "member" + System.currentTimeMillis();
        userService.create(admin, admin, userToAdd, password, userToAdd + domain, firstName, lastName);
        Assert.assertTrue(userService.createSiteMember(globalUser, password, userToAdd, globalSite, "SiteContributor"));
    }
    
    @Test
    public void addMemberToPrivateSite()
    { 
        String userToAdd = "member" + System.currentTimeMillis();
        String privateSite = "site" + System.currentTimeMillis();
        userService.create(admin, admin, userToAdd, password, userToAdd, firstName, lastName);
        site.create(globalUser, password, "mydomain", privateSite, privateSite, Visibility.PRIVATE);
        Assert.assertTrue(userService.createSiteMember(globalUser, password, userToAdd, privateSite, "SiteContributor"));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addMemberToInvalidSite()
    { 
        userService.createSiteMember(globalUser, password, admin, "fakeSite", "SiteContributor");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addMemberInvalidRole()
    {
        String user = "userInvalidRole" + System.currentTimeMillis();
        userService.create(admin, admin, user, password, user, firstName, lastName);
        userService.createSiteMember(globalUser, password, user, globalSite, "fakeRole");
    }

    @Test
    public void addMemberToSiteTwice()
    {
        String userToAdd = "member" + System.currentTimeMillis();
        userService.create(admin, admin, userToAdd, password, userToAdd, firstName, lastName);
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
        userService.create(admin, admin, user, password, user, firstName, lastName);
        userService.create(admin, admin, userToFollow1, password, userToFollow1, firstName, lastName);
        userService.create(admin, admin, userToFollow2, password, userToFollow2, firstName, lastName);
        userService.create(admin, admin, userToFollow3, password, userToFollow3, firstName, lastName);
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
        userService.create(admin, admin, userToFollow, password, userToFollow, firstName, lastName);
        userService.create(admin, admin, user1, password, user1, firstName, lastName);
        userService.create(admin, admin, user2, password, user2, firstName, lastName);
        userService.create(admin, admin, user3, password, user3, firstName, lastName);
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
        userService.create(admin, admin, user, password, user, firstName, lastName);
        userService.create(admin, admin, userToFollow1, password, userToFollow1, firstName, lastName);
        userService.create(admin, admin, userToFollow2, password, userToFollow2, firstName, lastName);
        userService.create(admin, admin, userToFollow3, password, userToFollow3, firstName, lastName);
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
        Assert.assertTrue(userService.categoryExists(admin, admin, rootCateg));
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
        String subCateg2 = "sub2" + System.currentTimeMillis();
        String subCateg3 = "sub3" + System.currentTimeMillis();
        Assert.assertTrue(userService.createRootCategory(ADMIN, ADMIN, rootCateg));
        Assert.assertTrue(userService.createSubCategory(admin, admin, rootCateg, subCateg1));
        Assert.assertTrue(userService.createSubCategory(admin, admin, subCateg1, subCateg2));
        Assert.assertTrue(userService.createSubCategory(admin, admin, subCateg2, subCateg3));
    }
    
    @Test
    public void deleteRootCategory()
    {
        String rootCateg = "firstCateg" + System.currentTimeMillis();
        Assert.assertTrue(userService.createRootCategory(ADMIN, ADMIN, rootCateg));
        Assert.assertTrue(userService.deleteCategory(admin, admin, rootCateg));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void deleteInvalidCategory()
    {
        userService.deleteCategory(admin, admin, "fakeCategory");
    }
    
    @Test
    public void deleteSubCategory()
    {
        String rootCateg = "rootCateg" + System.currentTimeMillis();
        String subCateg1 = "sub1" + System.currentTimeMillis();
        String subCateg2 = "sub2" + System.currentTimeMillis();
        String subCateg3 = "sub3" + System.currentTimeMillis();
        Assert.assertTrue(userService.createRootCategory(ADMIN, ADMIN, rootCateg));
        Assert.assertTrue(userService.createSubCategory(admin, admin, rootCateg, subCateg1));
        Assert.assertTrue(userService.createSubCategory(admin, admin, subCateg1, subCateg2));
        Assert.assertTrue(userService.createSubCategory(admin, admin, subCateg2, subCateg3));
        Assert.assertTrue(userService.categoryExists(admin, admin, rootCateg));
        Assert.assertTrue(userService.deleteCategory(admin, admin, subCateg3));
        Assert.assertTrue(userService.deleteCategory(admin, admin, rootCateg));
        Assert.assertFalse(userService.categoryExists(admin, admin, rootCateg));
    }
}
