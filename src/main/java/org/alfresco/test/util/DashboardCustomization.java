package org.alfresco.test.util;


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
}
