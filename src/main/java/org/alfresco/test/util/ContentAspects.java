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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
@Service
/**
 * Class to add aspects to documents and folders
 * 
 * @author Bocancea Bogdan
 */
public class ContentAspects extends CMISUtil
{
    
    /**
     * Add aspect for document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param aspect aspect to add
     * @throws Exception if error 
     */
    public void addAspect(final String userName,
                          final String password,
                          final String siteName,
                          final String contentName,
                          DocumentAspect aspect) throws Exception
    {
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        List<DocumentAspect> aspectsToAdd = new ArrayList<DocumentAspect>();
        aspectsToAdd.add(aspect);      
        addAspect(userName, password, contentNodeRef, aspectsToAdd);
    }
    
    /**
     * Remove aspect from document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param aspectToRemove aspect to be removed
     * @throws Exception if error
     */
    public void removeAspect(final String userName,
                             final String password,
                             final String siteName,
                             final String contentName,
                             DocumentAspect aspectToRemove) throws Exception
    {
        try
        {
            String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
            Session session = getCMISSession(userName, password);
            CmisObject contentObj = session.getObject(contentNodeRef);
            List<SecondaryType> secondaryTypesList = contentObj.getSecondaryTypes();
            List<String> secondaryTypes = new ArrayList<String>();
            for (SecondaryType secondaryType : secondaryTypesList)
            {
                secondaryTypes.add(secondaryType.getId());
            }        
            secondaryTypes.remove(aspectToRemove.getProperty());
            Map<String, Object> properties = new HashMap<String, Object>();
            {
                properties.put(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, secondaryTypes);
            }
            contentObj.updateProperties(properties);  
        }
        catch(CmisInvalidArgumentException ia)
        {
            throw new CmisRuntimeException("Invalid content " + contentName, ia);
        }
    }
    
    /**
     * Method to add Complianceable aspect
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param removeAfter date value for RemoveAfter property
     * @throws Exception if error
     */
    public void addComplianceable(final String userName,
                                  final String password,
                                  final String siteName,
                                  final String contentName,
                                  final Date removeAfter) throws Exception
    {       
        addAspect(userName, password, siteName, contentName, DocumentAspect.COMPLIANCEABLE);
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("cm:removeAfter", removeAfter);
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        addProperties(userName, password, contentNodeRef, propertyMap);
    }
    
    /**
     * Method to add Dublin Core aspect
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param publisher value for publisher property
     * @param contributor value for contributor property
     * @param type value for type property
     * @param identifier value for identifier property
     * @param source value for source property
     * @param coverage value for coverage property
     * @param rights value for rights property
     * @param subject value for subject property
     * @throws Exception if error
     */
    public void addDublinCore(final String userName,
                              final String password,
                              final String siteName,
                              final String contentName,
                              final String publisher,
                              final String contributor,
                              final String type,
                              final String identifier,
                              final String source,
                              final String coverage,
                              final String rights,
                              final String subject) throws Exception
    {       
        addAspect(userName, password, siteName, contentName, DocumentAspect.DUBLIN_CORE);     
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("cm:contributor", contributor);
        propertyMap.put("cm:publisher", publisher);
        propertyMap.put("cm:subject", subject);
        propertyMap.put("cm:type", type);
        propertyMap.put("cm:identifier", identifier);
        propertyMap.put("cm:rights", rights);
        propertyMap.put("cm:coverage", coverage);
        propertyMap.put("cm:dcsource", source);
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        addProperties(userName, password, contentNodeRef, propertyMap);
    }
    
    /**
     * Method to add Effectivity aspect
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param fromDate date value for From property
     * @param toDate date value for To property
     * @throws Exception if error
     * 
     */
    public void addEffectivity(final String userName,
                               final String password,
                               final String siteName,
                               final String contentName,
                               final Date fromDate,
                               final Date toDate) throws Exception
    {
        addAspect(userName, password, siteName, contentName, DocumentAspect.EFFECTIVITY);
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("cm:from", fromDate);
        propertyMap.put("cm:to", toDate);
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        addProperties(userName, password, contentNodeRef, propertyMap);
    }
    
    /**
     * Method to add Geographic Aspect 
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param longitude value for longitude property 
     * @param latitude value for latitude property 
     * @throws Exception if error
     */
    public void addGeographicAspect(final String userName,
                                    final String password,
                                    final String siteName,
                                    final String contentName,
                                    final double longitude,
                                    final double latitude) throws Exception
    {
        addAspect(userName, password, siteName, contentName, DocumentAspect.GEOGRAPHIC);
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("cm:longitude", longitude);
        propertyMap.put("cm:latitude", latitude);
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        addProperties(userName, password, contentNodeRef, propertyMap);
    }
    
    /**
     * Method to add Summarizable Aspect 
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param summary value for summary property
     * @throws Exception if error
     * 
     */
    public void addSummarizable(final String userName,
                                final String password,
                                final String siteName,
                                final String contentName,
                                final String summary) throws Exception
    {
        addAspect(userName, password, siteName, contentName, DocumentAspect.SUMMARIZABLE);
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("cm:summary", summary);
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        addProperties(userName, password, contentNodeRef, propertyMap);
    }
    
    /**
     * Method to add Templatable Aspect 
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param templateContent name of template
     * @throws Exception if error
     * 
     */
    public void addTemplatable(final String userName,
                               final String password,
                               final String siteName,
                               final String contentName,
                               final String templateContent) throws Exception
    {
        addAspect(userName, password, siteName, contentName, DocumentAspect.TEMPLATABLE);
        String templateNodeRef = getNodeRef(userName, password, siteName, templateContent);
        if(!StringUtils.isEmpty(templateNodeRef))
        {
            templateNodeRef = "workspace://SpacesStore/" + templateNodeRef;
            Map<String, Object> propertyMap = new HashMap<String, Object>();
            propertyMap.put("cm:template", templateNodeRef);
            String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
            addProperties(userName, password, contentNodeRef, propertyMap);
        }
        else
        {
            throw new CmisRuntimeException("Invalid template " + templateContent);
        }
    }
    
    /**
     * Add Emailed Aspect
     *  
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param addressee value of addressee property
     * @param addressees list of values for addressees property
     * @param subject value of subject
     * @param originator value for originator
     * @param sentDate date for sentDate property
     * @throws Exception if error
     */
    public void addEmailed(final String userName,
                           final String password,
                           final String siteName,
                           final String contentName,
                           final String addressee,
                           final List<String> addressees,
                           final String subject,
                           final String originator,
                           final Date sentDate) throws Exception
    {
        addAspect(userName, password, siteName, contentName, DocumentAspect.EMAILED);
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("cm:addressee", addressee);
        propertyMap.put("cm:addressees", addressees);
        propertyMap.put("cm:subjectline", subject);
        propertyMap.put("cm:originator", originator);
        propertyMap.put("cm:sentdate", sentDate);
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        addProperties(userName, password, contentNodeRef, propertyMap);
    }
    
    /**
     * Method to add Index Control Aspect 
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param isIndexed value of IsIndexed property
     * @param contentIndexed value of ContentIndexed property
     * @throws Exception if error 
     * 
     */
    public void addIndexControl(final String userName,
                                final String password,
                                final String siteName,
                                final String contentName,
                                final boolean isIndexed,
                                final boolean contentIndexed) throws Exception
    {
        addAspect(userName, password, siteName, contentName, DocumentAspect.INDEX_CONTROL);
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("cm:isIndexed", isIndexed);
        propertyMap.put("cm:isContentIndexed", contentIndexed);
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        addProperties(userName, password, contentNodeRef, propertyMap);
    }
    
    /**
     * Method to add Restrictable Aspect 
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param hours no of hours for OfflineExpiresAfter property
     * @throws Exception if error  
     */
    public void addRestrictable(final String userName,
                                final String password,
                                final String siteName,
                                final String contentName,
                                final int hours) throws Exception
    {
        addAspect(userName, password, siteName, contentName, DocumentAspect.RESTRICTABLE);
        long milliseconds = TimeUnit.HOURS.toMillis(hours);
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("dp:offlineExpiresAfter", milliseconds);
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        addProperties(userName, password, contentNodeRef, propertyMap);
    }
    
    /**
     * Method to add Clasifiable Aspect  
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param categoryName list of categories
     * @throws Exception if error  
     */
    public void addClasifiable(final String userName,
                               final String password,
                               final String siteName,
                               final String contentName,
                               final List<String> categoryName) throws Exception
    {   
        addAspect(userName, password, siteName, contentName, DocumentAspect.CLASSIFIABLE); 
        List<String> nodeRefs = new ArrayList<String>();       
        for(int i = 0; i < categoryName.size(); i++)
        {       
            nodeRefs.add("workspace://SpacesStore/" + getCategoryNodeRef(userName, password, categoryName.get(i)));
        }     
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put("cm:categories", nodeRefs);
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        addProperties(userName, password, contentNodeRef, propertyMap);     
    }
    
    /**
     * Method to read all basic Properties of a file 
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @return {@link Map} return map of the basic properties
     * @throws Exception if error  
     */
    public Map<String, Object> getBasicProperties(final String userName,
                               final String password,
                               final String siteName,
                               final String contentName) throws Exception
    {             
        List<Property<?>> basicProperties=getProperties(userName, password, siteName, contentName);
        
        Map<String, Object> propertiesMap = new HashMap<String, Object>();
        propertiesMap.put("Name", getPropertyValue(basicProperties, PropertyIds.NAME));
        propertiesMap.put("Title", getPropertyValue(basicProperties, "cm:title"));
        propertiesMap.put("Description", getPropertyValue(basicProperties, PropertyIds.DESCRIPTION));
        propertiesMap.put("MimeType", getPropertyValue(basicProperties, PropertyIds.CONTENT_STREAM_MIME_TYPE));
        propertiesMap.put("Author", getPropertyValue(basicProperties, "cm:author"));
        propertiesMap.put("Size", getPropertyValue(basicProperties, PropertyIds.CONTENT_STREAM_LENGTH));
        propertiesMap.put("Creator", getPropertyValue(basicProperties, PropertyIds.CREATED_BY));
        propertiesMap.put("CreatedDate", getPropertyValue(basicProperties, PropertyIds.CREATION_DATE));
        propertiesMap.put("Modifier", getPropertyValue(basicProperties, PropertyIds.LAST_MODIFIED_BY));
        propertiesMap.put("ModifiedDate", getPropertyValue(basicProperties, PropertyIds.LAST_MODIFICATION_DATE));

        return propertiesMap;
    }
    
    /**
     * Method to update basic Properties of a file
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param docName new value for name of document
     * @param docTitle new value for title of document
     * @param docDescription new value for description of document
     * @param author new value for author od the document
     * @throws Exception if error  
     */
    public void setBasicProperties(final String userName,
                                  final String password,
                                  final String siteName,
                                  final String contentName,
                                  final String docName,
                                  final String docTitle,
                                  final String docDescription,
                                  final String author) throws Exception
    {       
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        propertyMap.put(PropertyIds.NAME, docName);
        propertyMap.put("cm:title", docTitle);
        propertyMap.put(PropertyIds.DESCRIPTION, docDescription);
        propertyMap.put("cm:author", author);
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        addProperties(userName, password, contentNodeRef, propertyMap);
    }
}
