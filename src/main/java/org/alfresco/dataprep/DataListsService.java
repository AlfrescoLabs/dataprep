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
package org.alfresco.dataprep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
/**
 * Class to manage CRUD operations on data lists
 * 
 * @author Bocancea Bogdan
 */
public class DataListsService extends CMISUtil
{
    private static Log logger = LogFactory.getLog(DataListsService.class);
    @Autowired private UserService userService;
    
    public enum DataList
    {
        CONTACT_LIST("dl:contact"),
        EVENT_AGENDA("dl:eventAgenda"),
        EVENT_LIST("dl:event"),
        ISSUE_LIST("dl:issue"),
        LOCATION_LIST("dl:location"),
        MEETING_AGENDA("dl:meetingAgenda"),
        TASKS_ADVANCED("dl:task"),
        TASKS_SIMPLE("dl:simpletask"),
        TODO_LIST("dl:todoList");
        public final String listTypeId;
        DataList(String listTypeId)
        {
            this.listTypeId = listTypeId;
        }
    }

    /**
     * Create data list.
     * 
     * @param userName String username
     * @param password String password
     * @param siteName String site id
     * @param listType List data list type to be created
     * @param listName name of the new list
     * @param description String description
     * @return ObjectId id of the new created list
     */
    public ObjectId createDataList(final String userName,
                                   final String password,
                                   final String siteName,
                                   final DataList listType,
                                   final String listName,
                                   final String description)
    {
        getDataLists(userName, password, siteName);
        UUID uuid = UUID.randomUUID();
        Session session = getCMISSession(userName, password);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PropertyIds.OBJECT_TYPE_ID, "F:dl:dataList");
        props.put("dl:dataListItemType", listType.listTypeId);
        props.put(PropertyIds.NAME, uuid.toString());
        props.put(PropertyIds.DESCRIPTION, description);
        Folder fold = (Folder) session.getObjectByPath("/sites/" + siteName + "/datalists");
        ObjectId id = session.createFolder(props, fold);
        // set the title
        Folder newfold = (Folder) session.getObjectByPath("/sites/" + siteName + "/datalists/" + uuid.toString());
        Map<String, Object> newProp = new HashMap<String, Object>();
        newProp.put("cm:title", listName);
        newfold.updateProperties(newProp);
        return id;
    }

    /**
     * Get data list from a site.
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @return HttpResponse response
     */
    private HttpResponse getDataLists(final String userName,
                                      final String password,
                                      final String siteName)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getAlfrescoUrl() + "alfresco/s/slingshot/datalists/lists/site/" + siteName + "/dataLists";
        HttpGet get = new HttpGet(url);
        return client.execute(userName, password, get);
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getDataListIds(final String userName,
                                        final String password,
                                        final String siteName,
                                        final String dataListTitle)
    {
        List<String>ids = new ArrayList<String>();
        HttpResponse response = getDataLists(userName, password, siteName);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                String strResponse = "";
                Object obj = null;
                try
                {
                    strResponse = EntityUtils.toString(response.getEntity());
                    JSONParser parser = new JSONParser();
                    obj = parser.parse(strResponse);
                }
                catch (ParseException | IOException e)
                {
                    throw new RuntimeException("Failed to parse the response: " + strResponse);
                }
                JSONObject jsonObject = (JSONObject) obj;
                JSONArray jArray = (JSONArray) jsonObject.get("datalists");
                Iterator<JSONObject> iterator = ((List<JSONObject>) jArray).iterator();
                while (iterator.hasNext()) 
                {
                    JSONObject factObj = (JSONObject) iterator.next();
                    String theTitle = (String) factObj.get("title");
                    if(dataListTitle.toString().equalsIgnoreCase(theTitle))
                    {
                        ids.add((String) factObj.get("nodeRef"));
                        ids.add((String) factObj.get("name"));
                    }
                }
                return ids;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid site " + siteName);
            case HttpStatus.SC_UNAUTHORIZED:
                throw new RuntimeException("Invalid credentials");
            default:
                logger.error("Unable to find: " + dataListTitle + " " + response.toString());
                break;
        }
        return ids;
    }
    
    /**
     * Get data list node ref
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param dataListTitle String data list title
     * @return String data list node ref
     */
    public String getDataListNodeRef(final String userName,
                                     final String password,
                                     final String siteName,
                                     final String dataListTitle)
    {
        List<String> ids = getDataListIds(userName, password, siteName, dataListTitle);
        return ids.get(0);
    }
    
    /**
     * Get data list name
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param dataListTitle String data list title
     * @return String data list name
     */
    public String getDataListName(final String userName,
                                  final String password,
                                  final String siteName,
                                  final String dataListTitle)
    {
        List<String> ids = getDataListIds(userName, password, siteName, dataListTitle);
        return ids.get(1);
    }

    /**
     * Method to add items to a created data list
     *
     * @param userName String identifier
     * @param password String password
     * @param objectId ObjectId ID of created data list
     * @param propertiesMap details of the item
     * @return {@link ObjectId} of new created item
     */
    private ObjectId addItem(final String userName,
                            final String password,
                            final ObjectId objectId,
                            Map<String, Object> propertiesMap)
    {
        try
        {
            Session session = getCMISSession(userName, password);
            ObjectId itemId = session.createDocument(propertiesMap, objectId, null, null);
            return itemId;
        }
        catch(CmisInvalidArgumentException ia)
        {
            throw new CmisRuntimeException("Invalid content " + objectId, ia);
        }
    }
    
    /**
     * Add contact list item
     * @param userName String user name
     * @param password String password
     * @param list ObjectId id of the list
     * @param firstName String first name
     * @param lastName String last name
     * @param email String email
     * @param company String company
     * @param jobTitle String job title
     * @param phoneOffice String office phone
     * @param phoneMobile String mobile phone
     * @param notes String notes
     * @return {@link ObjectId} of new created item
     */
    public ObjectId addContactListItem(final String userName,
                                       final String password,
                                       final String siteName,
                                       final String contactListTitle,
                                       final String firstName,
                                       final String lastName,
                                       final String email,
                                       final String company,
                                       final String jobTitle,
                                       final String phoneOffice,
                                       final String phoneMobile,
                                       final String notes)
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        UUID uuid = UUID.randomUUID();
        propertyMap.put(PropertyIds.OBJECT_TYPE_ID, "D:dl:contact");
        propertyMap.put(PropertyIds.NAME, uuid.toString());
        propertyMap.put("dl:contactFirstName", firstName);
        propertyMap.put("dl:contactLastName", lastName);
        propertyMap.put("dl:contactEmail", email);
        propertyMap.put("dl:contactCompany", company);
        propertyMap.put("dl:contactJobTitle", jobTitle);
        propertyMap.put("dl:contactPhoneOffice", phoneOffice);
        propertyMap.put("dl:contactPhoneMobile", phoneMobile);
        propertyMap.put("dl:contactNotes", notes);
        Session session = getCMISSession(userName, password);
        CmisObject listNodeRef = session.getObject(getDataListNodeRef(userName, password, siteName, contactListTitle));
        return addItem(userName, password, listNodeRef, propertyMap);
    }

    /**
     * Add a new event agenda item.
     * @param userName String user name
     * @param password String password
     * @param siteName String site where the event agenda was created
     * @param eventAgendaTitle String event agenda title
     * @param reference String event reference
     * @param startTime String start time
     * @param endTime String end time
     * @param sessionName String session name
     * @param presenter String event presenter
     * @param audience String event audience
     * @param notes String notes
     * @param docsToAttach List<String> documents name to attach from siteName
     * @return {@link ObjectId} of new created item
     */
    public ObjectId addEventAgendaItem(final String userName,
                                       final String password,
                                       final String siteName,
                                       final String eventAgendaTitle,
                                       final String reference,
                                       final String startTime,
                                       final String endTime,
                                       final String sessionName,
                                       final String presenter,
                                       final String audience,
                                       final String notes,
                                       final List<String> docsToAttach)
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        UUID uuid = UUID.randomUUID();
        propertyMap.put(PropertyIds.OBJECT_TYPE_ID, "D:dl:eventAgenda");
        propertyMap.put(PropertyIds.NAME, uuid.toString());
        propertyMap.put("dl:eventAgendaRef", reference);
        propertyMap.put("dl:eventAgendaStartTime", startTime);
        propertyMap.put("dl:eventAgendaEndTime", endTime);
        propertyMap.put("dl:eventAgendaSessionName", sessionName);
        propertyMap.put("dl:eventAgendaPresenter", presenter);
        propertyMap.put("dl:eventAgendaAudience", audience);
        propertyMap.put("dl:eventAgendaNotes", notes);
        Session session = getCMISSession(userName, password);
        CmisObject listNodeRef = session.getObject(getDataListNodeRef(userName, password, siteName, eventAgendaTitle));
        // create the item
        ObjectId itemId = addItem(userName, password, listNodeRef, propertyMap);
        if(!docsToAttach.isEmpty())
        {
            attachDocuments(userName, password, siteName, docsToAttach, itemId);
        }
        return itemId;
    }
    
    /**
     * Create event list item
     * @param userName String user name
     * @param password String password
     * @param siteName String site where data list is created
     * @param eventListTitle String event list title
     * @param itemTitle String item title
     * @param description String description
     * @param location String location
     * @param startDate Date start date
     * @param endDate Date end date
     * @param registration String registration
     * @param notes String notes
     * @param docsToAttach List<String> docs name to attach from siteName
     * @return {@link ObjectId} of new created item
     */
    public ObjectId addEventListItem(final String userName,
                                     final String password,
                                     final String siteName,
                                     final String eventListTitle,
                                     final String itemTitle,
                                     final String description,
                                     final String location,
                                     Date startDate,
                                     Date endDate,
                                     final String registration,
                                     final String notes,
                                     final List<String> docsToAttach)
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        UUID uuid = UUID.randomUUID();
        propertyMap.put(PropertyIds.OBJECT_TYPE_ID, "D:dl:event");
        propertyMap.put(PropertyIds.NAME, uuid.toString());
        propertyMap.put(PropertyIds.DESCRIPTION, description);
        propertyMap.put("dl:eventLocation", location);
        propertyMap.put("dl:eventStartDate", startDate);
        propertyMap.put("dl:eventEndDate", endDate);
        propertyMap.put("dl:eventRegistrations", registration);
        propertyMap.put("dl:eventNote", notes);
        Session session = getCMISSession(userName, password);
        CmisObject listNodeRef = session.getObject(getDataListNodeRef(userName, password, siteName, eventListTitle));
        // create the item
        ObjectId itemId = addItem(userName, password, listNodeRef, propertyMap);
        String eventListName = getDataListName(userName, password, siteName, eventListTitle);
        Document newItem = (Document) session.getObjectByPath("/sites/" + siteName + "/datalists/" + eventListName + "/" + uuid.toString());
        Map<String, Object> newProp = new HashMap<String, Object>();
        newProp.put("cm:title", itemTitle);
        newItem.updateProperties(newProp);
        if(!docsToAttach.isEmpty())
        {
            attachDocuments(userName, password, siteName, docsToAttach, itemId);
        }
        return itemId;
    }
    
    /**
     * Create Issue List item
     *
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param dataListTitle String issue list title
     * @param issueId String issue item id
     * @param issueTitle String issue item title
     * @param assignedTo List<String> assign to users
     * @param status Status issue status
     * @param priority Priority issue priority
     * @param description String description
     * @param dueDate Date due date
     * @param comments String issue comments
     * @param docsToAttach List<String> documents to attach to issue
     * @return {@link ObjectId} of new created item
     */
    public ObjectId addIssueListItem(final String userName,
                                     final String password,
                                     final String siteName,
                                     final String issueListTitle,
                                     final String issueId,
                                     final String issueTitle,
                                     final List<String> assignedTo,
                                     final Status status,
                                     final Priority priority,
                                     final String description,
                                     Date dueDate,
                                     final String comments,
                                     final List<String> docsToAttach)
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        UUID uuid = UUID.randomUUID();
        propertyMap.put(PropertyIds.OBJECT_TYPE_ID, "D:dl:issue");
        propertyMap.put(PropertyIds.NAME, uuid.toString());
        propertyMap.put(PropertyIds.DESCRIPTION, description);
        propertyMap.put("dl:issueID", issueId);
        propertyMap.put("dl:issueStatus", status.getValue());
        propertyMap.put("dl:issuePriority", priority.name());
        propertyMap.put("dl:issueDueDate", dueDate);
        propertyMap.put("dl:issueComments", comments);
        Session session = getCMISSession(userName, password);
        CmisObject listNodeRef = session.getObject(getDataListNodeRef(userName, password, siteName, issueListTitle));
        // create the item
        ObjectId itemId = addItem(userName, password, listNodeRef, propertyMap);
        String eventListName = getDataListName(userName, password, siteName, issueListTitle);
        Document newItem = (Document) session.getObjectByPath("/sites/" + siteName + "/datalists/" + eventListName + "/" + uuid.toString());
        Map<String, Object> newProp = new HashMap<String, Object>();
        newProp.put("cm:title", issueTitle);
        newItem.updateProperties(newProp);
        if(!docsToAttach.isEmpty())
        {
            attachDocuments(userName, password, siteName, docsToAttach, itemId);
        }
        if(!assignedTo.isEmpty())
        {
            assignUser(userName, password, assignedTo, itemId, "R:dl:issueAssignedTo");
        }
        return itemId;
    }
    
    /**
     * Create location list item.
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param locationListTitle String location list title
     * @param itemTitle String location item title
     * @param addressLine1 String address line 1
     * @param addressLine2 String address line 2
     * @param addressLine3 String address line 3
     * @param zipCode String zip code
     * @param state String state
     * @param country String country
     * @param description String description
     * @param docsToAttach List<String> documents to attach to item
     * @return {@link ObjectId} of new created item
     */
    public ObjectId addLocationItem(final String userName,
                                    final String password,
                                    final String siteName,
                                    final String locationListTitle,
                                    final String itemTitle,
                                    final String addressLine1,
                                    final String addressLine2,
                                    final String addressLine3,
                                    final String zipCode,
                                    final String state,
                                    final String country,
                                    final String description,
                                    final List<String> docsToAttach)
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        UUID uuid = UUID.randomUUID();
        propertyMap.put(PropertyIds.OBJECT_TYPE_ID, "D:dl:location");
        propertyMap.put(PropertyIds.NAME, uuid.toString());
        propertyMap.put(PropertyIds.DESCRIPTION, description);
        propertyMap.put("dl:locationAddress1", addressLine1);
        propertyMap.put("dl:locationAddress2", addressLine2);
        propertyMap.put("dl:locationAddress3", addressLine3);
        propertyMap.put("dl:locationAddress3", addressLine3);
        propertyMap.put("dl:locationZip", zipCode);
        propertyMap.put("dl:locationState", state);
        propertyMap.put("dl:locationCountry", country);
        Session session = getCMISSession(userName, password);
        List<String> ids = getDataListIds(userName, password, siteName, locationListTitle);
        CmisObject listNodeRef = session.getObject(ids.get(0));
        ObjectId itemId = addItem(userName, password, listNodeRef, propertyMap);
        Document newItem = (Document) session.getObjectByPath("/sites/" + siteName + "/datalists/" + ids.get(1) + "/" + uuid.toString());
        Map<String, Object> newProp = new HashMap<String, Object>();
        newProp.put("cm:title", itemTitle);
        newItem.updateProperties(newProp);
        if(!docsToAttach.isEmpty())
        {
            attachDocuments(userName, password, siteName, docsToAttach, itemId);
        }
        return itemId;
    }
    
    /**
     * Create event meeting item
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param meetingListTitle String meeting list title
     * @param itemReference String item reference
     * @param itemTitle String item title
     * @param description String item description
     * @param time String time
     * @param owner String owner
     * @param docsToAttach List<String> documents to attach to item
     * @return {@link ObjectId} of new created item
     */
    public ObjectId addMeetingItem(final String userName,
                                   final String password,
                                   final String siteName,
                                   final String meetingListTitle,
                                   final String itemReference,
                                   final String itemTitle,
                                   final String description,
                                   final String time,
                                   final String owner,
                                   final List<String> docsToAttach)
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        UUID uuid = UUID.randomUUID();
        propertyMap.put(PropertyIds.OBJECT_TYPE_ID, "D:dl:meetingAgenda");
        propertyMap.put(PropertyIds.NAME, uuid.toString());
        propertyMap.put(PropertyIds.DESCRIPTION, description);
        propertyMap.put("dl:meetingAgendaRef", itemReference);
        propertyMap.put("dl:meetingAgendaTime", time);
        propertyMap.put("dl:meetingAgendaOwner", owner);
        Session session = getCMISSession(userName, password);
        List<String> ids = getDataListIds(userName, password, siteName, meetingListTitle);
        CmisObject listNodeRef = session.getObject(ids.get(0));
        ObjectId itemId = addItem(userName, password, listNodeRef, propertyMap);
        Document newItem = (Document) session.getObjectByPath("/sites/" + siteName + "/datalists/" + ids.get(1) + "/" + uuid.toString());
        Map<String, Object> newProp = new HashMap<String, Object>();
        newProp.put("cm:title", itemTitle);
        newItem.updateProperties(newProp);
        if(!docsToAttach.isEmpty())
        {
            attachDocuments(userName, password, siteName, docsToAttach, itemId);
        }
        return itemId;
    }
    
    /**
     * Create new task list advanced item
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param taskAdvListTitle String advance list title
     * @param itemTitle String new item title
     * @param description String description
     * @param startDate Date start date
     * @param endDate Date end date
     * @param assignedTo List<String> assign to users
     * @param priority Priority item priority
     * @param status Status item stats
     * @param complete int complete (0-100)
     * @param comments String item comments
     * @param docsToAttach List<String> documents to attach to item
     * @return {@link ObjectId} of new created item
     */
    public ObjectId addTaskAdvancedItem(final String userName,
                                        final String password,
                                        final String siteName,
                                        final String taskAdvListTitle,
                                        final String itemTitle,
                                        final String description,
                                        final Date startDate,
                                        final Date endDate,
                                        final List<String> assignedTo,
                                        final Priority priority,
                                        final Status status,
                                        final int complete,
                                        final String comments,
                                        final List<String> docsToAttach)
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        UUID uuid = UUID.randomUUID();
        propertyMap.put(PropertyIds.OBJECT_TYPE_ID, "D:dl:task");
        propertyMap.put(PropertyIds.NAME, uuid.toString());
        propertyMap.put(PropertyIds.DESCRIPTION, description);
        propertyMap.put("dl:taskPriority", priority.name());
        propertyMap.put("dl:taskStatus", status.getValue());
        propertyMap.put("dl:taskComments", comments);
        Session session = getCMISSession(userName, password);
        List<String> ids = getDataListIds(userName, password, siteName, taskAdvListTitle);
        CmisObject listNodeRef = session.getObject(ids.get(0));
        ObjectId itemId = addItem(userName, password, listNodeRef, propertyMap);
        Document newItem = (Document) session.getObjectByPath("/sites/" + siteName + "/datalists/" + ids.get(1) + "/" + uuid.toString());
        Map<String, Object> newProp = new HashMap<String, Object>();
        newProp.put("cm:title", itemTitle);
        newProp.put("dl:ganttStartDate", startDate);
        newProp.put("dl:ganttEndDate", endDate);
        if(complete < 0 || complete > 100)
        {
            throw new RuntimeException("'Complete' must be between 0 and 100");
        }
        newProp.put("dl:ganttPercentComplete", complete);
        newItem.updateProperties(newProp);
        if(!docsToAttach.isEmpty())
        {
            attachDocuments(userName, password, siteName, docsToAttach, itemId);
        }
        if(!assignedTo.isEmpty())
        {
            assignUser(userName, password, assignedTo, itemId, "R:dl:taskAssignee");
        }
        return itemId;
    }
    
    /**
     * Create new task simple item
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param taskSimpleListTitle String task list simple title
     * @param itemTitle String new item title
     * @param description String description
     * @param dueDate Date due date
     * @param priority Priority item priority
     * @param status Status item status
     * @param comments String comments
     * @return {@link ObjectId} of new created item
     */
    public ObjectId addTaskSimpleItem(final String userName,
                                      final String password,
                                      final String siteName,
                                      final String taskSimpleListTitle,
                                      final String itemTitle,
                                      final String description,
                                      final Date dueDate,
                                      final Priority priority,
                                      final Status status,
                                      final String comments)
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        UUID uuid = UUID.randomUUID();
        propertyMap.put(PropertyIds.OBJECT_TYPE_ID, "D:dl:simpletask");
        propertyMap.put(PropertyIds.NAME, uuid.toString());
        propertyMap.put(PropertyIds.DESCRIPTION, description);
        propertyMap.put("dl:simpletaskPriority", priority.name());
        propertyMap.put("dl:simpletaskStatus", status.getValue());
        propertyMap.put("dl:simpletaskDueDate", dueDate);
        propertyMap.put("dl:simpletaskComments", comments);
        Session session = getCMISSession(userName, password);
        List<String> ids = getDataListIds(userName, password, siteName, taskSimpleListTitle);
        CmisObject listNodeRef = session.getObject(ids.get(0));
        ObjectId itemId = addItem(userName, password, listNodeRef, propertyMap);
        Document newItem = (Document) session.getObjectByPath("/sites/" + siteName + "/datalists/" + ids.get(1) + "/" + uuid.toString());
        Map<String, Object> newProp = new HashMap<String, Object>();
        newProp.put("cm:title", itemTitle);
        newItem.updateProperties(newProp);
        return itemId;
    }
    
    /**
     * Create to do item
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param toDoListTitle String to do list title
     * @param itemTitle String item title
     * @param dueDate Date due date
     * @param priority int priority
     * @param status Status item status
     * @param notes String notes
     * @param assignedToUser String assigned user
     * @param docsToAttach List<String> documents to attach to item
     * @return {@link ObjectId} of new created item
     */
    public ObjectId addToDoItem(final String userName,
                                final String password,
                                final String siteName,
                                final String toDoListTitle,
                                final String itemTitle,
                                final Date dueDate,
                                final int priority,
                                final Status status,
                                final String notes,
                                final String assignedToUser,
                                final List<String> docsToAttach)
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        UUID uuid = UUID.randomUUID();
        propertyMap.put(PropertyIds.OBJECT_TYPE_ID, "D:dl:todoList");
        propertyMap.put(PropertyIds.NAME, uuid.toString());
        propertyMap.put("dl:todoTitle", itemTitle);
        propertyMap.put("dl:todoDueDate", dueDate);
        propertyMap.put("dl:todoPriority", priority);
        propertyMap.put("dl:todoStatus", status.getValue());
        propertyMap.put("dl:todoNotes", notes);
        Session session = getCMISSession(userName, password);
        List<String> ids = getDataListIds(userName, password, siteName, toDoListTitle);
        CmisObject listNodeRef = session.getObject(ids.get(0));
        ObjectId itemId = addItem(userName, password, listNodeRef, propertyMap);
        if(!StringUtils.isEmpty(assignedToUser))
        {
            List<String> user = new ArrayList<String>();
            user.add(assignedToUser);
            assignUser(userName, password, user, itemId, "R:dl:assignee");
        }
        if(!docsToAttach.isEmpty())
        {
            for(int i = 0; i<docsToAttach.size(); i++)
            {
                String docNodRef = getNodeRef(session, siteName, docsToAttach.get(i));
                if(StringUtils.isEmpty(docNodRef))
                {
                    throw new CmisRuntimeException(docsToAttach.get(i) + " doesn't exist");
                }
                Map<String,Object> relProps = new HashMap<String, Object>();
                relProps.put(PropertyIds.OBJECT_TYPE_ID, "R:dl:attachments");
                relProps.put(PropertyIds.SOURCE_ID, itemId.getId());
                relProps.put(PropertyIds.TARGET_ID, docNodRef);
                session.createRelationship(relProps);
            }
        }
        return itemId;
    }
    
    /**
     * Assign a user to a data list item
     * 
     * @param assigner String user that will assign someone
     * @param passwordAssigner String password
     * @param usersToAssign List<String> user names to be assigned
     * @param assignTo ObjectId the object that will be assigned
     * @param objectTypeId String type id
     */
    private void assignUser(final String assigner,
                            final String passwordAssigner,
                            final List<String>usersToAssign,
                            final ObjectId assignTo,
                            final String objectTypeId)
    {
        Session session = getCMISSession(assigner, passwordAssigner);
        for(int i = 0; i<usersToAssign.size(); i++)
        {
            String usrNodeRef = getUserNodeRef(assigner, passwordAssigner, usersToAssign.get(i));
            if(StringUtils.isEmpty(usrNodeRef))
            {
                throw new RuntimeException(usersToAssign.get(i) + " doesn't exist");
            }
            Map<String,Object> relProps = new HashMap<String, Object>();
            relProps.put(PropertyIds.OBJECT_TYPE_ID, objectTypeId);
            relProps.put(PropertyIds.SOURCE_ID, assignTo.getId());
            relProps.put(PropertyIds.TARGET_ID, usrNodeRef);
            session.createRelationship(relProps);
            session.clear();
        }
    }
}
