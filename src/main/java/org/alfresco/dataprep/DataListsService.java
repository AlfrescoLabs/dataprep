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
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
     * @throws Exception 
     */
    public ObjectId createDataList(final String userName,
                                   final String password,
                                   final String siteName,
                                   final DataList listType,
                                   final String listName,
                                   final String description) throws Exception
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
     * @throws Exception if error
     */
    private HttpResponse getDataLists(final String userName,
                                      final String password,
                                      final String siteName) throws Exception
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String url = client.getAlfrescoUrl() + "alfresco/s/slingshot/datalists/lists/site/" + siteName + "/dataLists";
        HttpGet get = new HttpGet(url);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(get);
            return response;
        }
        finally
        {
            client.close();
        }
    }
    
    /**
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param dataListTitle
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private List<String> getDataListIds(final String userName,
                                        final String password,
                                        final String siteName,
                                        final String dataListTitle) throws Exception
    {
        List<String>ids = new ArrayList<String>();
        HttpResponse response = getDataLists(userName, password, siteName);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                String strResponse = EntityUtils.toString(response.getEntity());
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(strResponse);
                JSONObject jsonObject = (JSONObject) obj;
                org.json.simple.JSONArray jArray = (org.json.simple.JSONArray) jsonObject.get("datalists");
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
    
    public String getDataListNodeRef(final String userName,
                                     final String password,
                                     final String siteName,
                                     final String dataListTitle) throws Exception
    {
        List<String> ids = getDataListIds(userName, password, siteName, dataListTitle);
        return ids.get(0);
    }
    
    public String getDataListName(final String userName,
                                  final String password,
                                  final String siteName,
                                  final String dataListTitle) throws Exception
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
     * @throws Exception if error
     */
    private ObjectId addItem(final String userName,
                            final String password,
                            final ObjectId objectId,
                            Map<String, Object> propertiesMap) throws Exception
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
     * @return
     * @throws Exception
     */
    public ObjectId addContactListItem(final String userName,
                                       final String password,
                                       final String siteName,
                                       final String contactListName,
                                       final String firstName,
                                       final String lastName,
                                       final String email,
                                       final String company,
                                       final String jobTitle,
                                       final String phoneOffice,
                                       final String phoneMobile,
                                       final String notes) throws Exception
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
        CmisObject listNodeRef = session.getObject(getDataListNodeRef(userName, password, siteName, contactListName));
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
     * @return ObjectId event agenda item
     * @throws Exception
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
                                       final List<String> docsToAttach) throws Exception
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
     * @return ObjectId of the created item
     * @throws Exception if error
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
                                     final List<String> docsToAttach) throws Exception
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
    
    public ObjectId addIssueListItem(final String userName,
                                     final String password,
                                     final String siteName,
                                     final String dataListTitle,
                                     final String issueId,
                                     final String issueTitle,
                                     final List<String> assignedTo,
                                     final String status,
                                     final String priority,
                                     final String description,
                                     Date dueDate,
                                     final String comments,
                                     final List<String> docsToAttach) throws Exception
    {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        UUID uuid = UUID.randomUUID();
        propertyMap.put(PropertyIds.OBJECT_TYPE_ID, "D:dl:issue");
        propertyMap.put(PropertyIds.NAME, uuid.toString());
        propertyMap.put(PropertyIds.DESCRIPTION, description);
        propertyMap.put("dl:issueID", issueId);
        propertyMap.put("dl:issueStatus", status);
        propertyMap.put("dl:issuePriority", priority);
        propertyMap.put("dl:issueDueDate", dueDate);
        propertyMap.put("dl:issueComments", comments);
        Session session = getCMISSession(userName, password);
        CmisObject listNodeRef = session.getObject(getDataListNodeRef(userName, password, siteName, dataListTitle));
        // create the item
        ObjectId itemId = addItem(userName, password, listNodeRef, propertyMap);
        String eventListName = getDataListName(userName, password, siteName, dataListTitle);
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
     * Assign a user to a data list item
     * 
     * @param assigner String user that will assign someone
     * @param passwordAssigner String password
     * @param usersToAssign List<String> user names to be assigned
     * @param assignTo ObjectId the object that will be assigned
     * @param objectTypeId String type
     * @throws Exception if error
     */
    private void assignUser(final String assigner,
                            final String passwordAssigner,
                            final List<String>usersToAssign,
                            final ObjectId assignTo,
                            final String objectTypeId) throws Exception
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
