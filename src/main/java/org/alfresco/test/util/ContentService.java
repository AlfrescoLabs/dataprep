package org.alfresco.test.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Create documents and folders using CMIS.
 * 
 * @author Bocancea Bogdan
 * @author Cristina Axinte
 */

public class ContentService extends CMISUtil
{
    private static Log logger = LogFactory.getLog(ContentService.class);
    
    public ContentService(AlfrescoHttpClientFactory alfrescoHttpClientFactory)
    {
        super(alfrescoHttpClientFactory);
    }
    
    /**
     * Create a new folder
     * 
     * @param userName
     * @param password
     * @return Cmis Session
     * @throws Exception if error
     */
    public Folder createFolder(final String userName,
                               final String password,
                               final String folderName,
                               final String siteName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(folderName)
                || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }      
        Map<String, String> properties = new HashMap<String, String >();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        properties.put(PropertyIds.NAME, folderName);
        Session session = getCMISSession(userName, password);
        try
        {
            Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
            Folder newFolder = documentLibrary.createFolder(properties);
            return newFolder;
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid Site " + siteName, nf);
        }  
        catch(CmisContentAlreadyExistsException ae)
        {
            throw new CmisRuntimeException("Folder already exists " + folderName, ae);
        }
    }
    
    /**
     * Delete a folder
     * 
     * @param userName
     * @param password
     * @param folderName
     * @return true if folder is deleted
     * @throws Exception if error
     */
    public void deleteFolder(final String userName,
                             final String password,
                             final String siteName,
                             final String folderName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(folderName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        try
        {
            Session session = getCMISSession(userName, password); 
            String folderId = getNodeRef(userName, password, siteName, folderName);
            session.getObject(folderId).delete();         
        }
        catch(CmisInvalidArgumentException nf)
        {
            throw new CmisRuntimeException("Invalid folder " + folderName, nf);
        }    
        catch(CmisConstraintException ce)
        {
            throw new CmisRuntimeException("Cannot delete folder with at least one child", ce);
        }
    }
    
    /**
     * Create a new document using CMIS
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param DocumentType 
     * @param docName
     * @param docContent
     * @return document
     * @throws Exception if error
     */
    public Document createDocument(final String userName,
                                   final String password,
                                   final String siteName,
                                   final DocumentType fileType,
                                   final String docName,
                                   final String docContent) throws Exception
    {
        ContentStream contentStream = null;
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(docName)
                || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, docName);     
        Session session = getCMISSession(userName, password);
        byte[] content = docContent.getBytes();
        InputStream stream = new ByteArrayInputStream(content);
        try
        {       
            contentStream = session.getObjectFactory().createContentStream(docName, Long.valueOf(content.length), fileType.type, stream);
            Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
            Document d = documentLibrary.createDocument(properties, contentStream, VersioningState.MAJOR);
            return d;
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid Site " + siteName, nf);
        }
        catch(CmisContentAlreadyExistsException ae)
        {
            throw new CmisRuntimeException("Document already exits " + siteName, ae);
        }
        finally
        {
            stream.close();
            contentStream.getStream().close();
        }
    }
    
    /**
     * Create a new document using CMIS
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param DocumentType 
     * @param File fileName
     * @param docContent
     * @return document
     * @throws Exception if error
     */
    public Document createDocument(final String userName,
                                   final String password,
                                   final String siteName,
                                   final DocumentType fileType,
                                   final File docName,
                                   final String docContent) throws Exception
    {
        ContentStream contentStream = null;
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }   
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, docName.getName());     
        Session session = getCMISSession(userName, password);
        byte[] content = docContent.getBytes();
        InputStream stream = new ByteArrayInputStream(content);
        try
        {        
            contentStream = session.getObjectFactory().createContentStream(docName.getName(), Long.valueOf(content.length), fileType.type, stream);
            Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
            Document d = documentLibrary.createDocument(properties, contentStream, VersioningState.MAJOR);
            return d;
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid Site " + siteName, nf);
        }
        catch(CmisContentAlreadyExistsException ae)
        {
            throw new CmisRuntimeException("Document already exits " + siteName, ae);
        }
        finally
        {
            stream.close();
            contentStream.getStream().close();
        }
    }
    
    /**
     * Create a new document into a folder using CMIS
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param folderName
     * @param DocumentType 
     * @param docName
     * @param docContent
     * @return document
     * @throws Exception if error
     */
    public Document createDocumentInFolder(final String userName,
                                           final String password,
                                           final String siteName,
                                           final String folderName,
                                           final DocumentType fileType,
                                           final String docName,
                                           final String docContent) throws Exception
    {
        ContentStream contentStream = null;
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(docName)
                || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        Document d = null;
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, docName);     
        Session session = getCMISSession(userName, password);
        byte[] content = docContent.getBytes();
        InputStream stream = new ByteArrayInputStream(content);
        try
        {           
            contentStream = session.getObjectFactory().createContentStream(docName, Long.valueOf(content.length), fileType.type, stream);
            String folderId = getNodeRef(userName, password, siteName, folderName);
            CmisObject folderObj = session.getObject(folderId);
            if(folderObj instanceof Folder)
            {
                Folder f = (Folder)folderObj;           
                d = f.createDocument(properties, contentStream, VersioningState.MAJOR);
                return d;
            }           
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid Site " + siteName, nf);
        }
        catch(CmisContentAlreadyExistsException ae)
        {
            throw new CmisRuntimeException("Document already exits " + siteName, ae);
        }
        catch(CmisInvalidArgumentException ia)
        {
            throw new CmisRuntimeException("Invalid folder " + folderName, ia);
        }
        finally
        {
           stream.close();
           contentStream.getStream().close();
        }
        return d;
    }
    
    /**
     * Delete a document using CMIS
     * 
     * @param userName
     * @param password
     * @param folderName
     * @return true if folder is deleted
     * @throws Exception if error
     */
    public void deleteDocument(final String userName,
                               final String password,
                               final String siteName,
                               final String docName) throws Exception
    {   
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(docName) 
                || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        try
        {
            String docId; 
            Session session = getCMISSession(userName, password); 
            docId = getNodeRef(userName, password, siteName, docName);
            session.getObject(docId).delete();
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid site " + siteName, nf);
        }       
    }
    
    /**
     * Delete a parent folder that has children using CMIS
     * 
     * @param userName
     * @param password
     * @param folderName
     * @return true if folder is deleted
     * @throws Exception if error
     */
    public void deleteTree(final String userName,
                           final String password,
                           final String siteName,
                           final String folderName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(folderName) 
                || StringUtils.isEmpty(folderName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        try
        {
            Session session = getCMISSession(userName, password); 
            String folderId = getNodeRef(userName, password, siteName, folderName);
            CmisObject o = session.getObject(folderId);
            if(o instanceof Folder)
            {
                Folder f = (Folder)o;
                f.deleteTree(true, UnfileObject.DELETE, true);    
            }
            else
            {
                throw new IllegalArgumentException("Object does not exist or is not a folder");
            } 
        }
        catch(CmisInvalidArgumentException nf)
        {
            throw new CmisRuntimeException("Invalid folder " + folderName, nf);
        } 
    }
  
    /**
     * Upload files from a physical location
     * 
     * @param filesPath physical path where the files are stored
     * @param userName user name
     * @param password user password
     * @param siteName site name
     * @return list of uploaded documents
     * @throws Exception if error
     */  
    public List<Document> uploadFiles(final String filesPath, 
                                      final String userName,
                                      final String password,
                                      final String siteName) throws Exception
    {
        ContentStream contentStream = null;
        List<Document> uploadedFiles=new ArrayList<Document>();
        String fileName=null;
        String fileExtention=null;
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }     
        File dir = new File(filesPath);
        if (!dir.exists() || !dir.isDirectory()) 
        {
            throw new UnsupportedOperationException("Invalid Path: " + dir.getPath());
        }     
        File[] fileList = dir.listFiles();
        for (File file : fileList)
        {
            fileName=file.getName();
            fileExtention=FilenameUtils.getExtension(file.getPath());
            FileInputStream fileContent=new FileInputStream(file);
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
            properties.put(PropertyIds.NAME, fileName);     
            Session session = getCMISSession(userName, password);
            try
            {
                contentStream = session.getObjectFactory().createContentStream(fileName, file.length(), fileExtention, fileContent);
                Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
                Document d = documentLibrary.createDocument(properties, contentStream, VersioningState.MAJOR);
                uploadedFiles.add(d);
            }
            catch(CmisObjectNotFoundException nf)
            {
                throw new CmisRuntimeException("Invalid Site " + siteName, nf);
            }
            catch(CmisContentAlreadyExistsException ae)
            {
                throw new CmisRuntimeException("Document already exits " + siteName, ae);
            }
            finally
            {
                fileContent.close();
                contentStream.getStream().close();
            }        
        }  
        return uploadedFiles;        
    }
    
    /**
     * Upload files in a folder from a physical location
     * 
     * @param filesPath physical path where the files are stored
     * @param userName user name
     * @param password user password
     * @param siteName site name
     * @param folderName folder name where files are uploaded
     * @return list of uploaded documents
     * @throws Exception if error
     */
    public List<Document> uploadFilesInFolder(final String filesPath, 
                                              final String userName,
                                              final String password,
                                              final String siteName,
                                              final String folderName) throws Exception
    {
        List<Document> uploadedFiles=new ArrayList<Document>();
        String fileName=null;
        String fileExtention=null;
        CmisObject folderObj=null;
        ContentStream contentStream=null;
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }      
        File dir = new File(filesPath);
        if (!dir.exists() || !dir.isDirectory()) 
        {
            throw new IllegalArgumentException("Invalid Path: " + dir.getPath());
        }   
        Session session = getCMISSession(userName, password);
        String folderId = getNodeRef(userName, password, siteName, folderName);
        if (StringUtils.isEmpty(folderId))
        {
            throw new CmisRuntimeException("Invalid folder: " + folderName);
        }       
        File[] fileList = dir.listFiles();
        for (File file : fileList)
        {
            fileName=file.getName();
            fileExtention=FilenameUtils.getExtension(file.getPath());
            FileInputStream fileContent=new FileInputStream(file);
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
            properties.put(PropertyIds.NAME, fileName);     
            try
            {
                contentStream = session.getObjectFactory().createContentStream(fileName, file.length(), fileExtention, fileContent);              
                folderObj = session.getObject(folderId);
                Folder f = (Folder)folderObj;           
                Document d = f.createDocument(properties, contentStream, VersioningState.MAJOR);
                uploadedFiles.add(d);
            }
            catch(CmisObjectNotFoundException nf)
            {
                throw new CmisRuntimeException("Invalid Site " + siteName, nf);
            }
            catch(CmisContentAlreadyExistsException ae)
            {
                throw new CmisRuntimeException("Document already exits " + siteName, ae);
            }
            catch(ClassCastException cce)
            {
                throw new CmisRuntimeException("Nole: " + folderName + " is not a folder.", cce);
            }
            finally
            {
                fileContent.close();
                contentStream.getStream().close();             
            }        
        }    
        return uploadedFiles;        
    }
       
    /**
     * Delete multiple files (from root or from folder) using CMIS (in root or in folder)
     * 
     * @param userName user name 
     * @param password user password
     * @param siteName site name
     * @param fileNames names of files to be deleted
     * @throws Exception
     */
    public void deleteFiles(final String userName,
                            final String password,
                            final String siteName,
                            final String... fileNames) throws Exception
    {
        for(int i=0; i<fileNames.length; i++)
        {
            deleteDocument(userName, password, siteName, fileNames[i]);
        }
    }
    
    /**
     * Create tag for a document or folder
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @param tag
     * @return true if request is successful
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public boolean addSingleTag(final String userName,
                                final String password, 
                                final String siteName,
                                final String contentName,
                                final String tag) throws Exception 
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
            || StringUtils.isEmpty(contentName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef = getNodeRef(userName, password, siteName, contentName);
        String api = client.getApiUrl().replace("/service", "");
        String reqUrl = api + "-default-/public/alfresco/versions/1/nodes/" + nodeRef + "/tags";
        HttpPost post  = new HttpPost(reqUrl);
        JSONObject body = new JSONObject();
        body.put("tag", tag);
        post.setEntity(client.setMessageBody(body));
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(post);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_CREATED:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Tag was added successfully " + tag);
                    }
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    throw new RuntimeException("Content doesn't exists " + contentName);
                default:
                    logger.error("Unable to create tag: " + response.toString());
                    break;
            }
        }
        finally
        {
            post.releaseConnection();
            client.close();
        } 
        return false;
    }
    
    /**
     * Create multiple tags for a document or folder
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @param List<String> tags
     * @return true if request is successful
     * @throws Exception if error
     */
    public boolean addMultipleTags(final String userName,
                                   final String password, 
                                   final String siteName,
                                   final String contentName,
                                   final List<String> tags) throws Exception 
    {
        String jsonInput = "";  
        
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
            || StringUtils.isEmpty(contentName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef = getNodeRef(userName, password, siteName, contentName);
        String reqUrl = client.getApiUrl() + "node/workspace/SpacesStore/" + nodeRef + "/tags";
        HttpPost post  = new HttpPost(reqUrl);  
        jsonInput =  ( "[\"" + tags.get(0) + "\"" );
        for( int i = 1; i < tags.size(); i++ )
        {
            jsonInput = ( jsonInput + "," + "\"" + tags.get(i) + "\"" );
        }
        jsonInput = ( jsonInput + "]" );        
        StringEntity se = new StringEntity(jsonInput.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        post.setEntity(se);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(post);
            if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                return true;
            }
        }
        finally
        {
            post.releaseConnection();
            client.close();
        }
        return false;
    }
    
    /**
     * Get all tags that are set for a document or folder
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @return String Json response
     * @throws Exception if error
     */
    private String getTags(final String userName,
                           final String password, 
                           final String siteName,
                           final String contentName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(contentName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef = getNodeRef(userName, password, siteName, contentName);
        String api = client.getApiUrl().replace("/service", "");
        String reqUrl = api + "-default-/public/alfresco/versions/1/nodes/" + nodeRef + "/tags";
        try
        {
            HttpGet get = new HttpGet(reqUrl);
            HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
            HttpResponse response = clientWithAuth.execute(get);
            if( HttpStatus.SC_OK  == response.getStatusLine().getStatusCode())
            {
                return client.readStream(response.getEntity()).toJSONString(); 
            }
        }
        finally
        {
            client.close();
        } 
        return "";
    }
    
    /**
     * Get list of tag names that are set for a document or folder
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @return List<String> tags
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public List<String> getTagNamesFromContent(final String userName,
                                               final String password, 
                                               final String siteName,
                                               final String contentName) throws Exception
    {
        List<String> tags = new ArrayList<String>(); 
        String result = getTags(userName, password, siteName, contentName);     
        if(!StringUtils.isEmpty(result))
        {
            JSONParser parser = new JSONParser();  
            Object obj = parser.parse(result);
            JSONObject jsonObject = (JSONObject) obj;  
            JSONObject list = (JSONObject) jsonObject.get("list");
            JSONArray jArray = (JSONArray) list.get("entries");
            Iterator<JSONObject> iterator = jArray.iterator();
            while (iterator.hasNext()) 
            {
                JSONObject factObj = (JSONObject) iterator.next();
                JSONObject entry = (JSONObject) factObj.get("entry");
                tags.add((String) entry.get("tag"));
            }           
            return tags;
        }
        else
        {
            return tags;
        }
    }  
    
    /**
     * Get the node ref from a tag
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @param tagName
     * @return String nodeRef
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    public String getTagNodeRef(final String userName,
                                final String password, 
                                final String siteName,
                                final String contentName,
                                final String tagName) throws Exception
    {
        String nodeRef = "";
        String result = getTags(userName, password, siteName, contentName);     
        if(!StringUtils.isEmpty(result))
        {
            JSONParser parser = new JSONParser();  
            Object obj = parser.parse(result);
            JSONObject jsonObject = (JSONObject) obj;  
            JSONObject list = (JSONObject) jsonObject.get("list");
            JSONArray jArray = (JSONArray) list.get("entries");
            Iterator<JSONObject> iterator = jArray.iterator();
            while (iterator.hasNext()) 
            {
                JSONObject factObj = (JSONObject) iterator.next();
                JSONObject entry = (JSONObject) factObj.get("entry");
                String name = (String) entry.get("tag");
                if(name.equalsIgnoreCase(tagName))
                {
                    nodeRef = (String) entry.get("id");               
                }
            }                   
            return nodeRef;
        }
        else
        {
            return nodeRef;
        }
    }
    
    /**
     * Remove tag from content
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @param tagName
     * @return true if deleted
     * @throws Exception if error
     */   
    public boolean removeTag(final String userName,
                             final String password,
                             final String siteName,
                             final String contentName,
                             final String tagName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(contentName) || StringUtils.isEmpty(tagName))
        {
                throw new IllegalArgumentException("Parameter missing");
        }       
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        String tagNodeRef = getTagNodeRef(userName, password, siteName, contentName, tagName);
        if(StringUtils.isEmpty(tagNodeRef) || StringUtils.isEmpty(contentNodeRef))
        {
            throw new RuntimeException("Tag or content doesn't exists");
        }
        String api = client.getApiUrl().replace("/service", "");
        String reqUrl = api + "-default-/public/alfresco/versions/1/nodes/" + contentNodeRef + "/tags/" + tagNodeRef;        
        try
        {
            HttpDelete delete = new HttpDelete(reqUrl);
            HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
            HttpResponse response = clientWithAuth.execute(delete);           
            if( HttpStatus.SC_NO_CONTENT  == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("Tag: " + tagName + " is removed successfully");
                }
                return true;
            }
        }
        finally
        {
            client.close();
        }    
        return false;
    }
}
