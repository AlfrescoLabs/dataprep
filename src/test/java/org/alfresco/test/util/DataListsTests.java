package org.alfresco.test.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.dataprep.CMISUtil.Status;
import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.DataListsService;
import org.alfresco.dataprep.UserService;
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
    @Autowired private UserService userService;
    String siteId;
    
    @BeforeClass
    public void setup() throws IOException
    {
        siteId = "list" + System.currentTimeMillis();
        site.create(ADMIN, ADMIN, "myDomain", siteId, "description", Visibility.PUBLIC);
    }
    
    @Test
    public void addContactList()
    {
        String contactList = "contact" + System.currentTimeMillis();
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.CONTACT_LIST, contactList, "contact description");
        Assert.assertFalse(id.getId().isEmpty());
        ObjectId contactItem = dataLists.addContactListItem(ADMIN, ADMIN, siteId, contactList, "first", "lastName", "email", 
                    "company", "jobTitle", "phoneOffice", "phoneMobile", "notes");
        ObjectId contactItem2 = dataLists.addContactListItem(ADMIN, ADMIN, siteId, contactList, "first", "lastName", 
                    "email", "company", "jobTitle", "phoneOffice", "phoneMobile", "notes");
        Assert.assertFalse(contactItem.getId().isEmpty());
        Assert.assertFalse(contactItem2.getId().isEmpty());
    }
    
    @Test
    public void addEventAgenda()
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
        ObjectId itemEventId = dataLists.addEventAgendaItem(ADMIN, ADMIN, siteId, eventAgenda, "reference", "startTime", "endTime", "sessionName", 
                "presenter", "audience", "notes", docs);
        Assert.assertFalse(itemEventId.getId().isEmpty());
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addEventAgendaFakeSite()
    {
        String eventAgenda = "eventAgenda" + System.currentTimeMillis();
        String doc1 = "doc1" + System.currentTimeMillis();
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.EVENT_AGENDA, eventAgenda, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSPOWERPOINT, doc1, " doc 1 for event agenda");
        List<String> docs = new ArrayList<String>();
        docs.add(doc1);
        dataLists.addEventAgendaItem(ADMIN, ADMIN, "fakeSite", eventAgenda, "reference", "startTime", "endTime", "sessionName", 
                "presenter", "audience", "notes", docs);
    }
    
    @Test(expectedExceptions = CmisRuntimeException.class)
    public void attachInvalidDoc()
    {
        String eventAgenda = "eventAgenda" + System.currentTimeMillis();
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.EVENT_AGENDA, eventAgenda, eventAgenda);
        Assert.assertFalse(id.getId().isEmpty());
        List<String> docs = new ArrayList<String>();
        docs.add("fakeDoc");
        dataLists.addEventAgendaItem(ADMIN, ADMIN, siteId, eventAgenda, "reference", "startTime", "endTime", "sessionName",
                "presenter", "audience", "notes", docs);
    }
    
    @Test
    public void addEventList()
    {
        String eventAgenda = "eventList";
        String doc1 = "doc1" + System.currentTimeMillis();
        String doc2 = "doc2" + System.currentTimeMillis();
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.EVENT_LIST, eventAgenda, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSPOWERPOINT, doc1, " doc 1 for event agenda");
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSEXCEL, doc2, " doc 2 for event agenda");
        List<String> docs = new ArrayList<String>();
        docs.add(doc1);
        docs.add(doc2);
        Date date = new Date();
        ObjectId itemEventId = dataLists.addEventListItem(ADMIN, ADMIN, siteId, eventAgenda, "title", "description", "location",
                    date, date, "registration", "notes", docs);
        Assert.assertFalse(itemEventId.getId().isEmpty());
    }
    
    @Test
    public void addIssueList()
    {
        String issueList = "issueList";
        String userToAssign = "assignUser" + System.currentTimeMillis();
        String userToAssign2 = "assignUser-2" + System.currentTimeMillis();
        String userName = "user-" + System.currentTimeMillis();
        String doc1 = "doc1" + System.currentTimeMillis();
        String doc2 = "doc2" + System.currentTimeMillis();
        String siteId = "issue-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToAssign, userToAssign, userToAssign, "fName", "lastName");
        userService.create(ADMIN, ADMIN, userToAssign2, userToAssign2, userToAssign2, "user2", "lastUsr2");
        userService.create(ADMIN, ADMIN, userName, userName, userName, "issueUser", "issue");
        site.create(userName, userName, "myDomain", siteId, "description", Visibility.PUBLIC);
        ObjectId id = dataLists.createDataList(userName, userName, siteId, DataList.ISSUE_LIST, issueList, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        content.createDocument(userName, userName, siteId, DocumentType.MSPOWERPOINT, doc1, " doc 1 for event agenda");
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSEXCEL, doc2, " doc 2 for event agenda");
        List<String> docs = new ArrayList<String>();
        docs.add(doc1);
        docs.add(doc2);
        List<String> users = new ArrayList<String>();
        users.add(userToAssign);
        users.add(userToAssign2);
        Date date = new Date();
        ObjectId itemEventId = dataLists.addIssueListItem(userName, userName, siteId, issueList, "issueID", "issueTitle", users, 
                Status.NOT_STARTED, Priority.Low, "desc", date, "comments", docs);
        Assert.assertFalse(itemEventId.getId().isEmpty());
    }
    
    @Test
    public void addLocationList()
    {
        String locationList = "LocationList";
        String doc1 = "doc1" + System.currentTimeMillis();
        String doc2 = "doc2" + System.currentTimeMillis();
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.LOCATION_LIST, locationList, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSPOWERPOINT, doc1, " doc 1 for location list");
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSEXCEL, doc2, " doc 2 for location");
        List<String> docs = new ArrayList<String>();
        docs.add(doc1);
        docs.add(doc2);
        ObjectId itemLocationId = dataLists.addLocationItem(ADMIN, ADMIN, siteId, locationList, "title", "addressLine1", "addressLine2", "addressLine3",
                "zipCode", "state", "country", "description", docs);
        Assert.assertFalse(itemLocationId.getId().isEmpty());
    }
    
    @Test
    public void addMeetingListItem()
    {
        String meetingList = "meetingList";
        String doc1 = "doc1" + System.currentTimeMillis();
        String doc2 = "doc2" + System.currentTimeMillis();
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.MEETING_AGENDA, meetingList, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSPOWERPOINT, doc1, " doc 1 for location list");
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSEXCEL, doc2, " doc 2 for location");
        List<String> docs = new ArrayList<String>();
        docs.add(doc1);
        docs.add(doc2);
        ObjectId itemLocationId = dataLists.addMeetingItem(ADMIN, ADMIN, siteId, meetingList, "reference", "item title", "description", "43", "owner", docs);
        Assert.assertFalse(itemLocationId.getId().isEmpty());
    }
    
    @Test
    public void addTaskAdvListItem()
    {
        String taskAdv = "taskAdvList";
        String userToAssign = "assignUser" + System.currentTimeMillis();
        String userToAssign2 = "assignUser-2" + System.currentTimeMillis();
        String userName = "user-" + System.currentTimeMillis();
        String doc1 = "doc1" + System.currentTimeMillis();
        String doc2 = "doc2" + System.currentTimeMillis();
        String siteId = "taskAdv-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToAssign, userToAssign, userToAssign, "fName", "lastName");
        userService.create(ADMIN, ADMIN, userToAssign2, userToAssign2, userToAssign2, "user2", "lastUsr2");
        userService.create(ADMIN, ADMIN, userName, userName, userName, "issueUser", "issue");
        site.create(userName, userName, "myDomain", siteId, "description", Visibility.PUBLIC);
        ObjectId id = dataLists.createDataList(userName, userName, siteId, DataList.TASKS_ADVANCED, taskAdv, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        content.createDocument(userName, userName, siteId, DocumentType.MSPOWERPOINT, doc1, " doc 1 for event agenda");
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSEXCEL, doc2, " doc 2 for event agenda");
        List<String> docs = new ArrayList<String>();
        docs.add(doc1);
        docs.add(doc2);
        List<String> users = new ArrayList<String>();
        users.add(userToAssign);
        users.add(userToAssign2);
        Date date = new Date();
        ObjectId itemEventId = dataLists.addTaskAdvancedItem(userName, userName, siteId, taskAdv, "itemTitle", "description", date, date, 
                users, Priority.Low, Status.ON_HOLD, 21, "comments", docs);
        Assert.assertFalse(itemEventId.getId().isEmpty());
    }
    
    @Test
    public void addTaskSimpleItem()
    {
        String taskSimple = "simpleTask";
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.TASKS_SIMPLE, taskSimple, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        Date date = new Date();
        ObjectId itemSimpleTask = dataLists.addTaskSimpleItem(ADMIN, ADMIN, siteId, taskSimple, "item title", "description", date, Priority.Low, Status.IN_PROGRESS, "comments");
        Assert.assertFalse(itemSimpleTask.getId().isEmpty());
    }
    
    @Test
    public void addToDoItem()
    {
        String toDo = "toDoList";
        String userToAssign = "assignUser" + System.currentTimeMillis();
        String userName = "user-" + System.currentTimeMillis();
        String doc1 = "doc1" + System.currentTimeMillis();
        String doc2 = "doc2" + System.currentTimeMillis();
        String siteId = "toDo-" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, userToAssign, userToAssign, userToAssign, "fName", "lastName");
        userService.create(ADMIN, ADMIN, userName, userName, userName, "issueUser", "issue");
        site.create(userName, userName, "myDomain", siteId, "description", Visibility.PUBLIC);
        ObjectId id = dataLists.createDataList(userName, userName, siteId, DataList.TODO_LIST, toDo, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        content.createDocument(userName, userName, siteId, DocumentType.MSPOWERPOINT, doc1, " doc 1 for event agenda");
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSEXCEL, doc2, " doc 2 for event agenda");
        List<String> docs = new ArrayList<String>();
        docs.add(doc1);
        docs.add(doc2);
        Date date = new Date();
        ObjectId itemEventId = dataLists.addToDoItem(userName, userName, siteId, toDo, "itemTitle", date, 1, Status.IN_PROGRESS, "notes", userToAssign, docs);
        Assert.assertFalse(itemEventId.getId().isEmpty());
    }
}
