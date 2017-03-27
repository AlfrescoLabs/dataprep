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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;
import org.apache.commons.io.FilenameUtils;
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
        UNDEFINED("N#A", ""),
        TEXT_PLAIN("text/plain", "txt"),
        XML("text/xml", "xml"),
        HTML("text/html", "html"),
        PDF("application/pdf", "pdf"),
        MSWORD("application/msword", "doc"),
        MSWORD2007("application/msword", "docx"),
        MSEXCEL( "application/vnd.ms-excel", "xls"),
        MSEXCEL2007("application/vnd.ms-excel", "xlsx"),
        MSPOWERPOINT("application/vnd.ms-powerpoint", "ppt"),
        MSPOWERPOINT2007("application/vnd.ms-powerpoint", "pptx");
        public final String type;
        public final String extention;
        DocumentType(String type, String extention)
        {
            this.type = type;
            this.extention = extention;
        }
        
        public static DocumentType fromName(String fileName)
        {
            String extension = FilenameUtils.getExtension(fileName);
            for(DocumentType docType : DocumentType.values())
            {
                if (docType.extention.equals(extension))
                    return docType;
            }
            return DocumentType.UNDEFINED;
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
        RESTRICTABLE("Restrictable", "P:dp:restrictable"),
        SYSTEM_SMART_FOLDER("System Smart Folder", "smf:systemConfigSmartFolder");
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
        High(1),
        Normal(2),
        Low(3);
        private int level;
        private Priority(int level)
        {
            this.level = level;
        }
        public int getLevel()
        {
            return this.level;
        }
    }
    
    @Autowired protected  AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    Map<String, String> contents = new HashMap<String,String>();

    /**
     * Method to get a CMIS session.
     * 
     * @param userName String identifier
     * @param password String password
     * @return Session the session
     * @throws CmisRuntimeException if invalid user and password
     */
    public Session getCMISSession(final String userName,
                                  final String password)
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
     * @throws CmisRuntimeException if site is not found
     */
    public String getNodeRef(final String userName,
                             final String password,
                             final String siteName,
                             final String contentName)
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
     * @throws CmisRuntimeException if site is not found
     */
    public String getNodeRef(final Session session,
                             final String siteName,
                             final String contentName)
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
     * Get the node ref for a item by path
     * @param userName String user
     * @param password String password
     * @param pathToContent String path to item (e.g. /Sites/siteId/documentLibrary/doc.txt)
     * @return String node ref of the item
     */
    public String getNodeRefByPath(final String userName,
                                   final String password,
                                   String pathToContent)
    {
        Session session = getCMISSession(userName, password);
        return getNodeRefByPath(session, pathToContent);
    }
    
    /**
     * Get the node ref for a item by path
     * @param session the session
     * @param pathToContent String path to item (e.g. /Sites/siteId/documentLibrary/doc.txt)
     * @return String node ref of the item
     */
    public String getNodeRefByPath(Session session,
                                   String pathToContent)
    {
        if(StringUtils.isEmpty(pathToContent))
        {
            throw new CmisRuntimeException("Path to content is missing");
        }
        try
        {
            if(!StringUtils.startsWith(pathToContent, "/"))
            {
                pathToContent = "/" + pathToContent;
            }
            if(StringUtils.endsWith(pathToContent, "/"))
            {
                pathToContent = StringUtils.removeEnd(pathToContent, "/");
            }
            CmisObject content = session.getObjectByPath(pathToContent);
            return content.getId().split(";")[0];
        }
        catch(CmisObjectNotFoundException nf)
        {
            return "";
        }
    }
    
    /**
     * Method to add aspect
     *
     * @param session cmis session
     * @param contentNodeRef String node identifier
     * @param documentAspects aspect to apply on node
     */
    protected void addAspect(final Session session,
                             final String contentNodeRef,
                             List<DocumentAspect> documentAspects)
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
     * Method to add aspect
     *
     * @param session cmis session
     * @param contentNodeRef String node identifier
     * @param aspectPropertyName aspect to apply on node
     */
    protected void addAspect(final Session session,
                             final String contentNodeRef,
                             final String... aspectPropertiesName)
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
            for(String aspect : aspectPropertiesName)
            {
                secondaryTypes.add(aspect);
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
     */
    public void addProperties(final Session session,
                              final String contentNodeRef,
                              Map<String, Object> propertiesMap)
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
     * Method to add properties for aspects by path
     *
     * @param session {@link Session}
     * @param contentNodeRef String node identifier
     * @param propertiesMap Map of properties
     */
    public void addPropertiesByPath(final Session session,
                                    final String pathToContent,
                                    Map<String, Object> propertiesMap)
    {
        try
        {
            CmisObject contentObj = session.getObjectByPath(pathToContent);
            contentObj.updateProperties(propertiesMap);
        }
        catch(CmisInvalidArgumentException ia)
        {
            throw new CmisRuntimeException("Invalid content from" + pathToContent, ia);
        }
    }
    
    public void addProperties(final String userName,
                              final String password,
                              final String pathToContent,
                              Map<String, Object> properties)
    {
        Session session = getCMISSession(userName, password);
        CmisObject content = session.getObjectByPath(pathToContent);
        content.updateProperties(properties);
    }
    
    private List<Property<?>> getProperties(final Session session,
                                            final String siteName,
                                            final String contentName,
                                            final boolean byPath,
                                            final String pathToContent)
    {
        String nodeRef;
        if(byPath)
        {
            nodeRef = getNodeRefByPath(session, pathToContent);
        }
        else
        {
            nodeRef = getNodeRef(session, siteName, contentName);
        }
        
        CmisObject obj = session.getObject(nodeRef);
        return obj.getProperties();
    }

    /**
     * Method to get all object properties for content from site
     *
     * @param session {@link Session}
     * @param siteName String site identifier
     * @param contentName String content identifier
     * @return {@link Property} list of content properties
     */
    public List<Property<?>> getProperties(final Session session,
                                           final String siteName,
                                           final String contentName)
    {
        return getProperties(session, siteName, contentName, false, null);
    }
    
    /**
     * Method to get all object properties for content from site
     *
     * @param userName String identifier
     * @param password String password
     * @param siteName String site identifier
     * @param contentName String content identifier
     * @return {@link Property} list of content properties
     */
    public List<Property<?>> getProperties(final String userName,
                                           final String password,
                                           final String siteName,
                                           final String contentName)
    {
        Session session = getCMISSession(userName, password);
        return getProperties(session, siteName, contentName, false, null);
    }
    
    /**
     * Method to get all object properties for content by path
     *
     * @param userName String identifier
     * @param password String password
     * @param pathToContent 
     * @return {@link Property} list of content properties
     */
    public List<Property<?>> getProperties(final String userName,
                                           final String password,
                                           final String pathToContent)
    {
        Session session = getCMISSession(userName, password);
        return getProperties(session, null, null, true, pathToContent);
    }
    
    /**
     * Method to get all object properties for content by path
     *
     * @param session {@link Session}
     * @param pathToContent
     * @return {@link Property} list of content properties
     */
    public List<Property<?>> getProperties(final Session session,
                                           final String pathToContent)
    {
        return getProperties(session, null, null, true, pathToContent);
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
     */
    public String getCategoryNodeRef(final String userName,
                                     final String password,
                                     final String categoryName)
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
     */
    public String getUserNodeRef(final String userManager,
                                 final String password,
                                 final String searchedUser)
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
     * Method to attach a document to an existent object
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param docsToAttach String document to attach
     * @param attachToObj ObjectId attach to object
     */
    public void attachDocuments(final String userName,
                                final String password,
                                final String siteName,
                                final List<String> docsToAttach,
                                final ObjectId attachToObj)
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
    protected void waitInSeconds(int seconds)
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
     */
    public CmisObject getCmisObject(final Session session,
                                    final String siteId,
                                    final String contentName)
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
     * Get cmis object by path
     * 
     * @param session {@link Session} the session
     * @param pathToItem String path to item
     * @return CmisObject cmis object
     */
    public CmisObject getCmisObject(final Session session,
                                    String pathToItem)
    {
        try
        {
            if(!StringUtils.startsWith(pathToItem, "/"))
            {
                pathToItem = "/" + pathToItem;
            }
            if(StringUtils.endsWith(pathToItem, "/"))
            {
                pathToItem = StringUtils.removeEnd(pathToItem, "/");
            }
            return session.getObjectByPath(pathToItem);
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Path doesn't exist " + pathToItem);
        }
    }
    
    /**
     * Get cmis object by path
     * 
     * @param userName String user name
     * @param password String user password
     * @param pathToItem String path to item
     * @return CmisObject cmis object
     */
    public CmisObject getCmisObject(final String userName,
                                    final String password,
                                    String pathToItem)
    {
        try
        {
            if(!StringUtils.startsWith(pathToItem, "/"))
            {
                pathToItem = "/" + pathToItem;
            }
            if(StringUtils.endsWith(pathToItem, "/"))
            {
                pathToItem = StringUtils.removeEnd(pathToItem, "/");
            }
            Session session = getCMISSession(userName, password);
            return session.getObjectByPath(pathToItem);
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Path doesn't exist " + pathToItem);
        }
    }
    
    /**
     * Get Document object for a file
     *
     * @param session the session
     * @param siteId site id
     * @param fileName file name
     * @return CmisObject cmis object
     */
    public Document getDocumentObject(final Session session,
                                      final String siteId,
                                      final String fileName)
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
     * Get Document object for a file
     *
     * @param session {@link Session}
     * @param pathToDocument path to document
     * @return {@link Document}
     */
    public Document getDocumentObject(final Session session,
                                      final String pathToDocument)
    {
        Document d = null;
       
        CmisObject docObj = getCmisObject(session, pathToDocument);
        if(docObj instanceof Document)
        {
            d = (Document)docObj;
        }
        else if(docObj instanceof Folder)
        {
            throw new CmisRuntimeException("Content from " + pathToDocument + " is not a document");
        }
        return d;
    }
    
    /**
     * Get Document object for a file
     *
     * @param userName
     * @param password
     * @param pathToDocument path to document
     * @return {@link Document}
     */
    public Document getDocumentObject(final String userName,
                                      final String password,
                                      final String pathToDocument)
    {
        Session session = getCMISSession(userName, password);
        return getDocumentObject(session, pathToDocument);
    }
    
    /**
     * Get Folder object for a folder
     *
     * @param session the session
     * @param siteId site id
     * @param folderName folder name
     * @return CmisObject cmis object
     */
    public Folder getFolderObject(final Session session,
                                  final String siteId,
                                  final String folderName)
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
    
    /**
     * Get Folder cmis object 
     *
     * @param session {@link Session}
     * @param pathToFolder path to folder
     * @return {@link Folder}
     */
    public Folder getFolderObject(final Session session,
                                  final String pathToFolder)
    {
        Folder f = null;
        CmisObject folderObj = getCmisObject(session, pathToFolder);
        if(folderObj instanceof Folder)
        {
            f = (Folder)folderObj;
        }
        else if(folderObj instanceof Document)
        {
            throw new CmisRuntimeException("Content from " + pathToFolder + " is not a folder");
        }
        return f;
    }
    
    /**
     * Get Folder cmis object 
     * 
     * @param userName 
     * @param password
     * @param pathToFolder
     * @return {@link Folder}
     */
    public Folder getFolderObject(final String userName,
                                  final String password,
                                  final String pathToFolder)
    {
        Session session = getCMISSession(userName, password);
        return getFolderObject(session, pathToFolder);
    }
    
    /**
     * Close streams
     * @param stream
     * @param contentStream
     */
    protected void closeStreams(InputStream stream,
                                ContentStream contentStream)
    {
        try
        {
            stream.close();
            contentStream.getStream().close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to close the stream", e);
        }
    }
}

