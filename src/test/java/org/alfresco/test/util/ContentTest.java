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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test CMIS crud operations.
 * 
 * @author Bogdan Bocancea
 * @author Cristina Axinte
 */
public class ContentTest extends AbstractTest
{
    @Autowired private UserService userService;
    @Autowired private SiteService site;
    @Autowired private ContentService content;
    String admin = "admin";
    String password = "password";
    String folder = "cmisFolder";
    String plainDoc = "plainDoc";

    @Test(expectedExceptions = CmisRuntimeException.class)
    public void testCreateFolderTwice()
    {
        String siteName = "siteCMIS-" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        content.createFolder(userName, password, folder, siteName);
    }

    @Test
    public void testCreateFolder()
    {
        String siteName = "siteCMIS-" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        Folder newFolder = content.createFolder(userName, password, folder, siteName);
        Assert.assertFalse(newFolder.getId().isEmpty());
        Folder parent = newFolder.getFolderParent();
        Assert.assertEquals(parent.getName(), "documentLibrary");
    }

    @Test(expectedExceptions = CmisRuntimeException.class)
    public void testCreateFolderInvalidSimbols()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String symbolFolder = "*/.:?|\\`\"";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createFolder(userName, password, symbolFolder, siteName);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void createFolderInvalidUser()
    {
        String siteName = "siteCMIS-" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createFolder("fakeUser", "fakePass", folder, siteName);
    }

    @Test(expectedExceptions = CmisRuntimeException.class)
    public void createFolderInvalidSite()
    {
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        content.createFolder(userName, password, folder, "fakeSite");
    }

    @Test
    public void testDeleteFolders()
    {
        String siteName = "siteCMISDelete" + System.currentTimeMillis();
        String userName = "cmisUserDelete" + System.currentTimeMillis();
        String rootFolder = "cmisFolderDelete";
        String secondFolder = "cmisSecondFolder";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
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
    public void deleteNonExistentFolder()
    {
        String userName = "cmisUserDelete" + System.currentTimeMillis();
        String siteName = "siteCMISDelete" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.deleteFolder(userName, password, siteName, "fakeFolder");
    }

    @Test
    public void testCreateDocument()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "plain";
        String msWord = "msWord";
        String msExcel = "msExcel";
        String html = "html";
        String xml = "xml";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
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
    public void testCreateDocInvalidSimbols()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String symbolDoc = "*/.:?|\\`\"";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, symbolDoc, symbolDoc);
    }

    @Test(expectedExceptions = CmisRuntimeException.class)
    public void createDocFakeSite()
    {
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        content.createDocument(userName, password, "fakeSite", DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
    }

    @Test(expectedExceptions = CmisRuntimeException.class)
    public void createDuplicatedDoc()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
    }

    @Test
    public void createDocumentInFolder()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        Document doc = content.createDocumentInFolder(userName, password, siteName, folder, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        Assert.assertFalse(doc.getId().isEmpty());
    }

    @Test(expectedExceptions = CmisRuntimeException.class)
    public void createDocInFolderInvalidSymbols()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String symbolDoc = "*/.:?|\\`\"";
        String folder = "cmisFolder";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        content.createDocumentInFolder(userName, password, siteName, folder, DocumentType.TEXT_PLAIN, symbolDoc, symbolDoc);
    }

    @Test(expectedExceptions = CmisRuntimeException.class)
    public void createDocInNonExistentFolder()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createDocumentInFolder(userName, password, siteName, "fakeFolder", DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
    }

    @Test
    public void createDocumentInSubFolder()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String subFolderDoc = "cmisDoc" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
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
    public void deleteDocumentFromRoot()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        content.deleteDocument(userName, password, siteName, plainDoc);
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, plainDoc).isEmpty());
    }

    @Test
    public void deleteDocumentFromFolder()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        content.createDocumentInFolder(userName, password, siteName, folder, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        content.deleteDocument(userName, password, siteName, plainDoc);
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, plainDoc).isEmpty());
    }

    @Test(expectedExceptions = CmisRuntimeException.class)
    public void deleteDocumentInvalidSite()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createFolder(userName, password, folder, siteName);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        content.deleteDocument(userName, password, "fakeSite", plainDoc);
    }

    @Test
    public void deleteTree()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String folder1 = "cmisFolder1" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.XML, "xmlDoc", "contentXmlDoc");
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

    @Test
    public void testUploadDocsInDocumentLibrary()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        List<Document> uploadedDocs = content.uploadFiles(DATA_FOLDER, userName, password, siteName);
        Assert.assertNotNull(uploadedDocs);
        for (Document d : uploadedDocs)
        {
            Assert.assertFalse(d.getId().isEmpty());
        }
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUploadDocsFromFileInsteadFolder()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String fileName = "cmisFile" + System.currentTimeMillis();
        String fileFromPath = DATA_FOLDER + SLASH + "UploadFile-xml.xml";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, fileName, "file node");
        content.uploadFiles(fileFromPath, userName, password, siteName);
    }

    @Test
    public void testUploadDocsInFolder()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String folderName = "cmisFolder" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createFolder(userName, password, folderName, siteName);
        List<Document> uploadedDocs = content.uploadFilesInFolder(DATA_FOLDER, userName, password, siteName, folderName);
        Assert.assertNotNull(uploadedDocs);
        for (Document d : uploadedDocs)
        {
            Assert.assertFalse(d.getId().isEmpty());
        }
    }

    @Test(expectedExceptions = CmisRuntimeException.class)
    public void testUploadDocsInNonExistentFolder()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.uploadFilesInFolder(DATA_FOLDER, userName, password, siteName, "NotExistFld");
    }

    @Test(expectedExceptions = CmisRuntimeException.class)
    public void testUploadDocsInFileInsteadFolder()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String fileName = "cmisFile" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, fileName, "file node");
        content.uploadFilesInFolder(DATA_FOLDER, userName, password, siteName, fileName);
    }

    @Test
    public void testCreateDocumentFile()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "plain" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        File file = new File(plainDoc);
        Document doc1 = content.createDocument(userName, password, siteName, DocumentType.MSWORD, file, plainDoc);
        Assert.assertFalse(doc1.getId().isEmpty());
    }

    @Test
    public void testDeleteFiles()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainFile = "UploadFile-plaintext.txt";
        String xmlFile = "UploadFile-xml.xml";
        String htmlFile = "UploadFile-html.html";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.uploadFiles(DATA_FOLDER, userName, password, siteName);
        content.deleteFiles(userName, password, siteName, plainFile, xmlFile, htmlFile);
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, plainFile).isEmpty());
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, xmlFile).isEmpty());
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, htmlFile).isEmpty());
    }

    @Test
    public void testDeleteNoFiles()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainFile = "UploadFile-plaintext.txt";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.uploadFiles(DATA_FOLDER, userName, password, siteName);
        content.deleteFiles(userName, password, siteName);
        Assert.assertFalse(content.getNodeRef(userName, password, siteName, plainFile).isEmpty());
    }

    @Test
    public void testDeleteFilesFromFolders()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String folderName = "cmisFolder" + System.currentTimeMillis();
        String plainFile = "UploadFile-plaintext.txt";
        String xmlFile = "UploadFile-xml.xml";
        String htmlFile = "UploadFile-html.html";
        String xlxsFile = "UploadFile-xlsx.xlsx";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.HTML, htmlFile, "content html");
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainFile, "content plain text");
        content.createFolder(userName, password, folderName, siteName);
        content.createDocumentInFolder(userName, password, siteName, folderName, DocumentType.XML, xmlFile, "content xml");
        content.createDocumentInFolder(userName, password, siteName, folderName, DocumentType.MSEXCEL, xlxsFile, "content excel");
        content.deleteFiles(userName, password, siteName, plainFile, xmlFile, xlxsFile);
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, plainFile).isEmpty());
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, xmlFile).isEmpty());
        Assert.assertTrue(content.getNodeRef(userName, password, siteName, xlxsFile).isEmpty());
        Assert.assertFalse(content.getNodeRef(userName, password, siteName, htmlFile).isEmpty());
    }

    @Test
    public void updateContent()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "plain" + System.currentTimeMillis();
        String xml = "xml" + System.currentTimeMillis();
        String docx  = "doc" + System.currentTimeMillis();
        String newContentPlain = "new plain content";
        String newContentXml = "new xml content";
        String newDocContent = "new doc content";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        content.createDocument(userName, password, siteName, DocumentType.XML, xml, xml);
        content.createDocument(userName, password, siteName, DocumentType.MSWORD, docx, docx);
        Assert.assertTrue(content.getDocumentContent(userName, password, siteName, plainDoc).equals(plainDoc));
        Assert.assertTrue(content.getDocumentContent(userName, password, siteName, xml).equals(xml));
        Assert.assertTrue(content.updateDocumentContent(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, newContentPlain));
        Assert.assertTrue(content.updateDocumentContent(userName, password, siteName, DocumentType.XML, xml, newContentXml));
        Assert.assertTrue(content.updateDocumentContent(userName, password, siteName, DocumentType.MSWORD, docx, newDocContent));
        Assert.assertTrue(content.getDocumentContent(userName, password, siteName, plainDoc).equals(newContentPlain));
        Assert.assertTrue(content.getDocumentContent(userName, password, siteName, xml).equals(newContentXml));
        Assert.assertTrue(content.getDocumentContent(userName, password, siteName, docx).equals(newDocContent));
    }

    @Test
    public void updateContentEmpty()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String plainDoc = "plain";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        Assert.assertTrue(content.updateDocumentContent(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, ""));
        Assert.assertTrue(content.getDocumentContent(userName, password, siteName, plainDoc).equals(""));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void updateContentInvalidDoc()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        content.updateDocumentContent(userName, password, siteName, DocumentType.TEXT_PLAIN, "fakeDoc", "new content");
    }

    @Test
    public void createFolderInRepository()
    {
        String folder = "newFolder" + System.currentTimeMillis();
        Folder newFolder = content.createFolderInRepository(admin, admin, folder, null);
        Assert.assertFalse(newFolder.getId().isEmpty());
        Assert.assertEquals(newFolder.getFolderParent().getName(), "Company Home");
        Assert.assertFalse(content.getNodeRefByPath(admin, admin, folder).isEmpty());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void createFolderInRepositoryTwice()
    {
        content.createFolderInRepository(admin, admin, folder, null);
        content.createFolderInRepository(admin, admin, folder, null);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void createFolderInRepositoryUnauthorized()
    {
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        String folder = "newFolder" + System.currentTimeMillis();
        content.createFolderInRepository(userName, password, folder, null);
    }

    @Test
    public void createFolderInRepositoryByPath()
    {
        String folder = "newFolder" + System.currentTimeMillis();
        Folder newFolder = content.createFolderInRepository(admin, admin, folder, "Guest Home");
        Assert.assertFalse(newFolder.getId().isEmpty());
        Assert.assertEquals(newFolder.getFolderParent().getName(), "Guest Home");
        Assert.assertFalse(content.getNodeRefByPath(admin, admin, "Guest Home/" + folder).isEmpty());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void createFolderInRepositoryInvalidPath()
    {
        String folder = "newFolder" + System.currentTimeMillis();
        content.createFolderInRepository(admin, admin, folder, "Shared/InvalidPath");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void deleteFolderFromRepoUnauthorized()
    {
        String userName = "cmisUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        String folder = "newFolder" + System.currentTimeMillis();
        Folder newFolder = content.createFolderInRepository(admin, admin, folder, null);
        Assert.assertFalse(newFolder.getId().isEmpty());
        content.deleteContentByPath(userName, password, folder);
    }

    @Test
    public void deleteFolderFromRepo()
    {
        String folder = "newFolder" + System.currentTimeMillis();
        Folder newFolder = content.createFolderInRepository(admin, admin, folder, null);
        Assert.assertFalse(newFolder.getId().isEmpty());
        content.deleteContentByPath(admin, admin, folder);
        Assert.assertTrue(content.getNodeRefByPath(admin, admin, folder).isEmpty());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void deleteFolderFromRepoInvalidPath()
    {
        String folder = "newFolder" + System.currentTimeMillis();
        Folder newFolder = content.createFolderInRepository(admin, admin, folder, null);
        Assert.assertFalse(newFolder.getId().isEmpty());
        content.deleteContentByPath(admin, admin, "Shared/Invalid");
    }

    @Test
    public void createDocInRepository()
    {
        String doc = "repoDoc-" + System.currentTimeMillis();
        Document theDoc = content.createDocumentInRepository(admin, admin, null, DocumentType.TEXT_PLAIN, doc, "doc content");
        Assert.assertFalse(theDoc.getId().isEmpty());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void createDocInRepositoryTwice()
    {
        String doc = "repoDoc-" + System.currentTimeMillis();
        content.createDocumentInRepository(admin, admin, null, DocumentType.TEXT_PLAIN, doc, "doc content");
        content.createDocumentInRepository(admin, admin, null, DocumentType.TEXT_PLAIN, doc, "doc content");
    }

    @Test
    public void createDocInRepositoryByPath()
    {
        String doc = "repoDoc-" + System.currentTimeMillis();
        String folder = "shareFolder" + System.currentTimeMillis();
        content.createFolderInRepository(admin, admin, folder, "Shared");
        Document theDoc = content.createDocumentInRepository(admin, admin, "Shared/" + folder, DocumentType.MSWORD, doc, "shared doc content");
        Assert.assertFalse(theDoc.getId().isEmpty());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void createDocInRepositoryInvalidPath()
    {
        String doc = "repoDoc-" + System.currentTimeMillis();
        Document theDoc = content.createDocumentInRepository(admin, admin, "invalidPath", DocumentType.TEXT_PLAIN, doc, "shared doc content");
        Assert.assertFalse(theDoc.getId().isEmpty());
    }

    @Test
    public void createFileInRepository()
    {
        String doc = "repoDoc-" + System.currentTimeMillis();
        File file = new File(doc);
        Document theDoc = content.createDocumentInRepository(admin, admin, "Shared", DocumentType.TEXT_PLAIN, file, "shared doc content");
        Assert.assertFalse(theDoc.getId().isEmpty());
    }

    @Test
    public void deleteFileFromRepository()
    {
        String doc = "repoDoc-" + System.currentTimeMillis();
        File file = new File(doc);
        Document theDoc = content.createDocumentInRepository(admin, admin, "Shared", DocumentType.TEXT_PLAIN, file, "shared doc content");
        Assert.assertFalse(theDoc.getId().isEmpty());
        content.deleteContentByPath(admin, admin, "Shared/" + file.getName());
        Assert.assertTrue(content.getNodeRefByPath(admin, admin,  "Shared/" + file.getName()).isEmpty());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void deleteFileFromRepoInvalidUser()
    {
        String doc = "repoDoc-" + System.currentTimeMillis();
        Document theDoc = content.createDocumentInRepository(admin, admin, "Shared", DocumentType.TEXT_PLAIN, doc, "shared doc content");
        Assert.assertFalse(theDoc.getId().isEmpty());
        String userName = "deleteUser" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        content.deleteContentByPath(userName, password, "Shared/" + doc);
        Assert.assertTrue(content.getNodeRefByPath(admin, admin, "Shared/" + doc).isEmpty());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void deleteFakeFileFromRepo()
    {
        content.deleteContentByPath(admin, admin, "fakeDoc");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void deleteFolderWithChildrenNegative()
    {
        String doc = "repoDoc-" + System.currentTimeMillis();
        File file = new File(doc);
        String folder = "shareFolder" + System.currentTimeMillis();
        content.createFolderInRepository(admin, admin, folder, "Shared");
        Document theDoc = content.createDocumentInRepository(admin, admin, "Shared/" + folder, DocumentType.MSWORD, file, "shared doc content");
        Assert.assertFalse(theDoc.getId().isEmpty());
        content.deleteContentByPath(admin, admin, "Shared/" + folder);
    }

    @Test
    public void deleteFolderWithChildren()
    {
        String doc = "repoDoc-" + System.currentTimeMillis();
        String folder = "sharedFolder" + System.currentTimeMillis();
        content.createFolderInRepository(admin, admin, folder, "Shared");
        Document theDoc = content.createDocumentInRepository(admin, admin, "Shared/" + folder, DocumentType.MSWORD, doc, "shared doc content");
        Assert.assertFalse(theDoc.getId().isEmpty());
        content.deleteTreeByPath(admin, admin, "Shared/" + folder);
        Assert.assertTrue(content.getNodeRefByPath(admin, admin, "Shared/" + folder).isEmpty());
    }

    @Test
    public void uploadDocInSite()
    {
        String siteName = "siteDocNew" + System.currentTimeMillis();
        String userName = "cmisUser" + System.currentTimeMillis();
        String pathToFile = DATA_FOLDER + SLASH + "UploadFile-xml.xml";
        userService.create(admin, admin, userName, password, password, "firstname", "lastname");
        site.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        Document d = content.uploadFileInSite(userName, password, siteName, pathToFile);
        Assert.assertFalse(d.getId().isEmpty());
    }

    @Test
    public void uploadDocInRepository()
    {
        String pathToFile = DATA_FOLDER + SLASH + "UploadFile-xml.xml";
        Document d = content.uploadFileInRepository(admin, admin, "Shared", pathToFile);
        Assert.assertFalse(d.getId().isEmpty());
        content.deleteContentByPath(admin, admin, "Shared/UploadFile-xml.xml");
        Assert.assertTrue(content.getNodeRefByPath(admin, admin, "Shared/UploadFile-xml.xml").isEmpty());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void uploadDocTwice()
    {
        String pathToFile = DATA_FOLDER + SLASH + "UploadFile-plaintext.txt";
        Document d = content.uploadFileInRepository(admin, admin, "Shared", pathToFile);
        Assert.assertFalse(d.getId().isEmpty());
        content.uploadFileInRepository(admin, admin, "Shared", pathToFile);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void uploadDocFakePath()
    {
        String pathToFile = DATA_FOLDER + SLASH + "fakeFile.txt";
        content.uploadFileInRepository(admin, admin, null, pathToFile);
    }
    
    @Test
    public void createDocInUserHomes()
    {
        String user = "user_" + System.currentTimeMillis();
        String doc = "repoDoc-" + System.currentTimeMillis();
        String folder = "shareFolder" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, user, user, user + "@test", "firstName", "lastName");
        content.createFolderInRepository(user, user, folder, "User Homes/" + user);
        Document theDoc = content.createDocumentInRepository(admin, admin, "User Homes/" + user + "/" + folder, DocumentType.MSWORD, doc, "shared doc content");
        Assert.assertFalse(theDoc.getId().isEmpty());
    }
}
