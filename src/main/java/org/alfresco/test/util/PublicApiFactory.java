package org.alfresco.test.util;

import org.springframework.social.alfresco.api.Alfresco;

/**
 * A factory for creating a public api connection for a specific user.
 * 
 * @author steveglover
 * @author Michael Suzuki
 *
 */
public interface PublicApiFactory
{
    /**
     * Get a public api connection for the "username"
     * 
     * @param username the user name of the user
     * @param password user password
     * @return a public api connection for the given user
     * @throws Exception if error
     */
    Alfresco getPublicApi(String username, String password);
    
    /**
     * Get a public api connection for the admin user of the given domain.
     * 
     * Note: may not be implemented by all factories.
     * 
     * @param domain        the domain id of the domain admin user
     * @return              a public api connection for the admin user of the given domain
     * @throws Exception if error
     */
    Alfresco getTenantAdminPublicApi(String domain);
    
    /**
     * Get a public api connection for the admin user.
     * 
     * Note: may not be implemented by all factories.
     * 
     * @return a public api connection for the admin user
     * @throws Exception if error
     */
    Alfresco getAdminPublicApi();
}

