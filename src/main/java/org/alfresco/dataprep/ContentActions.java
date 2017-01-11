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
import java.io.UnsupportedEncodingException;
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
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisStorageException;
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
import org.springframework.stereotype.Service;

/**
 * Class to manage different content actions (tagging, comments, likes, favorites)
 * 
 * @author Bogdan Bocancea 
 */
@Service
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
                                 final boolean repository,
                                 final String pathToItem,
                                 final ActionType option,
                                 final String value)
    {
        String nodeRef;
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        if(!repository)
        {
            nodeRef = getNodeRef(userName, password, siteName, contentName);
        }
        else
        {
            nodeRef = getNodeRefByPath(userName, password, pathToItem);
        }
        if(StringUtils.isEmpty(nodeRef))
        {
            throw new RuntimeException("Content doesn't exists " + contentName);
        }
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
            case HttpStatus.SC_BAD_REQUEST:
                throw new RuntimeException("Invalid value: " + value);
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
                                       final boolean repository,
                                       final String pathToItem,
                                       final ActionType optType,
                                       final List<String> values)
    {
        String jsonInput = "";
        String nodeRef;
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        if(!repository)
        {
            nodeRef = getNodeRef(userName, password, siteName, contentName);
        }
        else
        {
            nodeRef = getNodeRefByPath(userName, password, pathToItem);
        }
        String reqUrl = client.getApiVersionUrl() + "nodes/" + nodeRef + optType.name;
        HttpPost post  = new HttpPost(reqUrl);
        jsonInput =  ("[{" + "\"" + optType.bodyParam + "\"" + ": \"" + values.get(0) + "\"" );
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
                                 final boolean repository,
                                 final String pathToItem,
                                 final ActionType optType,
                                 final String actionValue)
    {
        String optionNodeRef = "";
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef;
        if(!repository)
        {
            nodeRef = getNodeRef(userName, password, siteName, contentName);
        }
        else
        {
            nodeRef = getNodeRefByPath(userName, password, pathToItem);
        }
        switch (optType)
        {
            case TAGS:
                if(!repository)
                {
                    optionNodeRef = getTagNodeRef(userName, password, siteName, contentName, actionValue);
                }
                else
                {
                    optionNodeRef = getTagNodeRef(userName, password, pathToItem, actionValue);
                }
                break;
            case COMMENTS:
                if(!repository)
                {
                    optionNodeRef = getCommentNodeRef(userName, password, siteName, contentName, actionValue);
                }
                else
                {
                    optionNodeRef = getCommentNodeRef(userName, password, pathToItem, actionValue);
                }
                break;
            default:
                break;
        }
        if(StringUtils.isEmpty(nodeRef))
        {
            throw new RuntimeException("Content doesn't exists");
        }
        String reqUrl = "";
        if(optType.equals(ActionType.COMMENTS) || optType.equals(ActionType.TAGS))
        {
            reqUrl = client.getApiVersionUrl() + "nodes/" + nodeRef + optType.name + "/" + optionNodeRef;
        }
        else
        {
            reqUrl = client.getApiVersionUrl() + "nodes/" + nodeRef + "/ratings/likes";
        }
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
            case HttpStatus.SC_METHOD_NOT_ALLOWED:
                throw new RuntimeException("Invalid item: " + actionValue);
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
        return addNewAction(userName, password, siteName, contentName, false, null, ActionType.TAGS, tag);
    }
    
    /**
     * Create tag for a document or folder from repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to document or folder
     * @param tag tag for file or folder
     * @return true if request is successful
     */
    public boolean addSingleTag(final String userName,
                                final String password,
                                final String pathToItem,
                                final String tag)
    {
        return addNewAction(userName, password, null, null, true, pathToItem, ActionType.TAGS, tag);
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
        return addMultipleActions(userName, password, siteName, contentName, false, null, ActionType.TAGS, tags);
    }
    
    /**
     * Create multiple tags for document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to document or folder
     * @param tags list of tags for a file or folder
     * @return true if request is successful
     */
    public boolean addMultipleTags(final String userName,
                                   final String password,
                                   final String pathToItem,
                                   final List<String> tags)
    {
        return addMultipleActions(userName, password, null, null, true, pathToItem, ActionType.TAGS, tags);
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
    private HttpResponse getOptionResponse(final String userName,
                                           final String password,
                                           final String siteName,
                                           final String contentName,
                                           final boolean repository,
                                           final String pathToItem,
                                           final ActionType actionType)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String nodeRef;
        if(!repository)
        {
            nodeRef = getNodeRef(userName, password, siteName, contentName);
        }
        else
        {
            nodeRef = getNodeRefByPath(userName, password, pathToItem);
        }
        String reqUrl = client.getApiVersionUrl() + "nodes/" + nodeRef + actionType.name;
        if(actionType.equals(ActionType.LIKES))
        {
            reqUrl = reqUrl + "/likes";
        }
        HttpGet get = new HttpGet(reqUrl);
        return client.executeRequest(userName, password, get);
    }

    /**
     * Return a list of tags or comments
     * 
     * @param response HttpResponse as String
     * @param optType content actions (e.g. tag, comments, likes)
     */
    @SuppressWarnings("unchecked")
    private List<String> getOptionValues(final HttpResponse response,
                                         final ActionType optType)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        JSONArray jArray = client.getJSONArray(response, "list", "entries");
        List<String> values = new ArrayList<String>();
        Iterator<JSONObject> iterator = jArray.iterator();
        while (iterator.hasNext())
        {
            JSONObject factObj = (JSONObject) iterator.next();
            JSONObject entry = (JSONObject) factObj.get("entry");
            values.add((String) entry.get(optType.bodyParam));
        }
        return values;
    }

    /**
     * Return the node ref for a tag or comment
     * 
     * @param response HttpResponse as String
     * @param value value of tag or comment
     * @param optType content actions (e.g. tag, comments, likes)
     */
    @SuppressWarnings("unchecked")
    private String getOptionNodeRef(final HttpResponse response,
                                    final String value,
                                    final ActionType optType)
    {
        String nodeRef = "";
        AlfrescoHttpClient jClient = alfrescoHttpClientFactory.getObject();
        JSONArray jArray = jClient.getJSONArray(response, "list", "entries");
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
        HttpResponse result = getOptionResponse(userName, password, siteName, contentName, false, null, ActionType.TAGS);
        return getOptionValues(result, ActionType.TAGS);
    }
    
    /**
     * Get list of tag names that are set for a document or folder in repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to item in repository
     * @return List list of tags for a file or folder
     */
    public List<String> getTagNamesFromContent(final String userName,
                                               final String password, 
                                               final String pathToItem)
    {
        HttpResponse result = getOptionResponse(userName, password, null, null, true, pathToItem, ActionType.TAGS);
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
        HttpResponse result = getOptionResponse(userName, password, siteName, contentName, false, null, ActionType.TAGS);
        return getOptionNodeRef(result, tagName, ActionType.TAGS);
    }
    
    /**
     * Get the node ref from a tag in repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to document or folder in repository
     * @param tagName tag of a file of folder
     * @return String nodeRef
     */
    public String getTagNodeRef(final String userName,
                                final String password,
                                final String pathToItem,
                                final String tagName)
    {
        HttpResponse result = getOptionResponse(userName, password, null, null, true, pathToItem, ActionType.TAGS);
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
        return removeAction(userName, password, siteName, contentName, false, null, ActionType.TAGS, tagName);
    }
    
    /**
     * Remove tag from content from repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to item in repository
     * @param tagName tag of a file or folder
     * @return true if deleted
     */
    public boolean removeTag(final String userName,
                             final String password,
                             final String pathToItem,
                             final String tagName)
    {
        return removeAction(userName, password, null, null, true, pathToItem, ActionType.TAGS, tagName);
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
        return addNewAction(userName, password, siteName, contentName, false, null, ActionType.COMMENTS, comment);
    }
    
    /**
     * Create a comment for a document or folder from repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to item in repository
     * @param comment comment for a file or folder
     * @return true if request is successful
     */
    public boolean addComment(final String userName,
                              final String password,
                              final String pathToItem,
                              final String comment)
    {
        return addNewAction(userName, password, null, null, true, pathToItem, ActionType.COMMENTS, comment);
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
        return addMultipleActions(userName, password, siteName, contentName, false, null, ActionType.COMMENTS, comments);
    }
    
    /**
     * Create multiple comments for a document or folder in repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to item in repository
     * @param comments list of comments
     * @return true if request is successful
     */
    public boolean addMultipleComments(final String userName,
                                       final String password,
                                       final String pathToItem,
                                       final List<String> comments)
    {
        return addMultipleActions(userName, password, null, null, true, pathToItem, ActionType.COMMENTS, comments);
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
        HttpResponse result = getOptionResponse(userName, password, siteName, contentName, false, null, ActionType.COMMENTS);
        List<String> comments = getOptionValues(result, ActionType.COMMENTS);
        return comments;
    }
    
    /**
     * Get list of comments that are set for a document or folder in repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to item in repository
     * @return List list of comments
     */
    public List<String> getComments(final String userName,
                                    final String password,
                                    final String pathToItem)
    {
        HttpResponse result = getOptionResponse(userName, password, null, null, true, pathToItem, ActionType.COMMENTS);
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
        HttpResponse result = getOptionResponse(userName, password, siteName, contentName, false, null, ActionType.COMMENTS);
        return getOptionNodeRef(result, comment, ActionType.COMMENTS);
    }
    
    /**
     * Get the node ref for comment from repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to item in repository
     * @param comment comment of a file or folder
     * @return String nodeRef
     */
    public String getCommentNodeRef(final String userName,
                                    final String password,
                                    final String pathToItem,
                                    final String comment)
    {
        HttpResponse result = getOptionResponse(userName, password, null, null, true, pathToItem, ActionType.COMMENTS);
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
        return removeAction(userName, password, siteName, contentName, false, null, ActionType.COMMENTS, comment);
    }
    
    /**
     * Remove comment from content in repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to item in repository
     * @param comment comment of a file or folder
     * @return true if deleted
     */
    public boolean removeComment(final String userName,
                                 final String password,
                                 final String pathToItem,
                                 final String comment)
    {
        return removeAction(userName, password, null, null, true, pathToItem, ActionType.COMMENTS, comment);
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
        return addNewAction(userName, password, siteName, contentName, false, null, ActionType.LIKES, "likes");
    }
    
    /**
     * Like a document or folder from repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to document or folder
     * @return true if request is successful
     */
    public boolean likeContent(final String userName,
                               final String password,
                               final String pathToItem)
    {
        return addNewAction(userName, password, null, null, true, pathToItem, ActionType.LIKES, "likes");
    }

    private int countLikes(final String userName,
                           final String password,
                           final String siteName,
                           final String contentName,
                           final boolean repository,
                           final String pathToItem)
    {
        AlfrescoHttpClient jClient = alfrescoHttpClientFactory.getObject();
        HttpResponse result;
        if(!repository)
        {
            result = getOptionResponse(userName, password, siteName, contentName, false, null, ActionType.LIKES);
        }
        else
        {
            result = getOptionResponse(userName, password, null, null, true, pathToItem, ActionType.LIKES);
        }
        if(200 == result.getStatusLine().getStatusCode())
        {
            String likes = jClient.getParameterFromJSON(result, "numberOfRatings", "entry", "aggregate");
            return Integer.parseInt(likes);
        }
        return 0;
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
        return countLikes(userName, password, siteName, contentName, false, null);
    }
    
    /**
     * Get the number of likes for a document or folder from repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to document or folder
     * @return int likes
     */
    public int countLikes(final String userName,
                          final String password,
                          final String pathToItem)
    {
        return countLikes(userName, password, null, null, true, pathToItem);
    }

    /**
     * Remove like from content
     * 
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
        return removeAction(userName, password, siteName, contentName, false, null, ActionType.LIKES, "");
    }
    
    /**
     * Remove like from content in repository
     * 
     * @param userName login username
     * @param password login password
     * @param pathToItem path to document or folder
     * @return true if removed
     */
    public boolean removeLike(final String userName,
                              final String password,
                              final String pathToItem)
    {
        return removeAction(userName, password, null, null, true, pathToItem, ActionType.LIKES, "");
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
        if(StringUtils.isEmpty(nodeRef))
        {
            throw new RuntimeException("Content doesn't exists " + contentName);
        }
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
        Session session = getCMISSession(userName, password);
        return checkOut(session, siteId, fileName);
    }
    
    /**
     * Checks out the document and returns the object id of the PWC (private
     * working copy).
     *
     * @param session {@link Session}
     * @param siteId site name
     * @param fileName file
     * @return ObjectId object id of PWC
     */
    public ObjectId checkOut(final Session session,
                             final String siteId,
                             final String fileName)
    {
        return checkOut(session, siteId, fileName, false, null);
    }
    
    /**
     * Checks out the document and returns the object id of the PWC (private
     * working copy).
     *
     * @param session {@link Session}
     * @param pathToFile String path to document (e.g. /Shared/testFile.txt)
     * @return ObjectId object id of PWC
     */
    public ObjectId checkOut(final Session session,
                             final String pathToFile)
    {
        return checkOut(session, null, null, true, pathToFile);
    }
    
    private ObjectId checkOut(final Session session,
                              final String siteId,
                              final String fileName,
                              final boolean inRepo,
                              final String pathToDocument)
    {
        try
        {
            if(inRepo)
            {
                return getDocumentObject(session, pathToDocument).checkOut();
            }
            else
            {
                return getDocumentObject(session, siteId, fileName).checkOut();
            }
        }
        catch(CmisVersioningException cv)
        {
            throw new CmisRuntimeException("File " + fileName + " is already checked out", cv);
        }
    }
    
    /**
     * If this is a PWC (private working copy) the check out will be reversed.
     * If this is not a PWC it an exception will be thrown.
     *
     * @param userName login username
     * @param password login password
     * @param siteId site name
     * @param fileName file
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
     * If this is a PWC (private working copy) the check out will be reversed.
     * If this is not a PWC it an exception will be thrown.
     *
     * @param session {@link Session}
     * @param pathToFile path to document (e.g. /Shared/testFile.txt)
     */
    public void cancelCheckOut(final Session session,
                               final String pathToFile)
    {
        try
        {
            getDocumentObject(session, pathToFile).cancelCheckOut();
        }
        catch(CmisRuntimeException re)
        {
            throw new CmisRuntimeException("File from" + pathToFile + " is not selected for editing", re);
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
        return checkIn(userName, password, false, null, siteId, docName, fileType, newContent, majorVersion, checkinComment);
    }
    
    /**
     * Check in document. If this is not a PWC(private working copy) check out for the 
     * file will be made.
     *
     * @param userName login username
     * @param password login password
     * @param pathToDocument path to document
     * @param fileType DocumentType type of document
     * @param newContent String new content to be set
     * @param majorVersion boolean true to set major version
     * @param checkinComment String check in comment
     * @return ObjectId object id of PWC
     */
    public ObjectId checkIn(final String userName,
                            final String password,
                            final String pathToDocument,
                            final DocumentType fileType,
                            final String newContent,
                            final boolean majorVersion,
                            final String checkinComment)
    {
        return checkIn(userName, password, true, pathToDocument, null, null, fileType, newContent, majorVersion, checkinComment);
    }
    
    private ObjectId checkIn(final String userName,
                             final String password,
                             final boolean byPath,
                             final String pathToDocument,
                             final String siteId,
                             final String docName,
                             final DocumentType fileType,
                             final String newContent,
                             final boolean majorVersion,
                             final String checkinComment)
    {
        Document pwc = null;
        Session session = getCMISSession(userName, password);
        Document docToModify = null;
        if(byPath)
        {
            docToModify = getDocumentObject(session, pathToDocument);
        }
        else
        {
            docToModify = getDocumentObject(session, siteId, docName);
        }
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
        byte[] content = null;
        try
        {
            content = newContent.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("Unable to read the new content", e);
        }
        try
        {
            stream = new ByteArrayInputStream(content);
            contentStream = session.getObjectFactory().createContentStream(docToModify.getName(), Long.valueOf(content.length), fileType.type, stream);
            try
            {
                return pwc.checkIn(majorVersion, null, contentStream, checkinComment);
            }
            catch(CmisStorageException se)
            {
                logger.error("Error when trying to checkin: ", se);
                contentStream = session.getObjectFactory().createContentStream(docToModify.getName(), Long.valueOf(content.length), fileType.type, stream);
                return pwc.checkIn(majorVersion, null, contentStream, checkinComment);
            }
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
     * Get the version of a file
     *
     * @param userName login username
     * @param password login password
     * @param pathToDocument
     * @return String file version
     */
    public String getVersion(final String userName,
                             final String password,
                             final String pathToDocument)
    {
        Session session = getCMISSession(userName, password);
        return getDocumentObject(session, pathToDocument).getVersionLabel();
    }
    
    private CmisObject copyTo(final Session session,
                              final String sourceSite,
                              final String contentName,
                              final String targetSite,
                              final String targetFolder,
                              final boolean byPath,
                              final String pathFrom,
                              final String pathTo)
    {
        CmisObject objTarget = null;
        CmisObject copiedContent = null;
        CmisObject objFrom = null;
        if(!byPath)
        {
            objFrom = getCmisObject(session, sourceSite, contentName);
            if(!StringUtils.isEmpty(targetFolder))
            {
                objTarget = getCmisObject(session, targetSite, targetFolder);
            }
            else
            {
                objTarget = getCmisObject(session, "/Sites/" + targetSite + "/documentLibrary");
            }
        }
        else
        {
            objFrom = getCmisObject(session, pathFrom);
            objTarget = getCmisObject(session, pathTo);
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
     * Copy file or folder
     *
     * @param userName user name
     * @param password password
     * @param sourceSite source site
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
        Session session = getCMISSession(userName, password);
        return copyTo(session, sourceSite, contentName, targetSite, targetFolder, false, null, null);
    }
    
    /**
     * Copy item by path
     * 
     * @param userName String user name
     * @param password String password
     * @param pathFrom String path from
     * @param pathTo String path to
     * @return
     */
    public CmisObject copyTo(final String userName,
                             final String password,
                             final String pathFrom,
                             final String pathTo)
    {
        Session session = getCMISSession(userName, password);
        return copyTo(session, null, null, null, null, true, pathFrom, pathTo);
    }
    
    /**
     * Copy item by path
     * 
     * @param session {@link Session}
     * @param pathFrom String path from
     * @param pathTo String path to
     * @return
     */
    public CmisObject copyTo(final Session session,
                             final String pathFrom,
                             final String pathTo)
    {
        return copyTo(session, null, null, null, null, true, pathFrom, pathTo);
    }

    /**
     * Copy folder with children
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
     * Copy children of the source folder to the target folder
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
                copyFolder((Folder) obj, targetFolder);
            }
        }
    }
    
    private CmisObject moveTo(final Session session,
                              final String sourceSite,
                              final String contentName,
                              final String targetSite,
                              final String targetFolder,
                              final boolean byPath,
                              final String pathFrom,
                              final String pathTo)
    {
        CmisObject objTarget = null;
        CmisObject movedContent = null;
        CmisObject objFrom = null;
        if(!byPath)
        {
            objFrom = getCmisObject(session, sourceSite, contentName);
            if(!StringUtils.isEmpty(targetFolder))
            {
                objTarget = getCmisObject(session, targetSite, targetFolder);
            }
            else
            {
                objTarget = getCmisObject(session, "/Sites/" + targetSite + "/documentLibrary");
            }
        }
        else
        {
            objFrom = getCmisObject(session, pathFrom);
            objTarget = getCmisObject(session, pathTo);
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
    
    /**
     * Move file or folder in site
     * 
     * @param userName user name
     * @param password password
     * @param siteName source site
     * @param contentName content to be moved
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
        Session session = getCMISSession(userName, password);
        return moveTo(session, sourceSite, contentName, targetSite, targetFolder, false, null, null);
    }
    
    /**
     * Move file or folder in site
     * 
     * @param session {@link Session}
     * @param siteName source site
     * @param contentName content to be moved
     * @param targetSite tartget site
     * @param targetFolder target folder. If null document library is set
     * @return CmisObject of new created object
     */
    public CmisObject moveTo(final Session session,
                             final String sourceSite,
                             final String contentName,
                             final String targetSite,
                             final String targetFolder)
    {
        return moveTo(session, sourceSite, contentName, targetSite, targetFolder, false, null, null);
    }
    
    /**
     * Move file or folder by path
     * 
     * @param userName user name
     * @param password password
     * @param pathFrom path to source
     * @param pathTo path to content
     * @return CmisObject of new created object
     */
    public CmisObject moveTo(final String userName,
                             final String password,
                             final String pathFrom,
                             final String pathTo)
    {
        Session session = getCMISSession(userName, password);
        return moveTo(session, null, null, null, null, true, pathFrom, pathTo);
    }
    
    /**
     * Move file or folder by path
     * 
     * @param session {@link Session}
     * @param pathFrom path to source
     * @param pathTo path to content
     * @return CmisObject of new created object
     */
    public CmisObject moveTo(final Session session,
                             final String pathFrom,
                             final String pathTo)
    {
        return moveTo(session, null, null, null, null, true, pathFrom, pathTo);
    }

    @SuppressWarnings("unchecked")
    private boolean managePermission(final String userName,
                                     final String password,
                                     final String siteName,
                                     final String contentName,
                                     final boolean isUser,
                                     final String userToAdd,
                                     final String groupName,
                                     final String role,
                                     final boolean isInherited,
                                     final boolean remove)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String node = getNodeRef(userName, password, siteName, contentName);
        if(StringUtils.isEmpty(node))
        {
            throw new RuntimeException("Invalid content " + contentName);
        }
        String api = client.getAlfrescoUrl() + "alfresco/s/slingshot/doclib/permissions/workspace/SpacesStore/" + node;
        HttpPost post = new HttpPost(api);
        JSONObject permission = new JSONObject();
        if(isUser)
        {
            permission.put("authority", userToAdd);
        }
        else
        {
            permission.put("authority", "GROUP_" + groupName);
        }
        permission.put("role", role);
        if(remove)
        {
            permission.put("remove", true);
        }
        JSONArray array = new JSONArray();
        array.add(permission);
        JSONObject body = new JSONObject();
        body.put("permissions", array);
        body.put("isInherited", isInherited);
        HttpResponse response = client.executeRequest(userName, password, body, post);
        if(200 == response.getStatusLine().getStatusCode())
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Successfuly added permission for: " + userToAdd);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Set permission for a user
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param contentName String content name
     * @param userToAdd user to set the permission
     * @param role String role
     * @param isInherited boolean is inherited
     * @return true (200 OK) if permission is set
     */
    public boolean setPermissionForUser(final String userName,
                                        final String password,
                                        final String siteName,
                                        final String contentName,
                                        final String userToAdd,
                                        final String role,
                                        final boolean isInherited)
    {
        return managePermission(userName, password, siteName, contentName, true, userToAdd, null, role, isInherited, false);
    }
    
    /**
     * Set permission for a group
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param contentName String content name
     * @param groupName group to set the permission
     * @param role String role
     * @param isInherited boolean is inherited
     * @return true (200 OK) if permission is set
     */
    public boolean setPermissionForGroup(final String userName,
                                         final String password,
                                         final String siteName,
                                         final String contentName,
                                         final String groupName,
                                         final String role,
                                         final boolean isInherited)
    {
        return managePermission(userName, password, siteName, contentName, false, null, groupName, role, isInherited, false);
    }
    
    /**
     * Remove permission from a user
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param contentName String content name
     * @param userToAdd user to set the permission
     * @param role String role
     * @param isInherited boolean is inherited
     * @return true (200 OK) if permission is set
     */
    public boolean removePermissionForUser(final String userName,
                                           final String password,
                                           final String siteName,
                                           final String contentName,
                                           final String userToRemove,
                                           final String role,
                                           final boolean isInherited)
    {
        return managePermission(userName, password, siteName, contentName, true, userToRemove, null, role, isInherited, true);
    }

    /**
     * Remove permission from a group
     * 
     * @param userName String user name
     * @param password String password
     * @param siteName String site name
     * @param contentName String content name
     * @param groupName group to set the permission
     * @param role String role
     * @param isInherited boolean is inherited
     * @return true (200 OK) if permission is set
     */
    public boolean removePermissionForGroup(final String userName,
                                            final String password,
                                            final String siteName,
                                            final String contentName,
                                            final String groupToRemove,
                                            final String role,
                                            final boolean isInherited)
    {
        return managePermission(userName, password, siteName, contentName, false, null, groupToRemove, role, isInherited, true);
    }
    
    /**
     * Rename content (file or folder) in site
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param contentName
     * @param newName
     */
    public void renameContent(final String userName,
                              final String password,
                              final String siteName,
                              final String contentName,
                              final String newName)
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, newName);
        Session session = getCMISSession(userName, password);
        addProperties(session, getNodeRef(session, siteName, contentName), properties);
    }
    
    /**
     * Rename content (file or folder) from path
     * 
     * @param userName
     * @param password
     * @param contentPath
     * @param newName
     */
    public void renameContent(final String userName,
                              final String password,
                              final String contentPath,
                              final String newName)
    {
        Session session = getCMISSession(userName, password);
        renameContent(session, contentPath, newName);
    }
    
    /**
     * Rename content (file or folder) from path
     * 
     * @param session {@link Session}
     * @param contentPath
     * @param newName
     */
    public void renameContent(final Session session,
                              final String contentPath,
                              final String newName)
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, newName);
        addProperties(session, getNodeRefByPath(session, contentPath), properties);
    }
}
