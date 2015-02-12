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
package org.alfresco.test.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;

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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
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
    private CloseableHttpClient client;
    private String scheme;
    private String host;
    private int port;
    private String apiUrl;

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
    
    /**
     * Generates an Alfresco Authentication Ticket based on the user name and password passed in.
     * 
     * @param username user identifier
     * @param password user password
     * @throws ParseException if error
     * @throws IOException if error
     * @return Sting authentication ticket
     */
    public String getAlfTicket(String username, String password) throws IOException, ParseException
    {
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            throw new IllegalArgumentException("Username and password are required");
        }
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
        catch (IOException e)
        {
            logger.error("Unable to generate ticket ", e);
        }
        throw new RuntimeException("Unable to get ticket");
    }

    /**
     * Creates the post request message body.
     * @param requestURL url end point
     * @param body content of request
     * @return {@link HttpPost} post request in json format
     * @throws Exception if error
     */
    public HttpPost generatePostRequest(String requestURL, JSONObject body) throws Exception
    {
        // Parameters check
        if (requestURL.isEmpty())
        {
            throw new IllegalArgumentException("Empty Request URL: Please correct");
        }
        if(body == null)
        {
            throw new IllegalArgumentException("JSON body is required");
        }
        HttpPost request = new HttpPost(requestURL);
        // set Headers
        String contentType = MIME_TYPE_JSON + ";charset=" + UTF_8_ENCODING;
        request.addHeader("Content-Type", contentType);
        // set Body
        request.setEntity(setMessageBody(body));

        return request;
    }


    /**
     * Populate HTTP message call with given content.
     * 
     * @param json {@link JSONObject} content
     * @return {@link StringEntity} content.
     * @throws UnsupportedEncodingException if unsupported
     */
    private StringEntity setMessageBody(final JSONObject json) throws UnsupportedEncodingException
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
     * @throws Exception if error
     */
    public HttpResponse executeRequest(HttpRequestBase request) throws Exception
    {
        HttpResponse response = null;
        try
        {
            response = client.execute(request);
            if(logger.isTraceEnabled())
            {
                logger.trace("Status Received:" + response.getStatusLine());
            }
            return response;
        }
        catch (Exception e)
        {
            logger.error(response);
            throw new RuntimeException("Error during execute request", e);
        }
    }
    /**
     * Get basic http client with basic credential.
     * @param username String username 
     * @param password String password
     * @return {@link HttpClient} client
     */
    public HttpClient getHttpClientWithBasicAuth(String username, String password)
    {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        provider.setCredentials(AuthScope.ANY, credentials);
        HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
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
     * @throws ParseException if error parsing
     */
    public String getParameterFromJSON(String result, String param) throws ParseException
    {
        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject) parser.parse(result);
        return (String) obj.get(param);
    }
    

    /**
     * Closes the HttpClient. 
     * @throws IOException if error
     */
    public void close() throws IOException
    {
        client.close();
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

}
