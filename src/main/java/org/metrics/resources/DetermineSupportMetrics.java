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

@Path("/process")
public class DetermineSupportMetrics {
    @GET
    @Path("/{file}")
    @Produces("application/octet-stream")
    public String handleDownload(@PathParam("file") final String file) throws Exception {
        new DetermineSupportMetrics().process(file);
        return "Success\n";
    }
    
    public void process(String file){
      try {
        String content = FileUtils.readFileToString(new File(file), "utf-8");
        Map<Integer, CaseDetails> caseTrackingMap = new HashMap<Integer, CaseDetails>();
        JSONArray jsonArray =  new JSONArray(content);
        for(int i = 0 ; i < jsonArray.length(); i++){
          JSONObject jsonObject = jsonArray.getJSONObject(i); 
          int caseId =  (Integer) jsonObject.get("case_id");
          CaseDetails caseDetails;							
          if(caseTrackingMap.get(caseId) == null){
            caseDetails = new CaseDetails();					
            caseTrackingMap.put(caseId, caseDetails);					
          }
          else{
            caseDetails = caseTrackingMap.get(caseId);										
          }					
          String timestamp = null;
          Boolean track = false;
          if(jsonObject.has("state")){
            JSONObject stateObj = (JSONObject) jsonObject.get("state");
            caseDetails.state = stateObj.getString("to");
            timestamp = jsonObject.getString("timestamp");
          }
          else if(jsonObject.has("assignee")){            
            if(caseDetails.team != null && caseDetails.team.equals("Runtime") && caseDetails.assignee != null && 
                    caseDetails.assignee != jsonObject.getString("assignee")){
              track = true;              
            }
            caseDetails.team = jsonObject.getString("team");
            caseDetails.assignee = jsonObject.getString("assignee");            
            timestamp = jsonObject.getString("timestamp");														
          }
          if(!track && caseDetails.state != null && caseDetails.state.equals("open") && caseDetails.team != null && caseDetails.team.equals("Runtime")){
            caseDetails.startTime = timestamp;
          }
          else if(caseDetails.startTime != null && ((!caseDetails.state.equals("open") || !caseDetails.team.equals("Runtime")) || track)){
            caseDetails.endTime = timestamp;            
            SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Date start = myFormat.parse(caseDetails.startTime);
            Date end = myFormat.parse(caseDetails.endTime);
            int diff = (int)((end.getTime() - start.getTime()) / (1000 * 60 * 60));
            caseDetails.hours += diff;
            if(track){
              caseDetails.startTime = caseDetails.endTime;
            }
          }									
        }
        JSONArray array = new JSONArray();
        for(int caseId : caseTrackingMap.keySet()){				
          JSONObject item = new JSONObject();
          item.put("case_id", caseId);
          item.put("hours", caseTrackingMap.get(caseId).hours);					
          array.put(item);					
        }
        System.out.println("Final Result =" + array.toString());							
      } 
      catch (Exception e) {
        System.out.println(e);
      } 		
    }
    
    private class CaseDetails{
  		private String state;
      	private String assignee;
  		private String team;
  		private String startTime;
  		private String endTime;
  		private int hours;
  	}    
}
