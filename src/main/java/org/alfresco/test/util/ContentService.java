package org.alfresco.test.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

/**
 * Create documents and folders using CMIS.
 * 
 * @author Bocancea Bogdan
 * @author Cristina Axinte
 */

public class ContentService extends CMISUtil
{  
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
        catch(CmisConstraintException ce)
        {
            throw new CmisRuntimeException("Invalid symbols in folder name " + folderName, ce);
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
            documentLibrary.refresh();
            Document d = documentLibrary.createDocument(properties, contentStream, VersioningState.MAJOR);
            return d;
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid Site " + siteName, nf);
        }
        catch(CmisContentAlreadyExistsException ae)
        {
            throw new CmisRuntimeException("Document already exits " + docName, ae);
        }
        catch(CmisConstraintException ce)
        {
            throw new CmisRuntimeException("Invalid symbols in file name " + docName, ce);
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
        catch(CmisConstraintException ce)
        {
            throw new CmisRuntimeException("Invalid symbols in file name " + docName, ce);
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
     * Get the content from a document
     * 
     * @param userName 
     * @param password
     * @param siteName 
     * @param docName
     * @return String content
     * @throws Exception
     * 
     */
    public String getDocumentContent(final String userName,
                                     final String password,
                                     final String siteName,
                                     final String docName) throws Exception
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String docNodeRef = getNodeRef(userName, password, siteName, docName);
        String serviceUrl = client.getApiUrl().replace("service/", "") + "-default-/public/cmis/versions/1.1/atom/content?id=" + docNodeRef;
        HttpGet get = new HttpGet(serviceUrl);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {        
            HttpResponse response = clientWithAuth.execute(get);
            if( HttpStatus.SC_OK  == response.getStatusLine().getStatusCode())
            {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "UTF-8");
            }
        }
        finally
        {
            get.releaseConnection();
            client.close();
        } 
        return "";
    }
    
    /**
     * Update content of a document
     * 
     * @param userName 
     * @param password
     * @param siteName 
     * @param DocumentType
     * @param docName
     * @param String new content
     * @throws Exception
     * 
     */
    public boolean updateDocumentContent(final String userName,
                                         final String password,
                                         final String siteName,
                                         final DocumentType docType,
                                         final String docName,
                                         final String newContent) throws Exception
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String docNodeRef = getNodeRef(userName, password, siteName, docName);
        if(StringUtils.isEmpty(docNodeRef))
        {
            throw new RuntimeException("Content doesn't exists");
        }
        String serviceUrl = client.getApiUrl().replace("service/", "") + "-default-/public/cmis/versions/1.1/atom/content?id=" + docNodeRef;    
        HttpPut request = new HttpPut(serviceUrl);
        String contentType = docType.type + ";charset=" + AlfrescoHttpClient.UTF_8_ENCODING;
        request.addHeader("Content-Type", contentType);    
        StringEntity se = new StringEntity(newContent.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        request.setEntity(se);
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(request);
            if(HttpStatus.SC_CREATED == response.getStatusLine().getStatusCode())
            {
                return true;
            }
        }
        finally
        {
            request.releaseConnection();
            client.close();
        }
        return false;
    }
}
