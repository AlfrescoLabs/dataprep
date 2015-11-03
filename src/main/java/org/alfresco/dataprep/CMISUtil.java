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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Helper class that provides CMIS utils.
 * 
 * @author Bogdan Bocancea
 */

public class CMISUtil
{
    public enum DocumentType
    {
        TEXT_PLAIN("text/plain"),
        XML("text/xml"),
        HTML("text/html"),
        PDF("application/pdf"),
        MSWORD("application/msword"),
        MSEXCEL( "application/vnd.ms-excel"),
        MSPOWERPOINT("application/vnd.ms-powerpoint");
        public final String type;
        DocumentType(String type)
        {
            this.type = type;
        }
    }
    
    /**
     * Different Aspects of Documents and folders.
     */
    public enum DocumentAspect
    {
        CLASSIFIABLE("Classifiable", "P:cm:generalclassifiable"),
        VERSIONABLE("Versionable", "P:cm:versionable"),
        AUDIO("Audio", "P:audio:audio"),
        INDEX_CONTROL("Index Control", "P:cm:indexControl"),
        COMPLIANCEABLE("Complianceable", "P:cm:complianceable"),
        DUBLIN_CORE("Dublin Core", "P:cm:dublincore"),
        EFFECTIVITY("Effectivity", "P:cm:effectivity"),
        SUMMARIZABLE("Summarizable", "P:cm:summarizable"),
        TEMPLATABLE("Templatable", "P:cm:templatable"),
        EMAILED("Emailed", "P:cm:emailed"),
        ALIASABLE_EMAIL("Email Alias", "P:emailserver:aliasable"),
        TAGGABLE("Taggable", "P:cm:taggable"),
        INLINE_EDITABLE("Inline Editable", "P:app:inlineeditable"),
        GEOGRAPHIC("Geographic", "P:cm:geographic"),
        EXIF("EXIF", "P:exif:exif"),
        RESTRICTABLE("Restrictable", "P:dp:restrictable");
        private String value;
        private String property;
        private DocumentAspect(String value, String property)
        {
            this.value = value;
            this.property = property;
        }

        public String getValue()
        {
            return this.value;
        }

        public String getProperty()
        {
            return this.property;
        }
    }
    
    public enum Status
    {
        NOT_STARTED("Not Started"),
        IN_PROGRESS("In Progress"),
        COMPLETE("Complete"),
        ON_HOLD("On Hold");
        private String value;
        private Status(String value)
        {
            this.value = value;
        }
        
        public String getValue()
        {
            return this.value;
        }
    }
    
    public enum Priority
    {
        High,
        Normal,
        Low
    }
    
    @Autowired protected  AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    @Autowired private UserService userService;
    Map<String, String> contents = new HashMap<String,String>();

    /**
     * Method to get a CMIS session.
     * 
     * @param userName String identifier
     * @param password String password
     * @return Session the session
     * @throws Exception if error
     */
    public Session getCMISSession(final String userName, 
                                  final String password) throws Exception
    {
        SessionFactory factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();
        // user credentials
        parameter.put(SessionParameter.USER, userName);
        parameter.put(SessionParameter.PASSWORD, password);
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String serviceUrl = client.getApiUrl().replace("service/", "") + "-default-/public/cmis/versions/1.1/browser";
        parameter.put(SessionParameter.BROWSER_URL, serviceUrl);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
        try
        {
            // create session
            List<Repository> repositories = factory.getRepositories(parameter);
            parameter.put(SessionParameter.REPOSITORY_ID, repositories.get(0).getId());
            Session session = repositories.get(0).createSession();
            return session;
        }
        catch (CmisUnauthorizedException unauthorized)
        {
            throw new CmisRuntimeException("Invalid user name and password", unauthorized);
        }
    }

    /**
     * Gets the object id for a document or folder.
     * 
     * @param userName String identifier
     * @param password String password
     * @param siteName String site identifier
     * @param contentName String content identifier
     * @return String node identifier
     * @throws Exception if error
     */
    public String getNodeRef(final String userName,
                             final String password,
                             final String siteName,
                             final String contentName) throws Exception
    {
        String nodeRef = "";
        contents.clear();
        Session session = getCMISSession(userName, password);
        try
        {
            Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
            for (Tree<FileableCmisObject> t : documentLibrary.getDescendants(-1)) 
            {
                contents = getId(t, contentName);
            }
            for (Map.Entry<String, String> entry : contents.entrySet()) 
            {
                if(entry.getKey().equalsIgnoreCase(contentName))
                {
                    nodeRef = entry.getValue().split(";")[0];
                    return nodeRef;
                }
            }
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Site doesn't exists: " + siteName, nf);
        }
        return nodeRef;
    }
    
    /**
     * Gets the object id for a document or folder.
     * 
     * @param session Session Cmis session
     * @param siteName String site identifier
     * @param contentName String content identifier
     * @return String node identifier
     * @throws Exception if error
     */
    public String getNodeRef(final Session session,
                             final String siteName,
                             final String contentName) throws Exception
    {
        String nodeRef = "";
        contents.clear();
        try
        {
            Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
            for (Tree<FileableCmisObject> t : documentLibrary.getDescendants(-1)) 
            {
                contents = getId(t, contentName);
            }
            for (Map.Entry<String, String> entry : contents.entrySet()) 
            {
                if(entry.getKey().equalsIgnoreCase(contentName))
                {
                    nodeRef = entry.getValue().split(";")[0];
                    return nodeRef;
                }
            }
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Site doesn't exists: " + siteName, nf);
        }
        return nodeRef;
    }

    private Map<String, String> getId(Tree<FileableCmisObject> tree,
                                      final String contentName)
    { 
        contents.put(tree.getItem().getName(), tree.getItem().getId());
        for (Tree<FileableCmisObject> t : tree.getChildren()) 
        {
            getId(t, contentName);
        }
        return contents;
    }

    /**
     * Gets the object id for a document or folder in repository
     * 
     * @param userName String identifier
     * @param password String password
     * @param contentName String content identifier
     * @param path String path to the object(e.g. 'Shared'). If empty or null 'Company home' is set.
     * @return String node identifier
     * @throws Exception if error
     */
    public String getNodeRefFromRepo(final String userName,
                                     final String password,
                                     final String contentName,
                                     String path) throws Exception
    {
        String nodeRef = "";
        Session session = getCMISSession(userName, password);
        if(path == null)
        {
            path = "";
        }
        try
        {
            CmisObject repo = session.getObjectByPath(session.getRootFolder().getPath() + "/" + path);
            Folder folder = (Folder) repo;
            ItemIterable<CmisObject> children = folder.getChildren();
            ItemIterable<CmisObject> page = children.getPage();
            Iterator<CmisObject> pageItems = page.iterator();
            while(pageItems.hasNext())
            {
                CmisObject item = pageItems.next();
                if(item.getName().equalsIgnoreCase(contentName))
                {
                    nodeRef = item.getId();
                    break;
                }
            }
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid path -> " + path, nf);
        }  
        return nodeRef;
    }
    
    /**
     * Method to add aspect
     *
     * @param userName String identifier
     * @param password String password
     * @param contentNodeRef String node identifier
     * @param documentAspects aspect to apply on node
     * @throws Exception if error
     */
    public void addAspect(final Session session,
                          final String contentNodeRef,
                          List<DocumentAspect> documentAspects) throws Exception
    {
        try
        {
            CmisObject contentObj = session.getObject(contentNodeRef);
            List<SecondaryType> secondaryTypesList = contentObj.getSecondaryTypes();
            List<String> secondaryTypes = new ArrayList<String>();
            if (secondaryTypesList != null)
            {
                for (SecondaryType secondaryType : secondaryTypesList)
                {
                    secondaryTypes.add(secondaryType.getId());
                }
            }
            for (DocumentAspect aspect : documentAspects)
            {
                secondaryTypes.add(aspect.getProperty());
            }
            Map<String, Object> properties = new HashMap<String, Object>();
            {
                properties.put(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, secondaryTypes);
            }
            contentObj.updateProperties(properties);
        }
        catch(CmisInvalidArgumentException ia)
        {
            throw new CmisRuntimeException("Invalid content " + contentNodeRef, ia);
        }
    }

    /**
     * Method to add properties for aspects
     *
     * @param userName String identifier
     * @param password String password
     * @param contentNodeRef String node identifier
     * @param propertiesMap Map of properties
     * @throws Exception if error
     */
    public void addProperties(final Session session,
                              final String contentNodeRef,
                              Map<String, Object> propertiesMap) throws Exception
    {
        try
        {
            CmisObject contentObj = session.getObject(contentNodeRef);
            contentObj.updateProperties(propertiesMap);
        }
        catch(CmisInvalidArgumentException ia)
        {
            throw new CmisRuntimeException("Invalid content " + contentNodeRef, ia);
        }
    }

    /**
     * Method to get all object properties
     *
     * @param userName String identifier
     * @param password String password
     * @param siteName String site identifier
     * @param contentName String content identifier
     * @return {@link Property} list of content properties
     * @throws Exception if error
     */
    public List<Property<?>> getProperties(final String userName,
                                           final String password,
                                           final String siteName,
                                           final String contentName) throws Exception
    {
        Session session = getCMISSession(userName, password);
        String nodeRef = getNodeRef(session, siteName, contentName);
        CmisObject obj = session.getObject(nodeRef);
        return obj.getProperties();
    }
    
    /**
     * Get a specific value from a property list
     * 
     * @param propertyList List of {@link Property}
     * @param propertyName String name of property
     * @return String property value
     */
    public String getPropertyValue(final List<Property<?>> propertyList, 
                                   final String propertyName)
    {
        String value = null;
        for (Property<?> property : propertyList)
        {
            if (property.getDefinition().getId().equals(propertyName))
            {
                value = property.getValueAsString();
                if (value==null)
                {
                    value="(None)";
                }
                break;
            }
        }
        return value;
    }
    
    /**
     * Get the list of values from a specific property list
     * 
     * @param propertyList List of {@link Property}
     * @param propertyName String name of property
     * @return List of values 
     */
    public List<?> getValues(final List<Property<?>> propertyList, 
                             final String propertyName)
    {
        List<?> values = new ArrayList<String>();
        for (Property<?> property : propertyList)
        {
            if (property.getDefinition().getId().equals(propertyName))
            {
                values = property.getValues();
                break;
            }
        }
        return values;
    }
    
    /**
     * Method to get the ID for a Category
     * @param userName String identifier
     * @param password String password
     * @param categoryName String category name
     * @return String node identifier
     * @throws Exception if error
     */
    public String getCategoryNodeRef(final String userName,
                                     final String password,
                                     final String categoryName) throws Exception
    {
        List<CmisObject> objList = new ArrayList<CmisObject>();
        String categoryNodeRef = "";
        Session session = getCMISSession(userName, password);
        // execute query
        ItemIterable<QueryResult> results = session.query("select cmis:objectId from cm:category where cmis:name = '" + categoryName + "'", false);
        for (QueryResult qResult : results) 
        {
            String objectId = "";
            PropertyData<?> propData = qResult.getPropertyById("cmis:objectId");
            objectId = (String) propData.getFirstValue();
            CmisObject obj = session.getObject(session.createObjectId(objectId));
            objList.add(obj);
        }
        for (CmisObject result : objList) 
        {
            if(result.getName().equalsIgnoreCase(categoryName))
            {
                categoryNodeRef = result.getId();
            }
        }
        return categoryNodeRef;
    }
    
    /**
     * 
     * @param userName String user name
     * @param password String password
     * @return String nodeRef of userName
     * @throws Exception
     */
    public String getUserNodeRef(final String userManager,
                                 final String password,
                                 final String searchedUser) throws Exception
    {
        Session session = getCMISSession(userManager, password);
        String objectId = "";
        ItemIterable<QueryResult> results = session.query("select cmis:objectId from cm:person where cm:userName = '" + searchedUser + "'", false);
        for (QueryResult qResult : results) 
        {
            PropertyData<?> propData = qResult.getPropertyById("cmis:objectId");
            objectId = "workspace://SpacesStore/" + (String) propData.getFirstValue();
        }
        return objectId;
    }
    
    /**
     * Method to attach a document
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param docsToAttach String document to attach
     * @param attachToObj ObjectId attach to object
     * @throws Exception
     */
    public void attachDocuments(final String userName,
                                final String password,
                                final String siteName,
                                final List<String> docsToAttach,
                                final ObjectId attachToObj) throws Exception
    {
        Session session = getCMISSession(userName, password);
        for(int i = 0; i<docsToAttach.size(); i++)
        {
            String docNodRef = getNodeRef(session, siteName, docsToAttach.get(i));
            if(StringUtils.isEmpty(docNodRef))
            {
                throw new CmisRuntimeException(docsToAttach.get(i) + " doesn't exist");
            }
            Map<String,Object> relProps = new HashMap<String, Object>();
            relProps.put(PropertyIds.OBJECT_TYPE_ID, "R:cm:attachments");
            relProps.put(PropertyIds.SOURCE_ID, attachToObj.getId());
            relProps.put(PropertyIds.TARGET_ID, docNodRef);
            session.createRelationship(relProps);
        }
    }
    
    /**
     * Method to wait for given seconds.
     * 
     * @param seconds time in seconds
     */
    public static void waitInSeconds(int seconds)
    {
        long time0;
        long time1;
        time0 = System.currentTimeMillis();
        do
        {
            time1 = System.currentTimeMillis();
        }
        while (time1 - time0 < seconds * 1000);
    }
    
    /**
     * Get Cmis Object for a file or folder
     * 
     * @param session the session
     * @param siteId site id
     * @param contentName file or folder name
     * @return CmisObject cmis object
     * @throws Exception if error
     */
    public CmisObject getCmisObject(final Session session,
                                    final String siteId,
                                    final String contentName) throws Exception
    {
        if (StringUtils.isEmpty(siteId) || StringUtils.isEmpty(contentName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        String nodeRef = getNodeRef(session, siteId, contentName);
        if(StringUtils.isEmpty(nodeRef))
        {
            throw new CmisRuntimeException("Content " + contentName + " doesn't exist");
        }
        CmisObject object = session.getObject(nodeRef);
        return object;
    }
    
    /**
     * Get Document object for a file
     *
     * @param session the session
     * @param siteId site id
     * @param contentName file or folder name
     * @return CmisObject cmis object
     * @throws Exception if error
     */
    public Document getDocumentObject(final Session session,
                                      final String siteId,
                                      final String fileName) throws Exception
    {
        Document d = null;
        CmisObject docObj = getCmisObject(session, siteId, fileName);
        if(docObj instanceof Document)
        {
            d = (Document)docObj;
        }
        else if(docObj instanceof Folder)
        {
            throw new CmisRuntimeException("Content " + fileName + " is not a document");
        }
        return d;
    }
    
    /**
     * Get Folder object for a folder
     *
     * @param session the session
     * @param siteId site id
     * @param contentName file or folder name
     * @return CmisObject cmis object
     * @throws Exception if error
     */
    public Folder getFolderObject(final Session session,
                                  final String siteId,
                                  final String folderName) throws Exception
    {
        Folder f = null;
        CmisObject folderObj = getCmisObject(session, siteId, folderName);
        if(folderObj instanceof Folder)
        {
            f = (Folder)folderObj;
        }
        else if(folderObj instanceof Document)
        {
            throw new CmisRuntimeException("Content " + folderName + " is not a folder");
        }
        return f;
    }
}

