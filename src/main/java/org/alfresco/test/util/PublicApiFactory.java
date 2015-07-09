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
     */
    Alfresco getPublicApi(String username, String password);
    
    /**
     * Get a public api connection for the admin user of the given domain.
     * 
     * Note: may not be implemented by all factories.
     * 
     * @param domain        the domain id of the domain admin user
     * @return              a public api connection for the admin user of the given domain
     */
    Alfresco getTenantAdminPublicApi(String domain);
    
    /**
     * Get a public api connection for the admin user.
     * 
     * Note: may not be implemented by all factories.
     * 
     * @return a public api connection for the admin user
     */
    Alfresco getAdminPublicApi();
}

