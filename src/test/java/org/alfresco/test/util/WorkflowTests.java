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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.GroupService;
import org.alfresco.dataprep.SitePagesService;
import org.alfresco.dataprep.SiteService;
import org.alfresco.dataprep.UserService;
import org.alfresco.dataprep.WorkflowService;
import org.alfresco.dataprep.WorkflowService.TaskStatus;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test process creation using APIs.
 * 
 * @author Bogdan Bocancea
 */
public class WorkflowTests extends AbstractTest
{
    @Autowired WorkflowService workflow;
    @Autowired ContentService contentService;
    @Autowired SiteService siteService;
    @Autowired UserService userService;
    @Autowired GroupService groupService;
    @Autowired SitePagesService sitePages;
    private String workflowUser = "workflowUser" + System.currentTimeMillis();
    private String workflowSite = "workflowSite" + System.currentTimeMillis();
    private String plainDoc = "plainDoc";
    private String msWord = "msWord";
    private List<String> docs = new ArrayList<String>();
    private List<String> pathToItems = new ArrayList<String>();
    private String groupName = "workGroup" + System.currentTimeMillis();
    private List<String> reviewers = new ArrayList<String>();
    private String reviewer1 = "reviewer1" + System.currentTimeMillis();
    private String reviewer2 = "reviewer1" + System.currentTimeMillis();
    
    @BeforeClass(alwaysRun = true)
    public void userSetup()
    {
        userService.create(ADMIN, ADMIN, workflowUser, password, workflowUser + domain, "firstname", "lastname");
        siteService.create(workflowUser, password, "mydomain", workflowSite, "my site description", Visibility.PUBLIC);
        contentService.createDocument(workflowUser, password, workflowSite, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        contentService.createDocument(workflowUser, password, workflowSite, DocumentType.MSWORD, msWord, msWord);
        docs.add(plainDoc);
        docs.add(msWord);
        pathToItems.add("Sites/" + workflowSite + "/documentLibrary/" + plainDoc);
        pathToItems.add("Sites/" + workflowSite + "/documentLibrary/" + msWord);
        groupService.createGroup(ADMIN, ADMIN, groupName);
        groupService.addUserToGroup(ADMIN, ADMIN, groupName, workflowUser);
        userService.create(ADMIN, ADMIN, reviewer1, password, reviewer1 + domain, "firstname1", "lastname1");
        userService.create(ADMIN, ADMIN, reviewer2, password, reviewer2 + domain, "firstname2", "lastname2");
        reviewers.add(reviewer1);
        reviewers.add(reviewer2);
    }
    
    @Test
    public void createNewTask()
    {
        String workflowId = workflow.startNewTask(workflowUser, password, "New Task Message", new Date(), workflowUser,
                Priority.High, workflowSite, docs, true);
        Assert.assertTrue(!workflowId.isEmpty());
        String taskId = workflow.getTaskId(workflowUser, password, workflowId);
        Assert.assertTrue(!taskId.isEmpty());
    }
    
    @Test
    public void createNewTaskItemsByPath()
    {
        Assert.assertTrue(!workflow.startNewTask(workflowUser, password, "NewTaskByPaths", new Date(), workflowUser,
                Priority.Low, pathToItems, true).isEmpty());
    }
    
    @Test
    public void createGroupReview()
    {
        Assert.assertTrue(!workflow.startGroupReview(workflowUser, password, "group message", new Date(), groupName,
                Priority.Low, workflowSite, docs, 27, false).isEmpty());
        Assert.assertTrue(!workflow.startGroupReview(workflowUser, password, "itemsByPath", new Date(), groupName,
                Priority.High, pathToItems, 69, true).isEmpty());
    }
    
    @Test
    public void createWorkflowMultipleReviewers()
    {
        String workflowId = workflow.startMultipleReviewers(workflowUser, password, "multipleReviews", new Date(),
                reviewers, Priority.High, workflowSite, docs, 98, false);
        Assert.assertFalse(workflowId.isEmpty());
        String taskUser1 = workflow.getTaskId(reviewer1, password, workflowId);
        Assert.assertFalse(taskUser1.isEmpty());
        String taskUser2 = workflow.getTaskId(reviewer2, password, workflowId);
        Assert.assertFalse(taskUser2.isEmpty());
        Assert.assertNotSame(taskUser2, taskUser1);
        Assert.assertTrue(!workflow.startMultipleReviewers(workflowUser, password, "pathMultiple", new Date(), reviewers, 
                Priority.Low, pathToItems, 80, true).isEmpty());
    }
    
    @Test
    public void createPooledReview()
    {
        Assert.assertTrue(!workflow.startPooledReview(workflowUser, password, "pooledPathItems", new Date(), groupName, 
                Priority.High, pathToItems, false).isEmpty());
        Assert.assertTrue(!workflow.startPooledReview(workflowUser, password, "pooledReview", new Date(), groupName, 
                Priority.Normal, workflowSite, docs, false).isEmpty());
    }
    
    @Test
    public void createSingleReviewer()
    {
        Assert.assertTrue(!workflow.startSingleReview(workflowUser, password, "singleReview Path", new Date(), workflowUser, 
                Priority.High, pathToItems, true).isEmpty());
        Assert.assertTrue(!workflow.startSingleReview(workflowUser, password, "singleReviewer", new Date(), workflowUser, 
                Priority.Low, workflowSite, docs, false).isEmpty());
    }
    
    @Test
    public void updateTaskStatus()
    {
        String workflowId = workflow.startNewTask(workflowUser, password, "update task status", new Date(), workflowUser,
                Priority.High, workflowSite, docs, true);
        Assert.assertTrue(workflow.updateTaskStatus(workflowUser, password, workflowId, TaskStatus.ON_HOLD));
        Assert.assertTrue(workflow.updateTaskStatus(workflowUser, password, workflowId, TaskStatus.COMPLETED));
        Assert.assertTrue(workflow.updateTaskStatus(workflowUser, password, workflowId, TaskStatus.CANCELLED));
        Assert.assertTrue(workflow.updateTaskStatus(workflowUser, password, workflowId, TaskStatus.NOT_STARTED));
        Assert.assertTrue(workflow.updateTaskStatus(workflowUser, password, workflowId, TaskStatus.IN_PROGRESS));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void updateTaskStatusInvalidProcess()
    {
        workflow.updateTaskStatus(workflowUser, password, "99999", TaskStatus.ON_HOLD);
    }
    
    @Test
    public void reassignTask()
    {
        String reassignUser = "tagYourIt" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, reassignUser, password, workflowUser + domain, "reassign", "toMe");
        String workflowId = workflow.startNewTask(workflowUser, password, "reassignTo", new Date(), workflowUser,
                Priority.High, workflowSite, docs, true);
        Assert.assertTrue(workflow.reassignTask(workflowUser, password, workflowId, reassignUser));
        Assert.assertFalse(workflow.getTaskId(reassignUser, password, workflowId).isEmpty());
        Assert.assertTrue(workflow.getTaskId(workflowUser, password, workflowId).isEmpty());
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void reassignTaskFakeProcess()
    {
        String reassignUser = "tagYourIt-2" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, reassignUser, password, workflowUser + domain, "reassign", "toMe");
        workflow.reassignTask(workflowUser, password, "99999", reassignUser);
    }
    
    @Test
    public void completeTask()
    {
        String assignedUser = "pickMe" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, assignedUser, password, workflowUser + domain, "pick", "me");
        String workflowId = workflow.startNewTask(workflowUser, password, "completeTask", new Date(), assignedUser,
                Priority.High, workflowSite, docs, true);
        Assert.assertTrue(workflow.completeTask(assignedUser, password, workflowId, TaskStatus.COMPLETED, "completed by " + assignedUser));
        Assert.assertTrue(workflow.completeTask(workflowUser, password, workflowId, TaskStatus.IN_PROGRESS, "completed by " + workflowUser));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void completeTaskInvalidProcess()
    {
        Assert.assertTrue(workflow.completeTask(workflowUser, password, "99999", TaskStatus.IN_PROGRESS, "completed by " + workflowUser));
    }
    
    @Test
    public void claimPooledProcessTask()
    {
        String workflowId = workflow.startPooledReview(workflowUser, password, "pooledClaim", new Date(), groupName, 
                Priority.High, pathToItems, false);
        Assert.assertTrue(workflow.claimTask(workflowUser, password, workflowId));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void claimPooledProcessTaskTwice()
    {
        String user1 = "user-1" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, user1, password, workflowUser + domain, "1", "usr");
        groupService.addUserToGroup(ADMIN, ADMIN, groupName, user1);
        String workflowId = workflow.startPooledReview(workflowUser, password, "pooledClaim", new Date(), groupName, 
                Priority.High, pathToItems, false);
        Assert.assertTrue(workflow.claimTask(workflowUser, password, workflowId));
        Assert.assertTrue(workflow.claimTask(user1, password, workflowId));
    }
    
    @Test
    public void reasignPooledTask()
    {
        String reassignUser = "reasign" + System.currentTimeMillis();
        String user1 = "user-1" + System.currentTimeMillis();
        String user2 =  "user-2" + System.currentTimeMillis();
        String group = "pooledGroup" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, user1, password, workflowUser + domain, "1", "usr");
        userService.create(ADMIN, ADMIN, user2, password, workflowUser + domain, "2", "usr");
        groupService.createGroup(ADMIN, ADMIN, group);
        groupService.addUserToGroup(ADMIN, ADMIN, group, user1);
        groupService.addUserToGroup(ADMIN, ADMIN, group, user2);
        userService.create(ADMIN, ADMIN, reassignUser, password, workflowUser + domain, "reasign", "usr");
        String workflowId = workflow.startPooledReview(workflowUser, password, "pooledClaim", new Date(), group, 
                Priority.High, pathToItems, false);
        Assert.assertTrue(workflow.reassignTask(user1, password, workflowId, reassignUser));
        Assert.assertFalse(workflow.completeTask(reassignUser, password, workflowId, TaskStatus.COMPLETED, "completed by " + reassignUser));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void claimPooledProcessTaskWrongUser()
    {
        String secondUser = "secondUsr" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, secondUser, password, workflowUser + domain, "second", "usr");
        String workflowId = workflow.startPooledReview(workflowUser, password, "pooledClaim", new Date(), groupName, 
                Priority.High, pathToItems, false);
        Assert.assertTrue(workflow.claimTask(secondUser, password, workflowId));
    }
    
    @Test
    public void releaseTaskToPool()
    {
        String user1 = "user-1" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, user1, password, workflowUser + domain, "1", "usr");
        groupService.addUserToGroup(ADMIN, ADMIN, groupName, user1);
        String workflowId = workflow.startPooledReview(workflowUser, password, "releasePool", new Date(), groupName, 
                Priority.High, pathToItems, false);
        Assert.assertTrue(workflow.claimTask(workflowUser, password, workflowId));
        Assert.assertTrue(workflow.releaseToPool(workflowUser, password, workflowId));
        Assert.assertTrue(workflow.claimTask(user1, password, workflowId));
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void releaseTaskToPoolUnclaimedTask()
    {
        String user1 = "user-2" + System.currentTimeMillis();
        userService.create(ADMIN, ADMIN, user1, password, workflowUser + domain, "1", "usr");
        groupService.addUserToGroup(ADMIN, ADMIN, groupName, user1);
        String workflowId = workflow.startPooledReview(workflowUser, password, "releasePool", new Date(), groupName, 
                Priority.High, pathToItems, false);
        Assert.assertTrue(workflow.claimTask(workflowUser, password, workflowId));
        Assert.assertTrue(workflow.releaseToPool(user1, password, workflowId));
    }
}
