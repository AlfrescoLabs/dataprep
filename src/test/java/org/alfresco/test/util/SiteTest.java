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

import java.util.ArrayList;
import java.util.List;

import org.alfresco.dataprep.DashboardCustomization.DashletLayout;
import org.alfresco.dataprep.DashboardCustomization.Page;
import org.alfresco.dataprep.DashboardCustomization.SiteDashlet;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
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
    @Autowired private SiteService site;
    @Autowired private UserService user;
    private String siteId;
    private String secondSite;
    private String theUser = "siteUser" + System.currentTimeMillis();
    
    @BeforeClass
    public void setup()
    {
        siteId = "michael" + System.currentTimeMillis();
        secondSite = "second" + System.currentTimeMillis();
        user.create(ADMIN, ADMIN, theUser, password, theUser + domain, theUser, theUser);
    }
    
    @Test
    public void create()
    {
        site.create(ADMIN,
                    ADMIN,
                    MY_DOMAIN,
                    siteId, 
                    "my site description",
                    Visibility.PUBLIC);
    }
    
    @Test(dependsOnMethods="create")
    public void getSiteNodeRef()
    {
        Assert.assertFalse(site.getSiteNodeRef(ADMIN, ADMIN, siteId).isEmpty());
    }
    
    @Test(dependsOnMethods="create")
    public void exists()
    {
        boolean exists = site.exists(siteId, ADMIN, ADMIN);
        Assert.assertTrue(exists);
    }
    /**
     * Test to create a site with shortname and title 
     */
    @Test
    public void createWithSiteURLName()
    {
        site.create(ADMIN,
                    ADMIN,
                    MY_DOMAIN,
                    secondSite, 
                    secondSite,
                    "my site description",
                    Visibility.PUBLIC);
    }
    
    @Test(dependsOnMethods="createWithSiteURLName")
    public void createWithSiteURLNameExists()
    {
        boolean exists = site.exists(secondSite, ADMIN, ADMIN);
        Assert.assertTrue(exists);
    }
    
    @Test
    public void fakeSiteDoesNotExists()
    {
        boolean exists = site.exists("bs-site",ADMIN, ADMIN);
        Assert.assertFalse(exists);
    }
    
    @Test(expectedExceptions=AlfrescoException.class)
    public void createSiteWithInvalidDetails()
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
    public void getAllSites()
    {
        List<String> sites= site.getSites(ADMIN, ADMIN);
        Assert.assertNotEquals(sites.size(), 0);
        Assert.assertTrue(sites.contains(siteId));
    }
    
    @Test(dependsOnMethods="getAllSites")
    public void setFavorite()
    {
        Assert.assertTrue(site.setFavorite(ADMIN, ADMIN, siteId));
        Assert.assertTrue(site.isFavorite(ADMIN, ADMIN, siteId));
    }
    
    @Test(dependsOnMethods="setFavorite")
    public void removeFavorite()
    {
        Assert.assertTrue(site.removeFavorite(ADMIN, ADMIN, siteId));
        Assert.assertFalse(site.isFavorite(ADMIN, ADMIN, siteId));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void favoriteFakeSite()
    {
        site.setFavorite(ADMIN, ADMIN, "fakeSite");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeFavFakeSite()
    {
        site.removeFavorite(ADMIN, ADMIN, "fakeSite");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void favSiteInvalidUser()
    {
        site.setFavorite("fakeUser", ADMIN, siteId);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void removeFavInvalidUser()
    {
        site.removeFavorite("fakeUser", ADMIN, siteId);
    }
    
    @Test(dependsOnMethods="removeFavorite")
    public void addPagesToSite()
    {
        List<Page> pagesToAdd = new ArrayList<Page>();
        pagesToAdd.add(Page.WIKI);
        pagesToAdd.add(Page.LINKS);
        pagesToAdd.add(Page.CALENDAR);
        Assert.assertTrue(site.addPagesToSite(ADMIN, ADMIN, siteId, pagesToAdd));
        Assert.assertTrue(site.addPageToSite(ADMIN, ADMIN, siteId, Page.BLOG, pagesToAdd));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addPageToInvalidSite()
    {
        site.addPageToSite(ADMIN, ADMIN, "fakeSite", Page.BLOG, null);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addPageToInvalidUser()
    {   
        site.addPageToSite("fakeUser", "fakePass", siteId, Page.BLOG, null);
    }
    
    @Test(dependsOnMethods="addPagesToSite")
    public void addDashlets()
    {   
        Assert.assertTrue(site.addDashlet(ADMIN, ADMIN, siteId, SiteDashlet.WEB_VIEW, DashletLayout.THREE_COLUMNS, 3, 1));
        Assert.assertTrue(site.addDashlet(ADMIN, ADMIN, siteId, SiteDashlet.SITE_CALENDAR, DashletLayout.THREE_COLUMNS, 3, 2));
        Assert.assertTrue(site.addDashlet(ADMIN, ADMIN, siteId, SiteDashlet.SAVED_SEARCH, DashletLayout.THREE_COLUMNS, 2, 3));
        Assert.assertTrue(site.addDashlet(ADMIN, ADMIN, siteId, SiteDashlet.RSS_FEED, DashletLayout.TWO_COLUMNS_WIDE_LEFT, 2, 4));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addDashletInvalidUser()
    {   
        site.addDashlet("fakeUser", "fakePass", siteId, SiteDashlet.WEB_VIEW, DashletLayout.THREE_COLUMNS, 3, 1);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addDashletToInvalidSite()
    {
        site.addDashlet(ADMIN, ADMIN, "fakeSite", SiteDashlet.ADDONS_RSS_FEED, DashletLayout.FOUR_COLUMNS, 2, 1);
    }
    
    @Test(dependsOnMethods="addDashlets")
    public void delete()
    {
        List<String> sites= site.getSites(ADMIN, ADMIN);
        Assert.assertTrue(sites.contains(siteId));
        site.delete(ADMIN, ADMIN, MY_DOMAIN, siteId);
        boolean exists = site.exists(siteId, ADMIN, ADMIN);
        Assert.assertFalse(exists);
        sites= site.getSites(ADMIN, ADMIN);
        Assert.assertFalse(sites.contains(siteId));
    }
    
    @Test
    public void getFavSiteEmpty()
    {
        Assert.assertTrue(site.getFavoriteSites(theUser, password).isEmpty());
    }
    
    @Test(dependsOnMethods="getFavSiteEmpty")
    public void getFavSite()
    {
        String site1 = "site-1" + System.currentTimeMillis();
        String site2 = "site-2" + System.currentTimeMillis();
        site.create(theUser, password, MY_DOMAIN, site1, "site-1", Visibility.PUBLIC);
        site.create(theUser, password, MY_DOMAIN, site2, "site-2", Visibility.PUBLIC);
        Assert.assertTrue(site.setFavorite(theUser, password, site1));
        Assert.assertTrue(site.setFavorite(theUser, password, site2));
        List<String> favorites = site.getFavoriteSites(theUser, password);
        Assert.assertTrue(favorites.get(0).equals(site1));
        Assert.assertTrue(favorites.get(1).equals(site2));
    }
    
    @Test()
    public void setSiteAsIMAPFavorite()
    {
        String site1 = "IMAPsetsite-1" + System.currentTimeMillis();
        site.create(theUser, password, MY_DOMAIN, site1, site1 + " description", Visibility.PUBLIC);
        Assert.assertTrue(site.setIMAPFavorite(theUser, password, site1));
    }
    
    @Test()
    public void removeSiteFromIMAPFavorites()
    {
        String site1 = "IMAPremovesite-1" + System.currentTimeMillis();
        site.create(theUser, password, MY_DOMAIN, site1, site1 + " description", Visibility.PUBLIC);
        Assert.assertTrue(site.setIMAPFavorite(theUser, password, site1));
        Assert.assertTrue(site.removeIMAPFavorite(theUser, password, site1));
    }
}
