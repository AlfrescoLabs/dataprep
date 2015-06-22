package org.alfresco.test.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.json.simple.parser.ParseException;

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
     * Create single tag and comment for document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param option action type
     * @param value String data
     * @return true if request is successful
     * @throws Exception if error
     */
    @SuppressWarnings("unchecked")
    private boolean addNewAction(final String userName,
                                 final String password, 
                                 final String siteName,
                                 final String contentName,
                                 final ActionType option,
                                 final String value) throws Exception
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
                        logger.trace(value + " was added successfully");
                    }
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    throw new RuntimeException("Content doesn't exists " + contentName);
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new RuntimeException("Invalid user name or password");
                default:
                    logger.error("Unable to add new action: " + response.toString());
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
     * Create multiple tags or comments for document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param optType action type
     * @param values List of values
     * @return true if request is successful
     * @throws Exception if error
     */
    private boolean addMultipleActions(final String userName,
                                       final String password, 
                                       final String siteName,
                                       final String contentName,
                                       final ActionType optType,
                                       final List<String> values) throws Exception 
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
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(post);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_CREATED:
                    return true;
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new RuntimeException("Invalid user name or password");
                default:
                    logger.error("Unable to add new action: " + response.toString());
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
     * Remove tag or comment from document or folder
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @param optType content actions (e.g. tag, comments, likes)
     * @param actionValue value of the action(e.g tag value, comment content)
     * @return true if tag or comment is removed 
     * @throws Exception if error
     */
    private boolean removeAction(final String userName,
                                 final String password,
                                 final String siteName,
                                 final String contentName,
                                 final ActionType optType,
                                 final String actionValue) throws Exception
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
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(delete);           
            if( HttpStatus.SC_NO_CONTENT  == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace(actionValue + " is removed successfully");
                }
                return true;
            }
        }
        finally
        {
            delete.releaseConnection();
            client.close();
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
     * @throws Exception if error
     */
    public boolean addSingleTag(final String userName,
                                final String password, 
                                final String siteName,
                                final String contentName,
                                final String tag) throws Exception 
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
     * @throws Exception if error
     */
    public boolean addMultipleTags(final String userName,
                                   final String password, 
                                   final String siteName,
                                   final String contentName,
                                   final List<String> tags) throws Exception 
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
     * @throws Exception if error
     */
    private String getOptionResponse(final String userName,
                                     final String password, 
                                     final String siteName,
                                     final String contentName,
                                     final ActionType actionType) throws Exception
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
     * Return a list of tags or comments
     * 
     * @param response HttpResponse as String
     * @param optType content actions (e.g. tag, comments, likes)
     */
    @SuppressWarnings("unchecked")
    private List<String> getOptionValues(final String response,
                                         final ActionType optType) throws ParseException
    {
        List<String> values = new ArrayList<String>();     
        if(!StringUtils.isEmpty(response))
        {
            JSONParser parser = new JSONParser();  
            Object obj = parser.parse(response);
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
                                    final ActionType optType) throws ParseException
    {
        String nodeRef = "";
        if(!StringUtils.isEmpty(response))
        {
            JSONParser parser = new JSONParser();  
            Object obj = parser.parse(response);
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
     * @throws Exception if error
     */
    public List<String> getTagNamesFromContent(final String userName,
                                               final String password, 
                                               final String siteName,
                                               final String contentName) throws Exception
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
     * @throws Exception if error
     */
    public String getTagNodeRef(final String userName,
                                final String password, 
                                final String siteName,
                                final String contentName,
                                final String tagName) throws Exception
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
     * @throws Exception if error
     */   
    public boolean removeTag(final String userName,
                             final String password,
                             final String siteName,
                             final String contentName,
                             final String tagName) throws Exception
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
     * @throws Exception if error
     */
    public boolean addComment(final String userName,
                              final String password,
                              final String siteName,
                              final String contentName,
                              final String comment) throws Exception
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
     * @throws Exception if error
     */
    public boolean addMultipleComments(final String userName,
                                       final String password, 
                                       final String siteName,
                                       final String contentName,
                                       final List<String> comments) throws Exception 
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
     * @throws Exception if error
     */
    public List<String> getComments(final String userName,
                                    final String password, 
                                    final String siteName,
                                    final String contentName) throws Exception
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
     * @throws Exception if error
     */
    public String getCommentNodeRef(final String userName,
                                    final String password, 
                                    final String siteName,
                                    final String contentName,
                                    final String comment) throws Exception
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
     * @throws Exception if error
     */   
    public boolean removeComment(final String userName,
                                 final String password,
                                 final String siteName,
                                 final String contentName,
                                 final String comment) throws Exception
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
     * @throws Exception if error
     */
    public boolean likeContent(final String userName,
                               final String password,
                               final String siteName,
                               final String contentName) throws Exception
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
     * @throws Exception if error
     */
    public int countLikes(final String userName,
                          final String password,
                          final String siteName,
                          final String contentName) throws Exception
    {
        String result = getOptionResponse(userName, password, siteName, contentName, ActionType.LIKES);
        if(!StringUtils.isEmpty(result))
        {
            JSONParser parser = new JSONParser();  
            Object obj = parser.parse(result);
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
     * @throws Exception if error
     */   
    public boolean removeLike(final String userName,
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
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);      
        if(StringUtils.isEmpty(contentNodeRef))
        {
            throw new RuntimeException("Content doesn't exists");
        }
        String reqUrl = client.getApiVersionUrl() + "nodes/" + contentNodeRef + "/ratings/likes";        
        try
        {
            HttpDelete delete = new HttpDelete(reqUrl);
            HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
            HttpResponse response = clientWithAuth.execute(delete);           
            if( HttpStatus.SC_NO_CONTENT  == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("Like is removed successfully from " + contentName);
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
    
    /**
     * Set document or folder as favorite
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentType ('file' or 'folder')
     * @param contentName file of folder name
     * @return true if marked as favorite
     * @throws Exception if error
     */ 
    private boolean setFavorite(final String userName,
                                final String password,
                                final String siteName,
                                final String contentType,
                                final String contentName) throws Exception
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
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            HttpResponse response = clientWithAuth.execute(post);
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_CREATED:        
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    throw new RuntimeException("Content doesn't exists " + contentName);
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new RuntimeException("Invalid user name or password ");
                default:
                    logger.error("Unable to mark as favorite: " + response.toString());
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
     * Set a document as favorite
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param fileName file name
     * @return true if marked as favorite
     * @throws Exception if error
     */ 
    public boolean setFileAsFavorite(final String userName,
                                     final String password,
                                     final String siteName,
                                     final String fileName) throws Exception
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
     * @throws Exception if error
     */ 
    public boolean setFolderAsFavorite(final String userName,
                                       final String password,
                                       final String siteName,
                                       final String folderName) throws Exception
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
     * @throws Exception if error
     */
    public boolean removeFavorite(final String userName,
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
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);      
        if(StringUtils.isEmpty(contentNodeRef))
        {
            throw new RuntimeException("Content doesn't exists");
        }
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites/" + contentNodeRef;;        
        try
        {
            HttpDelete delete = new HttpDelete(reqUrl);
            HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
            HttpResponse response = clientWithAuth.execute(delete);           
            if( HttpStatus.SC_NO_CONTENT  == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("favorite is removed successfully");
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
    
    /**
     * Verify if a document or folder is marked as favorite
     * 
     * @param userName login username
     * @param password login password
     * @param siteName site name
     * @param contentName file or folder name
     * @return true if marked as favorite
     * @throws Exception if error
     */
    public boolean isFavorite(final String userName,
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
        String contentNodeRef = getNodeRef(userName, password, siteName, contentName);      
        if(StringUtils.isEmpty(contentNodeRef))
        {
            throw new RuntimeException("Content doesn't exists");
        }
        String reqUrl = client.getApiVersionUrl() + "people/" + userName + "/favorites/" + contentNodeRef;;        
        try
        {
            HttpGet get = new HttpGet(reqUrl);
            HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
            HttpResponse response = clientWithAuth.execute(get);           
            if( HttpStatus.SC_OK  == response.getStatusLine().getStatusCode())
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace( "Content " + contentName + "is marked as favorite");
                }
                return true;
            }
            else
            {
                return false;
            }
        }
        finally
        {
            client.close();
        }    
    }
}
