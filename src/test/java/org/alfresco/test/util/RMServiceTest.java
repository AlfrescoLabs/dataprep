/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.test.util;

import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.SiteService.RMSiteCompliance;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test RM based APIs.
 * @author Michael Suzuki
 *
 */
public class RMServiceTest extends AbstractTest
{
    @Autowired private SiteService site;
    @SuppressWarnings("unused") private String siteId;
    
    @BeforeClass
    public void setup()
    {
        siteId = "michael" + System.currentTimeMillis();
    }
    @Test
    public void createRMSite() throws Exception
    {
        Assert.assertTrue(site.createRMSite(ADMIN, ADMIN, "FirstRMSite", "RM Site Description", RMSiteCompliance.STANDARD));
        site.delete(ADMIN, ADMIN, "domain", "rm");
        Assert.assertTrue(site.createRMSite(ADMIN, ADMIN, "FirstRMSite", "RM Site Description", RMSiteCompliance.DOD_5015_2_STD));
    }
    
    @Test(expectedExceptions = RuntimeException.class, dependsOnMethods="createRMSite")
    public void addRMSiteTwice() throws Exception
    {   
        site.createRMSite(ADMIN, ADMIN, "FirstRMSite", "description", RMSiteCompliance.STANDARD);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createRMSiteFakeCredentials() throws Exception
    {
        Assert.assertTrue(site.createRMSite(ADMIN, "fakepass", "FirstRMSite", "", RMSiteCompliance.STANDARD));
    }
}
