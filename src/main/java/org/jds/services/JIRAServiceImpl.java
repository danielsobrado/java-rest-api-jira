package org.jds.services;

import java.io.File;
import java.io.FileInputStream;
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
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.Transition;
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

    @Value("${jira.url}")
    private String url = null;

    @Value("${jira.username}")
    private String username = null;

    @Value("${jira.password}")
    private String password = null;

    private URI getJiraUri() {
        return URI.create(this.url);
    }

    private JiraRestClient getJiraRestClient() {
        return new AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(getJiraUri(), this.username, this.password);
    }

    private static Transition getTransitionByName(Iterable<Transition> transitions, String transitionName) {
        for (Transition transition : transitions) {
            if (transition.getName().equals(transitionName)) {
                return transition;
            }
        }
        return null;
    }

    @Override
    public void createIssue(String projectKey, String summary, String description, String assignee,
            String issueType) throws InterruptedException, ExecutionException {
        log.info("JIRAServiceV9Impl.createIssue()");

        AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        URI jiraServerUri = URI.create(url);
        JiraRestClient restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, username, password);

        // Get Project
        Promise<Project> projectType = restClient.getProjectClient().getProject(projectKey);

        // Get User by name
        UserRestClient userClient = restClient.getUserClient();
        Promise<User> user = userClient.getUser(assignee);
        User assigneeUser = user.get();

        var issueTypeId = getIssueTypeId(projectType, issueType);

        final IssueRestClient issueClient = restClient.getIssueClient();
        try {
            IssueInput isssue = new IssueInputBuilder(projectKey, issueTypeId, summary)
                    .setDescription(description)
                    .setAssignee(assigneeUser)
                    .build();

            Promise<BasicIssue> createIssue = issueClient.createIssue(isssue);
            BasicIssue issue = createIssue.claim();
            log.info("Created issue: {}", issue.getKey());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void updateIssue(String projectKey, String issueKey, String summary, String description, String assignee,
            String issueType)
            throws InterruptedException, ExecutionException {
        log.info("JIRAServiceV9Impl.updateIssue()");
        JiraRestClient restClient = getJiraRestClient();

        // Get Project
        Promise<Project> projectType = restClient.getProjectClient().getProject(projectKey);
        // Return of project doesn't exist
        if (projectType.get() == null) {
            log.error("Project {} doesn't exist", projectKey);
            return;
        }

        // Get Issue
        Promise<Issue> issue = restClient.getIssueClient().getIssue(issueKey);
        // Return of issue doesn't exist
        if (issue.get() == null) {
            log.error("Issue {} doesn't exist", issueKey);
            return;
        }

        // Get User by name
        User assigneeUser = null;
        if (assignee != null) {
            UserRestClient userClient = restClient.getUserClient();
            Promise<User> user = userClient.getUser(assignee);
            // Return of user doesn't exist
            if (user.get() == null) {
                log.error("User {} doesn't exist", assignee);
                return;
            }
            assigneeUser = user.get();
        }

        var issueTypeId = getIssueTypeId(projectType, issueType);

        // Update Issue
        final IssueRestClient issueClient = restClient.getIssueClient();
        try {
            IssueInput isssue = new IssueInputBuilder(projectKey, issueTypeId, summary)
                    .setDescription(description)
                    .setAssignee(assigneeUser)
                    .build();
            issueClient.updateIssue(issueKey, isssue);
            log.info("Updated issue: {}", issueKey);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void deleteIssue(String issueKey) {
        log.info("JIRAServiceV9Impl.deleteIssue()");
        JiraRestClient restClient = getJiraRestClient();
        final IssueRestClient issueClient = restClient.getIssueClient();
        try {
            Promise<Issue> issue = issueClient.getIssue(issueKey);
            Issue issueToDelete = issue.claim();
            issueClient.deleteIssue(issueToDelete.getKey(), true);
            log.info("Deleted issue: {}", issueToDelete.getKey());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void addComment(String issueKey, String comment) {
        log.info("JIRAServiceV9Impl.addComment()");
        JiraRestClient restClient = getJiraRestClient();
        final IssueRestClient issueClient = restClient.getIssueClient();
        try {
            Promise<Issue> issue = issueClient.getIssue(issueKey);
            Issue issueToGet = issue.claim();
            log.info("Got issue: {}", issueToGet.getKey());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void addAttachment(String issueKey, String filePath, String filename) {
        log.info("JIRAServiceV9Impl.addAttachment() - filePath: {} - filename: {}", filePath,
                filename);
        JiraRestClient restClient = getJiraRestClient();
        final IssueRestClient issueClient = restClient.getIssueClient();
        try {
            // Get Stream from File
			File file = new File(filePath);
			InputStream inputStream = new FileInputStream(file);
            
            Promise<Issue> issue = issueClient.getIssue(issueKey);
            Issue issueToGet = issue.claim();
            log.info("Got issue: {}", issueToGet.getKey());
            // Add Attachment
			issueClient.addAttachment(issueToGet.getAttachmentsUri(), inputStream, filename);
			log.info("Added attachment {} to issue: {}", filename, issueToGet.getKey());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    // Check if issue type exists
    @Override
    public boolean issueTypeExists(String projectKey, String issueType) {
        JiraRestClient restClient = getJiraRestClient();
        ProjectRestClient projectClient = restClient.getProjectClient();
        Promise<Project> projectType = projectClient.getProject(projectKey);
        try {
            for (IssueType type : projectType.get().getIssueTypes()) {
                if (type.getName().equalsIgnoreCase(issueType)) {
                    return true;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
        try {
            issueClient.getIssue(issueKey).claim();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Check if user exists
    @Override
    public boolean userExists(String userName) {
        JiraRestClient restClient = getJiraRestClient();
        UserRestClient userClient = restClient.getUserClient();
        try {
            userClient.getUser(userName).claim();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Check if project exists
    @Override
    public boolean projectExists(String projectKey) {
        JiraRestClient restClient = getJiraRestClient();
        ProjectRestClient projectClient = restClient.getProjectClient();
        try {
            projectClient.getProject(projectKey).claim();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Check if attachment exists
    @Override
    public boolean attachmentExists(String issueKey, String filename) {
        JiraRestClient restClient = getJiraRestClient();
        IssueRestClient issueClient = restClient.getIssueClient();
        var issue = issueClient.getIssue(issueKey).claim();
        try {
            Promise<Issue> issuePromise = issueClient.getIssue(issue.getKey());
            Issue issueToGet = issuePromise.claim();
            Iterable<Attachment> attachments = issueToGet.getAttachments();
            for (Attachment attachment : attachments) {
                log.info("Got attachment: {}", attachment.getFilename());
                if (attachment.getFilename().contains("test.txt")) {
                    return true;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();

        }
        return false;
    }

    @Override
    public Issue getIssue(String issueKey) {
        JiraRestClient restClient = getJiraRestClient();
        IssueRestClient issueClient = restClient.getIssueClient();
        try {
            Promise<Issue> issue = issueClient.getIssue(issueKey);
            Issue issueToGet = issue.claim();
            log.info("Got issue: {}", issueToGet.getKey());
            return issueToGet;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean commentExists(String issueKey, String comment) {
        JiraRestClient restClient = getJiraRestClient();
        IssueRestClient issueClient = restClient.getIssueClient();
        try {
            Promise<Issue> issue = issueClient.getIssue(issueKey);
            Issue issueToGet = issue.claim();
            log.info("Got issue: {}", issueToGet.getKey());
            Iterable<Comment> comments = issueToGet.getComments();
            for (Comment commentToGet : comments) {
                if (commentToGet.getBody().contains(comment)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

}