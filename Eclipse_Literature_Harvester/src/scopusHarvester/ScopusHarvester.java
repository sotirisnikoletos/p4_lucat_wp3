/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scopusHarvester;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import help.Helper;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONArray;

/**
 * 
 * https://dev.elsevier.com/cited_by_scopus.html
 * https://dev.elsevier.com/documentation/AbstractCitationCountAPI.wadl
 * https://dev.elsevier.com/documentation/SCOPUSSearchAPI.wadl
 *
 * @author tasosnent
 */
public class ScopusHarvester {
    static boolean debugMode = true;
    String base = "https://api.elsevier.com/content/search/scopus"; //https://dev.elsevier.com/documentation/SCOPUSSearchAPI.wadl
    // send GET request
    RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
    CloseableHttpClient client = null;
//    CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
    //dev.elsevier.com  API key
    String APIKey = "b8be449ef66c871b559014204d0e3c0e";

    public CitedByObject getByPMID(String pmid){
        JSONParser parser = new JSONParser();
        int citedByCount = -1;
        String href = "";
        JSONObject resultObj = null;
        //assemble the esearch URL
        // cited-by count and link to cited-by list of articles
        String url = base + "?query=PMID("+pmid+")&ref=scopus-citedby&httpAccept=application/json&apikey="+APIKey;
       
        // Only cited-by count 
//        String url = base + "?query=PMID("+pmid+")&field=citedby-count";
        try {
            // Log printing
            if(debugMode) {
                System.out.println(" " + new Date().toString() + " Search > Fetch url : " + url);
            }
            // Sent HTTP GET request to Scopus
  //FOT COMMENTED
            //HttpGet request = new HttpGet(url);
            //request.addHeader("X-ELS-APIKey", APIKey);
            // Get HTTP GET response from Scopus
            //HttpResponse response;
            //client = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
            //response = client.execute(request);

           //FOT ADDED IMPROVED CODE FOR THE HTTP GET REQUEST TO SCOPUS API:
            URL callURL = new URL(url);
			HttpURLConnection con = (HttpURLConnection) callURL.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");
			
			BufferedReader rd = new BufferedReader(
			  new InputStreamReader(con.getInputStream()));
			
            // Log printing
            if(debugMode) {
                System.out.println(" " + new Date().toString() + " response received ");
                //System.out.println(" " + new Date().toString() + " Headers :" + response.getAllHeaders());
                //String status = response.getStatusLine().toString();
                int status = con.getResponseCode();
                System.out.println(" " + new Date().toString() + " Status :" + status);
                //if(status.equals("HTTP/1.1 429 Too Many Requests")){
                if (status==429) {   // Too Many Requests
                    try {
                        int days = 4;
                        System.out.println(" " + new Date().toString() + " Status \"Too Many Requests\" received : Sleep for " + days + "days");
                        TimeUnit.DAYS.sleep(days);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ScopusHarvester.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            //FOT commented
            //BufferedReader rd = new BufferedReader( new InputStreamReader(response.getEntity().getContent()));
            //StringBuffer result = new StringBuffer();  //not used

            // Parse result in JSON format and extract cited-by information
    
            resultObj =  (JSONObject) parser.parse(rd);
            rd.close();
            System.out.println(resultObj);
            if(resultObj != null){
                JSONObject results = Helper.getJSONObject("search-results", resultObj);
                if(results != null){
                    JSONArray entry = Helper.getJSONArray("entry", results);
                    if(entry != null){
                        JSONObject entry_0 = (JSONObject) entry.get(0);
                        if(entry_0 != null){
                            // Extract cited-by count number
                            String citedByCountStr = Helper.getString("citedby-count", entry_0);
                            if(citedByCountStr != null){
                                citedByCount = Integer.parseInt(citedByCountStr);
                                // System.out.println("citedByCount: "+citedByCount);
                            } else {
                                System.out.println(" " + new Date().toString() + " Fetch > Empty citedby-count " + resultObj);                    
                            }
                            // Extract cited-by link to results page            
                            JSONArray link = Helper.getJSONArray("link", entry_0);
                            if(link != null){
                                Iterator linkIterator = link.iterator();
                                while(linkIterator.hasNext() & href.equals("")){
                                    JSONObject jo = (JSONObject)linkIterator.next();
                                    String ref = Helper.getString("@ref", jo);
                                    if(ref.equals("scopus-citedby")){
                                        href = Helper.getString("@href", jo);
                                    }
                                }
                            } else {
                                System.out.println(" " + new Date().toString() + " Fetch > Empty link list " + resultObj);                    
                            }
                        } else {
                    System.out.println(" " + new Date().toString() + " Fetch > Empty entry_0 " + resultObj);                    
                    }
                    } else {
                    System.out.println(" " + new Date().toString() + " Fetch > Empty entry " + resultObj);                    
                    }
                } else {
                    System.out.println(" " + new Date().toString() + " Fetch > Empty results " + resultObj);                    
                }
            } else {
                System.out.println(" " + new Date().toString() + " Fetch > Empty resultObj " + resultObj);
                // Use -2 value to distinguish cases where we have been cut from the API
                citedByCount = -2;
            }
        } catch (IOException ex) {
        // Log printing
            if(debugMode) {
                System.out.println(" " + new Date().toString() + " Fetch > IO Exception " + ex.getMessage());
                System.out.println(" " + new Date().toString() + " Fetch > " + resultObj);
            }
        } catch (ParseException ex) {
            // Use -2 value to distinguish cases where we have been cut from the API
            citedByCount = -2;
            System.out.println(" " + new Date().toString() + " ScopusCommunication > JSON Parse Exception " + ex.getMessage());
                System.out.println(" " + new Date().toString() + " Fetch > " + resultObj);
            Logger.getLogger(ScopusHarvester.class.getName()).log(Level.SEVERE, null, ex);
        } 
        // Log printing
//            if(debugMode) {
//                System.out.println(" " + new Date().toString() + " Search end " + citedByCount + " - " + href );
//            }

        return new CitedByObject(citedByCount,href);
    }
    public static void main(String[] args) {
        ScopusHarvester sh = new ScopusHarvester();
        String pmid = "26786552";
        CitedByObject cb = sh.getByPMID(pmid);
        System.out.println(pmid + " : " + cb.getCount() + " > " + cb.getLink());

    }
}
