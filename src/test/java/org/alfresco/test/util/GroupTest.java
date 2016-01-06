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

import org.alfresco.dataprep.GroupService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Group data load api crud operations.
 * 
 * @author Bogdan Bocancea
 */
public class GroupTest extends AbstractTest
{
    @Autowired private SiteService site;
    @Autowired private UserService userService;
    @Autowired private GroupService groupService;
    String firstName = "fname-" + System.currentTimeMillis();
    String lastName = "lname-" + System.currentTimeMillis();
    String password = "password";
    private String theGroup = "group" + System.currentTimeMillis();
    private String userGroup1 = "userGrp-1" + System.currentTimeMillis();
    private String userGroup2 = "userGrp-2" + System.currentTimeMillis();

    @BeforeClass(alwaysRun = true)
    public void userSetup()
    {
        userService.create(ADMIN, ADMIN, userGroup1, password, userGroup1 + domain, firstName, lastName);
        userService.create(ADMIN, ADMIN, userGroup2, password, userGroup2 + domain, firstName, lastName);
    }
    
    @Test
    public void createGroup()
    {
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, theGroup));
        Assert.assertTrue(groupService.groupExists(ADMIN, ADMIN, theGroup));
    }

    @Test(dependsOnMethods="createGroup")
    public void createSameGroup()
    {
        Assert.assertFalse(groupService.createGroup(ADMIN, ADMIN, theGroup));
    }

    @Test
    public void createGroupNonADMINUser()
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertFalse(groupService.createGroup(userGroup1, password, groupName));
        Assert.assertFalse(groupService.groupExists(userGroup1, password, groupName));
    }

    @Test
    public void addUserToGroup()
    {
        String groupName = "Group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup1));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup2));
        Assert.assertEquals(groupService.countAuthoritiesFromGroup(ADMIN, ADMIN, groupName), 2);
        Assert.assertTrue(groupService.isUserAddedToGroup(ADMIN, ADMIN, groupName, userGroup1));
        Assert.assertTrue(groupService.isUserAddedToGroup(ADMIN, ADMIN, groupName, userGroup2));
    }

    @Test
    public void addUserToSystemGroup()
    {
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, "ALFRESCO_SEARCH_ADMINISTRATORS", userGroup1));
    }

    @Test(dependsOnMethods="createGroup")
    public void addNonExistentUserToGroup()
    {
        Assert.assertFalse(groupService.addUserToGroup(ADMIN, ADMIN, theGroup, "fakeUser"));
    }

    @Test
    public void addUserToNonExistenGroup()
    {
        Assert.assertFalse(groupService.addUserToGroup(ADMIN, ADMIN, "fakeGroup", userGroup1));
    }

    @Test
    public void addUserToGroupTwice()
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup2));
        Assert.assertFalse(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup2));
    }

    @Test
    public void addNonExistentSubGroup()
    {
        String groupName = "group" + System.currentTimeMillis();
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addSubGroup(ADMIN, ADMIN, groupName, subGroup));
        Assert.assertTrue(groupService.groupExists(ADMIN, ADMIN, subGroup));
    }

    @Test
    public void addExistentSubGroup()
    {
        String groupName = "group" + System.currentTimeMillis();
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, subGroup));
        Assert.assertTrue(groupService.addSubGroup(ADMIN, ADMIN, groupName, subGroup));
        Assert.assertTrue(groupService.groupExists(ADMIN, ADMIN, subGroup));
        Assert.assertEquals(groupService.countAuthoritiesFromGroup(ADMIN, ADMIN, groupName), 1);
    }

    @Test
    public void addSubGroupToNonExistentGroup()
    {
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertFalse(groupService.addSubGroup(ADMIN, ADMIN, "fakeGroup", subGroup));
    }

    @Test
    public void removeUserFromGroup()
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup1));
        Assert.assertTrue(groupService.removeUserFromGroup(ADMIN, ADMIN, groupName, userGroup1));
        Assert.assertEquals(groupService.countAuthoritiesFromGroup(ADMIN, ADMIN, groupName), 0);
    }

    @Test
    public void removeNonExistentUserFromGroup()
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertFalse(groupService.removeUserFromGroup(ADMIN, ADMIN, groupName, "fakeUser"));
    }

    @Test
    public void removeSubgroupFromGroup()
    {
        String groupName = "group" + System.currentTimeMillis();
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addSubGroup(ADMIN, ADMIN, groupName, subGroup));
        Assert.assertTrue(groupService.removeSubgroupFromGroup(ADMIN, ADMIN, groupName, subGroup));
    }

    @Test
    public void removeGroup()
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.removeGroup(ADMIN, ADMIN, groupName));
        Assert.assertFalse(groupService.groupExists(ADMIN, ADMIN, groupName));
    }

    @Test
    public void removeGroupNonADMINUser()
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userName, password, userName, firstName, lastName);
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertFalse(groupService.removeGroup(userName, password, groupName));
    }

    @Test
    public void inviteGroupToSite()
    {
        String siteId = "siteinvite-" + System.currentTimeMillis();
        String groupName = "group" + System.currentTimeMillis();
        site.create(ADMIN, ADMIN, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup1));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup2));
        Assert.assertTrue(groupService.inviteGroupToSite(ADMIN, ADMIN, siteId, groupName, "SiteContributor"));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void inviteGroupInvalidSite()
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup1));
        groupService.inviteGroupToSite(ADMIN, ADMIN, "fakeSite", groupName, "SiteContributor");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void inviteGroupInvalidManager()
    {
        String siteId = "siteinvite-" + System.currentTimeMillis();
        String groupName = "group" + System.currentTimeMillis();
        site.create(ADMIN, ADMIN, "myDomain", siteId, "my site description", Visibility.PUBLIC);;
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup1));
        groupService.inviteGroupToSite("fakeManager", "fakePass", siteId, groupName, "SiteContributor");
    }

    @Test
    public void changeGroupRole()
    {
        String siteId = "siteinvite-" + System.currentTimeMillis();
        String groupName = "group" + System.currentTimeMillis();
        site.create(ADMIN, ADMIN, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup1));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userGroup2));
        Assert.assertTrue(groupService.inviteGroupToSite(ADMIN, ADMIN, siteId, groupName, "SiteConsumer"));
        Assert.assertTrue(groupService.changeGroupRole(ADMIN, ADMIN, siteId, groupName, "SiteCollaborator"));
    }
}
