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

import java.io.IOException;

import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.springframework.social.alfresco.connect.exception.AlfrescoException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test the AlfrescoHttpClient helper class.
 * @author Michael Suzuki
 *
 */
public class SitetTest extends AbstractTest
{
    private SiteService site;
    private String siteId;
    
    @BeforeClass
    public void setup()
    {
        site = (SiteService) ctx.getBean("siteService");
        siteId = "michael" + System.currentTimeMillis();
    }
    @Test
    public void create() throws IOException
    {
        site.create("admin",
                    "admin",
                    "mydomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
    }
    @Test(dependsOnMethods="create")
    public void exists() throws Exception
    {
        boolean exists = site.exists(siteId, "admin", "admin");
        Assert.assertTrue(exists);
    }
    @Test
    public void fakeSiteDoesNotExists() throws Exception
    {
        boolean exists = site.exists("bs-site","admin", "admin");
        Assert.assertFalse(exists);
    }
    @Test(expectedExceptions=AlfrescoException.class)
    public void createSiteWithInvalidDetails() throws IOException
    {
        String siteId = "michael" + System.currentTimeMillis();
        site.create("admin",
                    "fake",
                    "mydomain",
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
    }
}
