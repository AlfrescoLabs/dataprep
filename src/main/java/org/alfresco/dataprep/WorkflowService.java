package org.alfresco.dataprep;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;
/**
 *  Class to create new workflow processes.
 * 
 * @author Bogdan Bocancea
 */
@Service
public class WorkflowService extends CMISUtil
{
    private static Log logger = LogFactory.getLog(WorkflowService.class);
    
    public enum WorkflowType
    {
        NewTask("New Task","activitiAdhoc:1:4"),
        GroupReview("Review and Approve (group review)","activitiParallelGroupReview:1:20"),
        MultipleReviewers("Review and Approve (one or more reviewers)","activitiParallelReview:1:16"),
        SingleReviewer("Review And Approve (single reviewer)", "activitiReview:1:8"),
        PooledReview("Review and Approve (pooled review)", "activitiReviewPooled:1:12");
        private String title;
        private String id;
        private WorkflowType(String title, String id)
        {
            this.title = title;
            this.id = id;
        }
        public String getTitle()
        {
            return this.title;
        }
        public String getId()
        {
            return this.id;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean startWorkflow(final String userName,
                                  final String password,
                                  final WorkflowType workflowType,
                                  final String message,
                                  Date due,
                                  Priority priority,
                                  final List<String> assignedUsers,
                                  final String assignedGroup,
                                  final boolean docsByPath,
                                  final String documentsSite,
                                  final List<String> docsToAttach,
                                  final List<String> pathsToDocs,
                                  final int requiredApprovePercent,
                                  final boolean sendEmail)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String version = AlfrescoHttpClient.ALFRESCO_API_VERSION.replace("alfresco", "workflow");
        String api = client.getAlfrescoUrl() + "alfresco/api/" + version + "processes";
        logger.info("Create process using url: " + api);
        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'Z");
        SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd'T'Z");
        String dueDate = fullFormat.format(due);
        DateTime dateTime = dtf.parseDateTime(dueDate);
        HttpPost post = new HttpPost(api);
        JSONObject body = new JSONObject();
        body.put("processDefinitionId", workflowType.getId());
        JSONArray items = new JSONArray();
        if(!(docsToAttach == null) || !(pathsToDocs == null))
        {
            if(!docsByPath)
            {
                for(int i = 0; i<docsToAttach.size(); i++)
                {
                    items.add(getNodeRef(userName, password, documentsSite, docsToAttach.get(i)));
                }
            }
            else
            {
                for(int i = 0; i<pathsToDocs.size(); i++)
                {
                    items.add(getNodeRefFromPath(userName, password, pathsToDocs.get(i)));
                }
            }
        }
        JSONObject variables = new JSONObject();
        if(workflowType.equals(WorkflowType.GroupReview) || workflowType.equals(WorkflowType.PooledReview))
        {
            variables.put("bpm_groupAssignee", "GROUP_" + assignedGroup);
        }
        else if(workflowType.equals(WorkflowType.MultipleReviewers))
        {
            variables.put("bpm_assignees", assignedUsers);
        }
        else
        {
            variables.put("bpm_assignee", assignedUsers.get(0));
        }
        variables.put("bpm_workflowDescription", message);
        variables.put("bpm_sendEMailNotifications", sendEmail);
        variables.put("bpm_workflowPriority", priority.getLevel());
        variables.put("bpm_workflowDueDate", dateTime.toString());
        if(workflowType.equals(WorkflowType.GroupReview) || workflowType.equals(WorkflowType.MultipleReviewers))
        {
            variables.put("wf_requiredApprovePercent", requiredApprovePercent);
        }
        body.put("variables", variables);
        body.put("items", items);
        try
        {
            HttpResponse response = client.executeRequest(userName, password, body, post);
            logger.info("Response code: " + response.getStatusLine().getStatusCode());
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_CREATED:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Successfuly started workflow: " + message);
                    }
                    return true;
                default:
                    logger.error("Unable to start workflow " + response.toString());
            }
        }
        finally
        {
            client.close();
            post.releaseConnection();
        }
        return false;
    }
    
    /**
     * Start a new task with items added from a site
     * 
     * @param userName String user name
     * @param password String password
     * @param message String message
     * @param dueDate Date due date
     * @param assignee String assignee
     * @param priority Priority
     * @param documentsSite String site containing the items
     * @param documents List<String> documents to add to the task
     * @param sendEmail boolean send email
     * @return true if created 
     */
    public boolean startNewTask(final String userName,
                                final String password,
                                final String message,
                                final Date dueDate,
                                final String assignee,
                                final Priority priority,
                                final String documentsSite,
                                final List<String> documents,
                                final boolean sendEmail)
    {
        List<String> assignedUser = new ArrayList<String>();
        assignedUser.add(assignee);
        return startWorkflow(userName, password, WorkflowType.NewTask, message, dueDate, priority,
                assignedUser, null, false, documentsSite, documents, null, 0, sendEmail);
    }
    
    /**
     * Start a new task with items added by path
     * 
     * @param userName String user name
     * @param password String password
     * @param message String message
     * @param dueDate Date due date
     * @param assignee String assignee
     * @param priority Priority
     * @param pathsToItems List<String> path to items (e.g. Sites/siteId/documentLibrary/doc.txt)
     * @param sendEmail boolean send email
     * @return true if created 
     */
    public boolean startNewTask(final String userName,
                                final String password,
                                final String message,
                                final Date dueDate,
                                final String assignee,
                                final Priority priority,
                                final List<String> pathsToItems,
                                final boolean sendEmail)
    {
        List<String> assignedUser = new ArrayList<String>();
        assignedUser.add(assignee);
        return startWorkflow(userName, password, WorkflowType.NewTask, message, dueDate, priority,
                assignedUser, null, true, null, null, pathsToItems, 0, sendEmail);
    }

    /**
     * Start a Review And Approve (group review) with items added from a site
     * 
     * @param userName String user name
     * @param password String password
     * @param message String message
     * @param dueDate Date due date
     * @param assignedGroup String assignee group
     * @param priority Priority
     * @param documentsSite String site containing the items
     * @param documents List<String> documents to add to the task
     * @param requiredApprovePercent int required percent
     * @param sendEmail boolean send email
     * @return true if created 
     */
    public boolean startGroupReview(final String userName,
                                    final String password,
                                    final String message,
                                    final Date dueDate,
                                    final String assignedGroup,
                                    final Priority priority,
                                    final String documentsSite,
                                    final List<String> documents,
                                    final int requiredApprovePercent,
                                    final boolean sendEmail)
    {
        return startWorkflow(userName, password, WorkflowType.GroupReview, message, dueDate, priority,
                null, assignedGroup, false, documentsSite, documents, null, requiredApprovePercent, sendEmail);
    }
    
    /**
     * Start a Review And Approve (group review) with items added by path
     * 
     * @param userName String user name
     * @param password String password
     * @param message String message
     * @param dueDate Date due date
     * @param assignedGroup String assignee group
     * @param priority Priority
     * @param pathToDocs List<String> path to items (e.g. Sites/siteId/documentLibrary/doc.txt)
     * @param requiredApprovePercent int required percent
     * @param sendEmail boolean send email
     * @return true if created 
     */
    public boolean startGroupReview(final String userName,
                                    final String password,
                                    final String message,
                                    final Date dueDate,
                                    final String assignedGroup,
                                    final Priority priority,
                                    final List<String> pathToDocs,
                                    final int requiredApprovePercent,
                                    final boolean sendEmail)
    {
        return startWorkflow(userName, password, WorkflowType.GroupReview, message, dueDate, priority,
                null, assignedGroup, true, null, null, pathToDocs, requiredApprovePercent, sendEmail);
    }
    
    /**
     * Start a Review And Approve (one or more reviewers) with items added from a site
     * 
     * @param userName String user name
     * @param password String password
     * @param message String message
     * @param dueDate Date due date
     * @param reviewers List<String> list of reviewers
     * @param priority Priority
     * @param documentsSite String site containing the items
     * @param documents List<String> documents to add to the task
     * @param requiredApprovePercent int required percent
     * @param sendEmail boolean send email
     * @return true if created 
     */
    public boolean startMultipleReviewers(final String userName,
                                          final String password,
                                          final String message,
                                          final Date dueDate,
                                          final List<String> reviewers,
                                          final Priority priority,
                                          final String documentsSite,
                                          final List<String> documents,
                                          final int requiredApprovePercent,
                                          final boolean sendEmail)
    {
        return startWorkflow(userName, password, WorkflowType.MultipleReviewers, message, dueDate, priority, reviewers,
                null, false, documentsSite, documents, null, requiredApprovePercent, sendEmail);
    }
    
    /**
     * Start a Review And Approve (one or more reviewers) with items added by path
     * 
     * @param userName String user name
     * @param password String password
     * @param message String message
     * @param dueDate Date due date
     * @param reviewers List<String> list of reviewers
     * @param priority Priority
     * @param pathsToDocuments List<String> path to items (e.g. Sites/siteId/documentLibrary/doc.txt)
     * @param requiredApprovePercent int required percent
     * @param sendEmail boolean send email
     * @return true if created 
     */
    public boolean startMultipleReviewers(final String userName,
                                          final String password,
                                          final String message,
                                          final Date dueDate,
                                          final List<String> reviewers,
                                          final Priority priority,
                                          final List<String> pathsToDocuments,
                                          final int requiredApprovePercent,
                                          final boolean sendEmail)
    {
        return startWorkflow(userName, password, WorkflowType.MultipleReviewers, message, dueDate, priority, reviewers,
                null, true, null, null, pathsToDocuments, requiredApprovePercent, sendEmail);
    }
    
    /**
     * Start a Review And Approve (pooled review) with items added from a site
     * 
     * @param userName String user name
     * @param password String password
     * @param message String message
     * @param dueDate Date due date
     * @param assignedGroup String review group
     * @param priority Priority
     * @param documentsSite String site containing the items
     * @param documents List<String> documents to add to the task
     * @param sendEmail boolean send email
     * @return true if created 
     */
    public boolean startPooledReview(final String userName,
                                     final String password,
                                     final String message,
                                     final Date dueDate,
                                     final String assignedGroup,
                                     final Priority priority,
                                     final String documentsSite,
                                     final List<String> documents,
                                     final boolean sendEmail)
    {
        return startWorkflow(userName, password, WorkflowType.PooledReview, message, dueDate, priority,
                null, assignedGroup, false, documentsSite, documents, null, 0, sendEmail);
    }

    /**
     * Start a Review And Approve (pooled review) with items added by path
     * 
     * @param userName String user name
     * @param password String password
     * @param message String message
     * @param dueDate Date due date
     * @param assignedGroup String review group
     * @param priority Priority
     * @param pathToDocs List<String> path to items (e.g. Sites/siteId/documentLibrary/doc.txt)
     * @param sendEmail boolean send email
     * @return true if created 
     */
    public boolean startPooledReview(final String userName,
                                     final String password,
                                     final String message,
                                     final Date dueDate,
                                     final String assignedGroup,
                                     final Priority priority,
                                     final List<String> pathToDocs,
                                     final boolean sendEmail)
    {
        return startWorkflow(userName, password, WorkflowType.PooledReview, message, dueDate, priority,
                null, assignedGroup, true, null, null, pathToDocs, 0, sendEmail);
    }
    
    /**
     * Start a Review And Approve (single reviewer) with items added from a site
     * 
     * @param userName String user name
     * @param password String password
     * @param message String message
     * @param dueDate Date due date
     * @param assignee String user assigned
     * @param priority Priority
     * @param documentsSite String site containing the items
     * @param documents List<String> documents to add to the task
     * @param sendEmail boolean send email
     * @return true if created 
     */
    public boolean startSingleReview(final String userName,
                                     final String password,
                                     final String message,
                                     final Date dueDate,
                                     final String assignee,
                                     final Priority priority,
                                     final String documentsSite,
                                     final List<String> documents,
                                     final boolean sendEmail)
    {
        List<String> assignedUser = new ArrayList<String>();
        assignedUser.add(assignee);
        return startWorkflow(userName, password, WorkflowType.SingleReviewer, message, dueDate, priority,
                assignedUser, null, false, documentsSite, documents, null, 0, sendEmail);
    }
    
    /**
     * Start a Review And Approve (single reviewer) with items added by path
     * 
     * @param userName String user name
     * @param password String password
     * @param message String message
     * @param dueDate Date due date
     * @param assignee String user assigned
     * @param priority Priority
     * @param pathsToItems List<String> path to items (e.g. Sites/siteId/documentLibrary/doc.txt)
     * @param sendEmail boolean send email
     * @return true if created 
     */
    public boolean startSingleReview(final String userName,
                                     final String password,
                                     final String message,
                                     final Date dueDate,
                                     final String assignee,
                                     final Priority priority,
                                     final List<String> pathsToItems,
                                     final boolean sendEmail)
    {
        List<String> assignedUser = new ArrayList<String>();
        assignedUser.add(assignee);
        return startWorkflow(userName, password, WorkflowType.SingleReviewer, message, dueDate, priority,
                assignedUser, null, true, null, null, pathsToItems, 0, sendEmail);
    }
}
