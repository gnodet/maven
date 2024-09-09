/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

const axios = require('axios');

async function createJiraIssueAndUpdatePR() {
  const jiraDomain = process.env.JIRA_DOMAIN;
  const jiraToken = process.env.JIRA_API_TOKEN;
  const projectKey = process.env.JIRA_PROJECT_KEY;
  const repo = process.env.GITHUB_REPOSITORY;
  const prNumber = process.env.PR_NUMBER;
  const prTitle = process.env.PR_TITLE;

  const auth = {
    headers: {
      Authorization: `Basic ${Buffer.from(`gnodet:${jiraToken}`).toString('base64')}`,
      'Content-Type': 'application/json',
    }
  };

  // Step 1: Create a JIRA issue
  const issueData = {
    fields: {
      project: { key: projectKey },
      summary: `Dependency update: ${prTitle}`,
      description: `Automatically created for PR #${prNumber}.`,
      issuetype: { name: 'Task' }
    }
  };

  let jiraIssueKey;
  try {
    const response = await axios.post(`${jiraDomain}/rest/api/3/issue`, issueData, auth);
    jiraIssueKey = response.data.key;
    console.log(`Created JIRA issue ${jiraIssueKey}`);
  } catch (error) {
    console.error('Error creating JIRA issue:', error);
    process.exit(1);
  }

  // Step 2: Update PR title with the JIRA issue key
  const updatedPRTitle = `[${jiraIssueKey}] ${prTitle}`;

  try {
    const githubResponse = await axios.patch(
      `https://api.github.com/repos/${repo}/pulls/${prNumber}`,
      { title: updatedPRTitle },
      {
        headers: {
          Authorization: `token ${process.env.GITHUB_TOKEN}`,
          'Content-Type': 'application/json',
        },
      }
    );
    console.log(`Updated PR title to: ${updatedPRTitle}`);
  } catch (error) {
    console.error('Error updating PR title:', error);
    process.exit(1);
  }
}

createJiraIssueAndUpdatePR();
