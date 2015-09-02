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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.springframework.stereotype.Service;

@Service
/**
 * Class to manage CRUD operations on data lists
 * 
 * @author Bocancea Bogdan
 */
public class DataListsService extends CMISUtil
{
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
        Session session = getCMISSession(userName, password);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PropertyIds.OBJECT_TYPE_ID, "F:dl:dataList");
        props.put("dl:dataListItemType", listType.listTypeId);
        props.put(PropertyIds.NAME, listName);
        props.put(PropertyIds.DESCRIPTION, description);
        Folder fold = (Folder) session.getObjectByPath("/sites/" + siteName + "/datalists");
        ObjectId id = session.createFolder(props, fold);
        return id;
    }

    /**
     * Get the name (id) for blog post, link, discussion
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param title String blog title
     * @param draftBlog boolean is blog draft
     * @return String name (id)
     * @throws Exception if error
     */
    public HttpResponse getDataLists(final String userName,
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
            get.releaseConnection();
            client.close();
        }
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
    public ObjectId addItem(final String userName,
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
                                       final ObjectId list,
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
        return addItem(userName, password, list, propertyMap);
    }

    /**
     * Add a new event agenda item.
     * @param userName String user name
     * @param password String password
     * @param list ObjectId list created
     * @param reference String event reference
     * @param startTime String start time
     * @param endTime String end time
     * @param sessionName String session name
     * @param presenter String event presenter
     * @param audience String event audience
     * @param notes String notes
     * @param attachementsSite String site name from which to attach docs
     * @param docsToAttach List<String> documents name to attach
     * @return ObjectId event agenda item
     * @throws Exception
     */
    public ObjectId addEventAgendaItem(final String userName,
                                       final String password,
                                       final ObjectId list,
                                       final String reference,
                                       final String startTime,
                                       final String endTime,
                                       final String sessionName,
                                       final String presenter,
                                       final String audience,
                                       final String notes,
                                       final String attachementsSite,
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
        ObjectId itemId = addItem(userName, password, list, propertyMap);
        if(!docsToAttach.isEmpty())
        {
            attachDocuments(userName, password, attachementsSite, docsToAttach, itemId);
        }
        return itemId;
    }
}
