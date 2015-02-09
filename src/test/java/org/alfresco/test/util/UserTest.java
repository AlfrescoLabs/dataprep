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

import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test user data load api crud operations.
 * 
 * @author Michael Suzuki
 */
public class UserTest extends AbstractTest
{
    UserService userService;
    String userName = "userm-" + System.currentTimeMillis() + "@test.com";
    String password = "password";
    String email = userName;
    String admin = "admin";
    SiteService site;

    @BeforeClass
    public void setup()
    {
        userService = (UserService) ctx.getBean("userService");
        site = (SiteService) ctx.getBean("siteService");
    }

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
  
}
