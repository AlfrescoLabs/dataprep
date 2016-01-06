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
}
