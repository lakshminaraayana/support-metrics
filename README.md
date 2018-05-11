Steps to run the app:

OPTION-1 (local)
1. Download the folder in Zip format (support-metrics-master.zip), unzip the contents in to a folder (support-metrics-master).
2. Open a terminal window (#1) and go to the unzipped folder location (for example: /Users/slakshminaraayana/Downloads/support-metrics-master)
3. Run the command - "mvn package" to build the project. 
    1. Please install maven if it has not been installed.
    2. Ensure that a message "BUILD SUCCESS" is seen when the command is executed.
4. Run the command - "sh target/bin/app" to start the web service
    1. Ensure that a message "Jersey service started at http://localhost:9998/application.wadl." is seen when the command is executed.
    2. Leave the terminal window as is, do not close it
5. Open a new terminal window (#2) and run the command "curl http://localhost:9998/process/input.json"
6. In the initial terminal window (#1), the result of the analyzing the cases will be seen.
    1. For example: [Metrics for Runtime team: [{"hours":25,"case_id":100},{"hours":0,"case_id":101}]

OPTION-2 (heroku app)
1. The heroku app (https://salty-shelf-26282.herokuapp.com/) processes the file with the name provided in the URL and returns with the results in the browser screen directly.
2. The URL for the app needs to be appended with "/process/fileName", for instance https://salty-shelf-26282.herokuapp.com/process/input.json will process the file input.json and the processed output will be presented on the screen.
3. Other examples are :
   1. https://salty-shelf-26282.herokuapp.com/process/input1.json
   2. https://salty-shelf-26282.herokuapp.com/process/input2.json
   3. https://salty-shelf-26282.herokuapp.com/process/input3.json
   4. https://salty-shelf-26282.herokuapp.com/process/input4.json
   5. https://salty-shelf-26282.herokuapp.com/process/input5.json
   
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
7. The process also supports mulitple batches, where a case is being tracked across multiple files then the app will keep track of the case and aggregates the final time for the case. For example, the below calls are tracking 2 cases across multiple files.
      1. https://salty-shelf-26282.herokuapp.com/process/batch1.json
      2. https://salty-shelf-26282.herokuapp.com/process/batch2.json
In the second call we will see the aggregated result of the cases.
8. The new GET /cases will provide the consolidated view of all the cases that have been processed so far.
    1. https://salty-shelf-26282.herokuapp.com/cases


 Scenarios Tested:
 1. Pass a valid JSON file and assert the right hours were calculated for a single case.
 2. Pass a valid JSON file and assert the right hours were calculated for multiple cases
 3. Pass a valid JSON file and assert the right hours were calculated for multiple cases when the state is not in open at all.
 4. Pass a valid JSON file and assert the right hours were calculated for multiple cases when the team is not in runtime at all.
 5. Pass a valid JSON file and assert the right hours were calculated for multiple cases when the state is changed to open, to something else and then back to open.
 6. Pass a valid JSON file and assert the right hours were calculated for multiple cases when the team is changed to runtime, to something else and then back to runtime.
 7. Pass an invalid JSON file name and assert an error is thrown.
 8. Pass an invalid JSON format file and assert an error is thrown.
 9. Pass an empty and a null value of a file and assert an error is thrown.



