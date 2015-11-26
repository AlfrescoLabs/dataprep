package org.alfresco.test.util;

import org.alfresco.dataprep.GroupService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
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
    String userName = "userm-" + System.currentTimeMillis();
    String firstName = "fname-" + System.currentTimeMillis();
    String lastName = "lname-" + System.currentTimeMillis();
    String password = "password";
    String email = userName;
    String admin = "admin";

    @Test
    public void createGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.groupExists(admin, admin, groupName));
    }

    @Test
    public void createSameGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertFalse(groupService.createGroup(admin, admin, groupName));
    }

    @Test
    public void createGroupNonAdminUser() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertFalse(groupService.createGroup(userName, password, groupName));
        Assert.assertFalse(groupService.groupExists(userName, password, groupName));
    }

    @Test
    public void addUserToGroup() throws Exception
    {
        String groupName = "Group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String userName2 = "userm2-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.create(admin, admin, userName2, password, userName, firstName, lastName);
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, groupName, userName));
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, groupName, userName2));
        Assert.assertEquals(groupService.countAuthoritiesFromGroup(admin, admin, groupName), 2);
        Assert.assertTrue(groupService.isUserAddedToGroup(ADMIN, ADMIN, groupName, userName));
        Assert.assertTrue(groupService.isUserAddedToGroup(ADMIN, ADMIN, groupName, userName2));
    }

    @Test
    public void addUserToSystemGroup() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, "ALFRESCO_SEARCH_ADMINISTRATORS", userName));
    }

    @Test
    public void addNonExistentUserToGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertFalse(groupService.addUserToGroup(admin, admin, groupName, "fakeUser"));
    }

    @Test
    public void addUserToNonExistenGroup() throws Exception
    {
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertFalse(groupService.addUserToGroup(admin, admin, "fakeGroup", userName));
    }

    @Test
    public void addUserToGroupTwice() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, groupName, userName));
        Assert.assertFalse(groupService.addUserToGroup(admin, admin, groupName, userName));
    }

    @Test
    public void addNonExistentSubGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.addSubGroup(admin, admin, groupName, subGroup));
        Assert.assertTrue(groupService.groupExists(admin, admin, subGroup));
    }

    @Test
    public void addExistentSubGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.createGroup(admin, admin, subGroup));
        Assert.assertTrue(groupService.addSubGroup(admin, admin, groupName, subGroup));
        Assert.assertTrue(groupService.groupExists(admin, admin, subGroup));
        Assert.assertEquals(groupService.countAuthoritiesFromGroup(admin, admin, groupName), 1);
    }

    @Test
    public void addSubGroupToNonExistentGroup() throws Exception
    {
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertFalse(groupService.addSubGroup(admin, admin, "fakeGroup", subGroup));
    }

    @Test
    public void removeUserFromGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, groupName, userName));
        Assert.assertTrue(groupService.removeUserFromGroup(admin, admin, groupName, userName));
        Assert.assertEquals(groupService.countAuthoritiesFromGroup(admin, admin, groupName), 0);
    }

    @Test
    public void removeNonExistentUserFromGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertFalse(groupService.removeUserFromGroup(admin, admin, groupName, "fakeUser"));
    }

    @Test
    public void removeSubgroupFromGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String subGroup = "subgroup" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.addSubGroup(admin, admin, groupName, subGroup));
        Assert.assertTrue(groupService.removeSubgroupFromGroup(admin, admin, groupName, subGroup));
    }

    @Test
    public void removeGroup() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.removeGroup(admin, admin, groupName));
        Assert.assertFalse(groupService.groupExists(admin, admin, groupName));
    }

    @Test
    public void removeGroupNonAdminUser() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertFalse(groupService.removeGroup(userName, password, groupName));
    }

    @Test
    public void inviteGroupToSite() throws Exception
    {
        String siteId = "siteinvite-" + System.currentTimeMillis();
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String userName2 = "userm2-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.create(admin, admin, userName2, password, userName, firstName, lastName);
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, groupName, userName));
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, groupName, userName2));
        Assert.assertTrue(groupService.inviteGroupToSite(admin, admin, siteId, groupName, "SiteContributor"));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void inviteGroupInvalidSite() throws Exception
    {
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, groupName, userName));
        groupService.inviteGroupToSite(admin, admin, "fakeSite", groupName, "SiteContributor");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void inviteGroupInvalidManager() throws Exception
    {
        String siteId = "siteinvite-" + System.currentTimeMillis();
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, groupName, userName));
        groupService.inviteGroupToSite("fakeManager", "fakePass", siteId, groupName, "SiteContributor");
    }

    @Test
    public void changeGroupRole() throws Exception
    {
        String siteId = "siteinvite-" + System.currentTimeMillis();
        String groupName = "group" + System.currentTimeMillis();
        String userName = "userm-" + System.currentTimeMillis();
        String userName2 = "userm2-" + System.currentTimeMillis();
        site.create(admin, admin, "myDomain", siteId, "my site description", Visibility.PUBLIC);
        userService.create(admin, admin, userName, password, userName, firstName, lastName);
        userService.create(admin, admin, userName2, password, userName, firstName, lastName);
        Assert.assertTrue(groupService.createGroup(admin, admin, groupName));
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, groupName, userName));
        Assert.assertTrue(groupService.addUserToGroup(admin, admin, groupName, userName2));
        Assert.assertTrue(groupService.inviteGroupToSite(admin, admin, siteId, groupName, "SiteConsumer"));
        Assert.assertTrue(groupService.changeGroupRole(admin, admin, siteId, groupName, "SiteCollaborator"));
    }
}
