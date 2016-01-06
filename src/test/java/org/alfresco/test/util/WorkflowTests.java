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
    private final String password = "password";
    
    @Test
    public void createNewTask()
    {
        String siteName = "workflowSite" + System.currentTimeMillis();
        String userName = "wUser" + System.currentTimeMillis();
        String plainDoc = "plain";
        String msWord = "msWord";
        userService.create(ADMIN, ADMIN, userName, password, userName + domain, "firstname", "lastname");
        siteService.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        contentService.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        contentService.createDocument(userName, password, siteName, DocumentType.MSWORD, msWord, msWord);
        List<String> docs = new ArrayList<String>();
        docs.add(plainDoc);
        docs.add(msWord);
        String workflowId = workflow.startNewTask(userName, password, "New Task Message", new Date(), userName, Priority.High, siteName, docs, true);
        Assert.assertTrue(!workflowId.isEmpty());
        String taskId = workflow.getTaskId(userName, password, workflowId);
        Assert.assertTrue(!taskId.isEmpty());
    }
    
    @Test
    public void createNewTaskItemsByPath()
    {
        String siteName = "workflowSite" + System.currentTimeMillis();
        String userName = "wUser" + System.currentTimeMillis();
        String plainDoc = "plainDoc";
        String msWord = "msWord";
        String xls = "excel";
        userService.create(ADMIN, ADMIN, userName, password, userName + domain, "firstname", "lastname");
        siteService.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        contentService.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        contentService.createDocument(userName, password, siteName, DocumentType.MSWORD, msWord, msWord);
        contentService.createDocument(userName, password, siteName, DocumentType.MSEXCEL, xls, xls);
        List<String> pathToItems = new ArrayList<String>();
        pathToItems.add("Sites/" + siteName + "/documentLibrary/" + plainDoc);
        pathToItems.add("Sites/" + siteName + "/documentLibrary/" + msWord);
        pathToItems.add("Sites/" + siteName + "/documentLibrary/" + xls);
        Assert.assertTrue(!workflow.startNewTask(userName, password, "NewTaskByPaths", new Date(), userName, Priority.Low, pathToItems, true).isEmpty());
    }
    
    @Test
    public void createGroupReview()
    {
        String siteName = "workflowSite" + System.currentTimeMillis();
        String userName = "WGUser" + System.currentTimeMillis();
        String plainDoc = "plain";
        String msWord = "msWord";
        userService.create(ADMIN, ADMIN, userName, password, userName + domain, "firstname", "lastname");
        siteService.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        contentService.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        contentService.createDocument(userName, password, siteName, DocumentType.MSWORD, msWord, msWord);
        List<String> docs = new ArrayList<String>();
        docs.add(plainDoc);
        docs.add(msWord);
        List<String> docsByPath = new ArrayList<String>();
        docsByPath.add("Sites/" + siteName + "/documentLibrary/" + plainDoc);
        docsByPath.add("Sites/" + siteName + "/documentLibrary/" + msWord);
        String groupName = "workGroup" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userName));
        Assert.assertTrue(!workflow.startGroupReview(userName, password, "group message", new Date(), groupName, Priority.Low, siteName, docs, 27, false).isEmpty());
        Assert.assertTrue(!workflow.startGroupReview(userName, password, "itemsByPath", new Date(), groupName, Priority.High, docsByPath, 69, true).isEmpty());
    }
    
    @Test
    public void createWorkflowMultipleReviewers()
    {
        String siteName = "workflowSite" + System.currentTimeMillis();
        String userName = "multi-1" + System.currentTimeMillis();
        String userName2 = "multi-2" + System.currentTimeMillis();
        String userName3 = "multi-3" + System.currentTimeMillis();
        String plainDoc = "plain";
        String msWord = "msWord";
        userService.create(ADMIN, ADMIN, userName, password, userName + domain, "firstname", "lastname");
        userService.create(ADMIN, ADMIN, userName2, password, userName2 + domain, "firstname2", "lastname2");
        userService.create(ADMIN, ADMIN, userName3, password, userName3 + domain, "firstname3", "lastname3");
        siteService.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        contentService.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        contentService.createDocument(userName, password, siteName, DocumentType.MSWORD, msWord, msWord);
        List<String> docs = new ArrayList<String>();
        docs.add(plainDoc);
        docs.add(msWord);
        List<String> reviewers = new ArrayList<String>();
        reviewers.add(userName2);
        reviewers.add(userName3);
        List<String> paths = new ArrayList<String>();
        paths.add("Sites/" + siteName + "/documentLibrary/" + msWord);
        paths.add("Sites/" + siteName + "/documentLibrary/" + plainDoc);
        String workflowId = workflow.startMultipleReviewers(userName, password, "multipleReviews", new Date(),
                reviewers, Priority.High, siteName, docs, 98, false);
        Assert.assertFalse(workflowId.isEmpty());
        String taskUser1 = workflow.getTaskId(userName2, password, workflowId);
        Assert.assertFalse(taskUser1.isEmpty());
        String taskUser2 = workflow.getTaskId(userName3, password, workflowId);
        Assert.assertFalse(taskUser2.isEmpty());
        Assert.assertNotSame(taskUser2, taskUser1);
        Assert.assertTrue(!workflow.startMultipleReviewers(userName, password, "pathMultiple", new Date(), reviewers, 
                Priority.Low, paths, 80, true).isEmpty());
    }
    
    @Test
    public void createPooledReview()
    {
        String siteName = "workflowSite" + System.currentTimeMillis();
        String userName = "WGUser" + System.currentTimeMillis();
        String plainDoc = "plain";
        String msWord = "msWord";
        userService.create(ADMIN, ADMIN, userName, password, userName + domain, "firstname", "lastname");
        siteService.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        contentService.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        contentService.createDocument(userName, password, siteName, DocumentType.MSWORD, msWord, msWord);
        List<String> docs = new ArrayList<String>();
        docs.add(plainDoc);
        docs.add(msWord);
        List<String> docsByPath = new ArrayList<String>();
        docsByPath.add("Sites/" + siteName + "/documentLibrary/" + plainDoc);
        docsByPath.add("Sites/" + siteName + "/documentLibrary/" + msWord);
        String groupName = "workGroup" + System.currentTimeMillis();
        Assert.assertTrue(groupService.createGroup(ADMIN, ADMIN, groupName));
        Assert.assertTrue(groupService.addUserToGroup(ADMIN, ADMIN, groupName, userName));
        Assert.assertTrue(!workflow.startPooledReview(userName, password, "pooledPathItems", new Date(), groupName, Priority.High, docsByPath, false).isEmpty());
        Assert.assertTrue(!workflow.startPooledReview(userName, password, "pooledReview", new Date(), groupName, Priority.Normal, siteName, docs, false).isEmpty());
    }
    
    @Test
    public void createSingleReviewer()
    {
        String siteName = "workflowSite" + System.currentTimeMillis();
        String userName = "wUser" + System.currentTimeMillis();
        String plainDoc = "plain";
        String msWord = "msWord";
        userService.create(ADMIN, ADMIN, userName, password, userName + domain, "firstname", "lastname");
        siteService.create(userName, password, "mydomain", siteName, "my site description", Visibility.PUBLIC);
        contentService.createDocument(userName, password, siteName, DocumentType.TEXT_PLAIN, plainDoc, plainDoc);
        contentService.createDocument(userName, password, siteName, DocumentType.MSWORD, msWord, msWord);
        List<String> docs = new ArrayList<String>();
        docs.add(plainDoc);
        docs.add(msWord);
        List<String> docsByPath = new ArrayList<String>();
        docsByPath.add("Sites/" + siteName + "/documentLibrary/" + plainDoc);
        docsByPath.add("Sites/" + siteName + "/documentLibrary/" + msWord);
        Assert.assertTrue(!workflow.startSingleReview(userName, password, "singleReview Path", new Date(), userName, Priority.High, docsByPath, true).isEmpty());
        Assert.assertTrue(!workflow.startSingleReview(userName, password, "singleReviewer", new Date(), userName, Priority.Low, siteName, docs, false).isEmpty());
    }
}
