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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
/**
 * Test user data load api crud operations.
 * @author Michael Suzuki
 *
 */
public class UserTest extends AbstractTest
{
    UserService userService;
    String userName = "userm-" + System.currentTimeMillis() + "@test.com";
    String password = "password";
    String email = userName;
    String admin = "admin";
    @BeforeClass
    public void setup()
    {
        userService = (UserService) ctx.getBean("userService");
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

}
