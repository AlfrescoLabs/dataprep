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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;

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
     * @throws ParseException if error
     * @throws IOException if error
     * @return Sting authentication ticket
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
     * @throws Exception if error
     */
    public String getAlfrescoVersion() throws Exception
    {
        String url = apiUrl + "server";
        HttpGet get = new HttpGet(url);
        HttpResponse response = client.execute(get);
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
     * @throws UnsupportedEncodingException if unsupported
     */
    public StringEntity setMessageBody(final JSONObject json) throws UnsupportedEncodingException
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
    public HttpResponse executeRequest(HttpRequestBase request)
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
            throw new RuntimeException("Error while executing request", e);
        }
    }
    
    /**
     * Execute HttpClient request.
     * @param client AlfrescoHttpClient client
     * @param userName String user name 
     * @param password String password
     * @param url String api url
     * @param request HttpRequestBase the request
     * @return {@link HttpResponse} response
     * @throws Exception if error
     */
    public HttpResponse executeRequest(AlfrescoHttpClient client,
                                      final String userName,
                                      final String password,
                                      final String url,
                                      HttpRequestBase request) throws Exception
    {
        HttpResponse response = null;
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            response = clientWithAuth.execute(request);
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new RuntimeException("Invalid user name or password");   
            }      
        } 
        catch(Exception e)
        {
            logger.error(response);
            throw new RuntimeException("Error while executing request", e);
        }
        finally
        {
            request.releaseConnection();
            client.close();
        }
        return response;
    }
    
    /**
     * Execute HttpClient POST OR PUT
     * 
     * @param client AlfrescoHttpClient client
     * @param userName String user name 
     * @param password String password
     * @param url String api url
     * @param body JSONObject body of the request
     * @param request HttpEntityEnclosingRequestBase the request
     * @return {@link HttpResponse} response
     * @throws Exception if error
     */
    public HttpResponse executeRequest(AlfrescoHttpClient client,
                                      final String userName,
                                      final String password,
                                      final String url,
                                      final JSONObject body,
                                      HttpEntityEnclosingRequestBase request) throws Exception
    {
        HttpResponse response = null;
        HttpClient clientWithAuth = client.getHttpClientWithBasicAuth(userName, password);
        try
        {
            request.setEntity(setMessageBody(body));
        } 
        catch(UnsupportedEncodingException e)
        {
            throw new RuntimeException("Body content error: " ,e);
        }
        try
        {   
            response = clientWithAuth.execute(request);
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new RuntimeException("Invalid user name or password");   
            }      
        } 
        catch(Exception e)
        {
            logger.error(response);
            throw new RuntimeException("Error while executing request", e);
        }
        finally
        {
            request.releaseConnection();
            client.close();
        }
        return response;
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

    public static String contentRmSite = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<master>" +
              "<document path=\"/alfresco/site-data/components/page.title.site~" + "<shortName>" + "~dashboard.xml\">" +
                "<component>"+
                  "<guid>page.title.site~" + "<shortName>" + "~dashboard</guid>"+
                  "<scope>page</scope>"+
                  "<region-id>title</region-id>"+
                  "<source-id>site/" + "<shortName>" + "/dashboard</source-id>"+
                  "<url>/components/title/collaboration-title</url>"+
                "</component>"+
              "</document>"+
              "<document path=\"/alfresco/site-data/components/page.navigation.site~" + "<shortName>" + "~dashboard.xml\">"+
                "<component>"+
            "      <guid>page.navigation.site~" + "<shortName>" + "~dashboard</guid> "+
             "     <scope>page</scope> "+
             "     <region-id>navigation</region-id>"+
             "     <source-id>site/" + "<shortName>" + "/dashboard</source-id>"+
             "     <url>/components/navigation/collaboration-navigation</url>"+
             "   </component>"+
             " </document>"+
             " <document path=\"/alfresco/site-data/components/page.full-width-dashlet.site~" + "<shortName>" + "~dashboard.xml\">"+
             "   <component>"+
             "     <guid>page.full-width-dashlet.site~" + "<shortName>" + "~dashboard</guid>"+
             "     <scope>page</scope>"+
             "     <region-id>full-width-dashlet</region-id>"+
             "     <source-id>site/" + "<shortName>" + "/dashboard</source-id>"+
             "     <url>/components/dashlets/dynamic-welcome</url>"+
             "     <properties>"+
             "       <dashboardType>site</dashboardType>"+
             "     </properties> "+
             "   </component>"+
             " </document>"+
             " <document path=\"/alfresco/site-data/components/page.component-1-1.site~" + "<shortName>" + "~dashboard.xml\">"+
"                   <component>"+
    "             <guid>page.component-1-1.site~" + "<shortName>" + "~dashboard</guid>"+
    "             <scope>page</scope>"+
    "             <region-id>component-1-1</region-id>"+
    "             <source-id>site/" + "<shortName>" + "/dashboard</source-id>"+
    "             <url>/components/dashlets/colleagues</url>"+
    "           </component>"+
    "         </document>"+
    "         <document path=\"/alfresco/site-data/components/page.component-2-1.site~" + "<shortName>" + "~dashboard.xml\">"+
    "           <component>"+
    "             <guid>page.component-2-1.site~" + "<shortName>" + "~dashboard</guid>"+
    "             <scope>page</scope>"+
    "             <region-id>component-2-1</region-id>"+
    "             <source-id>site/" + "<shortName>" + "/dashboard</source-id>"+
    "             <url>/components/dashlets/docsummary</url>"+
    "           </component>"+
    "         </document>"+
    "         <document path=\"/alfresco/site-data/components/page.component-2-2.site~" + "<shortName>" + "~dashboard.xml\">"+
    "           <component>"+
    "             <guid>page.component-2-2.site~" + "<shortName>" + "~dashboard</guid>"+
    "             <scope>page</scope>"+
    "             <region-id>component-2-2</region-id>"+
    "             <source-id>site/" + "<shortName>" + "/dashboard</source-id>"+
    "             <url>/components/dashlets/activityfeed</url>"+
    "           </component>"+
    "         </document>"+
    "         <document path=\"/alfresco/site-data/pages/site/" + "<shortName>" + "/dashboard.xml\">"+
    "           <page>"+
    "             <title>Collaboration Site Dashboard</title>"+
    "             <title-id>page.siteDashboard.title</title-id>"+
    "             <description>Collaboration site's dashboard page</description>"+
    "             <description-id>page.siteDashboard.description</description-id>"+
    "             <authentication>user</authentication>"+
    "             <template-instance>dashboard-2-columns-wide-right</template-instance>"+
    "             <properties>"+
    "               <sitePages>[{\"pageId\":\"documentlibrary\", \"sitePageTitle\":\"File Plan\"}, {\"pageId\":\"rmsearch\"}]</sitePages>"+     
    "             </properties>"+
    "           </page>"+
    "         </document>"+
    "       </master>";
}
