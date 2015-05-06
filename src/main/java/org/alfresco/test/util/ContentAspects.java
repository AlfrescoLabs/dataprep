package org.alfresco.test.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to add aspects to documents and folders
 * 
 * @author Bocancea Bogdan
 */

public class ContentAspects extends CMISUtil
{
    public ContentAspects(AlfrescoHttpClientFactory alfrescoHttpClientFactory)
    {
        super(alfrescoHttpClientFactory);
    }
    
    /**
     * Add aspect for document or folder
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @param aspectToRemove
     * @throws Exception 
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
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @param aspectToRemove
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
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @param removeAfter
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
     * @throws Exception 
     * 
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
}
