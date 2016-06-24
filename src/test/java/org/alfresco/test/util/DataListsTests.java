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
    private String siteId = "list" + System.currentTimeMillis();;
    private String doc1 = "doc1" + System.currentTimeMillis();
    private String doc2 = "doc2" + System.currentTimeMillis();
    private List<String> docs = new ArrayList<String>();
    private String userToAssign1 = "assignUser-1" + System.currentTimeMillis();
    private String userToAssign2 = "assignUser-2" + System.currentTimeMillis();
    private List<String> users = new ArrayList<String>();
    
    @BeforeClass(alwaysRun = true)
    public void setup() throws IOException
    {
        site.create(ADMIN, ADMIN, "myDomain", siteId, "description", Visibility.PUBLIC);
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSPOWERPOINT, doc1, " doc 1 for event agenda");
        content.createDocument(ADMIN, ADMIN, siteId, DocumentType.MSPOWERPOINT, doc2, " doc 2 for event agenda");
        docs.add(doc1);
        docs.add(doc2);
        userService.create(ADMIN, ADMIN, userToAssign1, userToAssign1, userToAssign1 + domain, "fName", "lastName");
        userService.create(ADMIN, ADMIN, userToAssign2, userToAssign2, userToAssign2 + domain, "user2", "lastUsr2");
        users.add(userToAssign1);
        users.add(userToAssign2);
    }
    
    @Test
    public void addAndUpdateContactList()
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
        Assert.assertNotNull(dataLists.updateDataList(ADMIN, ADMIN, siteId, contactList, "new contact List", "new desc"));
    }
    
    @Test
    public void addEventAgenda()
    {
        String eventAgenda = "eventAgenda";
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.EVENT_AGENDA, eventAgenda, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        ObjectId itemEventId = dataLists.addEventAgendaItem(ADMIN, ADMIN, siteId, eventAgenda, "reference", "startTime", "endTime", "sessionName", 
                "presenter", "audience", "notes", docs);
        Assert.assertFalse(itemEventId.getId().isEmpty());
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void addEventAgendaFakeSite()
    {
        String eventAgenda = "eventAgenda" + System.currentTimeMillis();
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.EVENT_AGENDA, eventAgenda, "event description");
        Assert.assertFalse(id.getId().isEmpty());
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
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.EVENT_LIST, eventAgenda, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        Date date = new Date();
        ObjectId itemEventId = dataLists.addEventListItem(ADMIN, ADMIN, siteId, eventAgenda, "title", "description", "location",
                    date, date, "registration", "notes", docs);
        Assert.assertFalse(itemEventId.getId().isEmpty());
    }
    
    @Test
    public void addIssueList()
    {
        String issueList = "issueList";
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.ISSUE_LIST, issueList, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        Date date = new Date();
        ObjectId itemEventId = dataLists.addIssueListItem(ADMIN, ADMIN, siteId, issueList, "issueID", "issueTitle", users, 
                Status.NOT_STARTED, Priority.Low, "desc", date, "comments", docs);
        Assert.assertFalse(itemEventId.getId().isEmpty());
    }
    
    @Test
    public void addLocationList()
    {
        String locationList = "LocationList";
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.LOCATION_LIST, locationList, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        ObjectId itemLocationId = dataLists.addLocationItem(ADMIN, ADMIN, siteId, locationList, "title", "addressLine1", "addressLine2", "addressLine3",
                "zipCode", "state", "country", "description", docs);
        Assert.assertFalse(itemLocationId.getId().isEmpty());
    }
    
    @Test
    public void addMeetingListItem()
    {
        String meetingList = "meetingList";
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.MEETING_AGENDA, meetingList, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        ObjectId itemLocationId = dataLists.addMeetingItem(ADMIN, ADMIN, siteId, meetingList, "reference", "item title", "description", "43", "owner", docs);
        Assert.assertFalse(itemLocationId.getId().isEmpty());
    }
    
    @Test
    public void addTaskAdvListItem()
    {
        String taskAdv = "taskAdvList";
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.TASKS_ADVANCED, taskAdv, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        Date date = new Date();
        ObjectId itemEventId = dataLists.addTaskAdvancedItem(ADMIN, ADMIN, siteId, taskAdv, "itemTitle", "description", date, date, 
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
        ObjectId itemSimpleTask = dataLists.addTaskSimpleItem(ADMIN, ADMIN, siteId, taskSimple, "item title", "description", date,
                Priority.Low, Status.IN_PROGRESS, "comments");
        Assert.assertFalse(itemSimpleTask.getId().isEmpty());
    }
    
    @Test
    public void addToDoItem()
    {
        String toDo = "toDoList";
        ObjectId id = dataLists.createDataList(ADMIN, ADMIN, siteId, DataList.TODO_LIST, toDo, "event description");
        Assert.assertFalse(id.getId().isEmpty());
        Date date = new Date();
        ObjectId itemEventId = dataLists.addToDoItem(ADMIN, ADMIN, siteId, toDo, "itemTitle", date, 1, Status.IN_PROGRESS, "notes", userToAssign1, docs);
        Assert.assertFalse(itemEventId.getId().isEmpty());
    }
}
