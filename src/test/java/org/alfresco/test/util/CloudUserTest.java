package org.alfresco.test.util;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CloudUserTest extends AbstractTest
{
    CloudUserService cloudUserService;
    String randomDomain = "@domain" + System.currentTimeMillis();
    String email = "user-" + System.currentTimeMillis() + randomDomain;
    String password = "password";

    @BeforeClass
    public void setup()
    {
        cloudUserService = (CloudUserService) ctx.getBean("cloudUserService");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserInvalidUserName() throws Exception
    {
        cloudUserService.create(null, password);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createUserInvalidPassword() throws Exception
    {
        cloudUserService.create(email, null);
    }
    
    @Test
    public void noExistentUser() throws Exception
    {
        Assert.assertFalse(cloudUserService.userExists("fake@fake.com"));
    }
    
    @Test
    public void createCloudUser() throws Exception
    {    
        boolean create = cloudUserService.create(email, password);
        Assert.assertTrue(create);
        Assert.assertTrue(cloudUserService.userExists(email));
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void upgradeNullDomain() throws Exception
    {
        cloudUserService.upgradeCloudAccount(null, "admin", "admin", "1000");
    }
    
    @Test
    public void upgradeDomainTest() throws Exception
    {        
        String domain = "domain4.com";
        String email = "testUpgrade4" + "@" + domain; 
        cloudUserService.create(email, password);
        boolean domainUpgrade = cloudUserService.upgradeCloudAccount(domain, "admin", "admin", "1000");
        Assert.assertTrue(domainUpgrade);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inviteUserNullInviter() throws Exception
    {
        cloudUserService.inviteUserToSiteAndAccept(null, password, email, password, "siteName", "SiteConsumer", "");
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inviteUserNullSite() throws Exception
    {
        cloudUserService.inviteUserToSiteAndAccept("admin", password, email, password, null, "SiteConsumer", "");
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void inviteUserNullRole() throws Exception
    {
        cloudUserService.inviteUserToSiteAndAccept("admin", password, email, password, "siteName", null, "");
    }
    
    @Test
    public void inviteUserToSite() throws Exception
    {        
        String inviterUserName = "inviter" + System.currentTimeMillis();
        String inviterEmail = inviterUserName + randomDomain;
        String userInvite = "invitee" + System.currentTimeMillis() + randomDomain;
        String defaultSiteId = inviterUserName + "-" + randomDomain;
        Assert.assertTrue(cloudUserService.create(inviterEmail, password));
        Assert.assertTrue(cloudUserService.create(userInvite, password));
        boolean invite = cloudUserService.inviteUserToSiteAndAccept(inviterEmail, 
                                                                    password, 
                                                                    userInvite, 
                                                                    password, 
                                                                    defaultSiteId.replaceFirst("@", ""), 
                                                                    "SiteConsumer", 
                                                                    "Come to the dark side");
        Assert.assertTrue(invite);
    }
    
    @Test
    public void inviteUserToFakeSite() throws Exception
    {        
        String inviterEmail = "inviter" + System.currentTimeMillis() + randomDomain;
        String userInvite = "invitee" + System.currentTimeMillis() + randomDomain;
        boolean toInvite = cloudUserService.create(userInvite, password);
        Assert.assertTrue(toInvite);
        boolean invite = cloudUserService.inviteUserToSiteAndAccept(inviterEmail, 
                                                                    password, 
                                                                    userInvite, 
                                                                    password, 
                                                                    "Faaake", 
                                                                    "SiteConsumer", "");
        Assert.assertFalse(invite);
    }
    
    @Test
    public void promoteUserAsAdmin() throws Exception
    {        
        String promoteUser = "promoteUser" + System.currentTimeMillis() + "@ThePromotion2.com";
        Assert.assertTrue(cloudUserService.createUserAsTenantAdmin("admin", "admin", promoteUser, password));
        Assert.assertTrue(cloudUserService.userExists(promoteUser));
    }  
    
    @Test
    public void promoteFakeUser() throws Exception
    {        
        String promoteUser = "fakeUser" + System.currentTimeMillis() + "@fakepromotion1.com";
        Assert.assertFalse(cloudUserService.promoteUserAsAdmin("admin", 
                                                               "admin", 
                                                               promoteUser, 
                                                               cloudUserService.getUserDomain(promoteUser)));
    }  
}
