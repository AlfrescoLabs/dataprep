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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
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
    private int sharePort;
    private String apiUrl;
    private String alfrescoUrl;
    private String adminUser;
    private String adminPassword;

    public AlfrescoHttpClient(final String scheme, final String host, final int port, final int sharePort)
    {
        this(scheme, host, port, sharePort, null, null);
    }

    public AlfrescoHttpClient(final String scheme, final String host, final int port, final int sharePort, final String adminUser, final String adminPassword)
    {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.sharePort = sharePort;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
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
     * Get the alfresco version
     * 
     * @return double version of alfresco
     */
    public double getAlfVersion()
    {
        return Double.valueOf(StringUtils.substring(getAlfrescoVersion(), 0, 3));
    }

    /**
     * Populate HTTP message call with given content.
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
    * Execute HttpClient request.
    * @param request to send 
    * @return {@link HttpResponse} response
    */
    private HttpResponse execute(HttpRequestBase request)
    {
        HttpResponse response = null;
        try
        {
            response = client.execute(request);
            if(logger.isTraceEnabled())
            {
                logger.trace("Status Received:" + response.getStatusLine());
            }
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new RuntimeException("Invalid user name or password");
            }
            return response;
        }
        catch (Exception e)
        {
            logger.error(response);
            throw new RuntimeException("Error while executing request", e);
        }
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
        if(!StringUtils.isEmpty(userName) || !StringUtils.isEmpty(password))
        {
            client = getHttpClientWithBasicAuth(userName, password);
            setBasicAuthorization(userName, password, request);
        }
        return execute(request);
    }

    /**
     * Execute HttpClient request as admin user without releasing the connection
     * @param request to send 
     * @return {@link HttpResponse} response
     */
    public HttpResponse executeAsAdmin(HttpRequestBase request)
    {
        if(!StringUtils.isEmpty(this.adminUser) || !StringUtils.isEmpty(this.adminPassword))
        {
            client = getHttpClientWithBasicAuth(this.adminUser, this.adminPassword);
            setBasicAuthorization(this.adminUser, this.adminPassword, request);
        }
        return execute(request);
    }

    /**
     * Execute HttpClient request.
     * @param userName String user name 
     * @param password String password
     * @param request HttpRequestBase the request
     * @return {@link HttpResponse} response
     */
    public HttpResponse executeAndRelease(final String userName,
                                          final String password,
                                          HttpRequestBase request)
    {
        HttpResponse response;
        client = getHttpClientWithBasicAuth(userName, password);
        setBasicAuthorization(userName, password, request);
        try
        {
            response = execute(request);
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
     * @param userName String user name 
     * @param password String password
     * @param body JSONObject body of the request
     * @param request HttpEntityEnclosingRequestBase the request
     * @return {@link HttpResponse} response
     */
    public HttpResponse executeAndRelease(final String userName,
                                          final String password,
                                          final JSONObject body,
                                          HttpEntityEnclosingRequestBase request)
    {
        HttpResponse response;
        client = getHttpClientWithBasicAuth(userName, password);
        request.setEntity(setMessageBody(body));
        setBasicAuthorization(userName, password, request);
        try
        {
            response = execute(request);
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
    public CloseableHttpClient getHttpClientWithBasicAuth(String username,
                                                          String password)
    {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        provider.setCredentials(AuthScope.ANY, credentials);
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
        return client;
    }

    private HttpRequestBase setBasicAuthorization(String userName, String password, HttpRequestBase request)
    {
        byte[] credentials = Base64.encodeBase64((userName + ":" + password).getBytes(StandardCharsets.UTF_8));
        request.setHeader("Authorization", "Basic " + new String(credentials, StandardCharsets.UTF_8));
        return request;
    }
    
    /**
     * Parses http response stream into a {@link JSONObject}.
     * @param entity Http response entity
     * @return {@link JSONObject} response
     */
    public JSONObject readStream(final HttpEntity entity)
    {
        String rsp;
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
    
    private String getParameterFromJSON(boolean httpResp,
                                        String stringResponse,
                                        HttpResponse response,
                                        String parameter,
                                        String... jsonObjs)
    {
        String strResponse;
        Object obj;
        if(httpResp)
        {
            strResponse = readStream(response.getEntity()).toJSONString();
            obj = JSONValue.parse(strResponse);
        }
        else
        {
            obj = JSONValue.parse(stringResponse);
        }
        JSONObject jObj = (JSONObject) obj;
        if(!StringUtils.isEmpty(jsonObjs[0]))
        {
            for(int i=0; i<jsonObjs.length;i++)
            {
                jObj = (JSONObject) jObj.get(jsonObjs[i]);
            }
            return jObj.get(parameter).toString();
        }
        else
        {
            return (String) jObj.get(parameter);
        }
    }
    
    /**
     * Get an element from HttpResponse
     * 
     * @param response response
     * @param parameter wanted parameter
     * @param jsonObjs json objects to reach the parameter
     * @return
     */
    public String getParameterFromJSON(HttpResponse response,
                                       String parameter,
                                       String... jsonObjs)
    {
        return getParameterFromJSON(true, null, response, parameter, jsonObjs);
    }
    
    /**
     * Get an element from a string HttpResponse
     * 
     * @param response String response 
     * @param parameter String wanted parameter
     * @param jsonObjs String json objects to reach the parameter
     * @return
     */
    public String getParameterFromJSON(String response,
                                       String parameter,
                                       String... jsonObjs)
    {
        return getParameterFromJSON(false, response, null, parameter, jsonObjs);
    }
    
    /**
     * Get the elements from a JSONArray
     * @param response HttpResponse response
     * @param array String the name of the array
     * @param elementFromArray element from array
     * @return List<String> elements found in the array
     */
    public List<String> getElementsFromJsonArray(HttpResponse response,
                                                 String objAbove,
                                                 String array,
                                                 String elementFromArray)
    {
        List<String>elements = new ArrayList<String>();
        HttpEntity entity = response.getEntity();
        String responseString;
        try
        {
            responseString = EntityUtils.toString(entity , "UTF-8");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read the response", e);
        }
        Object obj = JSONValue.parse(responseString);
        JSONObject jsonObject;
        if(StringUtils.isEmpty(objAbove))
        {
            jsonObject = (JSONObject) obj;
        }
        else
        {
            jsonObject = (JSONObject) obj;
            jsonObject = (JSONObject) jsonObject.get(objAbove);
        }
        JSONArray jArray = (JSONArray) jsonObject.get(array);
        for (Object item:jArray)
        {
            JSONObject jobject = (JSONObject) item;
            elements.add(jobject.get(elementFromArray).toString());
        }
        return elements;
    }
    
    public String getSpecificElementFromJArray(HttpResponse response,
                                               String firstParam,
                                               String array,
                                               String itemName,
                                               String itemParameter,
                                               String requiredElement)
    {
        JSONArray jArray = getJSONArray(response, firstParam, array);
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
     * Get JSONArray from HttpResponse
     * @param response HttpResponse the request response
     * @param firstParam String parameter that has the array
     * @param arrayName String array name
     * @return JSONArray the array
     */
    public JSONArray getJSONArray(HttpResponse response,
                                  String firstParam,
                                  String arrayName)
    {
        HttpEntity entity = response.getEntity();
        String responseString;
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
        if(!StringUtils.isEmpty(firstParam))
        {
            jsonObject = (JSONObject) jsonObject.get(firstParam);
        }
        return (JSONArray) jsonObject.get(arrayName);
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
    
    public int getSharePort()
    {
        return sharePort;
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
    
    public String getShareUrl()
    {
        return String.format("%s://%s:%d/", scheme, host, sharePort);
    }
}
