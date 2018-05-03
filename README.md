README.md

Basic Logic:
1. Validate that an input is provided to parse. If no input (empty / null) then return.
2. Deserialize the provided JSON string.
3. Have a Map to track the each case (keyed by case_id and valued by time/hours)
4. Begin tracking a case only If state == open or team == Runtime.
    1. If string contains ”state”, check if the “to” is “open”
    2. If string contains ”state”, check if the “to” changes from “open”,  if so stop tracking
    3. If string contains ”assignee”, check if the “team” is “Runtime”
        1. If string contains ”assignee”, check if the “team” changes from “Runtime”
        1. If team is "Runtime" then check if the ”assignee” changes.
5. When the above conditions are true start incrementing the time/hours until either the state or the team changes or assignee changes for the runtime team.
6. Build a JSON string with the the case_id from the map and their values.

Steps to run the app:
1. Download the folder in Zip format (support-metrics-master.zip), unzip the contents in to a folder (support-metrics-master).
2. Open a terminal window (#1) and go to the unzipped folder location (for example: /Users/slakshminaraayana/Downloads/support-metrics-master)
3. Run the command - "mvn package" to build the project. 
    1. Please install maven if it has not been installed.
    2. Ensure that a message "BUILD SUCCESS" is seen when the command is executed.
4. Run the command - "sh target/bin/app" to start the web service
    1. Ensure that a message "Jersey service startedß at http://localhost:9998/application.wadl." is seen when the command is executed.
    2. Leave the terminal window as is, do not close it
5. Open a new terminal window (#2) and run the command "curl http://localhost:9998/process/input.json"
6. In the initial terminal window (#1), the result of the analyzing the cases will be seen.
    1. For example: [Metrics for Runtime team: [{"hours":25,"case_id":100},{"hours":0,"case_id":101}]
