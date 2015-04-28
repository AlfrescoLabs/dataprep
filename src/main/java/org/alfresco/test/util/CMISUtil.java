package org.alfresco.test.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;

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

    private AlfrescoHttpClientFactory alfrescoHttpClientFactory;
    Map<String, String> contents = new HashMap<String,String>();
    
    public CMISUtil(AlfrescoHttpClientFactory alfrescoHttpClientFactory)
    {
        this.alfrescoHttpClientFactory = alfrescoHttpClientFactory;
    }

    /**
     * Method to get a CMIS session
     * @param userName
     * @param password
     * @return Session
     * @throws Exception
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
        String serviceUrl = client.getApiUrl().replace("service/", "") + "-default-/public/cmis/versions/1.1/atom";
        parameter.put(SessionParameter.ATOMPUB_URL, serviceUrl);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        try
        {
            // create session
            List<Repository> repositories = factory.getRepositories(parameter);
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
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @return String
     * @throws Exception
     */
    public String getNodeRef(final String userName,
                             final String password,
                             final String siteName,
                             final String contentName) throws Exception
    {
        String nodeRef = "";
        contents.clear();
        Session session = getCMISSession(userName, password);
        Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
        for (Tree<FileableCmisObject> t : documentLibrary.getDescendants(-1)) 
        {
            contents = getId(t, contentName);      
        }      
        for (Map.Entry<String, String> entry : contents.entrySet()) 
        {
            if(entry.getKey().equalsIgnoreCase(contentName))
            {
                nodeRef = entry.getValue().split(";")[0];;
                return nodeRef;
            }
        }    
        return nodeRef;
    }
    
    private Map<String, String> getId(Tree<FileableCmisObject> tree,
                                      String contentName)
    { 
        contents.put(tree.getItem().getName(), tree.getItem().getId());
        for (Tree<FileableCmisObject> t : tree.getChildren()) 
        {   
            getId(t, contentName);
        }       
        return contents;
    }
}

