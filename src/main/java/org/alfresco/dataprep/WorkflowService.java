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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
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
    private String version = AlfrescoHttpClient.ALFRESCO_API_VERSION.replace("alfresco", "workflow");
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
    
    public enum TaskStatus
    {
        NOT_STARTED("Not Yet Started"),
        IN_PROGRESS("In Progress"),
        ON_HOLD("On Hold"),
        CANCELLED("Cancelled"),
        COMPLETED("Completed");
        private String status;
        private TaskStatus(String status)
        {
            this.status = status;
        }
        public String getStatus()
        {
            return this.status;
        }
        
    }

    @SuppressWarnings("unchecked")
    private String startWorkflow(final String userName,
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
                for(int i=0; i<pathsToDocs.size(); i++)
                {
                    items.add(getNodeRefByPath(userName, password, pathsToDocs.get(i)));
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
        post.setEntity(client.setMessageBody(body));
        try
        {
            HttpResponse response = client.execute(userName, password, post);
            logger.info("Response code: " + response.getStatusLine().getStatusCode());
            switch (response.getStatusLine().getStatusCode())
            {
                case HttpStatus.SC_CREATED:
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Successfuly started workflow: " + message);
                    }
                    return client.getParameterFromJSON(response, "entry", "id");
                default:
                    logger.error("Unable to start workflow " + response.toString());
            }
        }
        finally
        {
            client.close();
            post.releaseConnection();
        }
        return "";
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
     * @return String workflow id
     */
    public String startNewTask(final String userName,
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
     * @return String workflow id
     */
    public String startNewTask(final String userName,
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
     * @return String workflow id
     */
    public String startGroupReview(final String userName,
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
     * @return String workflow id
     */
    public String startGroupReview(final String userName,
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
     * @return String workflow id
     */
    public String startMultipleReviewers(final String userName,
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
     * @return String workflow id
     */
    public String startMultipleReviewers(final String userName,
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
     * @return String workflow id
     */
    public String startPooledReview(final String userName,
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
     * @return String workflow id
     */
    public String startPooledReview(final String userName,
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
     * @return String workflow id
     */
    public String startSingleReview(final String userName,
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
     * @return String workflow id
     */
    public String startSingleReview(final String userName,
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
    
    /**
     * Get the task id for the user assigned to the task
     * 
     * @param assignedUser String assigned user
     * @param password String password
     * @param workflowId String workflow id
     * @return String task id
     */
    public String getTaskId(final String assignedUser,
                            final String password,
                            final String workflowId)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String api = client.getAlfrescoUrl() + "alfresco/api/" + version + "processes/" + workflowId + "/tasks";
        HttpGet get = new HttpGet(api);
        try
        {
            HttpResponse response = client.executeRequest(assignedUser, password, get);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode())
            {
                JSONArray jArray = client.getJSONArray(response, "list", "entries");
                for (Object item:jArray)
                {
                    JSONObject jobject = (JSONObject) item;
                    JSONObject entry = (JSONObject) jobject.get("entry");
                    String assignee = (String) entry.get("assignee");
                    if(!StringUtils.isEmpty(assignee))
                    {
                        if(assignee.equals(assignedUser))
                        {
                            return (String) entry.get("id");
                        }
                    }
                    if(entry.get("state").equals("unclaimed"))
                    {
                        return (String) entry.get("id");
                    }
                }
            }
        }
        finally
        {
            client.close();
            get.releaseConnection();
        }
        return "";
    }
    
    private String checkTaskId(final String assignedUser,
                               final String password,
                               final String workflowId)
    {
        String taskId = getTaskId(assignedUser, password, workflowId);
        if(StringUtils.isEmpty(taskId))
        {
            throw new RuntimeException("Invalid process id (" + workflowId +") or wrong assigned user ->" + assignedUser);
        }
        return taskId;
    }
    
    /**
     * Update the status of a task
     * 
     * @param assignedUser String user assigned to the task
     * @param password String password
     * @param workflowId String workflow Id
     * @param status TaskStatus the status
     * @return true if 201 code is returned
     */
    public boolean updateTaskStatus(final String assignedUser,
                                    final String password,
                                    final String workflowId,
                                    final TaskStatus status)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String taskId = checkTaskId(assignedUser, password, workflowId);
        String api = client.getAlfrescoUrl() + "alfresco/api/" + version + "tasks/" + taskId + "/variables";
        HttpPost post = new HttpPost(api); 
        String jsonInput =  "[{" + "\"name\": \"bpm_status\",\"value\": \"" + status.getStatus() + "\", \"scope\": \"local\"";
        jsonInput = ( jsonInput + "}]");
        StringEntity se = new StringEntity(jsonInput.toString(), AlfrescoHttpClient.UTF_8_ENCODING);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, AlfrescoHttpClient.MIME_TYPE_JSON));
        post.setEntity(se);
        HttpResponse response = client.executeRequest(assignedUser, password, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_CREATED:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Successfuly updated the task status");
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid process id: " + workflowId);
            default:
                logger.error("Unable to change the task status " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Reassign a task to another user
     * 
     * @param assignedUser String assigned user
     * @param password String password
     * @param workflowId String workflow id
     * @param reassignTo String user to reassign task
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean reassignTask(final String assignedUser,
                                final String password,
                                final String workflowId,
                                final String reassignTo)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String taskId = checkTaskId(assignedUser, password, workflowId);
        String api = client.getAlfrescoUrl() + "alfresco/api/" + version + "tasks/" + taskId + "?select=state,assignee";
        HttpPut put = new HttpPut(api);
        JSONObject body = new JSONObject();
        body.put("state", "delegated");
        body.put("assignee", reassignTo);
        HttpResponse response = client.executeRequest(assignedUser, password, body, put);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Successfuly reassigned the task to " + reassignTo);
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid process id: " + workflowId);
            default:
                logger.error("Unable to reassign the task to " + reassignTo + " " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Complete a task
     * 
     * @param assignedUser String assigned user
     * @param password String password
     * @param workflowId String workflow id
     * @param status TaskStatus status
     * @param comment String comment
     * @return true if task is completed
     */
    @SuppressWarnings("unchecked")
    public boolean taskDone(final String assignedUser,
                            final String password,
                            final String workflowId,
                            final TaskStatus status,
                            final String comment)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String taskId = checkTaskId(assignedUser, password, workflowId);
        String api = client.getAlfrescoUrl() + "alfresco/api/" + version + "tasks/" + taskId + "?select=state,variables";
        HttpPut put = new HttpPut(api);
        JSONObject body = new JSONObject();
        body.put("state", "completed");
        JSONArray variables = new JSONArray();
        JSONObject jStatus = new JSONObject();
        jStatus.put("name", "bpm_status");
        jStatus.put("value", status.getStatus());
        jStatus.put("scope", "global");
        variables.add(jStatus);
        if(!StringUtils.isEmpty(comment))
        {
            JSONObject jComment = new JSONObject();
            jComment.put("name", "bpm_comment");
            jComment.put("value", comment);
            jComment.put("scope", "global");
            variables.add(jComment);
        }
        body.put("variables", variables);
        HttpResponse response = client.executeRequest(assignedUser, password, body, put);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Successfuly completed task " + taskId);
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid process id: " + workflowId);
            default:
                logger.error("Unable to complete the task " + taskId + " " + response.toString());
                break;
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private boolean claimTask(final String assignedUser,
                              final String password,
                              final String workflowId,
                              final boolean claim)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String taskId = checkTaskId(assignedUser, password, workflowId);
        String api = client.getAlfrescoUrl() + "alfresco/api/" + version + "tasks/" + taskId + "?select=state";
        HttpPut put = new HttpPut(api);
        JSONObject body = new JSONObject();
        if(claim)
        {
            body.put("state", "claimed");
        }
        else
        {
            body.put("state", "unclaimed");
        }
        HttpResponse response = client.executeRequest(assignedUser, password, body, put);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Successfuly executed " + put);
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid process id: " + workflowId);
            default:
                logger.error("Unable to execute request " + taskId + " " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Claim a task
     * 
     * @param assignedUser String assigned user
     * @param password String password
     * @param workflowId String workflow id
     * @return true if task is claimed
     */
    public boolean claimTask(final String assignedUser,
                             final String password,
                             final String workflowId)
    {
        return claimTask(assignedUser, password, workflowId, true);
    }
    
    /**
     * Realese a task to pool
     * 
     * @param assignedUser String assigned user
     * @param password String password
     * @param workflowId String workflow id
     * @return true if task is released
     */
    public boolean releaseToPool(final String assignedUser,
                                 final String password,
                                 final String workflowId)
    {
        return claimTask(assignedUser, password, workflowId, false);
    }
    
    /**
     * Approve or reject a task
     * 
     * @param assignedUser String assigned user
     * @param password String password
     * @param workflowId String workflow id
     * @param approve boolean approve or reject
     * @param status TaskStatus task status
     * @param comment String comment
     * @return 200 OK if successful
     */
    @SuppressWarnings("unchecked")
    public boolean approveTask(final String assignedUser,
                               final String password,
                               final String workflowId,
                               final boolean approve,
                               final TaskStatus status,
                               final String comment)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String taskId = checkTaskId(assignedUser, password, workflowId);
        String api = client.getAlfrescoUrl() + "alfresco/s/api/task/activiti%24" + taskId + "/formprocessor";
        HttpPost post = new HttpPost(api);
        JSONObject data = new JSONObject();
        if(approve)
        {
            data.put("prop_wf_reviewOutcome", "Approve");
        }
        else
        {
            data.put("prop_wf_reviewOutcome", "Reject");
        }
        data.put("prop_transitions", "Next");
        data.put("prop_bpm_status", status.getStatus());
        data.put("prop_bpm_comment", comment);
        HttpResponse response = client.executeRequest(assignedUser, password, data, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_OK:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Successfuly executed " + post);
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid process id: " + workflowId);
            default:
                logger.error("Unable to execute request " + taskId + " " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Cancel workflow process
     * 
     * @param owner String workflow owner
     * @param password String password
     * @param workflowID String workflowId
     * @return true if canceled
     */
    public boolean cancelWorkflow(final String owner,
                                  final String password,
                                  final String workflowId)
     {
         AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
         String api = client.getApiUrl() + "workflow-instances/activiti$" + workflowId;
         HttpDelete delete = new HttpDelete(api);
         HttpResponse response = client.executeRequest(owner, password, delete);
         switch (response.getStatusLine().getStatusCode())
         {
             case HttpStatus.SC_OK:
                 if (logger.isTraceEnabled())
                 {
                     logger.trace("Successfuly canceled workflow " + workflowId);
                 }
                 return true;
             case HttpStatus.SC_NOT_FOUND:
                 throw new RuntimeException("Invalid process id: " + workflowId);
             default:
                 logger.error("Unable to cancel workflow. Try to delete it. -> " + workflowId + " " + response.toString());
                 break;
         }
         return false;
     }
    
    /**
     * Delete workflow process
     * 
     * @param owner String workflow owner
     * @param password String password
     * @param workflowID String workflowId
     * @return true if deleted
     */
    public boolean deleteWorkflow(final String owner,
                                  final String password,
                                  final String workflowID)
     {
         AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
         String api = client.getApiUrl() + "workflow-instances/activiti$" + workflowID + "?forced=true";
         HttpDelete delete = new HttpDelete(api);
         HttpResponse response = client.executeRequest(owner, password, delete);
         switch (response.getStatusLine().getStatusCode())
         {
             case HttpStatus.SC_OK:
                 if (logger.isTraceEnabled())
                 {
                     logger.trace("Successfuly deleted workflow " + workflowID);
                 }
                 return true;
             case HttpStatus.SC_NOT_FOUND:
                 throw new RuntimeException("Invalid process id: " + workflowID);
             default:
                 logger.error("Unable to delete workflow " + workflowID + " " + response.toString());
                 break;
         }
         return false;
     }
   
    @SuppressWarnings("unchecked")
    private boolean addItemToTask(final String assignedUser,
                                  final String password,
                                  final String workflowId,
                                  final boolean byPath,
                                  final String itemsSite,
                                  final String itemName,
                                  final String pathToItem)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String taskId = checkTaskId(assignedUser, password, workflowId);
        String api = client.getAlfrescoUrl() + "alfresco/api/" + version + "tasks/" + taskId + "/items";
        HttpPost post = new HttpPost(api);
        JSONObject data = new JSONObject();
        if(!byPath)
        {
            data.put("id", getNodeRef(assignedUser, password, itemsSite, itemName));
        }
        else
        {
            data.put("id", getNodeRefByPath(assignedUser, password, pathToItem));
        }
        HttpResponse response = client.executeRequest(assignedUser, password, data, post);
        switch (response.getStatusLine().getStatusCode())
        {
            case HttpStatus.SC_CREATED:
                if (logger.isTraceEnabled())
                {
                    logger.trace("Successfuly added item to task: " + taskId);
                }
                return true;
            case HttpStatus.SC_NOT_FOUND:
                throw new RuntimeException("Invalid task id: " + workflowId);
            default:
                logger.error("Unable to add items to " + taskId + " " + response.toString());
                break;
        }
        return false;
    }
    
    /**
     * Add an item to a task from site
     * 
     * @param assignedUser String assigned user
     * @param password String password
     * @param workflowId String workflow id
     * @param itemSite String site name
     * @param itemName String item
     * @return true(status 201) if item is added
     */
    public boolean addItemToTask(final String assignedUser,
                                 final String password,
                                 final String workflowId,
                                 final String itemSite,
                                 final String itemName)
    {
        return addItemToTask(assignedUser, password, workflowId, false, itemSite, itemName, null);
    }
    
    /**
     * Add an item to a task by path
     * 
     * @param assignedUser String assigned user
     * @param password String password
     * @param workflowId String workflow id
     * @param pathToItem String pathToItem
     * @return true(status 201) if item is added
     */
    public boolean addItemToTask(final String assignedUser,
                                 final String password,
                                 final String workflowId,
                                 final String pathToItem)
    {
        return addItemToTask(assignedUser, password, workflowId, true, null, null, pathToItem);
    }
}

