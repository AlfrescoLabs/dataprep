package org.alfresco.test.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.social.alfresco.api.Alfresco;
import org.springframework.social.alfresco.api.CMISEndpoint;
import org.springframework.social.alfresco.api.impl.ConnectionDetails;
import org.springframework.social.alfresco.connect.BasicAuthAlfrescoConnectionFactory;
import org.springframework.social.connect.Connection;

/**
 * A public api factory that uses basic authentication to communicate with a repository.
 * 
 * @author steveglover
 * @author Michael Suzuki
 *
 */
public class BasicAuthPublicApiFactory implements PublicApiFactory
{
    private String scheme;
    private String host;
    private int port;
    private String cloudUrl;
    private int maxNumberOfConnections;
    private int connectionTimeoutMs;
    private int socketTimeoutMs;
    private int socketTtlMs;
    private String context;
    private boolean ignoreServletName;
    private String publicApiServletName;
    private String serviceServletName;
    private CMISEndpoint preferredCMISEndPoint;
    private final static String ADMIN_DEFAULT = "admin";

    
    public BasicAuthPublicApiFactory(String scheme, String host, int port, CMISEndpoint preferredCMISEndPoint,
            int maxNumberOfConnections, int connectionTimeoutMs, 
            int socketTimeoutMs, int socketTtlMs, String cloudUrl)
    {
        this(scheme, host, port, preferredCMISEndPoint, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs,
                socketTtlMs, "alfresco", "api", "service", cloudUrl);
        this.preferredCMISEndPoint = preferredCMISEndPoint;
    }

    public BasicAuthPublicApiFactory(String scheme, String host, int port, CMISEndpoint preferredCMISEndPoint,
            int maxNumberOfConnections, int connectionTimeoutMs,  int socketTimeoutMs, int socketTtlMs,
            String context, String publicApiServletName, String serviceServletName, String cloudUrl)
    {
        super();
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.preferredCMISEndPoint = preferredCMISEndPoint;
        this.maxNumberOfConnections= maxNumberOfConnections;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.socketTimeoutMs = socketTimeoutMs;
        this.socketTtlMs= socketTtlMs; 
        this.context = context;
        this.publicApiServletName = publicApiServletName;
        this.serviceServletName = serviceServletName;
        this.cloudUrl = cloudUrl;
    }

    public String getContext()
    {
        return context;
    }

    public boolean isIgnoreServletName()
    {
        return ignoreServletName;
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
    
    public String getCloudUrl()
    {
        return cloudUrl;
    }
    
    public CMISEndpoint getPreferredCMISEndPoint()
    {
        return preferredCMISEndPoint;
    }

    public Alfresco getPublicApi(String username, String password)
    {
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            throw new IllegalArgumentException("Valid username and password are required");
        }
        Alfresco alfresco = null;
            ConnectionDetails connectionDetails = new ConnectionDetails(scheme, host, port, username, password, context,
                    publicApiServletName, serviceServletName, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs, socketTtlMs);
            BasicAuthAlfrescoConnectionFactory basicAuthConnectionFactory = new BasicAuthAlfrescoConnectionFactory(connectionDetails, null);
            Connection<Alfresco> connection = basicAuthConnectionFactory.createConnection();
            alfresco = connection.getApi();
        if (alfresco == null)
        {
            throw new RuntimeException("Unable to retrieve API connection to Alfresco.");
        }

        return alfresco;
    }
    public Alfresco getTenantAdminPublicApi(String domain)
    {
        if(StringUtils.isEmpty(domain))
        {
            throw new IllegalArgumentException("domain value is required");
        }
        ConnectionDetails connectionDetails = new ConnectionDetails(scheme, host, port, "admin@" + domain, ADMIN_DEFAULT, context,
                publicApiServletName, serviceServletName, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs, socketTtlMs);
        BasicAuthAlfrescoConnectionFactory connectionFactory = new BasicAuthAlfrescoConnectionFactory(connectionDetails, null);
        Connection<Alfresco> connection = connectionFactory.createConnection();
        Alfresco alfresco = connection.getApi();
        return alfresco;
    }
    
    public Alfresco getAdminPublicApi()
    {
        ConnectionDetails connectionDetails = new ConnectionDetails(scheme, host, port, ADMIN_DEFAULT, ADMIN_DEFAULT, context,
                publicApiServletName, serviceServletName, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs, socketTtlMs);
        BasicAuthAlfrescoConnectionFactory connectionFactory = new BasicAuthAlfrescoConnectionFactory(connectionDetails, null);
        Connection<Alfresco> connection = connectionFactory.createConnection();
        Alfresco alfresco = connection.getApi();
        return alfresco;
    }
}
