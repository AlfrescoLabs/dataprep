package org.alfresco.test.util;

/**
 *  Class that provides utils needed in customizing user and site dashboards.
 * 
 * @author Bogdan Bocancea
 */

public class DashboardCustomization
{
    public final static String SITE_PAGES_URL = "share/service/components/site/customise-pages";
    public final static String ADD_DASHLET_URL = "share/service/components/dashboard/customise-dashboard";
    
    public enum Page
    {
        BLOG("blog-postlist"),
        WIKI("wiki-page"),
        LINKS("links"),
        DOCLIB("documentlibrary"),
        CALENDAR("calendar"),
        DATALISTS("data-lists"),
        DISCUSSIONS("discussions-topiclist");
        
        public final String pageId;
        Page(String pageId)
        {
            this.pageId = pageId;
        }
    }
    
    public enum DashletLayout
    {
        THREE_COLUMNS("dashboard-3-columns"),
        TWO_COLUMNS_WIDE_LEFT("dashboard-2-columns-wide-left"),
        TWO_COLUMNS_WIDE_RIGHT("dashboard-2-columns-wide-right"),
        ONE_COLUMN("dashboard-1-column"),
        FOUR_COLUMNS("dashboard-4-columns");
        
        public final String id;
        DashletLayout(String id)
        {
            this.id = id;
        }
    }
    
    public enum SiteDashlet
    {
        IMAGE_PREVIEW("Image Preview", "/components/dashlets/imagesummary"),
        SITE_MEMBERS("Site Members", "/components/dashlets/colleagues"),
        SITE_CONNTENT("Site Content", "/components/dashlets/docsummary"),
        SITE_ACTIVITIES("Site Activities", "/components/dashlets/activityfeed"),
        SITE_CALENDAR("Site Calendar", "/components/dashlets/calendar"),
        SITE_DATA_LIST("Site Data List", "/components/dashlets/site-datalists"),
        SITE_PROFILE("Site Profile", "/components/dashlets/site-profile"),
        ADDONS_RSS_FEED("Alfresco Add-ons RSS Feed", "/components/dashlets/addonsfeed"),
        SAVED_SEARCH("Saved Search", "/components/dashlets/saved-search"),
        SITE_LINKS("Site Links", "/components/dashlets/site-links"),
        SITE_CONTRIB_BREAKDOWN("Site Contributor Breakdown", "/components/dashlets/top-site-contributor-report"),
        MY_DISCUSSIONS("My Discussions", "/components/dashlets/forum-summary"),
        SITE_SEARCH("Site Feed", "/components/dashlets/site-search"),
        FILE_TYPE_BREAKDOWN("Site File Type Breakdown", "/components/dashlets/site-content-report"),
        SITE_NOTICE("Site Notice", "/components/dashlets/site-notice"),
        WIKI("Wiki","/components/dashlets/wiki"),
        RSS_FEED("Rss Feed", "/components/dashlets/rssfeed"),
        WEB_VIEW("Web View", "/components/dashlets/webview");
        
        public final String name, id;
        SiteDashlet(String name, String id)
        {
            this.name = name;
            this.id = id;
        }
    }
    
    public enum UserDashlet
    {
        MY_SITES("My Sites", "/components/dashlets/my-sites"),
        MY_TASKS("My Tasks", "/components/dashlets/my-tasks"),
        MY_ACTIVITIES("My Activities", "/components/dashlets/my-activities"),
        MY_DOCUMENTS("My Documents", "/components/dashlets/my-documents"),
        MY_MEETING_WORKSPACES("My Meeting Workspaces", "/components/dashlets/my-meeting-workspaces"),
        ADDONS_RSS_FEED("Alfresco Add-ons RSS Feed", "/components/dashlets/addonsfeed"),
        SAVED_SEARCH("Saved Search", "/components/dashlets/saved-search"),
        CONTENT_EDITING("Content I'm Editing", "/components/dashlets/my-docs-editing"),
        MY_PROFILE("My Profile", "/components/dashlets/my-profile"),
        MY_DOC_WORKSPACES("My Document Workspaces","/components/dashlets/my-workspaces"),
        MY_DISCUSSIONS("My Discussions", "/components/dashlets/forum-summary"),
        SITE_SEARCH("Site Search", "/components/dashlets/site-search"),
        MY_CALENDAR("My Calendar", "/components/dashlets/user-calendar"),
        RSS_FEED("Rss Feed", "/components/dashlets/rssfeed"),
        WEB_VIEW("Web View", "/components/dashlets/webview");
        
        public final String name, id;
        UserDashlet(String name, String id)
        {
            this.name = name;
            this.id = id;
        }
    }
    
   
}
