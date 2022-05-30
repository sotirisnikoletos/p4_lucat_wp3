/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

//Commented for Server compile 
package entrezHarvester;

//import com.mongodb.*;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
//Commented for Server compile 
import help.Helper;
import static java.lang.Integer.min;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import mongoConnect.MongoDatasetConnector;
//add lucene-core-5.3.1.jar
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
//add lucene-analyzers-common-5.3.1.jar
import org.apache.lucene.analysis.standard.StandardAnalyzer;
//add lucene-queryparser-5.3.1.jar
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.LRUQueryCache;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.search.UsageTrackingQueryCachingPolicy;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.json.simple.JSONArray;
//add json-simple-1.1.1.jar
import org.json.simple.JSONObject;
import scopusHarvester.CitedByObject;
import scopusHarvester.ScopusHarvester;

/**
 *
 * @author tasosnent
 */
public class dataWriter {

    //Hardcoded values
    private final static boolean debugMode = true; //Enables printing of messages for normal functions
    private boolean extraFields = false; //Enables including of extra fields in testset JSON (MeSH terms in each document)
    private boolean citedByFields = false; //Enables including of citedBy fields in testset JSON (MeSH terms in each document)
    private static final int hitsMax = 1000; // "searchNwrite window" searchNwrite for hitsMax top documents each time.
    private static final int maxNumberOfCachedQueries = 250;
    private static final long maxRamBytesUsed = 500 * 1024L * 1024L; // 100MB
    private static HashMap <String, String> supportedIndexFields = new <String, String> HashMap(); //Supported index Fields and their pubmed counterpart ("Title","ArticleTitle")
                    
    private String indexPath = null; 
    private IndexReader reader = null;
    private IndexSearcher searcher = null;
    private Analyzer analyzer = null;
    private QueryParser parser = null;
    private Sort sort = null;
    private static LRUQueryCache queryCache;
    private static QueryCachingPolicy defaultCachingPolicy;
    
    //wrtiting variables
    private BufferedWriter jsonWriter = null;
    private boolean firstWrite = true;
    private BufferedWriter meshWriter = null;
    private boolean firstMeshWrite = true;
    private JSONArray relations; //variable to store "pmid hasMesh meshID" relations (initialized for each search)
    private MongoDatasetConnector articleCollection; // A MongoDB collection to store the harvested article JSON objects
    private MongoDatasetConnector meshCollection; // A MongoDB collection to store the harvested relation JSON objects

    // variables to make wirting into both JSON and lucene optional
        // true by default, if no path provided, becomes false and corresponfing output is not written
    private boolean json = true; 
    private boolean lucene = true; 
    private boolean mongodb = true; 
   
    //indexing variables
    private IndexWriter indexWriter = null;
    
    // Scopus harvesting variables
    private ScopusHarvester scopusHarvester = null;
    // Temporary code for retrospective Citedby-field harvesting
//    private static ArrayList<String> checkedPMIDS =  new ArrayList<String>();

    /**
     * 
     * @param indexPath                 The path for the old index to read
     * @param aNewIndexPath             [if null, skip writing lucene index] the path for the new index to be written 
     * @param jsonFile                  [if null, skip writing JSON file] the path for the JSON file for articles to be written 
     * @param meshFile                  [if null, skip writing MESH in JSON file] the path for the JSON file for MESH relations to be written 
     * @param articleCollection         [if null, skip writing JSON in MongoDB] the name for the MongoDB collection for articles to be written 
     * @param meshCollection            [if null, skip writing MESH in MongoDB] the name for the MongoDB collection for MESH relations to be written   
     * @param extraFields               Denotes that this test sets are for testing purposes, so save extra information (MesH terms in each document)
     * @throws IOException 
     */
    public dataWriter(String indexPath, String aNewIndexPath, String jsonFile, String meshFile, MongoDatasetConnector articleCollection,MongoDatasetConnector meshCollection, Boolean extraFields, Boolean citedByFields) throws IOException{
        this.extraFields = extraFields;
        this.citedByFields = citedByFields;
        this.setIndexPath(indexPath); 
        //wrtiting variables
        if(jsonFile != null){
            jsonWriter = new BufferedWriter(new FileWriter(jsonFile));
            if(meshFile != null){
                this.meshWriter = new BufferedWriter(new FileWriter(meshFile));                
            } else {
                System.out.println(" " + new Date().toString() + " dataWriter > Error!!! File for MeSH relations not found. Relations will be ommited.");
            }
        } else {
            json = false;
            // Log printing
            if(debugMode) {
                System.out.println(" " + new Date().toString() + " dataWriter > write JSON file skipped");
            }
        } 
        //TO DO : add config.properties file
        //read configuration file and update static variables adequately
        // Supported index Fields and their pubmed counterpart ("Title","ArticleTitle")       
        supportedIndexFields.put("Title", "ArticleTitle");
        supportedIndexFields.put("TI", "ArticleTitle");
        supportedIndexFields.put("Abstract", "AbstractText");
        supportedIndexFields.put("AB", "AbstractText");
        supportedIndexFields.put("PMID", "PMID");
        supportedIndexFields.put("UID", "PMID");
        
        // Lucene objects
        
        /* Sorting */
        // Fields used for reverse chronological sorting of results
        // This Fields are indexed with no tokenization (for each element a StringField AND a SortedDocValuesField are added)
            // Using SortField.Type.STRING is valid (as an exception) beacause years are 4-digit numbers resulting in identical String-sorting and number-sorting.
        SortField sortFieldYear = new SortField("PubDate-Year", SortField.Type.STRING, true);

        this.setSort(new Sort(sortFieldYear));
        
        /* Reading the index */
        this.setReader(DirectoryReader.open(FSDirectory.open(Paths.get(getIndexPath()))));
        this.setSearcher(new IndexSearcher(getReader()));
        this.setAnalyzer(new StandardAnalyzer());
        
        /* writing the new sub-index */
        if(aNewIndexPath != null){
            Directory newdir = MMapDirectory.open(Paths.get(aNewIndexPath));
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            indexWriter = new IndexWriter(newdir, iwc);
        } else {
            lucene = false;
            // Log printing
            if(debugMode) {
                System.out.println(" " + new Date().toString() + " dataWriter > write Lucene index skipped");
            }
        }
        
        /* Caching */
        // these cache and policy instances can be shared across several queries and readers
        // it is fine to eg. store them into static variables
        queryCache = new LRUQueryCache(maxNumberOfCachedQueries, maxRamBytesUsed);
        defaultCachingPolicy = new UsageTrackingQueryCachingPolicy();
        this.getSearcher().setQueryCache(queryCache);
        this.getSearcher().setQueryCachingPolicy(defaultCachingPolicy);
        
        /*  PMID as default for searchNwrite */
        this.setParser( new QueryParser( "PMID", getAnalyzer()));    
        
        // MongoDB objects
        if(articleCollection != null){
            this.articleCollection = articleCollection;
            if(meshCollection == null){
                System.out.println(" " + new Date().toString() + " dataWriter > Error!!! mongoDB collection for MeSH relations not found. Relations will be ommited.");
            } else {
                this.meshCollection = meshCollection;
            }
        } else {
            mongodb = false;
            // Log printing
            if(debugMode) {
                System.out.println(" " + new Date().toString() + " dataWriter > write in mongoDB skipped");
            }
        }
        
        /* Harvesting Scopus Cited-by */
        this.setScopusHarvester(new ScopusHarvester());
    }
    /**
     * Just for testing functionalities during development stages or for retrospective-adhoc independent jobs.
     * @param args 
     */
    public static void main(String[] args) {
        
        //Get articles already checked for CitedBy
        MongoClient mongoClient = new MongoClient("143.233.226.90", 27017);
        MongoDatabase db = mongoClient.getDatabase("iasis_addScopus");
        ArrayList<String> collectionsCitedBy =  new ArrayList<String>();
        collectionsCitedBy.add("LC_scopus");
        for(String collectionName : collectionsCitedBy){
        MongoCollection collection = db.getCollection(collectionName);
            FindIterable<org.bson.Document> iterable = collection.find();
            iterable.forEach(new Block<org.bson.Document>() {
                @Override
                public void apply(final org.bson.Document document) {
                    org.bson.Document currentQuestion = new org.bson.Document(document);
    //                System.out.println(" document " +document);
// Temporary code for retrospective Citedby-field harvesting
//                    String pmid =  document.get("pmid").toString();  
//                    checkedPMIDS.add(pmid);
                }
            });
        }
//        System.out.println("\t Excluding " + checkedPMIDS.size() + " from Scopus harvesting.");
        
//        String ipath = "D:\\42 IASIS tmp files\\AD_20180417\\pubmed\\Lucene_index";
//        String ipath = "D:\\42 IASIS tmp files\\20171011_FullDataSets\\LC_20171011\\pubmed\\Lucene_index";
        String ipath = "D:\\42 IASIS tmp files\\20190620_KGupdate\\LC_20190620\\pubmed\\Lucene_index";
//        String ipath = "D:\\42 IASIS tmp files\\20171011_FullDataSets\\AD_20171011\\pubmed\\Lucene_index";
//        String ipath = "D:\\42 IASIS tmp files\\20190620_KGupdate\\AD_20190620\\pubmed\\Lucene_index";
//        String ipath = "D:\\42 IASIS tmp files\\AD_20200123\\pubmed\\Lucene_index";
//        MongoDatasetConnector mdcAD = new MongoDatasetConnector("143.233.226.90",27017,"iasis_unitTesting","AD_20171011");
//        MongoDatasetConnector mdcAD = new MongoDatasetConnector("localhost",27017,"iasis","LC_PubMed_20171011_b");
        MongoDatasetConnector mdcAD = new MongoDatasetConnector("143.233.226.90",27017,"iasis_addScopus","LC_scopus");
        try {
            dataWriter dw = new dataWriter(ipath, null,null,null,mdcAD,null,true,true);
            dw.searchNwrite("+AbstractText:[\\\"\\\" TO *] ");
        } catch (IOException ex) {
            Logger.getLogger(dataWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(dataWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Search in Index, in all Fields by default.
     *      Write results into JSON file and/or Lucene new index
     * @param queryString   query terms to searchNwrite
     * @throws Exception 
     */
    public void searchNwrite( String queryString) throws Exception { 
        // Initialize relations variable
        relations =  new JSONArray();
        // Log printing
        if(debugMode) {
            String writeMsg = "";
            if(json){
                writeMsg += "JSON file, ";
            }
            if(lucene){
                writeMsg += "lucene index, ";
            }
            if(mongodb){
                writeMsg += "mongo db, ";
            }
            System.out.println(" " + new Date().toString() + " dataWriter > writing in " + writeMsg );
        }
        
        // Initiate json file
        if(json){
            jsonWriter.append("{\"documents\":[\n");
            if(meshWriter != null ){
                meshWriter.append("{\"relations\":[\n");
            }
        }
        Query query = getParser().parse(queryString);
        //chache query
        query = query.rewrite(getReader());
            Query cacheQuery = queryCache.doCache(query.createWeight(getSearcher(), true), defaultCachingPolicy).getQuery();

        ConstantScoreQuery constantScoreQuery = new ConstantScoreQuery(cacheQuery);
        
        //Search and calculate the searchNwrite time in MS
        Date startDate = new Date();
        // Collect hitsMax top Docs sorting by reverse chronological ranking (only year taken into acount so far)
        TopDocs results = getSearcher().search(constantScoreQuery, getHitsMax(), getSort(), true, false);
        Date endDate = new Date();
        // Test code Log printing
        Long timeMS = endDate.getTime()-startDate.getTime();
                    
        ScoreDoc[] hits = results.scoreDocs;

        int numTotalHits = results.totalHits;
        int numOfSearches = 0;
        if(numTotalHits > getHitsMax()) {
            numOfSearches = numTotalHits/getHitsMax();
        }
        if(numTotalHits%getHitsMax() != 0) {
            numOfSearches++;
        }
        // Log printing
        if(debugMode) {
            System.out.println(" " + new Date().toString() + " Search Documents > [queryString: " + queryString + ", total matching documents: " + numTotalHits + " (" + numOfSearches + " searches ), time: " + timeMS + " MS]" + " > [cacheQuery.ramBytesUsed(): " + queryCache.ramBytesUsed()+"]");
        }
        
        writeHits(hits, json, lucene, mongodb);
        
        if ( numOfSearches  > 1 ) { // we have more searches to do

            //do the required searches, for the next hitsMax top results

            ScoreDoc maxDoc = hits[hits.length-1];
            for(int i = 1; i < numOfSearches; i++ ){   
                // Log printing
                if(debugMode){
                    System.out.println(" " + new Date().toString() + " Search Documents > Additional Search " + i );
                }

                startDate = new Date();
                results = getSearcher().searchAfter(maxDoc, constantScoreQuery, getHitsMax(), getSort(), true, false);
                endDate = new Date();

                timeMS = endDate.getTime()-startDate.getTime();
                hits = results.scoreDocs;

                maxDoc = hits[hits.length-1];

                writeHits(hits, json, lucene, mongodb);

            }
        }
        
        // finish json file writing
        if(json){
            jsonWriter.append("]");            
            jsonWriter.append("}");
            jsonWriter.flush();
            jsonWriter.close();
            meshWriter.append("]");            
            meshWriter.append("}");
            meshWriter.flush();
            meshWriter.close();

        }
        // finish lucene wirting
        if(lucene){
            indexWriter.close();
        }
     }
       
    /**
     * Write all hits into JSON file and/or Lucene index /mongoDB
     * @param hits          hits found in Lucene index
     * @param json          if true, write in json file, else skip
     * @param lucene        if true, write in lucene index, else skip
     * @param mongodb       if true, write in mongoDB collection, else skip
     * @throws IOException 
     */
    private void writeHits(ScoreDoc[] hits, boolean json, boolean lucene, boolean mongodb) throws IOException {     
        for (int i = 0 ; i < hits.length; i++) {
            Document doc = getSearcher().doc(hits[i].doc);
//            TO DO: Add this check so that doc with no abstracts are not included in the set!
            if (doc != null) {
                String documentAbstract = Helper.StringArrayToString(doc.getValues("AbstractText"));
                if(!documentAbstract.equals("")){
                    if(json){
                        if(firstWrite){
                            firstWrite = false;
                        } else {
                            jsonWriter.append(",\n");
                        }
                        if(this.firstMeshWrite){
                            firstMeshWrite = false;
                        } else {
                            meshWriter.append(",\n");
                        }
                    }
                    writeDocument(doc, json, lucene, mongodb);
                } else {
                    // Log printing
                    System.out.println(" " + new Date().toString() + " Search Documents > Warning! [document with empty Abstract, index : " + i + " ]");
                }
            } else {
                // Log printing
                System.out.println(" " + new Date().toString() + " Search Documents > Warning! [Empty document with index : " + i + " ]");
            }
        }    
    }
    
    /**
     * Write this doc to JSON file and/or Lucene index /mongoDB
     *      Also harvest additional "citedBy" information from Scopus
     * @param doc           The Lucene doc to write
     * @param json          if true, write in json file, else skip
     * @param lucene        if true, write in lucene index, else skip
     * @param mongodb       if true, write in mongoDB collection, else skip
     * @throws IOException 
     */
    private void writeDocument(Document doc, boolean json, boolean lucene, boolean mongodb ) throws IOException{

        //get fields values
        String documentAbstract = Helper.StringArrayToString(doc.getValues("AbstractText")); // (?)/(*) 1 Synonym: OtherAbstract(*)/AbstractText(+) - List
        String journal = Helper.StringArrayToString(doc.getValues("Title")); // (?) No synonyms
//        String journal = doc.get("Title"); // (?) No synonyms
        String pmid = Helper.StringArrayToString(doc.getValues("PMID")); // 1 with 2 synonyms (DeleteCitation/PMID, CommentsCorrections/PMID) but MedlineCitation/PMID is always the first encountered
// Temporary code for retrospective Citedby-field harvesting
// Add a check to avoid harvesting already harvested articles
//        if(!checkedPMIDS.contains(pmid)){
//            checkedPMIDS.add(pmid);
        
          // FOT article doi
            String doi = Helper.StringArrayToString(doc.getValues("ELocationID"));
 
           
           
        String title = Helper.StringArrayToString(doc.getValues("ArticleTitle")); // 1 No synonyms
//        String year = doc.get("PubDate-Year"); // ? with synonyms but MedlineDate 
        String year = Helper.StringArrayToString(doc.getValues("PubDate-Year")); // ? with synonyms but MedlineDate 
        String month = doc.get("PubDate-Month"); // ? with synonyms but MedlineDate 
        String day = doc.get("PubDate-Day"); // ? with synonyms but MedlineDate 
//        String year = doc.get("DateCreated-Year"); // ? with synonyms but MedlineDate 
//        String month = doc.get("DateCreated-Month"); // ? with synonyms but MedlineDate 
//        String day = doc.get("DateCreated-Day"); // ? with synonyms but MedlineDate 
        // get list of authors
        String[] foreNames = doc.getValues("AuthorList-ForeName");
        String[] lastNames = doc.getValues("AuthorList-LastName");
        String authors = "";
        JSONArray aithorList = new JSONArray();
        int numOfAuthors = min(foreNames.length,lastNames.length); // The size of the two list is always the same, but get the min just in case
        for(int i = 0; i < numOfAuthors; i++){
            if(i!=0){
                authors+=", ";
            }
            authors+=lastNames[i] + " " + foreNames[i];
            aithorList.add(lastNames[i] + " " + foreNames[i]);
        }

        if(year==null){
                year = " ";
            }
        if(month==null){
                month = " ";
            }
        if(day==null){
                day = " ";
            }
        //TO DO : MedlineDate when year not present (find year in free text)    

        // Harvest citedBy from Scopus 
        CitedByObject cbo = null;
        if(this.citedByFields){
            cbo = this.getScopusHarvester().getByPMID(pmid);
        }
        // If we want a JSON file to write in jsonWriter or datasetCollection
       if(json || mongodb){
            //add field values to JSON object
            JSONObject docJSON = new JSONObject();
            docJSON.put("abstractText", documentAbstract);
            docJSON.put("journal", journal);
    //        to write pmid as integer
            docJSON.put("pmid",Integer.parseInt(pmid));
    //        docJSON.put("pmid",pmid);
            docJSON.put("title", title);
            docJSON.put("year", year);
            docJSON.put("authors", authors);
            docJSON.put("authorList", aithorList);
            
            //FOT - doi addition
            docJSON.put("doi", doi);

            // Mesh Headings
            JSONArray meshList = Helper.StringArrayToJSONList(doc.getValues("DescriptorName_UI"));
            meshList.addAll(Helper.StringArrayToJSONList(doc.getValues("NameOfSubstance_UI")));                        
            if(this.extraFields){     
                docJSON.put("MeshUI", meshList);
            }
            // Additional fields to be used for confidence astimation
            // Mesh Publication Types
            JSONArray meshPublicationTypeList = Helper.StringArrayToJSONList(doc.getValues("PublicationTypeList-PublicationType_UI"));
            if(this.extraFields){     
                docJSON.put("MeshPTUI", meshPublicationTypeList);
            }
            JSONArray meshPublicationTypeNameList = Helper.StringArrayToJSONList(doc.getValues("PublicationTypeList-PublicationType"));
            if(this.extraFields){     
                docJSON.put("MeshPTnames", meshPublicationTypeNameList);
            }
 
  
            // Author affiliations
            String[] AffiliationList = doc.getValues("AuthorList-Affiliation");
            // collect uniwue values
            ArrayList<String> AffiliationListUnique = new ArrayList<String>();
            for(String i : AffiliationList){
                if(!AffiliationListUnique.contains(i)){
                    AffiliationListUnique.add(i);
                }                    
            }
            // add to JSONArray
            JSONArray Affiliations = new JSONArray();
            for(String i : AffiliationListUnique){
                Affiliations.add(i);
            }
//            JSONArray Affiliations = Helper.StringArrayToJSONList(AffiliationList);
            if(this.extraFields){     
                docJSON.put("Affiliations", Affiliations);
            }

            if(cbo != null){
                 docJSON.put("CitedByCount", cbo.getCount());
                 docJSON.put("CitedByLink", cbo.getLink());
            }
            // TODO: Add "relations" for MeSH Publication types
            for(Object meshID : meshList){
                JSONObject relJSON = new JSONObject();
                relJSON.put("s",pmid);
                relJSON.put("p","HAS_MESH");
                relJSON.put("o",(String)meshID);
                relations.add(relJSON);
                if(this.meshCollection != null){
                    meshCollection.add(relJSON.toJSONString());
                }
                if(this.meshWriter != null){
                    meshWriter.append(relJSON.toJSONString());
                }
            }
            // If we want to write a JSON file 
            if(json){
                jsonWriter.append(docJSON.toJSONString());
            }
            // If we want to write in mongo DB collection
            if(mongodb){
                this.articleCollection.add(docJSON.toJSONString());
            }
        }

        //If we want a lucene index, write into indexwriter
        if(lucene){
            //write also to lucene index               
            Document newDoc = new Document();
            Field abstractTextField = new TextField("abstractText", documentAbstract,  Field.Store.YES); 
            Field journalField = new TextField("journal", journal,  Field.Store.YES); 
            Field pmidField = new TextField("pmid", pmid,  Field.Store.YES); 
            Field titleField = new TextField("title", title,  Field.Store.YES); 
            Field yearField = new TextField("year", year,  Field.Store.YES); 
            Field authorsField = new TextField("authors", authors,  Field.Store.YES); 
            if(this.extraFields){
                String[] meshList = doc.getValues("DescriptorName_UI");
                for(String meshUI : meshList){
                    Field MESHField = new TextField("Mesh_UI", meshUI,  Field.Store.YES);
                    newDoc.add(MESHField);
                }
//                Field yearField = new TextField("year", title,  Field.Store.YES);
//                newDoc.add(yearField);
            }
            newDoc.add(abstractTextField);
            newDoc.add(journalField);
            newDoc.add(pmidField);
            newDoc.add(titleField);
            newDoc.add(yearField);
            newDoc.add(authorsField);
            indexWriter.addDocument(newDoc);
        }
// Temporary code for retrospective Citedby-field harvesting            
//        } else {
//            System.out.println("\t PMID " + pmid + " skipped.");
//        }
    } 
  
    /**
     * @return the indexPath
     */
    public String getIndexPath() {
        return indexPath;
    }

    /**
     * @param aIndexPath the indexPath to set
     */
    public void setIndexPath(String aIndexPath) {
        indexPath = aIndexPath;
    }

    /**
     * @return the hitsMax
     */
    public static int getHitsMax() {
        return hitsMax;
    }

    /**
     * @return the reader
     */
    public IndexReader getReader() {
        return reader;
    }

    /**
     * @param reader the reader to set
     */
    public void setReader(IndexReader reader) {
        this.reader = reader;
    }

    /**
     * @return the searcher
     */
    public IndexSearcher getSearcher() {
        return searcher;
    }

    /**
     * @param searcher the searcher to set
     */
    public void setSearcher(IndexSearcher searcher) {
        this.searcher = searcher;
    }

    /**
     * @return the analyzer
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * @param analyzer the analyzer to set
     */
    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * @return the parser
     */
    public QueryParser getParser() {
        return parser;
    }

    /**
     * @param parser the parser to set
     */
    public void setParser(QueryParser parser) {
        this.parser = parser;
    }

    /**
     * @return the maxRamBytesUsed
     */
    public static long getMaxRamBytesUsed() {
        return maxRamBytesUsed;
    }    

    /**
     * @return the sort
     */
    public Sort getSort() {
        return sort;
    }

    /**
     * @param sort the sort to set
     */
    public void setSort(Sort sort) {
        this.sort = sort;
    }

    /**
     * @return the scopusHarvester
     */
    public ScopusHarvester getScopusHarvester() {
        return scopusHarvester;
    }

    /**
     * @param scopusHarvester the scopusHarvester to set
     */
    public void setScopusHarvester(ScopusHarvester scopusHarvester) {
        this.scopusHarvester = scopusHarvester;
    }
}
