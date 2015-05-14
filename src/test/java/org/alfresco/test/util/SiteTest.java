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
import java.util.List;

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
    private SiteService site;
    private String siteId;
    
    @BeforeClass
    public void setup()
    {
        site = (SiteService) ctx.getBean("siteService");
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
        Assert.assertTrue(sites.contains(siteId));
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
    public void delete() throws Exception
    {
        site.delete(ADMIN, ADMIN, MY_DOMAIN, siteId);
        boolean exists = site.exists(siteId, ADMIN, ADMIN);
        Assert.assertFalse(exists);
    }
}
