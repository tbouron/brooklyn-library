/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

node(label: 'ubuntu') {
    catchError {
        def environmentDockerImage

        def dockerTag = env.BUILD_TAG.replace('%2F', '-')

        withEnv(["DOCKER_TAG=${dockerTag}"]) {
            // TODO: Add timeout

            stage('Clone repository') {
                checkout scm
            }

            stage('Prepare environment') {
                echo 'Building docker image for test environment ...'
                environmentDockerImage = docker.build('brooklyn:${DOCKER_TAG}')
            }

            stage('Run tests') {
                // Run docker as the current user. This requires the uid as well as setting up the user home for the Docker container.
                // See section "Running as non-root" at https://hub.docker.com/_/maven/
                environmentDockerImage.inside('-i --name brooklyn-${DOCKER_TAG} -u $(id -u ${whoami}) -v ${HOME}:/var/maven -v ${WORKSPACE}:/usr/build -w /usr/build -e MAVEN_CONFIG=/var/maven/.m2') {
                    sh 'mvn clean install -Duser.home=/var/maven -Duser.name=$(whoami)'
                }
            }

            // Conditional stage to deploy artifacts, when not building a PR
            if (env.CHANGE_ID == null) {
                stage('Deploy artifacts') {
                    // Run docker as the current user. This requires the uid as well as setting up the user home for the Docker container.
                    // See section "Running as non-root" at https://hub.docker.com/_/maven/
                    environmentDockerImage.inside('-i --name brooklyn-${DOCKER_TAG} -u $(id -u ${whoami}) -v ${HOME}:/var/maven -v ${WORKSPACE}:/usr/build -w /usr/build -e MAVEN_CONFIG=/var/maven/.m2') {
                        // Skip the compilation and tests as it is already done in the previous step
                        sh 'mvn deploy -Dmaven.main.skip -DskipTests -Duser.home=/var/maven -Duser.name=$(whoami)'
                    }
                }

                // TODO: Publish docker image to https://hub.docker.com/r/apache/brooklyn/ ?
            }
        }
    }

    // ---- Post actions steps, to always perform ----

    stage('Publish test results') {
        // Publish JUnit results
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/junitreports/*.xml'

        // Publish TestNG results
        step([
            $class: 'Publisher',
            reportFilenamePattern: '**/testng-results.xml'
        ])
    }

    // Conditional stage, when not building a PR
    if (env.CHANGE_ID == null) {
        stage('Send notifications') {
            // Send email notifications
            step([
                $class: 'Mailer',
                notifyEveryUnstableBuild: true,
                recipients: 'dev@brooklyn.apache.org',
                sendToIndividuals: false // TODO: Change to true when finish test
            ])
        }
    }
}

properties([
    pipelineTriggers([
        githubPush(),
        issueCommentTrigger('.*retest this please.*')
    ])
])
