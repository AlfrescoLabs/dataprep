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
import java.util.ArrayList;
import java.util.List;

import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.DashboardCustomization.DashletLayout;
import org.alfresco.dataprep.DashboardCustomization.Page;
import org.alfresco.dataprep.DashboardCustomization.SiteDashlet;
import org.alfresco.dataprep.SiteService.RMSiteCompliance;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SiteTest extends AbstractTest 
{
    private static final String MY_DOMAIN = "mydomain";
    private static final String ADMIN = "admin"; 
    @Autowired
    private SiteService site;
    private String siteId;
    
    @BeforeClass
    public void setup()
    {
        siteId = "michael" + System.currentTimeMillis();
    }
    
    @Test
    public void create() throws Exception
    {
        site.create(ADMIN,
                    ADMIN,
                    MY_DOMAIN,
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
    }
    
    @Test(dependsOnMethods="create")
    public void exists() throws Exception
    {
        boolean exists = site.exists(siteId, ADMIN, ADMIN);
        Assert.assertTrue(exists);
    } 
    /**
     * Test to create a site with shortname and title 
     * @throws Exception
     */
    @Test
    public void createWithSiteURLName() throws Exception
    {
        siteId = "michael" + System.currentTimeMillis();
        site.create(ADMIN,
                    ADMIN,
                    MY_DOMAIN,
                    siteId, 
                    siteId + "_test",
                    "my site description", 
                    Visibility.PUBLIC);
    }
    
    @Test(dependsOnMethods="createWithSiteURLName")
    public void createWithSiteURLNameExists() throws Exception
    {
        boolean exists = site.exists(siteId, ADMIN, ADMIN);
        Assert.assertTrue(exists);
    } 
    
    @Test
    public void fakeSiteDoesNotExists() throws Exception
    {
        boolean exists = site.exists("bs-site",ADMIN, ADMIN);
        Assert.assertFalse(exists);
    }
    
    @Test(expectedExceptions=AlfrescoException.class)
    public void createSiteWithInvalidDetails() throws IOException
    {
        String siteId = "michael" + System.currentTimeMillis();
        site.create(ADMIN,
                    "fake",
                    MY_DOMAIN,
                    siteId, 
                    "my site description", 
                    Visibility.PUBLIC);
    }
    
    @Test(dependsOnMethods="exists")
    public void getAllSites() throws Exception
    {
        List<String> sites= site.getSites(ADMIN, ADMIN);
        Assert.assertNotEquals(sites.size(),0);
    }
    
    @Test(dependsOnMethods="getAllSites")
    public void setFavorite() throws Exception
    {
        Assert.assertTrue(site.setFavorite(ADMIN, ADMIN, siteId));
        Assert.assertTrue(site.isFavorite(ADMIN, ADMIN, siteId));
    }
    
    @Test(dependsOnMethods="setFavorite")
    public void removeFavorite() throws Exception
    {
        Assert.assertTrue(site.removeFavorite(ADMIN, ADMIN, siteId));
        Assert.assertFalse(site.isFavorite(ADMIN, ADMIN, siteId));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void favoriteFakeSite() throws Exception
    {
        site.setFavorite(ADMIN, ADMIN, "fakeSite");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeFavFakeSite() throws Exception
    {
        site.removeFavorite(ADMIN, ADMIN, "fakeSite");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void favSiteInvalidUser() throws Exception
    {
        site.setFavorite("fakeUser", ADMIN, siteId);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeFavInvalidUser() throws Exception
    {
        site.removeFavorite("fakeUser", ADMIN, siteId);
    }
    
    @Test(dependsOnMethods="removeFavorite")
    public void addPagesToSite() throws Exception
    {
        List<Page> pagesToAdd = new ArrayList<Page>();
        pagesToAdd.add(Page.WIKI);
        pagesToAdd.add(Page.LINKS);
        pagesToAdd.add(Page.CALENDAR);
        Assert.assertTrue(site.addPagesToSite(ADMIN, ADMIN, siteId, pagesToAdd));
        Assert.assertTrue(site.addPageToSite(ADMIN, ADMIN, siteId, Page.BLOG, pagesToAdd));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addPageToInvalidSite() throws Exception
    {   
        site.addPageToSite(ADMIN, ADMIN, "fakeSite", Page.BLOG, null);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addPageToInvalidUser() throws Exception
    {   
        site.addPageToSite("fakeUser", "fakePass", siteId, Page.BLOG, null);
    }
    
    @Test(dependsOnMethods="addPagesToSite")
    public void addDashlets() throws Exception
    {   
        Assert.assertTrue(site.addDashlet(ADMIN, ADMIN, siteId, SiteDashlet.WEB_VIEW, DashletLayout.THREE_COLUMNS, 3, 1));
        Assert.assertTrue(site.addDashlet(ADMIN, ADMIN, siteId, SiteDashlet.SITE_CALENDAR, DashletLayout.THREE_COLUMNS, 3, 2));
        Assert.assertTrue(site.addDashlet(ADMIN, ADMIN, siteId, SiteDashlet.SAVED_SEARCH, DashletLayout.THREE_COLUMNS, 2, 3));
        Assert.assertTrue(site.addDashlet(ADMIN, ADMIN, siteId, SiteDashlet.RSS_FEED, DashletLayout.TWO_COLUMNS_WIDE_LEFT, 2, 4));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addDashletInvalidUser() throws Exception
    {   
        site.addDashlet("fakeUser", "fakePass", siteId, SiteDashlet.WEB_VIEW, DashletLayout.THREE_COLUMNS, 3, 1);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addDashletToInvalidSite() throws Exception
    {   
        site.addDashlet(ADMIN, ADMIN, "fakeSite", SiteDashlet.ADDONS_RSS_FEED, DashletLayout.FOUR_COLUMNS, 2, 1);
    }
    
    @Test(dependsOnMethods="addDashlets")
    public void delete() throws Exception
    {
        site.delete(ADMIN, ADMIN, MY_DOMAIN, siteId);
        boolean exists = site.exists(siteId, ADMIN, ADMIN);
        Assert.assertFalse(exists);
    }
    
    //@Test
    public void createRMSite() throws Exception
    {
        Assert.assertTrue(site.createRMSite(ADMIN, ADMIN, "FirstRMSite", "RM Site Description", RMSiteCompliance.STANDARD));
        site.delete(ADMIN, ADMIN, "domain", "rm");
        Assert.assertTrue(site.createRMSite(ADMIN, ADMIN, "FirstRMSite", "RM Site Description", RMSiteCompliance.DOD_5015_2_STD));
    }
    
    //@Test(expectedExceptions = RuntimeException.class, dependsOnMethods="createRMSite")
    public void addRMSiteTwice() throws Exception
    {   
        site.createRMSite(ADMIN, ADMIN, "FirstRMSite", "description", RMSiteCompliance.STANDARD);
    }
    
    //@Test(expectedExceptions = RuntimeException.class)
    public void createRMSiteFakeCredentials() throws Exception
    {
        Assert.assertTrue(site.createRMSite(ADMIN, "fakepass", "FirstRMSite", "", RMSiteCompliance.STANDARD));
    }
}
