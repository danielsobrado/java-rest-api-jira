package org.jds.services;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.atlassian.jira.rest.client.api.domain.Issue;

/**
 * @author J. Daniel Sobrado
 * @version 1.0
 * @since 2022-06-25
 */
@Service
public interface JIRAService {
    public void createIssue(String projectKey, String summary, String description, String assignee, String issueType) throws InterruptedException, ExecutionException;
    public void updateIssue(String projectKey, String issueKey, String summary, String description, String assignee, String issueType) throws InterruptedException, ExecutionException;
    public void deleteIssue(String issueKey);
    public Issue getIssue(String issueKey);
    public void addComment(String issueKey, String comment);
    public void addAttachment(String issueKey, String filePath, String filename);
    public boolean userExists(String userName);
    public boolean projectExists(String projectKey);
    public boolean issueTypeExists(String projectKey, String issueType);
    public boolean issueExists(String issueKey);
    boolean commentExists(String issueKey, String comment);
    boolean attachmentExists(String issueKey, String filePath);
}