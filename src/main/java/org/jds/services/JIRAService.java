package org.jds.services;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.util.concurrent.Promise;

/**
 * @author J. Daniel Sobrado
 * @version 1.0
 * @since 2022-06-25
 */
@Service
public interface JIRAService {
    public BasicIssue createIssue(String projectKey, String summary, String description, String assignee, String issueType) throws InterruptedException, ExecutionException;
    public Promise<Void> updateIssue(String projectKey, String issueKey, String summary, String description, String assignee, String issueType) throws InterruptedException, ExecutionException;
    public void deleteIssue(String issueKey);
    public Issue getIssue(String issueKey);
    public void addComment(String issueKey, String comment);
    public void addAttachment(String issueKey, String filePath, String filename) throws IOException;
    public boolean userExists(String userName);
    public boolean projectExists(String projectKey);
    public boolean issueTypeExists(String projectKey, String issueType) throws InterruptedException, ExecutionException;
    public boolean issueExists(String issueKey);
    boolean commentExists(String issueKey, String comment);
    boolean attachmentExists(String issueKey, String filePath);
    public Project getProject(String projectKey);
    public String getIssueType(String projectKey, String issueType);
    public Iterable<Comment> getComments(String issueKey);
    public Iterable<Attachment> getAttachments(String key);
    public Iterable<IssueField> getFields(String issueKey);
}