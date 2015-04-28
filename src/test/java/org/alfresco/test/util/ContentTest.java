package org.alfresco.test.util;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.test.util.CMISUtil.DocumentType;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test CMIS crud operations.
 * 
 * @author Bogdan Bocancea
 */

public class ContentTest extends AbstractTest
{    
    UserService userService;
    SiteService site;
    ContentService content;
    String admin = "admin";
    String password = "password";
    
    @BeforeClass
    public void setup()
    {
        userService = (UserService) ctx.getBean("userService");
        site = (SiteService) ctx.getBean("siteService");
        content = (ContentService) ctx.getBean("contentService");
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void testCreateFolderTwice() throws Exception
    {
        String siteName = "siteCMIS-" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String folder = "cmisFolder" +  System.currentTimeMillis();      
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        content.createFolder(userName, password, folder, siteName);     
    }
    
    @Test 
    public void testCreateFolder() throws Exception
    {
        String siteName = "siteCMIS-" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String folder = "cmisFolder"; 
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        Folder newFolder = content.createFolder(userName, password, folder, siteName);
        Assert.assertFalse(newFolder.getId().isEmpty());
        Folder parent = newFolder.getFolderParent();
        Assert.assertEquals(parent.getName(), "documentLibrary");      
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void createFolderInvalidUser() throws Exception
    {
        String siteName = "siteCMIS-" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String folder = "cmisFolder" +  System.currentTimeMillis();
       
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createFolder("fakeUser", "fakePass", folder, siteName);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void createFolderInvalidSite() throws Exception
    {
        String userName = "cmisUser" + System.currentTimeMillis();
        String folder = "cmisFolder" +  System.currentTimeMillis();    
        userService.create(admin, admin, userName, password, password);
        content.createFolder(userName, password, folder, "fakeSite");
    }
    
    @Test
    public void testDeleteFolders() throws Exception
    {
        String siteName = "siteCMISDelete" + System.currentTimeMillis();
        String userName = "cmisUserDelete" + System.currentTimeMillis();
        String rootFolder = "cmisFolderDelete" +  System.currentTimeMillis();
        String secondFolder = "cmisSecondFolder" +  System.currentTimeMillis();   
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        Folder rootFld = content.createFolder(userName, password, rootFolder, siteName);
        Assert.assertFalse(rootFld.getId().isEmpty());    
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        properties.put(PropertyIds.NAME, secondFolder);
        Folder secondFld = rootFld.createFolder(properties);
        Assert.assertFalse(secondFld.getId().isEmpty());       
        content.deleteFolder(userName, password, siteName, secondFolder);
        content.deleteFolder(userName, password, siteName, rootFolder);
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, rootFolder).isEmpty());
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, secondFolder).isEmpty());
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void deleteNonExistentFolder() throws Exception
    {
        String userName = "cmisUserDelete" + System.currentTimeMillis(); 
        String siteName = "siteCMISDelete" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password);            
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                "my site description", 
                Visibility.PUBLIC);
        content.deleteFolder(userName, password, siteName, "fakeFolder");      
    }
    
    @Test
    public void testCreateDocument() throws Exception
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "plain" +  System.currentTimeMillis();
        String msWord = "msWord" + System.currentTimeMillis();
        String msExcel = "msExcel" + System.currentTimeMillis();
        String html = "html" + System.currentTimeMillis();
        String xml = "xml" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        Document doc1 = content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        Assert.assertFalse(doc1.getId().isEmpty());
        Document doc2 = content.createDocument(userName, password, siteName, DocumentType.MSWORD, msWord, msWord);
        Assert.assertFalse(doc2.getId().isEmpty());
        Document doc3 = content.createDocument(userName, password, siteName, DocumentType.MSEXCEL, msExcel, msExcel);
        Assert.assertFalse(doc3.getId().isEmpty());
        Document doc4 = content.createDocument(userName, password, siteName, DocumentType.HTML, html, html);
        Assert.assertFalse(doc4.getId().isEmpty());
        Document doc5 = content.createDocument(userName, password, siteName, DocumentType.XML, xml, xml);
        Assert.assertFalse(doc5.getId().isEmpty());
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void createDocFakeSite() throws Exception
    {
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "plain" +  System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password);
        content.createDocument(userName, password, "fakeSite", DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void createDuplicatedDoc() throws Exception
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "cmisDoc";
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
    }
    
    @Test
    public void createDocumentInFolder() throws Exception
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "cmisDoc" + System.currentTimeMillis();
        String folder = "cmisFolder" +  System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        Document doc = content.createDocumentInFolder(userName, password, siteName, folder, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        Assert.assertFalse(doc.getId().isEmpty());
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void createDocInNonExistentFolder() throws Exception
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "cmisDoc" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);    
        content.createDocumentInFolder(userName, password, siteName, "fakeFolder", DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
    }
    
    @Test
    public void createDocumentInSubFolder() throws Exception
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "cmisDoc" + System.currentTimeMillis();
        String subFolderDoc = "cmisDoc" + System.currentTimeMillis();
        String folder = "cmisFolder" +  System.currentTimeMillis();            
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);     
        Folder folderRoot = content.createFolder(userName, password, folder, siteName);
        content.createDocumentInFolder(userName, password, siteName, folder, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);   
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        properties.put(PropertyIds.NAME, folder);
        folderRoot.createFolder(properties);
        Document subDoc = content.createDocumentInFolder(userName, password, siteName, folder, DocumentType.MSWORD, subFolderDoc, subFolderDoc);
        Assert.assertFalse(subDoc.getId().isEmpty());
    }
    
    @Test
    public void deleteDocumentFromRoot() throws Exception
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "cmisDoc" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        content.deleteDocument(userName, password, siteName, plainDoc);
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, plainDoc).isEmpty());
    }
    
    @Test
    public void deleteDocumentFromFolder() throws Exception
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "cmisDoc" + System.currentTimeMillis();
        String folder = "cmisFolder" +  System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        content.createDocumentInFolder(userName, password, siteName, folder, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        content.deleteDocument(userName, password, siteName, plainDoc);
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, plainDoc).isEmpty());
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void deleteDocumentInvalidSite() throws Exception
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "cmisDoc" + System.currentTimeMillis();
        String folder = "cmisFolder" +  System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        content.deleteDocument(userName, password, "fakeSite", plainDoc);     
    }
    
    @Test
    public void deleteTree() throws Exception
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "cmisDoc" + System.currentTimeMillis();
        String folder = "cmisFolder" +  System.currentTimeMillis();
        String folder1 = "cmisFolder1" +  System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password);
        site.create(userName,
                    password,
                    "mydomain",
                    siteName, 
                    "my site description", 
                    Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.XML, "xmlDoc", "contentfwfwfwfwgwegwgw");
        Folder f = content.createFolder(userName, password, folder, siteName);
        content.createDocumentInFolder(userName, password, siteName, folder, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);      
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        properties.put(PropertyIds.NAME, folder1);
        f.createFolder(properties);
        content.deleteTree(userName, password, siteName, folder);
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, folder1).isEmpty());
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, plainDoc).isEmpty());       
    }
 
}
