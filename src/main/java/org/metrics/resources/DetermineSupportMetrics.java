package org.metrics.resources;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import java.util.Formatter;

/**
* Analyzes the data in the JSON file and determines the time spent on each cases by the runtime team.
*/
@Path("/")
public class DetermineSupportMetrics {
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String start() throws Exception {
    return "Welcome to the Runtime team metrics app!" ;
  } 
  
  @GET
  @Path("process/{file}")
  @Produces(MediaType.TEXT_HTML)
  public String processCases(@PathParam("file") final String file) throws Exception {
    if(file != null && !file.isEmpty()){
      String result = new DetermineSupportMetrics().process(file);
      return result;           
    }
    else{
      return "Error\n";
    }        
  }
  
  /**
  * Processes the JSON in the provided file and determines the time spent on each case by
  * the runtime team when the state is open.
  */
  public String process(String file){
    try {
      // Read the contents of the file
      String content = FileUtils.readFileToString(new File(file), "utf-8");        
      JSONArray jsonArray =  new JSONArray(content);
      
      // Map to track the case and the time spent on it by the team
      Map<Integer, CaseDetails> caseTrackingMap = new HashMap<Integer, CaseDetails>();
      
      // Iterate over the JSON input
      for(int i = 0 ; i < jsonArray.length(); i++){
        JSONObject jsonObject = jsonArray.getJSONObject(i); 
        // Unique Case Id used to track the cases
        int caseId =  (Integer) jsonObject.get("case_id");
        CaseDetails caseDetails;
        // New case, no value in the map for the caseId 							
        if(caseTrackingMap.get(caseId) == null){
          caseDetails = new CaseDetails();					
          caseTrackingMap.put(caseId, caseDetails);					
        }
        // Existing case in the map, get the case details
        else{
          caseDetails = caseTrackingMap.get(caseId);										
        }
        // Tracks the timestamp for the case (starttime and endtime)					
        String timestamp = null;
        // Tracks that the case is with runtime team but the assignee has changed
        Boolean isAssigneeOnlyChange = false;
        // JSON row for State details
        if(jsonObject.has("state")){
          JSONObject stateObj = (JSONObject) jsonObject.get("state");
          caseDetails.state = stateObj.getString("to");
          timestamp = jsonObject.getString("timestamp");
        }
        // JSON row for Team details
        else if(jsonObject.has("assignee")){            
          if(caseDetails.team != null && caseDetails.team.equals("Runtime") && caseDetails.assignee != null && 
          caseDetails.assignee != jsonObject.getString("assignee")){
            isAssigneeOnlyChange = true;              
          }
          caseDetails.team = jsonObject.getString("team");
          caseDetails.assignee = jsonObject.getString("assignee");            
          timestamp = jsonObject.getString("timestamp");														
        }
        // Determine if the start time needs to be tracked (when state = open and team = runtime)
        if(!isAssigneeOnlyChange && caseDetails.state != null && caseDetails.state.equals("open") && caseDetails.team != null && caseDetails.team.equals("Runtime")){
          caseDetails.startTime = timestamp;
        }
        // Determine if the ends time needs to be determined (when state != open and team != runtime OR assignee has changed when team = runtime)
        else if(caseDetails.startTime != null && ((!caseDetails.state.equals("open") || !caseDetails.team.equals("Runtime")) || isAssigneeOnlyChange)){
          caseDetails.endTime = timestamp;            
          // Increment the hours spent
          caseDetails.hours += calculateTimeDiff(caseDetails.startTime, caseDetails.endTime);
          // Set the endtime to starttime for upcoming case activities 
          if(isAssigneeOnlyChange){
            caseDetails.startTime = caseDetails.endTime;
          }
        }									
      }
      // Build string to display as a table
      StringBuilder sbuf = new StringBuilder();
      // To track the time spent on the cases
      int mostTime = 0;
      int mostTimeCaseId = 0;
      int leastTime = 0;
      int leastTimeCaseId = 0;
      Formatter fmt = new Formatter(sbuf);        
      sbuf.append("<PRE>");
      sbuf.append("Metrics for Runtime team\n\n");
      sbuf.append("-------------------------\n");
      fmt.format("%s       %s", "CaseId", "Hours\n");
      sbuf.append("-------------------------\n");
      // Build the result JSON from the map
      JSONArray array = new JSONArray();
      for(int caseId : caseTrackingMap.keySet()){				
        JSONObject item = new JSONObject();
        item.put("case_id", caseId);
        int hours = caseTrackingMap.get(caseId).hours;
        item.put("hours", hours);
        // Determine most time, and lease time spent on a case
        if(mostTime == 0 && leastTime == 0){
          mostTime = hours;
          mostTimeCaseId = caseId;
          leastTime = hours;
          leastTimeCaseId = caseId;
        }
        else{
          mostTime = hours > mostTime ? hours : mostTime;
          leastTime = hours < leastTime ? hours : leastTime;
          mostTimeCaseId = hours > mostTime ? caseId : mostTimeCaseId;
          leastTimeCaseId = hours < leastTime ? caseId : leastTimeCaseId;
        }
        // Build string to display as a table
        fmt.format("%s          %s", caseId, caseTrackingMap.get(caseId).hours);          
        sbuf.append("\n");					
        array.put(item);					
      }
      sbuf.append("\n\n\n\n");
      sbuf.append("Raw JSON Data: " + array.toString());
      sbuf.append("\n\n");
      sbuf.append("Most time was spent by the team on case with Id: " + mostTimeCaseId + " Time: " + mostTime);
      sbuf.append("\n");  
      sbuf.append("Least time was spent by the team on case with Id: " + leastTimeCaseId + " Time: " + leastTime);
      sbuf.append("</PRE>");
      sbuf.append("\n\n");
      return sbuf.toString();
    } 
    catch (Exception e) {
      System.out.println(e);
      return e.getMessage();
    } 		
  }
  
  /**
  * Calculates the time difference in hours for the provided times.
  */
  private static int calculateTimeDiff(String startTime, String endTime){
    int diff = 0;
    try {
      // Format for the provided timestamp             
      SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      Date start = utcFormat.parse(startTime);
      Date end = utcFormat.parse(endTime);
      // Determine the hours between startTime and endTime
      diff = (int)((end.getTime() - start.getTime()) / (1000 * 60 * 60));
    } 
    catch(Exception e) {        
    }      
    return diff;
  }
  
  /**
  * Inner class to track the case details like the state, assignee, team, etc.
  */
  private class CaseDetails{
    private String state;
    private String assignee;
    private String team;
    private String startTime;
    private String endTime;
    private int hours;
  }    
}
