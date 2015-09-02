package org.alfresco.test.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.DataListsService;
import org.alfresco.dataprep.DataListsService.DataList;
import org.alfresco.dataprep.SiteService;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DataListsTests extends AbstractTest
{
    @Autowired private DataListsService dataLists;
    @Autowired private SiteService site;
    @Autowired private ContentService content;
    String siteId;
    
    @BeforeClass
    public void setup() throws IOException
    {
        siteId = "list" + System.currentTimeMillis();
        site.create(ADMIN, ADMIN, "myDomain", siteId, "description", Visibility.PUBLIC);
    }
    
    @Test
    public void addContactList() throws Exception
    {
        String contactList = "contact" + System.currentTimeMillis();
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.CONTACT_LIST, contactList, "contact description");
        Assert.assertFalse(id.getId().isEmpty());
        ObjectId contactItem = dataLists.addContactListItem(ADMIN, ADMIN, id, "first", "lastName", "email", "company", "jobTitle", "phoneOffice", "phoneMobile", "notes");
        ObjectId contactItem2 = dataLists.addContactListItem(ADMIN, ADMIN, id, "first", "lastName", "email", "company", "jobTitle", "phoneOffice", "phoneMobile", "notes");
        Assert.assertFalse(contactItem.getId().isEmpty());
        Assert.assertFalse(contactItem2.getId().isEmpty());
    }
    
    @Test
    public void addEventAgenda() throws Exception
    {
        String eventAgenda = "eventAgenda";
        String doc1 = "doc1" + System.currentTimeMillis();;
        String doc2 = "doc2" + System.currentTimeMillis();;
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.EVENT_AGENDA, eventAgenda, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSPOWERPOINT, doc1, " doc 1 for event agenda");
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSPOWERPOINT, doc2, " doc 2 for event agenda");
        List<String> docs = new ArrayList<String>();
        docs.add(doc1);
        docs.add(doc2);
        ObjectId itemEventId = dataLists.addEventAgendaItem(ADMIN, ADMIN, id, "reference", "startTime", "endTime", "sessionName", 
                "presenter", "audience", "notes", siteId, docs);
        Assert.assertFalse(itemEventId.getId().isEmpty());
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void addEventAgendaFakeSite() throws Exception
    {
        String eventAgenda = "eventAgenda" + System.currentTimeMillis();
        String doc1 = "doc1" + System.currentTimeMillis();
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.EVENT_AGENDA, eventAgenda, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSPOWERPOINT, doc1, " doc 1 for event agenda");
        List<String> docs = new ArrayList<String>();
        docs.add(doc1);
        dataLists.addEventAgendaItem(ADMIN, ADMIN, id, "reference", "startTime", "endTime", "sessionName", 
                "presenter", "audience", "notes", "fakeSite", docs);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void attachInvalidDoc() throws Exception
    {
        String eventAgenda = "eventAgenda" + System.currentTimeMillis();
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.EVENT_AGENDA, eventAgenda, eventAgenda);
        Assert.assertFalse(id.getId().isEmpty());
        List<String> docs = new ArrayList<String>();
        docs.add("fakeDoc");
        dataLists.addEventAgendaItem(ADMIN, ADMIN, id, "reference", "startTime", "endTime", "sessionName", 
                "presenter", "audience", "notes", siteId, docs);
    }
}
