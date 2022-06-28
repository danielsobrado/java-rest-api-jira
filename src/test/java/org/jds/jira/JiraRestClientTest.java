package org.jds.jira;


import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.jds.services.JIRAService;
import org.jds.services.JIRAServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;

import lombok.extern.slf4j.Slf4j;

/**
 * @author J. Daniel Sobrado
 * @version 1.0
 * @since 2022-06-26
 * 
 * For this test case to run you need to have Jira server running on localhost:8081.
 * docker run -d --name jira -e JVM_SUPPORT_RECOMMENDED_ARGS=-Datlassian.recovery.password=demo -p 8081:8080 atlassian/jira-software:latest
 */
@Slf4j
public class JiraRestClientTest {

	@Value("${jira.username}")
	private static final String USERNAME = "dalamar01977";

	@Value("${jira.password}")
	private static final String PASSWORD = "admin";
	
	@Value("${jira.url}")
	private static final String HTTP_JIRA_URL = "http://127.0.0.1:8081/";

	// Before running this test case, you need to create a project in Jira server.
	// You can use the following command to create a project:
	// curl -u admin:admin -X POST -H "Content-Type:application/json" -d '{"key":"JDS", "name":"JDS","projectTypeKey":"business", "lead":"admin"}' http://localhost:8081/rest/api/2/project
	public static final String PROJECT_KEY = "JDS";

	// Before running this test case, you need to create a issue type in Jira server.
	// You can use the following command to create a issue type:
	// curl -u admin:admin -X POST -H "Content-Type:application/json" -d '{"name":"Task","description":"A Task."}' http://localhost:8081/rest/api/2/issuetype
	public static final String ISSUE_TYPE = "Task";

	static URI uri;
	static AsynchronousJiraRestClientFactory factory;
	static JiraRestClient restClient;

	static JIRAService jiraService;

	@BeforeAll
	public static void setup() {
		log.info("JiraRestClientTest.setup()");
		uri = URI.create(HTTP_JIRA_URL);
		factory = new AsynchronousJiraRestClientFactory();
		restClient = factory.createWithBasicHttpAuthentication(uri, USERNAME, PASSWORD);
		jiraService = new JIRAServiceImpl();
	}

	@Test  
	void testIssue() throws InterruptedException, ExecutionException {  

		// Get Project
		Promise<Project> projectType = restClient.getProjectClient().getProject(PROJECT_KEY);
		BasicProject project = projectType.get();

        var issueTypeId = getIssueTypeId(PROJECT_KEY, ISSUE_TYPE);

		final IssueRestClient issueClient = restClient.getIssueClient();
		String issueKey = "";
		try {
			IssueInput isssue = new IssueInputBuilder(PROJECT_KEY, issueTypeId, "API JIRA Test").build();
			Promise<BasicIssue> createIssue = issueClient.createIssue(isssue);
			BasicIssue issue = createIssue.claim();
			issueKey = issue.getKey();
			log.info("Created issue: {}", issueKey);
			assertTrue(issueKey.length() > 0);
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// Update Issue
		try {
			Promise<Issue> issue = issueClient.getIssue(issueKey);
			Issue issueToUpdate = issue.claim();
			IssueInput issueInput = new IssueInputBuilder(project, issueToUpdate.getIssueType()).setSummary("API JIRA Test 2.1").build();
			issueClient.updateIssue(issueKey, issueInput);
			log.info("Updated issue: {}", issueKey);
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// Delete Issue
		try {
			Promise<Issue> issue = issueClient.getIssue(issueKey);
			Issue issueToDelete = issue.claim();
			issueClient.deleteIssue(issueToDelete.getKey(), true);
			log.info("Deleted issue: " + issueToDelete.getKey());
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	// Get IssueTypeId
	private long getIssueTypeId(String project, String typeName) throws InterruptedException, ExecutionException {
        Promise<Project> projectType = restClient.getProjectClient().getProject(project);
		var issueTypeId = 0l;
        for(IssueType type : (projectType.get()).getIssueTypes())
        {
            if(type.getName().equalsIgnoreCase(typeName))
            {
				issueTypeId = type.getId();
				break;
            }
        }
		return issueTypeId;
	}

	@Test
	void testGetComments() throws InterruptedException, ExecutionException {

		// Create Issue
		final IssueRestClient issueClient = restClient.getIssueClient();
		String issueKey = "";

		var issueTypeId = getIssueTypeId(PROJECT_KEY, ISSUE_TYPE);

		try {
			IssueInput isssue = new IssueInputBuilder(PROJECT_KEY, issueTypeId, "API JIRA Test").build();
			Promise<BasicIssue> createIssue = issueClient.createIssue(isssue);
			BasicIssue issue = createIssue.claim();
			issueKey = issue.getKey();
			log.info("Created issue: {}", issueKey);
			assertTrue(issueKey.length() > 0);
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// Comment Issue
		try {
			Promise<Issue> issue = issueClient.getIssue(issueKey);
			Issue issueToGet = issue.claim();
			issueClient.addComment(issueToGet.getCommentsUri(), Comment.valueOf("This is an important comment to reply."));
			log.info("Added comment to issue: {}", issueToGet.getKey());
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// Get Comments and reply
		try {
			Promise<Issue> issue = issueClient.getIssue(issueKey);
			Issue issueToGet = issue.claim();
			Iterable<Comment> comments = issueToGet.getComments();
			assertTrue(comments.iterator().hasNext());
			for(Comment comment : comments) {
				log.info("Got comment: {}", comment.getBody());

				// if comment contains "important" then reply to it
				if(comment.getBody().contains("important")) {
					issueClient.addComment(issueToGet.getCommentsUri(), Comment.valueOf("This is an automated reply to the important comment."));
					log.info("Added comment to issue: {}", issueToGet.getKey());
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// Assert that the comment is available
		try {
			Promise<Issue> issue = issueClient.getIssue(issueKey);
			Issue issueToGet = issue.claim();
			Iterable<Comment> comments = issueToGet.getComments();
			for(Comment comment : comments) {
				if (comment.getBody().contains("automated reply")) {
					log.info("Got comment: {}", comment.getBody());
					assertTrue(comment.getBody().contains("automated reply"));
				}
				log.info("Got comment: {}", comment.getBody());
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// Delete Issue
		try {
			issueClient.deleteIssue(issueKey, true).claim();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	@Test
	void testUploadAttachment() throws InterruptedException, ExecutionException {

		// Create Issue
		var issueTypeId = getIssueTypeId(PROJECT_KEY, ISSUE_TYPE);
		BasicIssue issue = null;
		final IssueRestClient issueClient = restClient.getIssueClient();
		try {
			IssueInput isssue = new IssueInputBuilder("JDS", issueTypeId, "API JIRA Attach test").build();
			Promise<BasicIssue> createIssue = issueClient.createIssue(isssue);
			issue = createIssue.claim();
			log.info("Created issue: {}", issue.getKey());
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// Upload Attachment
		try {
			// Get Stream from File
			File file = new File("src/test/resources/test.txt");
			InputStream inputStream = new FileInputStream(file);

			Promise<Issue> issuePromise = issueClient.getIssue(issue.getKey());
			Issue issueToGet = issuePromise.claim();

			issueClient.addAttachment(issueToGet.getAttachmentsUri(), inputStream, "test.txt");
			log.info("Added attachment to issue: {}", issue.getKey());
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// Get Attachments
		try {
			Promise<Issue> issuePromise = issueClient.getIssue(issue.getKey());
			Issue issueToGet = issuePromise.claim();
			Iterable<Attachment> attachments = issueToGet.getAttachments();
			// check that there is at least one attachment
			assertTrue(attachments.iterator().hasNext());
			for(Attachment attachment : attachments) {
				log.info("Got attachment: {}", attachment.getFilename());
				assertTrue(attachment.getFilename().contains("test.txt"));
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// Delete Issue
		try {
			issueClient.deleteIssue(issue.getKey(), true);
			log.info("Deleted issue: {}", issue.getKey());
			assertNull(issueClient.getIssue(issue.getKey()).claim());
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	@Test
	void testGetFields() throws InterruptedException, ExecutionException {

		// Create Issue
		final IssueRestClient issueClient = restClient.getIssueClient();
		String issueKey = "";

		var issueTypeId = getIssueTypeId(PROJECT_KEY, ISSUE_TYPE);

		try {
			IssueInput isssue = new IssueInputBuilder(PROJECT_KEY, issueTypeId, "API JIRA Test").build();
			Promise<BasicIssue> createIssue = issueClient.createIssue(isssue);
			BasicIssue issue = createIssue.claim();
			issueKey = issue.getKey();
			log.info("Created issue: {}", issueKey);
			assertTrue(issueKey.length() > 0);
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// Get Fields
		try {
			Promise<Issue> issue = issueClient.getIssue("JDS-1");
			Issue issueToGet = issue.claim();
			Iterable<IssueField> fields = issueToGet.getFields();
			for(IssueField field : fields) {
				log.info("Got field: {}", field.getName());
			}
			assertTrue(fields.iterator().hasNext());
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}