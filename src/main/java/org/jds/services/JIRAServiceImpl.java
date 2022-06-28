package org.jds.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.ProjectRestClient;
import com.atlassian.jira.rest.client.api.UserRestClient;
import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.User;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;

import lombok.extern.slf4j.Slf4j;

/**
 * @author J. Daniel Sobrado
 * @version 1.0
 * @since 2022-06-25
 */
@Slf4j
@Service
public class JIRAServiceImpl implements JIRAService {

	@Value("${jira.username}")
	private static final String USERNAME = "dalamar01977";

	@Value("${jira.password}")
	private static final String PASSWORD = "admin";
	
	@Value("${jira.url}")
	private static final String HTTP_JIRA_URL = "http://127.0.0.1:8081/";

    private URI getJiraUri() {
        return URI.create(HTTP_JIRA_URL);
    }

    private JiraRestClient getJiraRestClient() {
        return new AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(getJiraUri(), USERNAME, PASSWORD);
    }

    @Override
    public BasicIssue createIssue(String projectKey, String summary, String description, String assignee,
            String issueType) throws InterruptedException, ExecutionException {
        log.info("JIRAServiceV9Impl.createIssue()");

        AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        URI jiraServerUri = URI.create(HTTP_JIRA_URL);
        JiraRestClient restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, USERNAME, PASSWORD);

        // Get Project
        Promise<Project> projectType = restClient.getProjectClient().getProject(projectKey);

        // Get User by name
        UserRestClient userClient = restClient.getUserClient();
        Promise<User> user = userClient.getUser(assignee);
        User assigneeUser = user.get();

        var issueTypeId = getIssueTypeId(projectType, issueType);

        final IssueRestClient issueClient = restClient.getIssueClient();
        IssueInput isssue = new IssueInputBuilder(projectKey, issueTypeId, summary)
                .setDescription(description)
                .setAssignee(assigneeUser)
                .build();

        var createIssue = issueClient.createIssue(isssue);
        BasicIssue issue = createIssue.claim();
        log.info("Created issue: {}", issue.getKey());
        return issue;
    }

    @Override
    public Promise<Void> updateIssue(String projectKey, String issueKey, String summary, String description, String assignee,
            String issueType)
            throws InterruptedException, ExecutionException {
        log.info("JIRAServiceV9Impl.updateIssue()");
        JiraRestClient restClient = getJiraRestClient();

        // Get Project
        Promise<Project> projectType = restClient.getProjectClient().getProject(projectKey);
        // Return of project doesn't exist
        if (projectType.get() == null) {
            log.error("Project {} doesn't exist", projectKey);
            return null;
        }

        // Get Issue
        Promise<Issue> issue = restClient.getIssueClient().getIssue(issueKey);
        // Return of issue doesn't exist
        if (issue.get() == null) {
            log.error("Issue {} doesn't exist", issueKey);
            return null;
        }

        // Get User by name
        User assigneeUser = null;
        if (assignee != null) {
            UserRestClient userClient = restClient.getUserClient();
            Promise<User> user = userClient.getUser(assignee);
            // Return of user doesn't exist
            if (user.get() == null) {
                log.error("User {} doesn't exist", assignee);
                return null;
            }
            assigneeUser = user.get();
        }

        var issueTypeId = getIssueTypeId(projectType, issueType);

        // Update Issue
        final IssueRestClient issueClient = restClient.getIssueClient();
        IssueInput isssue = new IssueInputBuilder(projectKey, issueTypeId, summary)
                .setDescription(description)
                .setAssignee(assigneeUser)
                .build();
        Promise<Void> promise = issueClient.updateIssue(issueKey, isssue);
        log.info("Updated issue: {}", issueKey);
        return promise;
    }

    @Override
    public void deleteIssue(String issueKey) {
        log.info("JIRAServiceV9Impl.deleteIssue()");
        JiraRestClient restClient = getJiraRestClient();
        final IssueRestClient issueClient = restClient.getIssueClient();
        Promise<Issue> issue = issueClient.getIssue(issueKey);
        Issue issueToDelete = issue.claim();
        issueClient.deleteIssue(issueToDelete.getKey(), true);
        log.info("Deleted issue: {}", issueToDelete.getKey());
    }

    @Override
    public void addComment(String issueKey, String comment) {
        log.info("JIRAServiceV9Impl.addComment()");
        JiraRestClient restClient = getJiraRestClient();
        final IssueRestClient issueClient = restClient.getIssueClient();
        Promise<Issue> issue = issueClient.getIssue(issueKey);
        Issue issueToGet = issue.claim();
        log.info("Got issue: {}", issueToGet.getKey());
    }

    @Override
    public void addAttachment(String issueKey, String filePath, String filename) throws IOException {
        log.info("JIRAServiceV9Impl.addAttachment() - filePath: {} - filename: {}", filePath,
                filename);
        JiraRestClient restClient = getJiraRestClient();
        final IssueRestClient issueClient = restClient.getIssueClient();
        // Get Stream from File
        File file = new File(filePath);
        InputStream inputStream = new FileInputStream(file);
        
        Promise<Issue> issue = issueClient.getIssue(issueKey);
        Issue issueToGet = issue.claim();
        log.info("Got issue: {}", issueToGet.getKey());
        // Add Attachment
        issueClient.addAttachment(issueToGet.getAttachmentsUri(), inputStream, filename);
        log.info("Added attachment {} to issue: {}", filename, issueToGet.getKey());
    }

    // Check if issue type exists
    @Override
    public boolean issueTypeExists(String projectKey, String issueType) throws InterruptedException, ExecutionException {
        JiraRestClient restClient = getJiraRestClient();
        ProjectRestClient projectClient = restClient.getProjectClient();
        Promise<Project> projectType = projectClient.getProject(projectKey);
        for (IssueType type : projectType.get().getIssueTypes()) {
            if (type.getName().equalsIgnoreCase(issueType)) {
                return true;
            }
        }
        return false;
    }

    // Get IssueTypeId
    private long getIssueTypeId(Promise<Project> projectType, String typeName)
            throws InterruptedException, ExecutionException {
        var issueTypeId = 0l;
        for (IssueType type : (projectType.get()).getIssueTypes()) {
            if (type.getName().equalsIgnoreCase(typeName)) {
                issueTypeId = type.getId();
                break;
            }
        }
        return issueTypeId;
    }

    // Check if issue exists
    @Override
    public boolean issueExists(String issueKey) {
        JiraRestClient restClient = getJiraRestClient();
        IssueRestClient issueClient = restClient.getIssueClient();
        issueClient.getIssue(issueKey).claim();
        return true;
    }

    // Check if user exists
    @Override
    public boolean userExists(String userName) {
        JiraRestClient restClient = getJiraRestClient();
        UserRestClient userClient = restClient.getUserClient();
        userClient.getUser(userName).claim();
        return true;
    }

    // Check if project exists
    @Override
    public boolean projectExists(String projectKey) {
        JiraRestClient restClient = getJiraRestClient();
        ProjectRestClient projectClient = restClient.getProjectClient();
        projectClient.getProject(projectKey).claim();
        return true;
    }

    // Check if attachment exists
    @Override
    public boolean attachmentExists(String issueKey, String filename) {
        JiraRestClient restClient = getJiraRestClient();
        IssueRestClient issueClient = restClient.getIssueClient();
        var issue = issueClient.getIssue(issueKey).claim();
        Promise<Issue> issuePromise = issueClient.getIssue(issue.getKey());
        Issue issueToGet = issuePromise.claim();
        Iterable<Attachment> attachments = issueToGet.getAttachments();
        for (Attachment attachment : attachments) {
            log.info("Got attachment: {}", attachment.getFilename());
            if (attachment.getFilename().contains(filename)) {
                return true;
            }
        }
        return false;
    }

    // Get Issue by Key
    @Override
    public Issue getIssue(String issueKey) {
        JiraRestClient restClient = getJiraRestClient();
        IssueRestClient issueClient = restClient.getIssueClient();
        Promise<Issue> issue = issueClient.getIssue(issueKey);
        Issue issueToGet = issue.claim();
        log.info("Got issue: {}", issueToGet.getKey());
        return issueToGet;
    }

    // Check if comment exists
    @Override
    public boolean commentExists(String issueKey, String comment) {
        JiraRestClient restClient = getJiraRestClient();
        IssueRestClient issueClient = restClient.getIssueClient();
        Promise<Issue> issue = issueClient.getIssue(issueKey);
        Issue issueToGet = issue.claim();
        log.info("Got issue: {}", issueToGet.getKey());
        Iterable<Comment> comments = issueToGet.getComments();
        for (Comment commentToGet : comments) {
            if (commentToGet.getBody().contains(comment)) {
                return true;
            }
        }
        return false;
    }

    // Get Project by Key
    @Override
    public Project getProject(String projectKey) {
        JiraRestClient restClient = getJiraRestClient();
        ProjectRestClient projectClient = restClient.getProjectClient();
        Promise<Project> project = projectClient.getProject(projectKey);
        Project projectToGet = project.claim();
        log.info("Got project: {}", projectToGet.getKey());
        return projectToGet;
    }

    // Get Issue Type
    @Override
    public String getIssueType(String projectKey, String issueType) {
        JiraRestClient restClient = getJiraRestClient();
        ProjectRestClient projectClient = restClient.getProjectClient();
        Promise<Project> project = projectClient.getProject(projectKey);
        Project projectToGet = project.claim();
        log.info("Got project: {}", projectToGet.getKey());
        for (IssueType type : projectToGet.getIssueTypes()) {
            if (type.getName().equalsIgnoreCase(issueType)) {
                log.info("Got issue type: {}", type.getName());
                return type.getName();
            }
        }
        return null;
    }

    // Get comments for an issue
    @Override
    public Iterable<Comment> getComments(String issueKey) {
        JiraRestClient restClient = getJiraRestClient();
        IssueRestClient issueClient = restClient.getIssueClient();
        Promise<Issue> issue = issueClient.getIssue(issueKey);
        Issue issueToGet = issue.claim();
        log.info("Got issue: {}", issueToGet.getKey());
        return issueToGet.getComments();
    }

    // Get attachments for an issue
    @Override
    public Iterable<Attachment> getAttachments(String key) {
        JiraRestClient restClient = getJiraRestClient();
        IssueRestClient issueClient = restClient.getIssueClient();
        Promise<Issue> issue = issueClient.getIssue(key);
        Issue issueToGet = issue.claim();
        log.info("Got issue: {}", issueToGet.getKey());
        return issueToGet.getAttachments();
    }

    // Get fields for an issue
    @Override
    public Iterable<IssueField> getFields(String issueKey) {
        JiraRestClient restClient = getJiraRestClient();
        IssueRestClient issueClient = restClient.getIssueClient();
        Promise<Issue> issue = issueClient.getIssue(issueKey);
        Issue issueToGet = issue.claim();
        log.info("Got issue: {}", issueToGet.getKey());
        return issueToGet.getFields();
    }

}