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
import org.alfresco.dataprep.GroupService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
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
    @Autowired private GroupService groupService;
    String userName = "userm-" + System.currentTimeMillis();
    String firstName = "fname-" + System.currentTimeMillis();
    String lastName = "lname-" + System.currentTimeMillis();
    String password = "password";
    String email = userName;
    String admin = "admin";

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserInvalidUserName() throws Exception
    {
        userService.create(admin, admin, null, password, email, firstName, lastName);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserInvalidPassword() throws Exception
    {
        userService.create(admin, admin, userName, null, email, firstName, lastName);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserNullAdmin() throws Exception
    {
        userService.create(null, admin, userName, password, email, firstName, lastName);
    }

    @Test
    public void creatEnterpriseUser() throws Exception
    {
        boolean result = userService.create(admin, admin, userName, password, email, firstName, lastName);
        Assert.assertTrue(result);
        Assert.assertTrue(userService.userExists(admin, admin, userName));
    }

    @Test
    public void checkUserExistsWhenHeDoesnt() throws Exception
    {
        Assert.assertFalse(userService.userExists(admin, admin, "booo"));
    }

    @Test
    public void createSameEnterpriseUser() throws Exception
    {
        String userName = "sameUserR1";
        String password = "password";
        userService.create(admin, admin, userName, password, email, firstName, lastName);
        boolean result = userService.create(admin, admin, userName, password, email, firstName, lastName);
        Assert.assertFalse(result);
    }

    @Test
    public void deleteUser() throws Exception
    {
        String userName = "deleteUser";
        userService.create(admin, admin, userName, password, email, firstName, lastName);
        Assert.assertTrue(userService.delete(admin, admin, userName));
        Assert.assertFalse(userService.userExists(admin, admin, userName));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void deleteNonExistent() throws Exception
    {
        String userName = "booo";
        userService.delete(admin, admin, userName);
    }

    @Test
    public void deleteUserContainingSpecialCharacters() throws Exception
    {
        String userNameWithSpecialCharacters = "delete \"#user;<=>with?[]^special`{|}characters";
        userService.create(admin, admin, userNameWithSpecialCharacters, password, email, firstName, lastName);
        Assert.assertTrue(userService.delete(admin, admin, userNameWithSpecialCharacters));
        Assert.assertFalse(userService.userExists(admin, admin, userNameWithSpecialCharacters));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inviteUserInvalidinvitingUser() throws Exception
    {
        userService.inviteUserToSiteAndAccept(null, password, "userToInvite", "site", "SiteConsumer");
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inviteUserInvalidSite() throws Exception
    {
        userService.inviteUserToSiteAndAccept("userSiteManager", password, "userToInvite", null , "SiteConsumer");
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inviteUserInvalidRole() throws Exception
    {
        userService.inviteUserToSiteAndAccept("userSiteManager", password, "userToInvite", "site", null);
    }

    @Test
    public void inviteUserToSiteAndAccept() throws Exception
    {
        String userManager = "userSiteManager" + System.currentTimeMillis();
        String userToInvite = "inviteUser" + System.currentTimeMillis();
        String emailUserManager = userManager + "@test.com";
        String emailUserInvited = userToInvite + "@test.com";
        String siteId = "site" + System.currentTimeMillis();
        userService.create(admin, admin, userManager, password, emailUserManager, firstName, lastName);
        userService.create(admin, admin, userToInvite, password, emailUserInvited, "z" + firstName, lastName);
        site.create(userManager, password, "mydomain", siteId, siteId, Visibility.PUBLIC);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userManager, password, userToInvite, siteId, "SiteConsumer"));
    }
    
    @Test
    public void inviteUserToNonExistentSite() throws Exception
    {
        String userManager = "userSiteManager" + System.currentTimeMillis();
        String userToInvite = "inviteUser" + System.currentTimeMillis();
        String emailUserManager = userManager + "@test.com";
        userService.create(admin, admin, userManager, password, emailUserManager, firstName, lastName);
        Assert.assertFalse(userService.inviteUserToSiteAndAccept(userManager, password, userToInvite, "whatSite", "SiteConsumer"));
    }
  
    @Test
    public void requestSiteMembership() throws Exception
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
    }
    
    @Test
    public void requestSiteMembershipTwice() throws Exception
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        Assert.assertFalse(userService.requestSiteMembership(userName, password, siteId));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void requestSiteMembershipNoExistentSite() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.requestSiteMembership(userName, password, "fakeSite");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void requestSiteMembershipNoExistentUser() throws Exception
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
        userService.requestSiteMembership("fakeUser", password, "fakeSite");
    }
    
    @Test
    public void removePendingRequestModeratedSite() throws Exception
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
    public void removePendingRequestPublicSite() throws Exception
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        Assert.assertFalse(userService.removePendingSiteRequest(admin, admin, userName, siteId));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removePendingReqNonExistentSite() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.removePendingSiteRequest(admin, admin, userName, "fakeSite");
    }
    
    @Test
    public void removeSiteMembership() throws Exception
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        Assert.assertTrue(userService.removeSiteMembership(admin, admin, userName, siteId));
    }
    
    @Test
    public void removeSiteMembershipByUser() throws Exception
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        Assert.assertTrue(userService.removeSiteMembership(userName, password, userName, siteId));
    }
    
    @Test
    public void removeSiteMembershipNonMemberUser() throws Exception
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String nonMemberUser = "nonMember-" + System.currentTimeMillis();
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.create(admin, admin, nonMemberUser, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        Assert.assertFalse(userService.removeSiteMembership(nonMemberUser, password, userName, siteId));
    }
    
    @Test
    public void removeSiteMembershiNoExistentUser() throws Exception
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
        Assert.assertFalse(userService.removeSiteMembership(admin, admin, "fakeUser", siteId));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeSiteMembFakeSiteManager() throws Exception
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        userService.removeSiteMembership("fakeSiteManager", "fakePass", userName, siteId);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeSiteMembershipNoExistentSite() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.removeSiteMembership(admin, admin, userName, "fakeSite");
    }
    
    
    
    @Test
    public void countSiteMembers() throws Exception
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
    public void countSiteMembersWithNoInvitee() throws Exception
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
    public void addDashlets() throws Exception
    {
        String theUser = "alfrescouser" + System.currentTimeMillis();
        userService.create(admin, admin, theUser, password, theUser, firstName, lastName);
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.MY_MEETING_WORKSPACES, DashletLayout.THREE_COLUMNS, 3, 1));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.MY_DISCUSSIONS, DashletLayout.THREE_COLUMNS, 3, 2));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.WEB_VIEW, DashletLayout.FOUR_COLUMNS, 4, 1));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.MY_DOC_WORKSPACES, DashletLayout.FOUR_COLUMNS, 4, 2));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.RSS_FEED, DashletLayout.FOUR_COLUMNS, 4, 3));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.SITE_SEARCH, DashletLayout.FOUR_COLUMNS, 4, 4));
    }
    
    @Test
    public void addDashletToInvalidUser() throws Exception
    {   
        Assert.assertFalse(userService.addDashlet("fakeUser", "fakePass", UserDashlet.WEB_VIEW, DashletLayout.THREE_COLUMNS, 3, 1));
    }
    
    @Test
    public void changeUserRole() throws Exception
    {
        String siteId = "siteInvite-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId,  "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.inviteUserToSiteAndAccept(admin, admin, userName, siteId, "SiteConsumer");
        Assert.assertTrue(userService.changeUserRole(admin, admin, siteId, userName, "SiteCollaborator"));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidMember() throws Exception
    {
        String siteId = "siteInvite-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.changeUserRole(admin, admin, siteId, userName, "SiteCollaborator");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidSite() throws Exception
    {
        String siteId = "siteInvite-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(admin, admin, userName, siteId, "SiteConsumer"));
        userService.changeUserRole(admin, admin, "fakeSite", userName, "SiteCollaborator");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidRole() throws Exception
    {
        String siteId = "siteInvite-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(admin, admin, userName, siteId, "SiteConsumer"));
        userService.changeUserRole(admin, admin, siteId, userName, "Role");
    }
    
    @Test
    public void addMemberToModeratedSite() throws Exception
    {
        String userManager = "siteManager" + System.currentTimeMillis();
        String userToAdd = "member" + System.currentTimeMillis();
        String siteId = "site" + System.currentTimeMillis();
        userService.create(admin, admin, userManager, password, userManager, firstName, lastName);
        userService.create(admin, admin, userToAdd, password, userToAdd, firstName, lastName);
        site.create(userManager, password, "mydomain", siteId, siteId, Visibility.MODERATED);
        Assert.assertTrue(userService.createSiteMember(userManager, password, userToAdd, siteId, "SiteContributor"));
    }
    
    @Test
    public void addMemberToPrivateSite() throws Exception
    { 
        String userManager = "siteManager" + System.currentTimeMillis();
        String userToAdd = "member" + System.currentTimeMillis();
        String siteId = "site" + System.currentTimeMillis();
        userService.create(admin, admin, userManager, password, userManager, firstName, lastName);
        userService.create(admin, admin, userToAdd, password, userToAdd, firstName, lastName);
        site.create(userManager, password, "mydomain", siteId, siteId, Visibility.PRIVATE);
        Assert.assertTrue(userService.createSiteMember(userManager, password, userToAdd, siteId, "SiteContributor"));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addMemberToInvalidSite() throws Exception
    { 
        String userManager = "siteManager" + System.currentTimeMillis();
        String userToAdd = "member" + System.currentTimeMillis();
        userService.create(admin, admin, userManager, password, userManager, firstName, lastName);
        userService.create(admin, admin, userToAdd, password, userToAdd, firstName, lastName);
        userService.createSiteMember(userManager, password, userToAdd, "fakeSite", "SiteContributor");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addMemberInvalidRole() throws Exception
    { 
        String userManager = "siteManager" + System.currentTimeMillis();
        String userToAdd = "member" + System.currentTimeMillis();
        String siteId = "site" + System.currentTimeMillis();
        userService.create(admin, admin, userManager, password, userManager, firstName, lastName);
        userService.create(admin, admin, userToAdd, password, userToAdd, firstName, lastName);
        site.create(userManager, password, "mydomain", siteId, siteId, Visibility.PRIVATE);
        userService.createSiteMember(userManager, password, userToAdd, siteId, "fakeRole");
    }

    @Test
    public void addMemberToSiteTwice() throws Exception
    {
        String userManager = "siteManager" + System.currentTimeMillis();
        String userToAdd = "member" + System.currentTimeMillis();
        String siteId = "site" + System.currentTimeMillis();
        userService.create(admin, admin, userManager, password, userManager, firstName, lastName);
        userService.create(admin, admin, userToAdd, password, userToAdd, firstName, lastName);
        site.create(userManager, password, "mydomain", siteId, siteId, Visibility.PRIVATE);
        Assert.assertTrue(userService.createSiteMember(userManager, password, userToAdd, siteId, "SiteContributor"));
        Assert.assertFalse(userService.createSiteMember(userManager, password, userToAdd, siteId, "SiteContributor"));
    }
    
    @Test
    public void followUser() throws Exception
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
    public void followInvalidUser() throws Exception
    {
        Assert.assertFalse(userService.followUser(ADMIN, ADMIN, "fakeUser"));
    }
    
    @Test
    public void getFollowers() throws Exception
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
    public void unfollowUser() throws Exception
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
        Assert.assertTrue(following.size()== 3);
        Assert.assertTrue(userService.unfollowUser(user, password, userToFollow1));
        Assert.assertTrue(userService.unfollowUser(user, password, userToFollow2));
        following = userService.getFollowingUsers(user, password);
        Assert.assertTrue(following.size()== 1);
    }
}
