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
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;
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
import org.springframework.stereotype.Service;
@Service
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
     * @param userName login username
     * @param password login password
     * @param folderName folder name
     * @param siteName site name
     * @param inRepository if folder is created in repository
     * @return Folder CMIS folder object
     * @throws Exception if error
     */
    private Folder addFolder(final String userName,
                             final String password,
                             final String folderName,
                             final String siteName,
                             final boolean inRepository,
                             String path) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(folderName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }      
        Map<String, String> properties = new HashMap<String, String >();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        properties.put(PropertyIds.NAME, folderName);
        Session session = getCMISSession(userName, password);
        Folder newFolder;
        try
        {
            if(!inRepository)
            {
                Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
                newFolder = documentLibrary.createFolder(properties);
            }
            else
            {
                if(path == null)
                {
                    path = "";
                }
                Folder repository = (Folder) session.getObjectByPath(session.getRootFolder().getPath() + "/" + path);
                newFolder = repository.createFolder(properties);
            }
            return newFolder;
        }
        catch(CmisObjectNotFoundException nf)
        {
            if(siteName != null)
            {
                throw new CmisRuntimeException("Invalid Site " + siteName, nf);
            }
            else
            {
                throw new CmisRuntimeException("Invalid path -> " + path, nf);
            }
        }  
        catch(CmisContentAlreadyExistsException ae)
        {
            throw new CmisRuntimeException("Folder already exists " + folderName, ae);
        }
        catch(CmisConstraintException ce)
        {
            throw new CmisRuntimeException("Invalid symbols in folder name " + folderName, ce);
        }
        catch (CmisUnauthorizedException ue) 
        {
            throw new CmisRuntimeException("User " + userName + " is not authorized to create folder in repository");
        }
    }

    /**
     * Create a new folder in site
     * 
     * @param userName login username
     * @param password login password
     * @param folderName folder name
     * @param siteName site name
     * @return Folder CMIS folder object
     * @throws Exception if error
     */
    public Folder createFolder(final String userName,
                               final String password,
                               final String folderName,
                               final String siteName) throws Exception
    {
        return addFolder(userName, password, folderName, siteName, false, null);
    }

    /**
     * Create a new folder by path in repository
     * If path is NULL, folder will be created in ROOT of repository.
     *
     * @param userName login username
     * @param password login password
     * @param folderName folder name
     * @param path (e.g: 'Shared', 'Data Dictionary/Scripts)
     * @return Folder CMIS folder object
     * @throws Exception if error
     */
    public Folder createFolderInRepository(final String userName,
                                           final String password,
                                           final String folderName,
                                           final String path) throws Exception
    {
        if(StringUtils.isEmpty(userName))
        {
            throw new IllegalArgumentException("Please provide a path!");
        }
        return addFolder(userName, password, folderName, null, true, path);
    }

    /**
     * Delete a folder from site
     *
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param folderName folder name
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
     * Delete a folder from site.
     * If the folder is in ROOT(Company Home) set path to NULL.
     * 
     * @param userName login username
     * @param password login password
     * @param folderName folder name
     * @param path String path to folder(e.g.: Shared, Guest Home)
     * @throws Exception if error
     */
    public void deleteFolderFromRepository(final String userName,
                                           final String password,
                                           final String folderName,
                                           final String path) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(folderName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        try
        {
            Session session = getCMISSession(userName, password); 
            String folderId = getNodeRefFromRepo(userName, password, folderName, path);
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
        catch (CmisUnauthorizedException ue) 
        {
            throw new CmisRuntimeException("User " + userName + " is not authorized to create folder in repository");
        }
    }

    /**
     * Create a new document in site
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param fileType file type
     * @param docName String file name
     * @param docContent file content
     * @return Document CMIS document object
     * @throws Exception if error
     */
    public Document createDocument(final String userName,
                                   final String password,
                                   final String siteName,
                                   final DocumentType fileType,
                                   final File docName,
                                   final String docContent) throws Exception
    {
        return createDoc(userName, password, siteName, fileType, true, null, docName, docContent, false, null);
    }

    /**
     * Create a new document in site
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param fileType file type
     * @param docName file name
     * @param docContent file content
     * @return Document CMIS document object
     * @throws Exception if error
     */
    public Document createDocument(final String userName,
                                   final String password,
                                   final String siteName,
                                   final DocumentType fileType,
                                   final String docName,
                                   final String docContent) throws Exception
    {
        return createDoc(userName, password, siteName, fileType, false, docName, null, docContent, false, null);
    }

    /**
     * Create a new document in repository
     * If path is NULL, document will be created in ROOT of repository.
     * 
     * @param userName login username
     * @param password login password
     * @param path where to create the document(e.g.: Shared, Data Dictionary/Messages)
     * @param fileType file type
     * @param docName file name
     * @param docContent file content
     * @return Document CMIS document object
     * @throws Exception if error
     */
    public Document createDocumentInRepository(final String userName,
                                               final String password,
                                               final String path,
                                               final DocumentType fileType,
                                               final String docName,
                                               final String docContent) throws Exception
    {
        return createDoc(userName, password, null, fileType, false, docName, null, docContent, true, path);
    }

    /**
     * Create a new document in repository
     * If path is NULL, document will be created in ROOT of repository.
     * 
     * @param userName login username
     * @param password login password
     * @param path where to create the document(e.g.: Shared, Data Dictionary/Messages)
     * @param fileType file type
     * @param docName File file name
     * @param docContent file content
     * @return Document CMIS document object
     * @throws Exception if error
     */
    public Document createDocumentInRepository(final String userName,
                                               final String password,
                                               final String path,
                                               final DocumentType fileType,
                                               final File docName,
                                               final String docContent) throws Exception
    {
        return createDoc(userName, password, null, fileType, true, null, docName, docContent, true, path);
    }

    /**
     * Create a new document
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param fileType file type
     * @param isFile boolean true if type File
     * @param docName file name
     * @param docFile file doc
     * @param docContent file content
     * @param inRepository boolean create in repository
     * @param path path in repository
     * @return Document CMIS document object
     * @throws Exception if error
     */
    private Document createDoc(final String userName,
                               final String password,
                               final String siteName,
                               final DocumentType fileType,
                               final boolean isFile,
                               final String docName,
                               final File docFile,
                               final String docContent,
                               final boolean inRepository,
                               String path) throws Exception
    {
        ContentStream contentStream = null;
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password))
        {
            throw new IllegalArgumentException("Parameter missing");
        }  
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        if(!isFile)
        {
            properties.put(PropertyIds.NAME, docName);
        }
        else
        {
            properties.put(PropertyIds.NAME, docFile.getName());
        }
        Session session = getCMISSession(userName, password);
        byte[] content = docContent.getBytes();
        InputStream stream = new ByteArrayInputStream(content);
        Document d;
        try
        { 
            if(!isFile)
            {
                contentStream = session.getObjectFactory().createContentStream(docName, Long.valueOf(content.length), fileType.type, stream);
            }
            else
            {
                contentStream = session.getObjectFactory().createContentStream(docFile.getName(), Long.valueOf(content.length), fileType.type, stream);
            }
            if(!inRepository)
            {
                Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
                documentLibrary.refresh();
                d = documentLibrary.createDocument(properties, contentStream, VersioningState.MAJOR);
            }
            else
            {
                if(path == null)
                {
                    path = "";
                }
                Folder repository = (Folder) session.getObjectByPath(session.getRootFolder().getPath() + "/" + path);
                d = repository.createDocument(properties, contentStream, VersioningState.MAJOR);
            }
            return d;
        }
        catch(CmisObjectNotFoundException nf)
        {
            if(!isFile)
            {
                throw new CmisRuntimeException("Invalid Site " + siteName, nf);
            }
            else
            {
                throw new CmisRuntimeException("Invalid path ->" + path, nf);
            }
        }
        catch(CmisContentAlreadyExistsException ae)
        {
            if(!isFile)
            {
                throw new CmisRuntimeException("Document already exits " + docName, ae);
            }
            else
            {
                throw new CmisRuntimeException("Document already exits " + docFile, ae);
            }
        }
        catch(CmisConstraintException ce)
        {
            if(!isFile)
            {
                throw new CmisRuntimeException("Invalid symbols in file name " + docName, ce);
            }
            else
            {
                throw new CmisRuntimeException("Invalid symbols in file name " + docFile.getName(), ce);
            }
        }
        finally
        {
            stream.close();
            contentStream.getStream().close();
        }
    }

    /**
     * Create a new document into a folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param folderName folder name
     * @param fileType file type (e.g. text/plain, text/html)
     * @param docName file name
     * @param docContent file content
     * @return Document CMIS document object
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
     * Delete a document
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param docName file name
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
        catch(CmisUnauthorizedException ue)
        {
            throw new CmisRuntimeException("User " + userName + "doesn't have rights to delete " + docName, ue);
        }
        catch(CmisInvalidArgumentException ia)
        {
            throw new CmisRuntimeException("Invalid file: " + docName, ia);
        }
    }

    /**
     * Delete a document created in repository.
     * If the document is in ROOT (Company Home) set path to NULL.
     * 
     * @param userName username
     * @param password password
     * @param path path in repository
     * @param docName file name
     * @throws Exception if error
     */
    public void deleteDocumentRepository(final String userName,
                                         final String password,
                                         final String path,
                                         final String docName) throws Exception
    {   
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(docName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        try
        {
            String docId; 
            Session session = getCMISSession(userName, password); 
            docId = getNodeRefFromRepo(userName, password, docName, path);
            session.getObject(docId).delete();
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid path: " + path, nf);
        }
        catch(CmisInvalidArgumentException ia)
        {
            throw new CmisRuntimeException("Invalid file: " + docName, ia);
        }
        catch(CmisUnauthorizedException ue)
        {
            throw new CmisRuntimeException("User " + userName + "doesn't have rights to delete " + docName, ue);
        }
    }

    /**
     * Delete a parent folder that has children
     * 
     * @param userName login username
     * @param password login password
     * @param inRepository boolean if folder is in repository
     * @param siteName site name
     * @param path in repository
     * @param folderName folder name
     * @throws Exception if error
     */
    private void deleteTreeFolder(final String userName,
                                  final String password,
                                  final boolean inRepository,
                                  final String siteName,
                                  String path,
                                  final String folderName) throws Exception
    {   
        String folderId;
        try
        {
            Session session = getCMISSession(userName, password);
            if(!inRepository)
            {
                folderId = getNodeRef(userName, password, siteName, folderName);
            }
            else
            {
                if(path == null)
                {
                    path = "";
                }
                folderId = getNodeRefFromRepo(userName, password, folderName, path);
            }
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
     * Delete a parent folder that has children in site
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param folderName folder name
     * @throws Exception if error
     */
    public void deleteTree(final String userName,
                           final String password,
                           final String siteName,
                           final String folderName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName) 
                || StringUtils.isEmpty(folderName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        deleteTreeFolder(userName, password, false, siteName, null, folderName);
    }

    /**
     * Delete a parent folder that has children in repository
     * If folder is in root of repository, set PATH to NULL.
     * 
     * @param userName login username
     * @param password login password
     * @param path path to folder (e.g. 'Shared')
     * @param folderName folder name
     * @throws Exception if error
     */
    public void deleteTreeRepository(final String userName,
                                     final String password,
                                     final String path,
                                     final String folderName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(folderName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        deleteTreeFolder(userName, password, true, null, path, folderName);
    }

    /**
     * Upload a single file from a location on disk.
     *
     * @param userName login username
     * @param password user password
     * @param inRepo boolean to upload file in repository
     * @param siteName site name
     * @param pathInRepo path in repository
     * @param pathToFile path to file
     * @return Document uploaded file
     * @throws Exception if error
     */
    private Document uploadFile(final String userName,
                                final String password,
                                final boolean inRepo,
                                final String siteName,
                                String pathInRepo,
                                final String pathToFile) throws Exception
    {
        ContentStream contentStream = null;
        String fileExtention = null;
        File file = new File(pathToFile);
        if (!file.exists() || !file.isFile())
        {
            throw new UnsupportedOperationException("Invalid Path: " + file.getPath());
        }
        fileExtention = FilenameUtils.getExtension(file.getPath());
        FileInputStream fileContent = new FileInputStream(file);
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, file.getName());
        Session session = getCMISSession(userName, password);
        Document d;
        try
        {
            contentStream = session.getObjectFactory().createContentStream(file.getName(), file.length(), fileExtention, fileContent);
            if(!inRepo)
            {
                Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
                d = documentLibrary.createDocument(properties, contentStream, VersioningState.MAJOR);
            }
            else
            {
                if(pathInRepo == null)
                {
                    pathInRepo = "";
                }
                Folder repository = (Folder) session.getObjectByPath(session.getRootFolder().getPath() + "/" + pathInRepo);
                d = repository.createDocument(properties, contentStream, VersioningState.MAJOR);
            }
            return d;
        }
        catch(CmisObjectNotFoundException nf)
        {
            if(!inRepo)
            {
                throw new CmisRuntimeException("Invalid Site " + siteName, nf);
            }
            else
            {
                throw new CmisRuntimeException("Invalid path in repository " + pathInRepo, nf);
            }
        }
        catch(CmisContentAlreadyExistsException ae)
        {
            throw new CmisRuntimeException("Document already exits " + file.getName(), ae);
        }
        finally
        {
            fileContent.close();
            contentStream.getStream().close();
        }
    }

    /**
     * Upload a single file in site from a location on disk.
     *
     * @param userName login username
     * @param password user password
     * @param siteName site name
     * @param pathToFile path to file
     * @return Document uploaded file
     * @throws Exception if error
     */
    public Document uploadFileInSite(final String userName,
                                     final String password,
                                     final String siteName,
                                     final String pathToFile) throws Exception
    {
        if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(pathToFile))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        return uploadFile(userName, password, false, siteName, null, pathToFile);
    }

    /**
     * Upload a single file in repository from a location on disk.
     * If pathInRepo is NULL, document will be uploaded in ROOT of repository.
     *
     * @param userName login username
     * @param password user password
     * @param pathInRepo path in repository. If NULL, ROOT is set!
     * @param pathToFile path to file
     * @return Document uploaded file
     * @throws Exception if error
     */
    public Document uploadFileInRepository(final String userName,
                                           final String password,
                                           final String pathInRepo,
                                           final String pathToFile) throws Exception
    {
        if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(pathToFile))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        return uploadFile(userName, password, true, null, pathInRepo, pathToFile);
    }

    /**
     * Upload files from a physical location
     * 
     * @param filesPath physical path where the files are stored
     * @param userName login username
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
        String fileName = null;
        String fileExtention = null;
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
            fileName = file.getName();
            fileExtention = FilenameUtils.getExtension(file.getPath());
            FileInputStream fileContent = new FileInputStream(file);
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
     * @param userName login username
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
                throw new CmisRuntimeException("Folder: " + folderName + " is not a folder.", cce);
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
     * @param userName login username
     * @param password user password
     * @param siteName site name
     * @param fileNames names of files to be deleted
     * @throws Exception if error
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
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param docName file name
     * @return String content of document
     * @throws Exception if error
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
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param docType file type
     * @param docName file name
     * @param newContent new content of the file
     * @throws Exception if error
     * @return if updated
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
        waitInSeconds(1);
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
