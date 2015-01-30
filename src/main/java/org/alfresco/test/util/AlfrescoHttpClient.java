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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
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

    public AlfrescoHttpClient()
    {
        client = HttpClientBuilder.create().build();
    }
    /**
     * Parase the share url and returns the alfresco
     * api url.
     * @param shareUrl
     * @return String the url + alfresco/service/api/
     */
    public String parsePath(final String shareUrl)
    {
        if(StringUtils.isEmpty(shareUrl))
        {
            throw new IllegalArgumentException("URL is required");
        }
        return shareUrl.replaceFirst("share", AlfrescoHttpClient.ALFRESCO_API_PATH);
    }
    /**
     * Generates an Alfresco Authentication Ticket based on the user name and password passed in.
     * 
     * @param alfUrl
     * @param userName
     * @param password
     * @throws ParseException 
     */
    public String getAlfTicket(String shareUrl, String username, String password) throws IOException, ParseException
    {
        String alfUrl = parsePath(shareUrl);
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            throw new IllegalArgumentException("Username and password are required");
        }
        try
        {
            URL url = new URL(alfUrl + "login?u=" + username + "&pw=" + password + "&format=json");
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
     * @param requestURL
     * @param body
     * @return {@link HttpPost} post request in json format
     * @throws Exception
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
     * 
     * @param HttpClient
     * @param HttpPost
     * @return HttpResponse
     * @throws Exception
     */
    public HttpResponse executeRequest(HttpRequestBase request) throws Exception
    {
        HttpResponse response = null;
        try
        {
            response = client.execute(request);
            logger.info("Status Received:" + response.getStatusLine());
            return response;
        }
        catch (Exception e)
        {
            logger.error(response);
            throw new RuntimeException("Error during execute request", e);
        }
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

}
