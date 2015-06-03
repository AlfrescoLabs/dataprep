package org.alfresco.test.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.Alfresco;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
