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

async function updatePRDescription() {
  const repo = process.env.GITHUB_REPOSITORY;
  const prNumber = process.env.PR_NUMBER;
  let prBody = process.env.PR_BODY || '';  // In case there's no body initially

  // Step 1: Append command info to the PR description
  const commandInfo = `\n\n---\n\n### MavenBot Commands:\nTo create a JIRA issue, comment: \`@mavenbot create jira\``;

  // Check if the command info is already in the description
  if (prBody.includes('@mavenbot create jira')) {
    console.log('PR description already contains the command.');
    return;
  }

  // Append the command information to the PR body
  prBody += commandInfo;

  // Step 2: Update the PR with the new description
  try {
    await axios.patch(
      `https://api.github.com/repos/${repo}/pulls/${prNumber}`,
      { body: prBody },
      {
        headers: {
          Authorization: `token ${process.env.GITHUB_TOKEN}`,
          'Content-Type': 'application/json',
        }
      }
    );
    console.log(`Updated PR #${prNumber} description with MavenBot command.`);
  } catch (error) {
    console.error('Error updating PR description:', error);
    process.exit(1);
  }
}

updatePRDescription();
