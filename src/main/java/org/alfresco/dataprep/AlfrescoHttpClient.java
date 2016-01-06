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
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Helper class that provides HttpClient.
 * 
 * @author Michael Suzuki
 */
public class AlfrescoHttpClient
{
    private static Log logger = LogFactory.getLog(AlfrescoHttpClient.class);
    public static final String UTF_8_ENCODING = "UTF-8";
    public static final String MIME_TYPE_JSON = "application/json";
    public static String ALFRESCO_API_PATH = "alfresco/service/api/";
    public static String ALFRESCO_API_VERSION = "-default-/public/alfresco/versions/1/";
    private CloseableHttpClient client;
    private String scheme;
    private String host;
    private int port;
    private String apiUrl;
    private String alfrescoUrl;

    public AlfrescoHttpClient(final String scheme, final String host)
    {
        this(scheme, host, 80);
    }
    public AlfrescoHttpClient(final String scheme, final String host, final int port)
    {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        apiUrl = String.format("%s://%s:%d/%s", scheme, host, port,ALFRESCO_API_PATH);
        client = HttpClientBuilder.create().build();
    }
    
    public String getApiVersionUrl()
    {
        String versionApi = getApiUrl().replace("/service", "");
        return versionApi + ALFRESCO_API_VERSION;
    }
    
    /**
     * Generates an Alfresco Authentication Ticket based on the user name and password passed in.
     * 
     * @param username user identifier
     * @param password user password
     * @return String authentication ticket
     */
    public String getAlfTicket(String username, String password)
    {
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            throw new IllegalArgumentException("Username and password are required");
        }
        String targetUrl = apiUrl + "login?u=" + username + "&pw=" + password + "&format=json";
        try
        {
            URL url = new URL(apiUrl + "login?u=" + username + "&pw=" + password + "&format=json");
            URLConnection con = url.openConnection();
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? UTF_8_ENCODING : encoding;
            String json = IOUtils.toString(in, encoding);
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject)parser.parse(json);
            JSONObject data = (JSONObject) obj.get("data");
            return (String) data.get("ticket");
        }
        catch (IOException | ParseException e)
        {
            logger.error(String.format("Unable to generate ticket, url: %s",targetUrl), e);
        }
        throw new RuntimeException("Unable to get ticket");
    }
    
    /**
     * Get the alfresco version
     * @return String version of alfresco
     */
    public String getAlfrescoVersion()
    {
        String url = apiUrl + "server";
        HttpGet get = new HttpGet(url);
        HttpResponse response = execute("", "", get);
        if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
        {
            try
            {
                String json_string = EntityUtils.toString(response.getEntity());
                JSONParser parser = new JSONParser();
                JSONObject obj = (JSONObject) parser.parse(json_string);
                JSONObject data = (JSONObject) obj.get("data");
                return (String) data.get("version");
            }
            catch (IOException | ParseException e)
            {
                return "";
            }
        }
        return "";
    }

    /**
     * Populate HTTP message call with given content.
     * 
     * @param json {@link JSONObject} content
     * @return {@link StringEntity} content.
     */
    public StringEntity setMessageBody(final JSONObject json)
    {
        if (json == null || json.toString().isEmpty())
        {
            throw new IllegalArgumentException("JSON Content is required.");
        }
        StringEntity se = new StringEntity(json.toString(), UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, MIME_TYPE_JSON));
        if (logger.isDebugEnabled())
        {
            logger.debug("Json string value: " + se);
        }
        return se;
    }

    /**
     * Execute HttpClient request without releasing the connection
     * @param userName user name
     * @param password password
     * @param request to send 
     * @return {@link HttpResponse} response
     */
    public HttpResponse execute(final String userName,
                                final String password,
                                HttpRequestBase request)
    {
        HttpResponse response = null;
        if(!StringUtils.isEmpty(userName) || !StringUtils.isEmpty(password))
        {
            client = getHttpClientWithBasicAuth(userName, password);
        }
        try
        {
            response = client.execute(request);
            if(logger.isTraceEnabled())
            {
                logger.trace("Status Received:" + response.getStatusLine());
            }
            return response;
        }
        catch (IOException e)
        {
            logger.error(response);
            throw new RuntimeException("Error while executing request", e);
        }
    }
    
    /**
     * Execute HttpClient request.
     * @param userName String user name 
     * @param password String password
     * @param request HttpRequestBase the request
     * @return {@link HttpResponse} response
     */
    public HttpResponse executeRequest(final String userName,
                                       final String password,
                                       HttpRequestBase request)
    {
        HttpResponse response = null;
        client = getHttpClientWithBasicAuth(userName, password);
        try
        {
            response = client.execute(request);
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new RuntimeException("Invalid user name or password");
            }
        }
        catch (IOException e)
        {
            logger.error(response);
            throw new RuntimeException("Error while executing request", e);
        }
        finally
        {
            request.releaseConnection();
            close();
        }
        return response;
    }
    
    /**
     * Execute HttpClient POST OR PUT
     * 
     * @param userName String user name 
     * @param password String password
     * @param body JSONObject body of the request
     * @param request HttpEntityEnclosingRequestBase the request
     * @return {@link HttpResponse} response
     */
    public HttpResponse executeRequest(final String userName,
                                       final String password,
                                       final JSONObject body,
                                       HttpEntityEnclosingRequestBase request)
    {
        HttpResponse response = null;
        client = getHttpClientWithBasicAuth(userName, password);
        request.setEntity(setMessageBody(body));
        try
        {   
            response = client.execute(request);
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new RuntimeException("Invalid user name or password");
            }
        } 
        catch(IOException e)
        {
            logger.error(response);
            throw new RuntimeException("Error while executing request", e);
        }
        finally
        {
            request.releaseConnection();
            close();
        }
        return response;
    }
    
    /**
     * Get basic http client with basic credential.
     * @param username String username 
     * @param password String password
     * @return {@link CloseableHttpClient} client
     */
    public CloseableHttpClient getHttpClientWithBasicAuth(String username, String password)
    {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        provider.setCredentials(AuthScope.ANY, credentials);
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
        return client;
    }
    
    /**
     * Parses http response stream into a {@link JSONObject}.
     * 
     * @param entity Http response entity
     * @return {@link JSONObject} response
     */
    public JSONObject readStream(final HttpEntity entity)
    {
        String rsp = null;
        try
        {
            rsp = EntityUtils.toString(entity, UTF_8_ENCODING);
        }
        catch (Throwable ex)
        {
            throw new RuntimeException("Failed to read HTTP entity stream.", ex);
        }
        finally
        {
            EntityUtils.consumeQuietly(entity);
        }
        try
        {
            JSONParser parser = new JSONParser();
            JSONObject result = (JSONObject) parser.parse(rsp);
            return result;
        }
        catch (Throwable e)
        {
            throw new RuntimeException("Failed to convert response to JSON: \n" + "   Response: \r\n" + rsp, e);
        }
    }
    
    /**
     * Method to get parameters from JSON
     * 
     * @param result json message
     * @param param key identifier
     * @return String value of key
     */
    public String getParameterFromJSON(String result,
                                       String param)
    {
        JSONParser parser = new JSONParser();
        JSONObject obj;
        try
        {
            obj = (JSONObject) parser.parse(result);
        }
        catch (ParseException e)
        {
            throw new RuntimeException("Failed to parse the result: " + result, e);
        }
        return (String) obj.get(param);
    }
    
    /**
     * Method to get parameters from JSON
     * 
     * @param response HttpResponse response
     * @param jsonObject String json object from response
     * @param parameter String parameter from json
     * @return String result
     */
    public String getParameterFromJSON(HttpResponse response,
                                       String jsonObject,
                                       String parameter)
    { 
        String strResponse = "";
        try
        {
            strResponse = EntityUtils.toString(response.getEntity());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read the response", e);
        }
        Object obj = JSONValue.parse(strResponse);
        JSONObject jObj = (JSONObject) obj;
        JSONObject entry = (JSONObject) jObj.get(jsonObject);
        return (String) entry.get(parameter);
    }
    
    /**
     * Get the elements from a JSONArray
     * 
     * @param response HttpResponse response
     * @param array String the name of the array
     * @param elementFromArray element from array
     * @return List<String> elements found in the array
     */
    public List<String> getElementsFromJsonArray(HttpResponse response,
                                                 String array,
                                                 String elementFromArray)
    {
        List<String>elements = new ArrayList<String>();
        HttpEntity entity = response.getEntity();
        String responseString = "";
        try
        {
            responseString = EntityUtils.toString(entity , "UTF-8");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read the response", e);
        }
        Object obj = JSONValue.parse(responseString);
        JSONObject jsonObject = (JSONObject) obj;
        JSONArray jArray = (JSONArray) jsonObject.get(array);
        for (Object item:jArray)
        {
            JSONObject jobject = (JSONObject) item;
            elements.add(jobject.get(elementFromArray).toString());
        }
        return elements;
    }
    
    public String getSpecificElementFromJArray(HttpResponse response,
                                               String array,
                                               String itemName,
                                               String itemParameter,
                                               String requiredElement)
    {
        HttpEntity entity = response.getEntity();
        String responseString = "";
        try
        {
            responseString = EntityUtils.toString(entity , "UTF-8");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read the response", e);
        }
        Object obj = JSONValue.parse(responseString);
        JSONObject jsonObject = (JSONObject) obj;
        JSONArray jArray = (JSONArray) jsonObject.get(array);
        for (Object item:jArray)
        {
            JSONObject jobject = (JSONObject) item;
            if(jobject.get(itemParameter).toString().equals(itemName))
            {
                return (String) jobject.get(requiredElement);
            }
        }
        return "";
    }
    
    /**
     * Closes the HttpClient. 
     * @throws IOException if error
     */
    public void close() 
    {
        try
        {
            client.close();
        } 
        catch (IOException e)
        {
            logger.error("Unable to close http client" ,e);
        }
    }
    
    /**
     * Closes the HttpClient.
     * @param client CloseableHttpClient the client
     * @throws IOException if error
     */
    public void close(CloseableHttpClient client)
    {
        try
        {
            client.close();
        } 
        catch (IOException e)
        {
            logger.error("Unable to close http client" ,e);
        }
    }
    
    public HttpClient getClient()
    {
        return client;
    }
    
    public String getScheme()
    {
        return scheme;
    }
    
    public String getHost()
    {
        return host;
    }
    
    public int getPort()
    {
        return port;
    }
    
    public String getApiUrl()
    {
        return apiUrl;
    }
    
    public String getAlfrescoUrl()
    {
        alfrescoUrl = String.format("%s://%s:%d/", scheme, host, port);
        return alfrescoUrl;
    }
}
