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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.Alfresco;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
/**
 * Test and validates connections via public api.
 * @author Michael suzuki
 *
 */
public class BasicAuthPublicApiFactoryTest extends AbstractTest
{
    @Autowired
    PublicApiFactory factory;
    @BeforeClass 
    public void setupFactory()
    {
        Assert.assertNotNull(factory);
    }
    @Test
    public void getPublicApi()
    {
        Alfresco alf = factory.getAdminPublicApi();
        Assert.assertNotNull(alf);
    }
    @Test
    public void getPublicApiByUser()
    {
        Alfresco alf = factory.getPublicApi("admin", "admin");
        Assert.assertNotNull(alf);
    }
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getPublicApiByInvalidUser()
    {
        Alfresco alf = factory.getPublicApi("joe123", "");
        Assert.assertNotNull(alf);
    }
    @Test
    public void getTenantAdminPublicApi()
    {
        Alfresco alf = factory.getTenantAdminPublicApi("default");
        Assert.assertNotNull(alf);
    }
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getNullTenantAdminPublicApi()
    {
        Alfresco alf = factory.getTenantAdminPublicApi(null);
        Assert.assertNotNull(alf);
    }
}
