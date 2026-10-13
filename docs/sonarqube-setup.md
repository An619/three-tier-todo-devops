# SonarQube Setup

This project runs SonarQube analysis from Jenkins using the repository-root `sonar-project.properties` file. The pipeline enforces the Quality Gate before it builds/publishes container images.

## Configure Jenkins

1. Install the Jenkins **SonarQube Scanner** and **Pipeline** plugins.
2. In **Manage Jenkins → Tools**, configure a scanner named exactly `sonar-scanner`.
3. In **Manage Jenkins → System**, configure a SonarQube server named exactly `sonar`, and select a Jenkins-managed credential containing the SonarQube token.
4. In SonarQube, create or confirm the project key `three-tier-todo-devops`. The scanner properties define this key and the frontend/backend source paths.
5. Configure a SonarQube webhook to `https://YOUR_JENKINS_URL/sonarqube-webhook/`. Replace the example host and preserve the trailing slash.
6. Run the Jenkins pipeline and review the analysis in SonarQube. The Jenkins stage named **SonarQube Quality Gate** should receive the webhook result and fail the pipeline when the gate fails.

## Notes

- Do not commit tokens or server credentials.
- Maven verification runs before scanning so Java bytecode exists at `backend/target/classes`.
- If your SonarQube edition or server configuration differs, adjust the project and gate settings deliberately; do not set `abortPipeline: false` just to make the build green.
- The repository configuration is a starting point and still requires validation against the SonarQube server and Jenkins plugins you actually install.
