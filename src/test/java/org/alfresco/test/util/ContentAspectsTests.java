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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.alfresco.dataprep.CMISUtil.DocumentAspect;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.ContentActions;
import org.alfresco.dataprep.ContentAspects;
import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test content aspects CRUD operations.
 * 
 * @author Bogdan Bocancea
 */
public class ContentAspectsTests extends AbstractTest
{
    @Autowired private UserService userService;
    @Autowired private SiteService site;
    @Autowired private ContentService content;
    @Autowired private ContentAspects contentAspect;
    @Autowired private ContentActions contentAction;
    String admin = "admin";
    String password = "password";
    private final String DATE_FORMAT = "EEE MMM dd HH:mm";
    private String plainDoc = "testDoc.txt";
    private String folder = "cmisFolder";
    private String userName = "contentAspectUser" + System.currentTimeMillis();
    private String siteName = "contentAspectSite" + System.currentTimeMillis();
    private Document doc;
    private String templateDoc = "template" +  System.currentTimeMillis();
    private Document template;
    
    @BeforeClass(alwaysRun = true)
    public void userSetup()
    {
        userService.create(admin, admin, userName, password, userName + domain,"firstname","lastname");
        site.create(userName, password, "mydomain", siteName,  "my site description", Visibility.PUBLIC);
        doc = content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        content.createFolder(userName, password, folder, siteName);
        template = content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, templateDoc, templateDoc);
    }
    
    @Test
    public void addDocAspect()
    {
        Assert.assertFalse(doc.getId().isEmpty());
        contentAspect.addAspect(userName, password, siteName, plainDoc, DocumentAspect.DUBLIN_CORE);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertTrue(properties.toString().contains(DocumentAspect.DUBLIN_CORE.getProperty()));
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void addAspectInvalidDoc()
    {
        contentAspect.addAspect(userName, password, siteName, "fakeDoc", DocumentAspect.DUBLIN_CORE);
    }
    
    @Test
    public void addDocComplianceableAspect()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 3);
        Date removeAfter = calendar.getTime();
        contentAspect.addComplianceable(userName, password, siteName, plainDoc, removeAfter);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertTrue(contentAspect.getPropertyValue(properties, "cm:removeAfter").contains(new SimpleDateFormat(DATE_FORMAT).format(removeAfter)));
    }
    
    @Test
    public void addFolderComplianceableAspect()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 3);
        Date removeAfter = calendar.getTime();
        contentAspect.addComplianceable(userName, password, siteName, folder, removeAfter);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, folder);
        Assert.assertTrue(contentAspect.getPropertyValue(properties, "cm:removeAfter").contains(new SimpleDateFormat(DATE_FORMAT).format(removeAfter)));
    }
    
    @Test(dependsOnMethods="addDocComplianceableAspect")
    public void removeDocComplianceable()
    {
        contentAspect.removeAspect(userName, password, siteName, plainDoc, DocumentAspect.COMPLIANCEABLE);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:removeAfter"), null);
    }
    
    @Test(dependsOnMethods="addFolderComplianceableAspect")
    public void removeFolderComplianceable()
    {
        contentAspect.removeAspect(userName, password, siteName, folder, DocumentAspect.COMPLIANCEABLE);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, folder);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "removeAfter"), null);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void removeAspectInvalidContent()
    {
        contentAspect.removeAspect(userName, password, siteName, "fakeFolder", DocumentAspect.COMPLIANCEABLE);
    }
    
    @Test
    public void addDocDublinCore()
    {
        contentAspect.addDublinCore(userName, password, siteName, plainDoc, userName, userName, "Plain text", userName, 
                plainDoc, "90", "Best Rights", plainDoc);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:contributor"), userName);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:publisher"), userName);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:type"), "Plain text");
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:identifier"), userName);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:dcsource"), plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:coverage"), "90");
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:rights"), "Best Rights");
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:subject"), plainDoc);
    }
    
    @Test(dependsOnMethods="addDocDublinCore")
    public void removeDublinCore()
    {
        contentAspect.removeAspect(userName, password, siteName, plainDoc, DocumentAspect.DUBLIN_CORE);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, DocumentAspect.DUBLIN_CORE.getProperty()), null);
    }
    
    @Test
    public void addEffectivity()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 3);
        Date toDate = calendar.getTime();
        Date fromDate = new Date();
        contentAspect.addEffectivity(userName, password, siteName, plainDoc, fromDate, toDate);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);  
        Assert.assertTrue(contentAspect.getPropertyValue(properties, "cm:from").contains(new SimpleDateFormat(DATE_FORMAT).format(fromDate)));
        Assert.assertTrue(contentAspect.getPropertyValue(properties, "cm:to").contains(new SimpleDateFormat(DATE_FORMAT).format(toDate)));
    }
    
    @Test
    public void addGeographicAspect()
    {
        Double longitude = 46.803138;
        Double latitude = 26.614806;
        contentAspect.addGeographicAspect(userName, password, siteName, plainDoc, longitude, latitude);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:longitude"), longitude.toString());
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:latitude"), latitude.toString());
    }
    
    @Test
    public void addSummarizableAspect()
    {
        String summary = "Test summary";
        contentAspect.addSummarizable(userName, password, siteName, plainDoc, summary);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:summary"), summary);
    }
    
    @Test
    public void addTemplatableAspect()
    {
        contentAspect.addTemplatable(userName, password, siteName, plainDoc, templateDoc);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:template"), template.getId().split(";")[0]);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addTemplatableFakeTemplate()
    {
        contentAspect.addTemplatable(userName, password, siteName, plainDoc, "fakeTemplate");
    }
    
    @Test(dependsOnMethods="addTemplatableAspect")
    public void removeTemplatableAspect()
    {
        contentAspect.removeAspect(userName, password, siteName, plainDoc, DocumentAspect.TEMPLATABLE);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:template"), null);
    }
    
    @Test
    public void addEmailedAspect()
    {
        String addressee = "testUser@test.com";
        Date sentDate = new Date();
        List<String> addressees = new ArrayList<String>();
        addressees.add("test2@test.com");
        addressees.add("test3@test.com");
        contentAspect.addEmailed(userName, password, siteName, plainDoc, addressee, addressees, "Subject", userName, sentDate);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:addressee"), addressee);
        Assert.assertEquals(contentAspect.getValues(properties, "cm:addressees"), addressees);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:subjectline"), "Subject");
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:originator"), userName);
        Assert.assertTrue(contentAspect.getPropertyValue(properties, "cm:sentdate").contains(new SimpleDateFormat(DATE_FORMAT).format(sentDate)));
    }
    
    @Test
    public void addIndexControlAspect()
    {
        contentAspect.addIndexControl(userName, password, siteName, plainDoc, true, true);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:isIndexed"), "true");
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:isContentIndexed"), "true");
    }
    
    @Test
    public void addRestrictableAspectAspect()
    {
        int hours = 5;
        contentAspect.addRestrictable(userName, password, siteName, plainDoc, hours);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "dp:offlineExpiresAfter"), String.valueOf(TimeUnit.HOURS.toMillis(hours)));
    }
    
    @Test
    public void addClasifiableAspect()
    {
        List<String> categories = new ArrayList<String>();
        categories.add("Regions");
        categories.add("Languages");
        contentAspect.addClasifiable(userName, password, siteName, plainDoc, categories);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        List<?> values = contentAspect.getValues(properties, "cm:categories");
        Assert.assertEquals(values.get(0), contentAspect.getCategoryNodeRef(userName, password, categories.get(0)));
        Assert.assertEquals(values.get(1), contentAspect.getCategoryNodeRef(userName, password, categories.get(1)));
    }
    
    @Test(dependsOnMethods="addClasifiableAspect")
    public void removeClasifiableAspect()
    {
        contentAspect.removeAspect(userName, password, siteName, plainDoc, DocumentAspect.CLASSIFIABLE);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        List<?> values = contentAspect.getValues(properties, "cm:categories");
        Assert.assertTrue(values.isEmpty());
    }
    
    @Test
    public void addAudioAspect()
    {
        contentAspect.addAspect(userName, password, siteName, plainDoc, DocumentAspect.AUDIO);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertTrue(properties.toString().contains(DocumentAspect.AUDIO.getProperty()));
    }
    
    @Test
    public void addExifAspect()
    {
        contentAspect.addAspect(userName, password, siteName, plainDoc, DocumentAspect.EXIF);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertTrue(properties.toString().contains(DocumentAspect.EXIF.getProperty()));
    }
    
    @Test
    public void addTagDocument()
    {
        String tag1 = "tag1" + System.currentTimeMillis();
        Assert.assertTrue(contentAction.addSingleTag(userName, password, siteName, plainDoc, tag1));
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, plainDoc);
        Assert.assertTrue(properties.toString().contains(DocumentAspect.TAGGABLE.getProperty()));
        String tagNodeRef = contentAction.getTagNodeRef(userName, password, siteName, plainDoc, tag1);
        List<?> values = contentAspect.getValues(properties, "cm:taggable");
        Assert.assertTrue(values.contains(tagNodeRef));
    }
    
    @Test
    public void getBasicProperties()
    {
        String contentName = "contentNameBasic" + System.currentTimeMillis();
        String docContent = "contentBasic" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        Document doc1 = content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, contentName, docContent);
        Assert.assertFalse(doc1.getId().isEmpty());
        Map<String, Object> basicProperties=contentAspect.getBasicProperties(userName, password, siteName, contentName);
        Assert.assertEquals(basicProperties.get("Name").toString(),contentName);
        Assert.assertEquals(basicProperties.get("Title").toString(),"(None)");
        Assert.assertEquals(basicProperties.get("Description").toString(),"(None)");
        Assert.assertEquals(basicProperties.get("MimeType").toString(),doc1.getContentStream().getMimeType());
        Assert.assertEquals(basicProperties.get("Author").toString(),"(None)");
        Assert.assertEquals(basicProperties.get("Size").toString(),"25");
        Assert.assertEquals(basicProperties.get("Creator").toString(),userName);
        Assert.assertEquals(basicProperties.get("Modifier").toString(),userName);
    }
    
    @Test
    public void setBasicProperties()
    {
        String docName = "contentNameSetBasic" + System.currentTimeMillis();
        String docContent = "content SetBasic " + System.currentTimeMillis();
        String newName="new-name-" + System.currentTimeMillis();
        String newTitle="new-title-" + System.currentTimeMillis();
        String newDescription="new description-" + System.currentTimeMillis();
        String newAuthor="new-author-" + System.currentTimeMillis();
        userService.create(admin, admin, userName, password, password,"firstname","lastname");
        Document doc1 = content.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, docName, docContent);
        Assert.assertFalse(doc1.getId().isEmpty());
        contentAspect.setBasicProperties(userName, password, siteName, docName, newName, newTitle, newDescription, newAuthor);
        List<Property<?>> properties = contentAspect.getProperties(userName, password, siteName, newName);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, PropertyIds.NAME), newName);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:title"), newTitle);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, PropertyIds.DESCRIPTION), newDescription);
        Assert.assertEquals(contentAspect.getPropertyValue(properties, "cm:author"), newAuthor);
    }
}
