package org.alfresco.test.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.social.alfresco.api.Alfresco;
import org.springframework.social.alfresco.api.impl.ConnectionDetails;
import org.springframework.social.alfresco.connect.BasicAuthAlfrescoConnectionFactory;
import org.springframework.social.connect.Connection;
import org.springframework.stereotype.Service;
@Service
@PropertySource("classpath:dataprep.properties")
/**
 * A public api factory that uses basic authentication to communicate with a repository.
 * 
 * @author steveglover
 * @author Michael Suzuki
 *
 */
public class BasicAuthPublicApiFactory implements PublicApiFactory
{
    @Value("${alfresco.scheme}") private String scheme;
    @Value("${alfresco.server}") private String host;
    @Value("${alfresco.port}") private int port;
    @Value("${http.connection.max}") private int maxNumberOfConnections;
    @Value("${http.connection.timeoutMs}") private int connectionTimeoutMs;
    @Value("${http.socket.timeoutMs}") private int socketTimeoutMs;
    @Value("${http.socket.ttlMs}") private int socketTtlMs;
    @Value("${alfresco.context}") private String context;
    @Value("${alfresco.public.api.servlet.name}") private String publicApiServletName;;
    @Value("${alfresco.service.servlet.name}") private String serviceServletName;
    private boolean ignoreServletName;
    private final static String ADMIN_DEFAULT = "admin";

    
    public BasicAuthPublicApiFactory(String scheme, String host, int port, 
            int maxNumberOfConnections, int connectionTimeoutMs, 
            int socketTimeoutMs, int socketTtlMs)
    {
        this(scheme, host, port, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs,
                socketTtlMs, "alfresco", "api", "service");
    }

    public BasicAuthPublicApiFactory(String scheme, String host, int port, 
            int maxNumberOfConnections, int connectionTimeoutMs,  int socketTimeoutMs, int socketTtlMs,
            String context, String publicApiServletName, String serviceServletName)
    {
        super();
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.maxNumberOfConnections= maxNumberOfConnections;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.socketTimeoutMs = socketTimeoutMs;
        this.socketTtlMs= socketTtlMs; 
        this.context = context;
        this.publicApiServletName = publicApiServletName;
        this.serviceServletName = serviceServletName;
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
