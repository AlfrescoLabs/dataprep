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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisVersioningException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;
@Service
/**
 * Class to manage different content actions (tagging, comments, likes, favorites)
 * 
 * @author Bogdan Bocancea 
 */

public class ContentActions extends CMISUtil
{
    private enum ActionType
    {
        TAGS("/tags", "tag"),
        COMMENTS("/comments", "content"),
        LIKES("/ratings", "id");
        public final String name;
        public final String bodyParam;
        ActionType(String name, String bodyParam)
        {
            this.name = name;
            this.bodyParam = bodyParam;
        }
    }     
    private static Log logger = LogFactory.getLog(ContentActions.class);

    /**
     * Create single tag or comment for document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param option action type
     * @param value String data
     * @return true if request is successful
     */
    @SuppressWarnings("unchecked")
    private boolean addNewAction(final String userName,
                                 final String password, 
                                 final String siteName,
                                 final String contentName,
                                 final ActionType option,
                                 final String value)
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(contentName) || StringUtils.isEmpty(value))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef = getNodeRef(userName, password, siteName, contentName);
        String reqUrl = client.getApiVersionUrl() + "nodes/" + nodeRef + option.name;
        HttpPost post  = new HttpPost(reqUrl);
        JSONObject body = new JSONObject();
        body.put(option.bodyParam, value);
        if(option.equals(ActionType.LIKES))
        {
            body.put("myRating", true);
        }
        HttpResponse response = client.executeRequest(userName, password, body, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_CREATED:
                if (logger.isTraceEnabled())
                {
                    logger.trace(value + " was added successfully");
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Content doesn't exists " + contentName);
            case HttpStatus.SC_FORBIDDEN:
                throw new RuntimeException("User " + userName + " doesn't have enough rights");
            default:
                logger.error("Unable to add new action: " + response.toString());
                break;
        }
        return false;
    }

    /**
     * Create multiple tags or comments for document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param optType action type
     * @param values List of values
     * @return true if request is successful
     */
    private boolean addMultipleActions(final String userName,
                                       final String password, 
                                       final String siteName,
                                       final String contentName,
                                       final ActionType optType,
                                       final List<String> values)
    {
        String jsonInput = "";
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
            || StringUtils.isEmpty(contentName) || StringUtils.isEmpty(values.toString()))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef = getNodeRef(userName, password, siteName, contentName);
        String reqUrl = client.getApiVersionUrl() + "nodes/" + nodeRef + optType.name;
        HttpPost post  = new HttpPost(reqUrl);
        jsonInput =  ( "[{" + "\"" + optType.bodyParam + "\"" + ": \"" + values.get(0) + "\"" );
        for( int i = 1; i < values.size(); i++ )
        {
            jsonInput = ( jsonInput + "},{" + "\"" + optType.bodyParam  + "\"" + ": \"" + values.get(i) + "\"" );
        }
        jsonInput = ( jsonInput + "}]" );
        StringEntity se = new StringEntity(jsonInput.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        post.setEntity(se);
        HttpResponse response = client.executeRequest(userName, password, post);
        if(HttpStatus.SC_CREATED == response.getStatusLine().getStatusCode())
        {      
            return true;
        }
        else
        {
            logger.error("Unable to add new action: " + response.toString());
        }
        return false;
    }

    /**
     * Remove tag or comment from document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param optType content actions (e.g. tag, comments, likes)
     * @param actionValue value of the action(e.g tag value, comment content)
     * @return true if tag or comment is removed 
     */
    private boolean removeAction(final String userName,
                                 final String password,
                                 final String siteName,
                                 final String contentName,
                                 final ActionType optType,
                                 final String actionValue)
    {
        String optionNodeRef = "";
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(contentName) || StringUtils.isEmpty(actionValue))
        {
            throw new IllegalArgumentException("Parameter missing");
        }       
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        switch (optType)
        {
            case TAGS:
                optionNodeRef = getTagNodeRef(userName, password, siteName, contentName, actionValue);
                break;
            case COMMENTS:
                optionNodeRef = getCommentNodeRef(userName, password, siteName, contentName, actionValue);
                break;
            default:
                break;
        }
        if(StringUtils.isEmpty(optionNodeRef) || StringUtils.isEmpty(contentNodeRef))
        {
            throw new RuntimeException("Content doesn't exists");
        }
        String reqUrl = client.getApiVersionUrl() + "nodes/" + contentNodeRef + optType.name + "/" + optionNodeRef;
        HttpDelete delete = new HttpDelete(reqUrl);
        HttpResponse response = client.executeRequest(userName, password, delete);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_NO_CONTENT:
                if(logger.isTraceEnabled())
                {
                    logger.trace(actionValue + " is removed successfully");
                }
                return true;
            case HttpStatus.SC_FORBIDDEN:
                throw new RuntimeException("User" + userName + " doesn't have enough rights");
            default:
                logger.error("Unable to add new action: " + response.toString());
                break;
        }
        return false;
    }

    /**
     * Create tag for a document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param tag tag for file or folder
     * @return true if request is successful
     */
    public boolean addSingleTag(final String userName,
                                final String password, 
                                final String siteName,
                                final String contentName,
                                final String tag)
    {
        return addNewAction(userName, password, siteName, contentName, ActionType.TAGS, tag);
    }

    /**
     * Create multiple tags for document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param tags list of tags for a file or folder
     * @return true if request is successful
     */
    public boolean addMultipleTags(final String userName,
                                   final String password, 
                                   final String siteName,
                                   final String contentName,
                                   final List<String> tags)
    {
        return addMultipleActions(userName, password, siteName, contentName, ActionType.TAGS, tags);
    }

    /**
     * Get the response from HttpGet for added tags, comments, ratings
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param actionType content actions (e.g. tag, comments, likes)
     * @return String Json response
     */
    private String getOptionResponse(final String userName,
                                     final String password, 
                                     final String siteName,
                                     final String contentName,
                                     final ActionType actionType)
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(contentName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef = getNodeRef(userName, password, siteName, contentName);
        String reqUrl = client.getApiVersionUrl() + "nodes/" + nodeRef + actionType.name;
        if(actionType.equals(ActionType.LIKES))
        {
            reqUrl = reqUrl + "/likes";
        }
        HttpGet get = new HttpGet(reqUrl);
        HttpResponse response = client.executeRequest(userName, password, get);
        if(HttpStatus.SC_OK  == response.getStatusLine().getStatusCode())
        {
            return client.readStream(response.getEntity()).toJSONString(); 
        }
        return "";
    }

    /**
     * Return a list of tags or comments
     * 
     * @param response HttpResponse as String
     * @param optType content actions (e.g. tag, comments, likes)
     */
    @SuppressWarnings("unchecked")
    private List<String> getOptionValues(final String response,
                                         final ActionType optType)
    {
        List<String> values = new ArrayList<String>();
        if(!StringUtils.isEmpty(response))
        {
            JSONParser parser = new JSONParser();
            Object obj;
            try
            {
                obj = parser.parse(response);
            }
            catch (ParseException e)
            {
                throw new RuntimeException("Unable to parse response " + response);
            }
            JSONObject jsonObject = (JSONObject) obj;
            JSONObject list = (JSONObject) jsonObject.get("list");
            JSONArray jArray = (JSONArray) list.get("entries");
            Iterator<JSONObject> iterator = jArray.iterator();
            while (iterator.hasNext()) 
            {
                JSONObject factObj = (JSONObject) iterator.next();
                JSONObject entry = (JSONObject) factObj.get("entry");
                values.add((String) entry.get(optType.bodyParam));
            }
            return values;
        }
        else
        {
            return values;
        }
    }

    /**
     * Return the node ref for a tag or comment
     * 
     * @param response HttpResponse as String
     * @param value value of tag or comment
     * @param optType content actions (e.g. tag, comments, likes)
     */
    @SuppressWarnings("unchecked")
    private String getOptionNodeRef(final String response,
                                    final String value,
                                    final ActionType optType)
    {
        String nodeRef = "";
        if(!StringUtils.isEmpty(response))
        {
            JSONParser parser = new JSONParser();
            Object obj;
            try
            {
                obj = parser.parse(response);
            }
            catch (ParseException e)
            {
                throw new RuntimeException("Unable to parse response " + response);
            }
            JSONObject jsonObject = (JSONObject) obj;
            JSONObject list = (JSONObject) jsonObject.get("list");
            JSONArray jArray = (JSONArray) list.get("entries");
            Iterator<JSONObject> iterator = jArray.iterator();
            while (iterator.hasNext()) 
            {
                JSONObject factObj = (JSONObject) iterator.next();
                JSONObject entry = (JSONObject) factObj.get("entry");
                String name = (String) entry.get(optType.bodyParam);
                if(name.equalsIgnoreCase(value))
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
     * Get list of tag names that are set for a document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @return List list of tags for a file or folder
     */
    public List<String> getTagNamesFromContent(final String userName,
                                               final String password, 
                                               final String siteName,
                                               final String contentName)
    {
        String result = getOptionResponse(userName, password, siteName, contentName, ActionType.TAGS);
        return getOptionValues(result, ActionType.TAGS);
    }

    /**
     * Get the node ref from a tag
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param tagName tag of a file of folder
     * @return String nodeRef
     */
    public String getTagNodeRef(final String userName,
                                final String password, 
                                final String siteName,
                                final String contentName,
                                final String tagName)
    {
        String result = getOptionResponse(userName, password, siteName, contentName, ActionType.TAGS);
        return getOptionNodeRef(result, tagName, ActionType.TAGS);
    }

    /**
     * Remove tag from content
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param tagName tag of a file or folder
     * @return true if deleted
     */
    public boolean removeTag(final String userName,
                             final String password,
                             final String siteName,
                             final String contentName,
                             final String tagName)
    {
        return removeAction(userName, password, siteName, contentName, ActionType.TAGS, tagName);
    }

    /**
     * Create a comment for a document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param comment comment for a file or folder
     * @return true if request is successful
     */
    public boolean addComment(final String userName,
                              final String password,
                              final String siteName,
                              final String contentName,
                              final String comment)
    {
        return addNewAction(userName, password, siteName, contentName, ActionType.COMMENTS, comment);
    }

    /**
     * Create multiple comments for a document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param comments list of comments
     * @return true if request is successful
     */
    public boolean addMultipleComments(final String userName,
                                       final String password, 
                                       final String siteName,
                                       final String contentName,
                                       final List<String> comments)
    {
        return addMultipleActions(userName, password, siteName, contentName, ActionType.COMMENTS, comments);
    }

    /**
     * Get list of comments that are set for a document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @return List list of comments
     */
    public List<String> getComments(final String userName,
                                    final String password, 
                                    final String siteName,
                                    final String contentName)
    {
        String result = getOptionResponse(userName, password, siteName, contentName, ActionType.COMMENTS);
        List<String> comments = getOptionValues(result, ActionType.COMMENTS);
        return comments;
    }

    /**
     * Get the node ref for comment
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param comment comment of a file or folder
     * @return String nodeRef
     */
    public String getCommentNodeRef(final String userName,
                                    final String password,
                                    final String siteName,
                                    final String contentName,
                                    final String comment)
    {
        String result = getOptionResponse(userName, password, siteName, contentName, ActionType.COMMENTS);
        return getOptionNodeRef(result, comment, ActionType.COMMENTS);
    }

    /**
     * Remove comment from content
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param comment comment of a file or folder
     * @return true if deleted
     */
    public boolean removeComment(final String userName,
                                 final String password,
                                 final String siteName,
                                 final String contentName,
                                 final String comment)
    {
        return removeAction(userName, password, siteName, contentName, ActionType.COMMENTS, comment);
    }

    /**
     * Like a document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @return true if request is successful
     */
    public boolean likeContent(final String userName,
                               final String password,
                               final String siteName,
                               final String contentName)
    {
        return addNewAction(userName, password, siteName, contentName, ActionType.LIKES, "likes");
    }

    /**
     * Get the number of likes for a document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @return int likes
     */
    public int countLikes(final String userName,
                          final String password,
                          final String siteName,
                          final String contentName)
    {
        String result = getOptionResponse(userName, password, siteName, contentName, ActionType.LIKES);
        if(!StringUtils.isEmpty(result))
        {
            JSONParser parser = new JSONParser();
            Object obj;
            try
            {
                obj = parser.parse(result);
            }
            catch (ParseException e)
            {
                throw new RuntimeException("Unable to parse the response " + result, e);
            }
            JSONObject jsonObject = (JSONObject) obj;
            JSONObject list = (JSONObject) jsonObject.get("entry");
            JSONObject aggregate = (JSONObject) list.get("aggregate");
            String strLikes =  (String) aggregate.get("numberOfRatings").toString();
            int nrLikes = Integer.parseInt(strLikes);
            return nrLikes;
        }
        return 0;
    }

    /**
     * Remove like from content
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @return true if removed
     */
    public boolean removeLike(final String userName,
                              final String password,
                              final String siteName,
                              final String contentName)
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(contentName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }       
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        if(StringUtils.isEmpty(contentNodeRef))
        {
            throw new RuntimeException("Content doesn't exists");
        }
        String reqUrl = client.getApiVersionUrl() + "nodes/" + contentNodeRef + "/ratings/likes";
        HttpDelete delete = new HttpDelete(reqUrl);  
        HttpResponse response = client.executeRequest(userName, password, delete);
        if( HttpStatus.SC_NO_CONTENT  == response.getStatusLine().getStatusCode())
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Like is removed successfully from " + contentName);
            }
            return true;
        }
        return false;
    }

    /**
     * Set document or folder as favorite
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentType ('file' or 'folder')
     * @param contentName file of folder name
     * @return true if marked as favorite
     */ 
    private boolean setFavorite(final String userName,
                                final String password,
                                final String siteName,
                                final String contentType,
                                final String contentName)
    {
        if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(contentName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef = getNodeRef(userName, password, siteName, contentName);
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites";
        HttpPost post  = new HttpPost(reqUrl);
        String jsonInput;
        jsonInput = "{\"target\": {\"" + contentType + "\" : {\"guid\" : \"" + nodeRef + "\"}}}";
        StringEntity se = new StringEntity(jsonInput.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        post.setEntity(se);
            HttpResponse response = client.executeRequest(userName, password, post);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_CREATED:
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    throw new RuntimeException("Content doesn't exists " + contentName);
                default:
                    logger.error("Unable to mark as favorite: " + response.toString());
                    break;
            }
        return false;
    }

    /**
     * Set a document as favorite
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param fileName file name
     * @return true if marked as favorite
     */ 
    public boolean setFileAsFavorite(final String userName,
                                     final String password,
                                     final String siteName,
                                     final String fileName)
    {
        return setFavorite(userName, password, siteName, "file", fileName);
    }
    
    /**
     * Set a folder as favorite
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param folderName folder name
     * @return true if marked as favorite
     */ 
    public boolean setFolderAsFavorite(final String userName,
                                       final String password,
                                       final String siteName,
                                       final String folderName)
    {
        return setFavorite(userName, password, siteName, "folder", folderName);
    }

    /**
     * Remove favorite from document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @return true if favorite is removed
     */
    public boolean removeFavorite(final String userName,
                                  final String password,
                                  final String siteName,
                                  final String contentName)
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(contentName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        if(StringUtils.isEmpty(contentNodeRef))
        {
            throw new RuntimeException("Content doesn't exists");
        }
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites/" + contentNodeRef;
        HttpDelete delete = new HttpDelete(reqUrl);
        HttpResponse response = client.executeRequest(userName, password, delete);
        if( HttpStatus.SC_NO_CONTENT  == response.getStatusLine().getStatusCode())
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Favorite is removed successfully from " + contentName);
            }
            return true;
        }   
        return false;
    }

    /**
     * Verify if a document or folder is marked as favorite
     *
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @return true if marked as favorite
     */
    public boolean isFavorite(final String userName,
                              final String password,
                              final String siteName,
                              final String contentName)
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(siteName)
                || StringUtils.isEmpty(contentName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);
        if(StringUtils.isEmpty(contentNodeRef))
        {
            throw new RuntimeException("Content doesn't exists");
        }
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites/" + contentNodeRef;
        HttpGet get = new HttpGet(reqUrl);
        HttpResponse response = client.executeRequest(userName, password, get);
        if( HttpStatus.SC_OK  == response.getStatusLine().getStatusCode())
        {
            if(logger.isTraceEnabled())
            {
                logger.trace( "Content " + contentName + "is marked as favorite");
            }
            return true;
        }
        return false;
    }
    
    /**
     * Checks out the document and returns the object id of the PWC (private
     * working copy).
     *
     * @param userName login username
     * @param password login password
     * @param siteId site name
     * @param fileName file
     * @return ObjectId object id of PWC
     */
    public ObjectId checkOut(final String userName,
                             final String password,
                             final String siteId,
                             final String fileName)
    {
        ObjectId id;
        try
        {
            Session session = getCMISSession(userName, password);
            id = getDocumentObject(session, siteId, fileName).checkOut();
        }
        catch(CmisVersioningException cv)
        {
            throw new CmisRuntimeException("File " + fileName + " is already checked out", cv);
        }
        return id;
    }
    
    /**
     * If this is a PWC (private working copy) the check out will be reversed.
     * If this is not a PWC it an exception will be thrown.
     *
     * @param userName login username
     * @param password login password
     * @param siteId site name
     * @param fileName file
     * @return ObjectId object id of PWC
     */
    public void cancelCheckOut(final String userName,
                               final String password,
                               final String siteId,
                               final String fileName)
    {
        try
        {
            Session session = getCMISSession(userName, password);
            getDocumentObject(session, siteId, fileName).cancelCheckOut();
        }
        catch(CmisRuntimeException re)
        {
            throw new CmisRuntimeException("File " + fileName + " is not selected for editing", re);
        }
    }
    
    /**
     * Check in document. If this is not a PWC(private working copy) check out for the 
     * file will be made.
     *
     * @param userName login username
     * @param password login password
     * @param siteId site name
     * @param docName document name
     * @param fileType DocumentType type of document
     * @param newContent String new content to be set
     * @param majorVersion boolean true to set major version
     * @param checkinComment String check in comment
     * @return ObjectId object id of PWC
     */
    public ObjectId checkIn(final String userName,
                            final String password,
                            final String siteId,
                            final String docName,
                            final DocumentType fileType,
                            final String newContent,
                            final boolean majorVersion,
                            final String checkinComment)
    {
        Document pwc = null;
        Session session = getCMISSession(userName, password);
        Document docToModify = getDocumentObject(session, siteId, docName);
        String id = docToModify.getVersionSeriesCheckedOutId();
        InputStream stream = null;
        ContentStream contentStream = null;
        if(!StringUtils.isEmpty(id))
        {
            pwc = (Document) session.getObject(id);
        }
        else
        {
            // checkout doc
            ObjectId idPwc = docToModify.checkOut();
            pwc = (Document) session.getObject(idPwc);
        }
        byte[] content = newContent.getBytes();
        try
        {
            stream = new ByteArrayInputStream(content);
            contentStream = session.getObjectFactory().createContentStream(docName, Long.valueOf(content.length), fileType.type, stream);
            return pwc.checkIn(majorVersion, null, contentStream, checkinComment);
        }
        finally
        {
            closeStreams(stream, contentStream);
        }
    }
    
    /**
     * Get the version of a file
     *
     * @param userName login username
     * @param password login password
     * @param siteId site name
     * @param fileName file
     * @return String file version
     */
    public String getVersion(final String userName,
                             final String password,
                             final String siteId,
                             final String fileName)
    {
        Session session = getCMISSession(userName, password);
        return getDocumentObject(session, siteId, fileName).getVersionLabel();
    }
    
    /**
     * Copy file or folder
     *
     * @param userName user name
     * @param password password
     * @param siteName source site
     * @param contentName content to be copied
     * @param targetSite tartget site
     * @param targetFolder target folder. If null document library is set
     * @return CmisObject of new created object
     */
    public CmisObject copyTo(final String userName,
                             final String password,
                             final String sourceSite,
                             final String contentName,
                             final String targetSite,
                             final String targetFolder)
    {
        CmisObject objTarget = null;
        CmisObject copiedContent = null;
        Session session = getCMISSession(userName, password);
        CmisObject objFrom = getCmisObject(session, sourceSite, contentName);
        try
        {
            if(!StringUtils.isEmpty(targetFolder))
            {
                objTarget = getCmisObject(session, targetSite, targetFolder);
            }
            else
            {
                objTarget = session.getObjectByPath("/Sites/" + targetSite + "/documentLibrary");
            }
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Target doesnt exists: " + targetSite + " or " + targetFolder, nf);
        }
        
        if(objFrom instanceof Document)
        {
            Document d = (Document)objFrom;
            copiedContent = d.copy(objTarget);
        }
        else if(objFrom instanceof Folder)
        {
            Folder fFrom = (Folder)objFrom;
            Folder toFolder = (Folder)objTarget;
            copiedContent = copyFolder(fFrom, toFolder);
        }
        return copiedContent;
    }
    
    /**
     * Copy folder with all childs 
     * 
     * @param toCopyFolder source folder
     * @param targetFolder target folder
     * @return CmisObject of new created folder
     */
    private CmisObject copyFolder(Folder sourceFolder,
                                  Folder targetFolder)
    {
        Map<String, Object> folderProperties = new HashMap<String, Object>(2);
        folderProperties.put(PropertyIds.NAME, sourceFolder.getName());
        folderProperties.put(PropertyIds.OBJECT_TYPE_ID, sourceFolder.getBaseTypeId().value());
        Folder newFolder = targetFolder.createFolder(folderProperties);
        copyChildrenFromFolder(sourceFolder, newFolder);
        return newFolder;
    }
    
    /**
     * Copy all the childs of the source folder to the target folder
     * 
     * @param sourceFolder
     * @param targetFolder
     */
    private void copyChildrenFromFolder(Folder sourceFolder,
                                        Folder targetFolder)
    {
        for (Tree<FileableCmisObject> t : sourceFolder.getDescendants(-1))
        {
            CmisObject obj = t.getItem();
            if (obj instanceof Document)
            {
                Document d = (Document)obj;
                d.copy(targetFolder);
            } 
            else if (obj instanceof Folder)
            {
               copyFolder( (Folder) obj, targetFolder);
            }
        }
    }
    
    /**
     * Move file or folder
     * 
     * @param userName user name
     * @param password password
     * @param siteName source site
     * @param contentName content to be copied
     * @param targetSite tartget site
     * @param targetFolder target folder. If null document library is set
     * @return CmisObject of new created object
     */
    public CmisObject moveTo(final String userName,
                             final String password,
                             final String sourceSite,
                             final String contentName,
                             final String targetSite,
                             final String targetFolder)
    {
        CmisObject objTarget = null;
        CmisObject movedContent = null;
        Session session = getCMISSession(userName, password);
        CmisObject objFrom = getCmisObject(session, sourceSite, contentName);
        try
        {
            if(!StringUtils.isEmpty(targetFolder))
            {
                objTarget = getCmisObject(session, targetSite, targetFolder);
            }
            else
            {
                objTarget = session.getObjectByPath("/Sites/" + targetSite + "/documentLibrary");
            }
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Target doesnt exists: " + targetSite + " or " + targetFolder, nf);
        }
        if(objFrom instanceof Document)
        {
            Document d = (Document)objFrom;
            List<Folder> parents = d.getParents();
            CmisObject parent = session.getObject(parents.get(0).getId());
            movedContent = d.move(parent, objTarget);
        }
        else if(objFrom instanceof Folder)
        {
            Folder f = (Folder)objFrom;
            List<Folder> parents = f.getParents();
            CmisObject parent = session.getObject(parents.get(0).getId());
            movedContent = f.move(parent, objTarget);
        }
        return movedContent;
    }
}
