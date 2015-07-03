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

import org.alfresco.test.util.DashboardCustomization.DashletLayout;
import org.alfresco.test.util.DashboardCustomization.UserDashlet;
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
    String userName = "userm-" + System.currentTimeMillis() + "@test.com";
    String password = "password";
    String email = userName;
    String admin = "admin";


    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserInvalidUserName() throws Exception
    {
        userService.create(admin, admin, null, password, email);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserInvalidPassword() throws Exception
    {
        userService.create(admin, admin, userName, null, email);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserNullAdmin() throws Exception
    {
        userService.create(null, admin, userName, password, email);
    }

    @Test
    public void creatEnterpriseUser() throws Exception
    {
        boolean result = userService.create(admin, admin, userName, password, email);
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
        userService.create(admin, admin, userName, password, email);
        boolean result = userService.create(admin, admin, userName, password, email);
        Assert.assertFalse(result);
    }

    @Test
    public void deleteUser() throws Exception
    {
        String userName = "deleteUser";
        userService.create(admin, admin, userName, password, email);
        Assert.assertTrue(userService.delete(admin, admin, userName));
        Assert.assertFalse(userService.userExists(admin, admin, userName));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void deleteNonExistent() throws Exception
    {
        String userName = "booo";
        userService.delete(admin, admin, userName);
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
        userService.create(admin, admin, userManager, password, emailUserManager);
        userService.create(admin, admin, userToInvite, password, emailUserInvited);
        site.create(userManager, password, "mydomain", siteId, siteId, Visibility.PUBLIC);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(userManager, password, userToInvite, siteId, "SiteConsumer"));
    }
    
    @Test
    public void inviteUserToNonExistentSite() throws Exception
    {
        String userManager = "userSiteManager" + System.currentTimeMillis();
        String userToInvite = "inviteUser" + System.currentTimeMillis();
        String emailUserManager = userManager + "@test.com";
        userService.create(admin, admin, userManager, password, emailUserManager);
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
        userService.create(admin, admin, userName, password, userName);
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
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        Assert.assertFalse(userService.requestSiteMembership(userName, password, siteId));
    }
    
    @Test
    public void requestSiteMembershipNoExistentSite() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName);
        Assert.assertFalse(userService.requestSiteMembership(userName, password, "fakeSite"));
    }
    
    @Test
    public void requestSiteMembershipNoExistentUser() throws Exception
    {
        String siteId = "siteMembership-" + System.currentTimeMillis();
        site.create(admin,
                    admin,
                    "myDomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
        Assert.assertFalse(userService.requestSiteMembership("fakeUser", password, "fakeSite"));
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
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        Assert.assertTrue(userService.removePendingSiteRequest(admin, admin, userName, siteId));
    }
    
    @Test
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
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.requestSiteMembership(userName, password, siteId));
        Assert.assertFalse(userService.removePendingSiteRequest(admin, admin, userName, siteId));
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
        userService.create(admin, admin, userName, password, userName);
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
        userService.create(admin, admin, userName, password, userName);
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
        userService.create(admin, admin, userName, password, userName);
        userService.create(admin, admin, nonMemberUser, password, userName);
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
    
    @Test
    public void removeSiteMembershipNoExistentSite() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName);
        Assert.assertFalse(userService.removeSiteMembership(admin, admin, userName, "fakeSite"));
    }
    
    @Test
    public void createGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.groupExists(admin, admin, groupName));
    }
    
    @Test
    public void createSameGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertFalse(userService.createGroup(admin, admin, groupName));
    }
    
    @Test
    public void createGroupNonAdminUser() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName);
        Assert.assertFalse(userService.createGroup(userName, password, groupName));
        Assert.assertFalse(userService.groupExists(userName, password, groupName));
    }
    
    @Test
    public void addUserToGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String userName2 = "userm2-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName);
        userService.create(admin, admin, userName2, password, userName);
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.addUserToGroup(admin, admin, groupName, userName));
        Assert.assertTrue(userService.addUserToGroup(admin, admin, groupName, userName2));
        Assert.assertEquals(userService.countAuthoritiesFromGroup(admin, admin, groupName), 2);
    }
    
    @Test
    public void addNonExistentUserToGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertFalse(userService.addUserToGroup(admin, admin, groupName, "fakeUser"));
    }
    
    @Test
    public void addUserToNonExistenGroup() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName);
        Assert.assertFalse(userService.addUserToGroup(admin, admin, "fakeGroup", userName));
    }
    
    @Test
    public void addUserToGroupTwice() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.addUserToGroup(admin, admin, groupName, userName));
        Assert.assertFalse(userService.addUserToGroup(admin, admin, groupName, userName));
    }
    
    @Test
    public void addNonExistentSubGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.addSubGroup(admin, admin, groupName, subGroup));
        Assert.assertTrue(userService.groupExists(admin, admin, subGroup));     
    }
    
    @Test
    public void addExistentSubGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.createGroup(admin, admin, subGroup));
        Assert.assertTrue(userService.addSubGroup(admin, admin, groupName, subGroup));
        Assert.assertTrue(userService.groupExists(admin, admin, subGroup));
        Assert.assertEquals(userService.countAuthoritiesFromGroup(admin, admin, groupName), 1);
    }
    
    @Test
    public void addSubGroupToNonExistentGroup() throws Exception
    {
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertFalse(userService.addSubGroup(admin, admin, "fakeGroup", subGroup));  
    }
    
    @Test
    public void removeUserFromGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.addUserToGroup(admin, admin, groupName, userName));
        Assert.assertTrue(userService.removeUserFromGroup(admin, admin, groupName, userName));
        Assert.assertEquals(userService.countAuthoritiesFromGroup(admin, admin, groupName), 0);
    }
    
    @Test
    public void removeNonExistentUserFromGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertFalse(userService.removeUserFromGroup(admin, admin, groupName, "fakeUser"));
    }
    
    @Test
    public void removeSubgroupFromGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.addSubGroup(admin, admin, groupName, subGroup));
        Assert.assertTrue(userService.removeSubgroupFromGroup(admin, admin, groupName, subGroup));
    }
    
    @Test
    public void removeGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.removeGroup(admin, admin, groupName));
        Assert.assertFalse(userService.groupExists(admin, admin, groupName));
    }
    
    @Test
    public void removeGroupNonAdminUser() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertFalse(userService.removeGroup(userName, password, groupName));
    }
    
    @Test
    public void countSiteMembers() throws Exception
    {
        String userManager = "user1-" + System.currentTimeMillis();
        String userToInvite = "user2-" + System.currentTimeMillis();
        String siteName = "site-" + System.currentTimeMillis();
        String emailUserManager = userManager + "@test.com";
        String emailUserToInvite = userToInvite + "@test.com";
        userService.create(admin, admin, userManager, password, emailUserManager);
        userService.create(admin, admin, userToInvite, password, emailUserToInvite);
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
        userService.create(admin, admin, userName, password, userEmail);
        site.create(userName, password, "mydomain", siteName, siteName, Visibility.PUBLIC);
        int noOfSiteMembers=userService.countSiteMembers(userName, password, siteName);
        Assert.assertEquals(noOfSiteMembers,1);
    }
   
    @Test
    public void addDashlets() throws Exception
    {
        String theUser = "alfrescouser" + System.currentTimeMillis();     
        userService.create(admin, admin, theUser, password, theUser);
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.MY_MEETING_WORKSPACES, DashletLayout.THREE_COLUMNS, 3, 1));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.MY_DISCUSSIONS, DashletLayout.THREE_COLUMNS, 3, 2));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.WEB_VIEW, DashletLayout.FOUR_COLUMNS, 4, 2));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.MY_DOC_WORKSPACES, DashletLayout.FOUR_COLUMNS, 4, 3));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.RSS_FEED, DashletLayout.FOUR_COLUMNS, 4, 4));
        Assert.assertTrue(userService.addDashlet(theUser, password, UserDashlet.SITE_SEARCH, DashletLayout.FOUR_COLUMNS, 4, 5));
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
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(admin, admin, userName, siteId, "SiteConsumer"));
        Assert.assertTrue(userService.changeUserRole(admin, admin, siteId, userName, "SiteCollaborator"));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidMember() throws Exception
    {
        String siteId = "siteInvite-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName);
        userService.changeUserRole(admin, admin, siteId, userName, "SiteCollaborator");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidSite() throws Exception
    {
        String siteId = "siteInvite-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(admin, admin, userName, siteId, "SiteConsumer"));
        userService.changeUserRole(admin, admin, "fakeSite", userName, "SiteCollaborator");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void changeRoleInvalidRole() throws Exception
    {
        String siteId = "siteInvite-" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.inviteUserToSiteAndAccept(admin, admin, userName, siteId, "SiteConsumer"));
        userService.changeUserRole(admin, admin, siteId, userName, "Role");
    }
    
    @Test
    public void inviteGroupToSite() throws Exception
    {
        String siteId = "siteinvite-" + System.currentTimeMillis();
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String userName2 = "userm2-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName);
        userService.create(admin, admin, userName2, password, userName);
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.addUserToGroup(admin, admin, groupName, userName));
        Assert.assertTrue(userService.addUserToGroup(admin, admin, groupName, userName2));
        Assert.assertTrue(userService.inviteGroupToSite(admin, admin, siteId, groupName, "SiteContributor"));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void inviteGroupInvalidSite() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.addUserToGroup(admin, admin, groupName, userName));
        userService.inviteGroupToSite(admin, admin, "fakeSite", groupName, "SiteContributor");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void inviteGroupInvalidManager() throws Exception
    {
        String siteId = "siteinvite-" + System.currentTimeMillis();
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName);
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.addUserToGroup(admin, admin, groupName, userName));
        userService.inviteGroupToSite("fakeManager", "fakePass", siteId, groupName, "SiteContributor");
    }
    
    @Test
    public void changeGroupRole() throws Exception
    {
        String siteId = "siteinvite-" + System.currentTimeMillis();
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String userName2 = "userm2-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName);
        userService.create(admin, admin, userName2, password, userName);
        Assert.assertTrue(userService.createGroup(admin, admin, groupName));
        Assert.assertTrue(userService.addUserToGroup(admin, admin, groupName, userName));
        Assert.assertTrue(userService.addUserToGroup(admin, admin, groupName, userName2));
        Assert.assertTrue(userService.inviteGroupToSite(admin, admin, siteId, groupName, "SiteConsumer"));
        Assert.assertTrue(userService.changeGroupRole(admin, admin, siteId, groupName, "SiteCollaborator"));
    }
    
    @Test
    public void addMemberToModeratedSite() throws Exception
    {
        String userManager = "siteManager" + System.currentTimeMillis();
        String userToAdd = "member" + System.currentTimeMillis();
        String siteId = "site" + System.currentTimeMillis();
        userService.create(admin, admin, userManager, password, userManager);
        userService.create(admin, admin, userToAdd, password, userToAdd);
        site.create(userManager, password, "mydomain", siteId, siteId, Visibility.MODERATED);
        Assert.assertTrue(userService.createSiteMember(userManager, password, userToAdd, siteId, "SiteContributor"));
    }
    
    @Test
    public void addMemberToPrivateSite() throws Exception
    { 
        String userManager = "siteManager" + System.currentTimeMillis();
        String userToAdd = "member" + System.currentTimeMillis();
        String siteId = "site" + System.currentTimeMillis();
        userService.create(admin, admin, userManager, password, userManager);
        userService.create(admin, admin, userToAdd, password, userToAdd);
        site.create(userManager, password, "mydomain", siteId, siteId, Visibility.PRIVATE);
        Assert.assertTrue(userService.createSiteMember(userManager, password, userToAdd, siteId, "SiteContributor"));
    }
}
