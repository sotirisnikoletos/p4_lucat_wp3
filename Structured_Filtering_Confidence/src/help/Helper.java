/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//Commented for Server compile 
package help;


import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import static java.lang.System.exit;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
// Add Tasks\00 Logs etc\Libs\gson-2.0.jar
import com.google.gson.GsonBuilder;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
// Add Tasks\00 Logs etc\Libs\httpcomponents-client-4.5.1\lib\*.jar
// httpcore-4.4.3.jar
import org.apache.http.HttpResponse;
// httpclient-4.5.1.jar
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
// Add Tasks\00 Logs etc\Libs\json-simple-1.1.1.jar
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
// Add Tasks\00 Logs etc\Libs\commons-io\commons-io\2.4\commons-io-2.4.jar
import static org.apache.commons.io.FileUtils.copyFile;


/**
 *
 * @author tasosnent
 * 
 * Various Helper methods
 *      Updated 20/12/2016
 * 
 */
public class Helper {
    public static boolean debugMode = true;
   
    public static String readFile(String filePath) {
        String text = "";
            // open input stream filePath.txt for reading purpose.
            BufferedReader br;
        try {
            br = new BufferedReader( new FileReader(filePath));
            String line = "";
            while ((line = br.readLine()) != null ) { 
                text += line;
            }
            br.close();
        } catch (FileNotFoundException ex) {
            printMessage(" " + new Date().toString() + " caught a FileNotFoundException for file " + filePath,2);
        } catch (IOException ex) {
            printMessage(" " + new Date().toString() + " caught a IOException for file " + filePath,2);
        } 
        return text;
    } 
    
    /** Read a JSON file into a JSONObject
     * 
     * @param filePath  File to read a JSON
     * @return          The JSONObject read
     */
    public static JSONObject readJsonFile(String filePath) {
        JSONObject json = null;
            // open input stream filePath.txt for reading purpose.
            BufferedReader br;
        try {
            br = new BufferedReader( new FileReader(filePath));
            //  Parse json object into a String to be parsed 
            json = (JSONObject) JSONValue.parseWithException(br);
            br.close();
        } catch (FileNotFoundException ex) {
            printMessage(" " + new Date().toString() + " caught a FileNotFoundException for file " + filePath,2);
        } catch (IOException ex) {
            printMessage(" " + new Date().toString() + " caught a IOException for file " + filePath,2);
        } catch (ParseException ex) {
            printMessage(" " + new Date().toString() + " caught a ParseException for file " + filePath,2);
            printMessage(ex.getMessage(),3);
            ex.printStackTrace();
        }
        return json;
    } 
  
    public static void main(String[] args) {  
//        printQuestionIDs("D:\\task4b\\golden_files\\golden_14.json");
//        printQuestionIDs("D:\\task4b\\golden_files\\golden_15.json");
//        printQuestionIDs("D:\\task4b\\golden_files\\golden_16.json");
//        printQuestionIDs("D:\\task4b\\golden_files\\golden_17.json");
//        printQuestionIDs("D:\\task4b\\golden_files\\golden_18.json");       
    }
    /**
     * Write a JSON Object into a file
     * 
     * @param filePath      Path to write the file
     * @param subbmission   JSON object to be written
     */
    public static void writeJsonFile(String filePath, JSONObject subbmission){
//        try {
//            BufferedWriter jsonWriter = new BufferedWriter(new FileWriter(filePath));
////            jsonWriter.append(subbmission.toJSONString());
//            // print human readable using GsonBuilder library
//            // disableHtmlEscaping to not replace sepcial characters
//            jsonWriter.append(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(subbmission));
//            jsonWriter.flush();
//            jsonWriter.close();
//        } catch (IOException ex) {
//            printMessage(" " + new Date().toString() + " caught a IOException for file " + filePath,2);
//        }
        writeFile(filePath, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(subbmission));
    }
    /**
     * Write a String into a file
     * 
     * @param filePath      Path to write the file
     * @param content       String value to be written
     */
    public static void writeFile(String filePath, String content){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
//            jsonWriter.append(subbmission.toJSONString());
            // print human readable using GsonBuilder library
            // disableHtmlEscaping to not replace sepcial characters
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            printMessage(" " + new Date().toString() + " caught a IOException writing in file " + filePath,2);
            ex.printStackTrace();
        }
    }
    /** print a message in specified depth
     * 
     * @param msg       String message
     * @param depth     number of tabs to intend
     */
    public static void printMessage(String msg, int depth ){
        for(int i = 0; i < depth; i++)
             System.out.print("\t");
        System.out.println(msg);
    }
    
    /** 
     * return "JSONArray of key provided" or null, in case of absence or wrong type
     * 
     * @param key   Name of field to search
     * @param o     JSONObject to search in 
     * @return      JSONArray found or null
     */
    public static JSONArray getJSONArray(String key, JSONObject o){
        JSONArray a = null;
        if(o.containsKey(key)){
           if(o.get(key) instanceof JSONArray){
               a = (JSONArray) o.get(key);
           } else {
//                printMessage("Warning: element with key "+key+" not a valid JSONArray. in " + o.toJSONString());               
           }
        }// else { printMessage("Error: element with key "+key+" not present in " + o.toJSONString());}
        return a;
    }
    
    /**
     * Get an Array from a JSONObject written in given JSON file.
     * @param key       Name of the array to get
     * @param jsonFile  Path of the file to read
     * @return          "JSONArray of key provided" or null, in case of absence or wrong type 
     */
    public static JSONArray getJSONArray(String key,String jsonFile){
        JSONObject ob = readJsonFile(jsonFile);
        return getJSONArray(key, ob);
    }
    
    /** Return "JSONObject of key provided" or null, in case of absence or wrong type
     * 
     * @param key   Name of field to search
     * @param o     JSONObject to search in 
     * @return      JSONObject found or null
     */
    public static JSONObject getJSONObject(String key, JSONObject o){
        JSONObject a = null;
        if(o.containsKey(key)){
           if(o.get(key) instanceof JSONObject){
               a = (JSONObject) o.get(key);
           } else {
                printMessage("Warning: element with key "+key+" not a valid JSONObject. in " + o.toJSONString(),2);               
           }
        } //else {  printMessage("Warning: element with key "+key+" not present in " + o.toJSONString()); }
        return a;
    }
    
    /** Return "String of key provided" or null, in case of absence or wrong type
     * 
     * @param key   Name of field to search
     * @param o     JSONObject to search in 
     * @return      String found or null
     */
    public static String getString(String key, JSONObject o){
        String a = null;
        if(o.containsKey(key)){
           if(o.get(key) instanceof String){
               a = (String) o.get(key);
           } else {
                printMessage("Warning: element with key "+key+" not a valid String. in " + o.toJSONString(),2);               
           }
        } //else {  printMessage("Warning: element with key "+key+" not present in " + o.toJSONString()); }
        return a;
    }   

    /** 
     * Return "Integer value of key provided" or null, in case of absence or wrong type
     * 
     * @param key   Name of field to search
     * @param o     JSONObject to search in 
     * @return      Integer found or null
     */
    public static Integer getInteger(String key, JSONObject o){
        if(o.containsKey(key)){
           if(o.get(key) instanceof Integer){
              int a = (Integer) o.get(key);
                return a;
           } else {
                printMessage("Warning: element with key "+key+" not a valid int. in " + o.toJSONString(),2);               
           }
        } //else {  printMessage("Warning: element with key "+key+" not present in " + o.toJSONString()); }
        return null;
    }  
    
    /** Return "Double value of key provided" or null, in case of absence or wrong type
     * 
     * @param key   Name of field to search
     * @param o     JSONObject to search in 
     * @return      Double found or null
     */
    public static Double getDouble(String key, JSONObject o){
        if(o.containsKey(key)){
           if(o.get(key) instanceof Double){
              double a = (Double) o.get(key);
                return a;
           } else {
                printMessage("Warning: element with key "+key+" not a valid Double. in " + o.toJSONString(),2);               
           }
        } //else {  printMessage("Warning: element with key "+key+" not present in " + o.toJSONString()); }
        return null;
    }   

    /** Return "String value of key provided" or null, in case of absence or wrong type
     * 
     * @param key   Name of field to search
     * @param o     JSONObject to search in 
     * @return      String found or null
     */
    public static String getAnyType(String key, JSONObject o){
        if(o.containsKey(key)){
            return o.get(key)+"";           
        } //else {  printMessage("Warning: element with key "+key+" not present in " + o.toJSONString()); }
        return null;
    }  
    
    /** 
     * Return "Float value of key provided" or null, in case of absence or wrong type
     * 
     * @param key   Name of field to search
     * @param o     JSONObject to search in 
     * @return      Float found or null
     */
    public static Float getFloat(String key, JSONObject o){
        if(o.containsKey(key)){
           if(o.get(key) instanceof Float){
              float a = (Float) o.get(key);
                return a;
           } else {
                printMessage("Warning: element with key "+key+" not a valid float. in " + o.toJSONString(),2);               
           }
        } //else {  printMessage("Warning: element with key "+key+" not present in " + o.toJSONString()); }
        return null;
    }    
    
    /**
     * Create a folder in the specified path
     * 
     * @param folder Path to create the folder
     * @return  true if folder created, false otherwise
     */
    public static boolean createFolder(String folder){
        if (!(new File(folder)).mkdirs()) {
           printMessage("Warning!: Folder "+ folder +" allready Exists",2);   
           return false;
        } else {
//            printMessage(" Folder "+ folder +" succesfully created");            
           return true;
        }
    }
    
    /**
     * Replace old HostNames with new ones
     * 
     * @param url   URL to update with new host name
     * @return      Updated URL, with new host name
     */
    public static String replaceOldHosts(String url){
        HashMap <String,String> alternativeNS = new HashMap <> (); 
        // Hardcoded values for BioASQ4 submissions 
        // pairs of alternative-defualt namepsaces to be replaced
        alternativeNS.put("http://www.nlm.nih.gov/cgi/mesh/2015/MB_cgi?field=uid&exact=Find+Exact+Term&term=","http://www.nlm.nih.gov/cgi/mesh/2016/MB_cgi?field=uid&exact=Find+Exact+Term&term=");
        alternativeNS.put("http://www.nlm.nih.gov/cgi/mesh/2014/MB_cgi?field=uid&exact=Find+Exact+Term&term=","http://www.nlm.nih.gov/cgi/mesh/2016/MB_cgi?field=uid&exact=Find+Exact+Term&term=");
        alternativeNS.put("http://www.nlm.nih.gov/cgi/mesh/2013/MB_cgi?field=uid&exact=Find+Exact+Term&term=","http://www.nlm.nih.gov/cgi/mesh/2016/MB_cgi?field=uid&exact=Find+Exact+Term&term=");
        alternativeNS.put("http://www.nlm.nih.gov/cgi/mesh/2012/MB_cgi?field=uid&exact=Find+Exact+Term&term=","http://www.nlm.nih.gov/cgi/mesh/2016/MB_cgi?field=uid&exact=Find+Exact+Term&term=");
        alternativeNS.put("http://purl.bioontology.org/ontology/MESH/","http://www.nlm.nih.gov/cgi/mesh/2016/MB_cgi?field=uid&exact=Find+Exact+Term&term=");
        alternativeNS.put("http://purl.obolibrary.org/obo/DOID_","http://www.disease-ontology.org/api/metadata/DOID:");
        alternativeNS.put("http://purl.obolibrary.org/obo/GO_","http://amigo.geneontology.org/amigo/term/GO:");
        alternativeNS.put("http://amigo.geneontology.org/cgi-bin/amigo/term_details?term=GO:","http://amigo.geneontology.org/amigo/term/GO:");
        alternativeNS.put("http://amigo.geneontology.org/cgi-bin/amigo/term_details?term=","http://amigo.geneontology.org/amigo/term/GO:");
        String response = url;
        for(String oldHost : alternativeNS.keySet()){
            if(url.contains(oldHost))
                response = url.replace(oldHost, alternativeNS.get(oldHost));
        }
        return response;
    }
    
    /** 
     * Get a list of all files (FilePaths) contained in the folder (and sub-folders)
     * 
     * @param folderPath    the path of a folder with files 
     * @return              a ArrayList of all files(FilePaths) contained in the folder (and sub-folders)
     */
    public static ArrayList <Path> readSubmissionsFolder(String folderPath){
    final ArrayList <Path> files = new ArrayList <> ();
    Path path = Paths.get(folderPath);
    if (Files.isDirectory(path)) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    files.add(file);
//                    System.out.println(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            //log printing
            printMessage(" " + new Date().toString() + " Exception walking in directory : " + path,2); 
        }
    } else {
        //log printing
        printMessage(" " + new Date().toString() + " not a Directory : " + path,2);              
    }
    return files;
}
    
    /** 
     * Get a list of all files (FileNames) contained in the folder (and sub-folders)
     * 
     * @param folderPath    the path of a folder with files 
     * @return              a ArrayList of all files(FileNames) contained in the folder (and sub-folders)
     */
    public static ArrayList <String> readSubmissionsFileNames(String folderPath){
         ArrayList <String> s = new ArrayList <>();
         ArrayList <Path> paths = readSubmissionsFolder(folderPath);
            for(Path p : paths){
                s.add(p.toFile().getName().replace(".json", ""));
//                System.out.println(p.toFile().getName().replace(".json", ""));
            }
         return s;
     }
    
    /**
     * Get a list of files in a folder
     * @param basePath
     * @return 
     */
    public static ArrayList<Path> getFilesInFolder(String basePath)   {
        final ArrayList<Path> files = new ArrayList();
        Path path = Paths.get(basePath);
        if (Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        files.add(file);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ex) {
                System.out.println(" " + new Date().toString() + " Exception : " + ex.getLocalizedMessage());                         
            }
            
        } else {
            //log printing
            System.out.println(" " + new Date().toString() + " not a Directory : " + path);              
        }
        return files;
    }  

    /**
     * Convert a PMCID to PMID
     * @param pmcid     PubMed Central ID to convert 
     * @return          corresponding PubMed id 
     */
    public static String pmcidTopmid(String pmcid){
        // API string for id convertion
        String API = "https://www.ncbi.nlm.nih.gov/pmc/utils/idconv/v1.0/?tool=my_tool&email=my_email@example.com&format=json&ids=";
        String s = "";
            HttpClient client = HttpClientBuilder.create().build();
            String url = API+pmcid;
            
            HttpGet request = new HttpGet(url);

            HttpResponse response;
            String respBody = "";
            try {
                response = client.execute(request);

                BufferedReader rd = new BufferedReader( new InputStreamReader(response.getEntity().getContent()));
                StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = rd.readLine()) != null) {
                    respBody+=line;
                }
                rd.close();
            } catch (IOException ex) {
                System.out.println("IOException : " + ex.getLocalizedMessage());
            }
//            System.out.println(respBody);
            try {
                JSONObject jsonResponce = (JSONObject) JSONValue.parseWithException(respBody);
                JSONArray records = getJSONArray("records", jsonResponce);
                for(Object recO : records){
                    JSONObject rec = (JSONObject)recO;
                    if(rec.containsKey("pmid")){
                        s = Helper.getString("pmid", rec);
                    }
                }
            } catch (ParseException ex) {
                System.out.println("ParseException : " + ex.getLocalizedMessage());
            }
         return s;
     }
    
    /** Read a file with a list of IDs (one ID per line) and return a list of them 
     * 
     * @param filePath  Path to file containing a list of IDs (one ID per line)
     * @return          ArrayList of IDs
     */
    public static ArrayList<String> readListIds(String filePath) {
        ArrayList <String> Ids = new ArrayList <String>();
        String json = "";
        try{
            // open input stream filePath.txt for reading purpose.
            BufferedReader br = new BufferedReader( new FileReader(filePath));
            String line = "";
            while ((line = br.readLine()) != null ) { 
                line = line.trim();
                if(!line.equals("")){
                    Ids.add(line);                    
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println(" \t Error\t: readListIds FileNotFoundException : " + ex.getLocalizedMessage());
            exit(100);
        } catch (UnsupportedEncodingException ex) {
            System.out.println(" \t Error\t: readListIds UnsupportedEncodingException : " + ex.getLocalizedMessage());
            exit(100);
        } catch (IOException ex) {
            System.out.println(" \t Error\t: readListIds IOException : " + ex.getLocalizedMessage());
            exit(100);
        }
        return Ids;
    }  
    
    /**
     * Reads a test data JSON file of Task A and returns a PMID list 
     * 
     * @param filePathTestData  test data JSON file of Task A 
     * @return                  a PMID list of articles in test set
     */
    public static ArrayList<String> getPmids(String filePathTestData) {
        ArrayList <String> currentPmids = new ArrayList <String>();
        String json = "";
        try{
            // open input stream filePath.txt for reading purpose.
            BufferedReader br = new BufferedReader( new FileReader(filePathTestData));
            
            //  Parse json object into a String to be parsed 
            JSONObject results = (JSONObject) JSONValue.parseWithException(br);
            br.close();
                JSONArray documents = (JSONArray) results.get("documents");

            JSONObject document = null;
            String docPmid = null;
            for(int i=0 ; i < documents.size() ; i++){
                document = (JSONObject) documents.get(i);
                docPmid =  document.get("pmid").toString();
                currentPmids.add(docPmid);
            }
        } catch (FileNotFoundException ex) {
            System.out.println(" \t\t Error\t: Helper.getPmids FileNotFoundException : " + ex.getLocalizedMessage());
        } catch (UnsupportedEncodingException ex) {
            System.out.println(" \t\t Error\t: Helper.getPmids UnsupportedEncodingException : " + ex.getLocalizedMessage());
        } catch (IOException | ParseException ex) {
            System.out.println(" \t\t Error\t: Helper.getPmids IOException | ParseException : " + ex.getLocalizedMessage());
        }
        return currentPmids;
    } 
 
    /**
     * Convert JSONArray of Strings into a String
     *      Used to concatenate PMC full text parts into a string for PMC articles 
     * 
     * @param StrArr    Array of Strings
     * @return          String of concatenated Strings
     */
    public static String JSONArrayToString(JSONArray StrArr){
        return JSONArrayToString(StrArr, "");
    }

    public static String JSONArrayToString(JSONArray StrArr, String delimiter){
        String str = new String();
            for(int i=0 ; i < StrArr.size(); i++){
                if(i != 0){
                    str += delimiter;
                }
                str += StrArr.get(i).toString();
            }
        return str;
    }
    
    /**
     * Convert Array of Strings into a String
     *      Used to concatenate Abstract sections into an abstract string for PubMed articles 
     * 
     * @param StrArr    Array of Strings
     * @return          String of concatenated Strings
     */
    public static String StringArrayToString(String[] StrArr){
        String str = new String();
            for(int i=0 ; i < StrArr.length; i++){
                str += StrArr[i];
            }
        return str;
    }
    
    /**
     * Convert Array of Strings into a JSONArray of Strings
     * @param StrArr    Array of Strings
     * @return          corresponding JSONArray of Strings
     */
    public static JSONArray StringArrayToJSONList(String[] StrArr){
        JSONArray List = new JSONArray();
            for(int i=0 ; i < StrArr.length; i++){
                List.add(StrArr[i]);
            }
        return List;
    }
    
    /**
     * Prints a message of time passed for a job
     * 
     * @param start Date object before the job
     * @param end   Date object after the job
     * @param job   a String describing the job
     */
    public static void printTime(Date start, Date end, String job){
         long miliseconds = end.getTime() - start.getTime();
                String totalTime = String.format("%02d:%02d:%02d", 
                    TimeUnit.MILLISECONDS.toHours(miliseconds),
                    TimeUnit.MILLISECONDS.toMinutes(miliseconds) - 
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(miliseconds)),
                    TimeUnit.MILLISECONDS.toSeconds(miliseconds) - 
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(miliseconds)));
                //log printing
                System.out.println(" " + new Date().toString() + " Total " + job + " time : " + totalTime); 
    }
    
    /**
     * print all question IDs contained in a JSON file, in a JSONArray under field "questions"
     * 
     * @param filePathTestData  the file path for golden file
     */
    public static void printQuestionIDs(String filePathTestData){
         JSONObject a = Helper.readJsonFile(filePathTestData);
        JSONArray aa = Helper.getJSONArray("questions", a);
        for(Object o : aa){
            JSONObject jo = (JSONObject)o;
            System.out.println(Helper.getString("id", jo));
        }
    }

    /**
     *  Copies a file from one path to another
     * 
     * @param sourceFile    Source file to be copied
     * @param targetFolder  Folder to copy file there
     */
    public static void copyFileToFile(String sourceFile, String targetFile){
        try { 
                File source = new File(sourceFile);
                copyFile(source, new File(targetFile));
            } catch (IOException ex) {
                System.out.println(" " + new Date().toString() + " Exception copying " + sourceFile + " to folder " + targetFile );
                System.out.println(ex.getMessage() );
            }
    }
    
    /**
     *  Copies a file from one path to another
     * 
     * @param sourceFile    Source file to be copied
     * @param targetFolder  Folder to copy file there
     */
    public static void copyFileToFolder(String sourceFile, String targetFolder){
        try { 
                File source = new File(sourceFile);
                copyFile(source, new File(targetFolder + "\\" + source.getName()));
            } catch (IOException ex) {
                System.out.println(" " + new Date().toString() + " Exception copying " + sourceFile + " to folder " + targetFolder );
                System.out.println(ex.getMessage() );
            }
    }
    
    /**
     * Convert a JSONArray of articles (as JSONObjects) into a Corresponding HashMap (with PMID as key)
     * @param list      JSONArray of articles (as JSONObjects with PMID as key) 
    * @return           Corresponding HashMap with PMID as key
     */
    public static HashMap <String,JSONObject> articlesJSONArraytoHashMap(JSONArray list){
        HashMap <String,JSONObject> articles = new HashMap();
//        System.out.println(" " + new Date().toString() + " articlesJSONArraytoHashMap called with a list of size : " + list.size());

        for(Object o : list){
            JSONObject jo = (JSONObject)o;

            if(jo.containsKey("pmid")){
                String pmid = Helper.getAnyType("pmid", jo);
                articles.put(pmid, jo);
            } else {
                System.out.println(" " + new Date().toString() + " Error : object without pmid found in Array : " + jo);
            }
        }
//                System.out.println(" " + new Date().toString() + " articlesJSONArraytoHashMap finished with a map of size : " + articles.size());
        return articles;
    }
    
    /**
     * Load questions from Task B JSON files (test sets, submissions or training data)
     * 
     * @param fileToRead    String name of JSON file of training data for taskB
     * @param idString      String name of id field in JSON question objects (should have values "id" or "_id" accordingly.)
     * @return              HashMap <String, JSONObject> question id -> questions object
     */
    public static HashMap <String,JSONObject> loadQuestions(String fileToRead, String idString){
        JSONObject questionsObj = readJsonFile(fileToRead);
        JSONArray arr = getJSONArray("questions", questionsObj);
        printMessage("questions read : " + arr.size(), 0);
        HashMap <String,JSONObject> questions = new HashMap <> ();
        for(Object o : arr){
            JSONObject concept = (JSONObject)o;
            String id;
            if(idString.equals("id")){
                id = getString(idString, concept);
            } else { //idString.equals("_id")
                JSONObject oid =  getJSONObject(idString, concept);
                id = getString("$oid", oid);
            }
            if(id == null){
                printMessage(" NULL id: " + o.toString(), 0);
            }
            if(questions.containsKey(id)){
                printMessage(" DUPLICATE id: " + id, 0);
            }   
            questions.put(id, concept);
        }
        printMessage("questions returned : " + questions.size(), 0);
        return questions;
    }
    
    /**
     * Load Task B questions from JSONArray into a given HashMap
     * 
     * @param questionsArr
     * @param questionsMap
     * @param idString      String name of id field in JSON question objects (should have values "id" or "_id" accordingly.)
     */
    public static void loadQuestions(JSONArray questionsArr, HashMap <String,JSONObject> questionsMap, String idString){
        printMessage("questions given : " + questionsArr.size(), 0);
        for(Object o : questionsArr){
            JSONObject jo = (JSONObject)o;
            String id;
            if(idString.equals("id")){
                id = getString(idString, jo);
            } else { //idString.equals("_id")
                JSONObject oid =  getJSONObject(idString, jo);
                id = getString("$oid", oid);
            }
            if(id == null){
                printMessage(" NULL id: " + o.toString(), 0);
            }
            if(questionsMap.containsKey(id)){
                printMessage(" DUPLICATE id: " + id, 0);
            }   
            questionsMap.put(id, jo);
        }
        printMessage("questions loaded : " + questionsMap.size(), 0);
    }
    
    /**
     * Finds and prints duplicate Strings in a Test-Set of Task A
     * @param filePathTestData  path to Task A test-set
     * @return                  True if duplicate PMIDs found, false otherwise
     */
    public static boolean findDuplicatePMIDs( String filePathTestData) {
        ArrayList <String> pmids = Helper.getPmids(filePathTestData);
        System.out.println(" Articles count of pmid list " + pmids.size());
        boolean duplicatesFount =  false;
        Set<String> hs = new HashSet<>();
        hs.addAll(pmids);
        int duplicates = pmids.size() - hs.size();
        if(duplicates > 0){
            duplicatesFount = true;
            System.out.println(duplicates + " Duplicate pmids: ");
            for(String s : hs){
                pmids.remove(s);
                if(pmids.contains(s)){
                    System.out.println(s);
                }
            }
        } else {
            System.out.println(duplicates + " OK. No duplicate pmids containd in testset ");
        }
        return duplicatesFount;
    } 
    
    /**
     * Get current date in specified format for PubMed Queries
     * @return      a date in format of PubMed queries
     */
    public static String dateNow(){
        String date = "";
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy/MM/dd");
            Date now = new Date();
            date = format1.format(now);
        return date;
    }
    /**
     * Prints out put or error stream messages produced during the execution of a process
     * @param p the process executed
     */
    public static void runPythonScript(String pythonScript){
        try {
            //Run python script
            Process p = Runtime.getRuntime().exec("python \"" + pythonScript +"\"");
            // Get any output or err message
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String ret;                 
            String er; 
            while((ret = in.readLine()) != null){
                System.out.println("\t"+ret);
            }
            while((er = err.readLine()) != null){
                System.out.println("\t"+er);
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    /**
     * Get a set of String and convert it into a String list in the form recognized by Cypher queries
     * 
     * @param set e.g. An ArrayList <String>
     * @return          String e.g. "['C0205104','C0392905','C0019552','C0205250']"
     */
    public static String setToCypherList(Set <String> set){
        String list = "[";
        boolean first = true;
        for(String s : set){
            if(first){
                first = false;
            } else {
                list += ",";
            }
            list += "'" + s + "'";
        }
        list += "]";
        return list;
    }
    
        public static String setToCypherList(ArrayList <String> set){
        String list = "[";
        boolean first = true;
        for(String s : set){
            if(first){
                first = false;
            } else {
                list += ",";
            }
            list += "'" + s + "'";
        }
        list += "]";
        return list;
    }
    
    /**
     * Human readable print of a JSON or Hash etc
     * @param set 
     */
    public static void printPrettyJSON(Object set) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(set);
        System.out.println(json);
    }
}
