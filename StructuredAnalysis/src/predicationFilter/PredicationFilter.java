/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package predicationFilter;

import java.util.List;
// add javaMongoDriver/*.jar
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import help.Helper;
import static help.Helper.printMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import neo4JControler.Neo4JControler;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import rabbitmqConnector.RabbitMQConnector;
import yamlSettings.Settings;

/**
 *
 * @author tasosnent
 */
public class PredicationFilter {
    private static String pathDelimiter = "\\";    // The delimiter in this system (i.e. "\\" for Windows, "/" for Unix)

    /**
     * @return the pathDelimiter
     */
    public static String getPathDelimiter() {
        return pathDelimiter;
    }

    /**
     * @param aPathDelimiter the pathDelimiter to set
     */
    public static void setPathDelimiter(String aPathDelimiter) {
        pathDelimiter = aPathDelimiter;
    }

    // Neo4j DB controler
    Neo4JControler n4j_inputGrpah;
    Neo4JControler n4j_outputGrpah;
    // Step for querying pagination
    int step = 1000;
    // Negation flag : The value used in the "negation" list of each predication to indicate if negation found in e specific instance of the relation
    String negationFlag;
    // Text resource : The value used in the "resource" list of each predication to indicate that the resource of the specific instance is an article
    ArrayList <String> textSource;
    // Maps from Scimago Scientific Ranking of Journals CSV file: Rank	Sourceid	Title	Type	Issn	SJR	SJR Best Quartile	H index	Total Docs. (2017)	Total Docs. (3years)	Total Refs.	Total Cites (3years)	Citable Docs. (3years)	Cites / Doc. (2years)	Ref. / Doc.	Country	Publisher	Categories
    HashMap <String, Float> scimagoMapSJR;
    HashMap <String, Integer> scimagoMapHindex;
    // List of MeSH prefered concept CUIs, used for fine-grained semantic indexing
    ArrayList <String> MeSHPreferredConcepts;
    
    //  The mongo installation to use
    private MongoClient mongoClient;
    // The database (iASiS test DB)       
    private MongoDatabase db;
    //  The collection to be used
    private MongoCollection collection;
        
    /**
     * 
     * @param s 
     */
    public PredicationFilter(Settings s){
        
        // MongoDB
        
        // MongoDB settings for collection to read articles from
        String mongoHost = s.getProperty("mongodb/host").toString();
        int mongoPort = Integer.parseInt(s.getProperty("mongodb/port").toString());
        String mongoDB = s.getProperty("mongodb/dbname").toString();
        String mongoCollection = s.getProperty("mongodb/collection").toString();
        // Create MongoDB Client
//        this.mongoDB = new MongoDatasetConnector("143.233.226.92", 27017,"iasis","AD_20171011_pubmed");
        mongoClient = new MongoClient(mongoHost, mongoPort);
        db = mongoClient.getDatabase(mongoDB);
        collection = db.getCollection(mongoCollection);
        
        // Neo4j
        
        // Create n4j_inputGrpah Contolller (The default neo4j server running locally on the PC should be available as input)
        // Port for communication with java and web interface (bolt://localhost:7687) is different from the web interface port (http://127.0.0.1:7474)
        this.n4j_inputGrpah =  new Neo4JControler(s,"neo4jInputGrpah");
        // create n4j_outputGrpah Contolller (A container of neo4j server running on docker on the PC should be available as output with data folder mounted externally in the filesystem)
        this.n4j_outputGrpah =  new Neo4JControler(s,"neo4jOutputGrpah");
        
        
        // Negation flag : The value used in the "negation" list of each predication to indicate if negation found in e specific instance of the relation
        negationFlag = "negation";
        // Text resource : The value used in the "resource" list of each predication to indicate that the resource of the specific instance is an article
        textSource = new ArrayList <>();
        textSource.add("text");
        // Scimago Scientific Ranking of Journals CSV file
        scimagoMapSJR = new HashMap<String, Float> ();
        scimagoMapHindex = new HashMap<String, Integer> ();
        try {
            // Open the Scimago CSV file                   
            BufferedReader br = new BufferedReader(new FileReader(new File(s.getProperty("scimagojr").toString())));
            // Read the first line with headers to be ignored in further processing: Rank	Sourceid	Title	Type	Issn	SJR	SJR Best Quartile	H index	Total Docs. (2017)	Total Docs. (3years)	Total Refs.	Total Cites (3years)	Citable Docs. (3years)	Cites / Doc. (2years)	Ref. / Doc.	Country	Publisher	Categories
            String st = br.readLine(); 
            // Read each line until the journal has been found
            while ((st = br.readLine()) != null) {
                // Split by the delimiter ";" 
                String[] parts = st.split(";(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                // journal/conference Name
                String jName = parts[2];
                // remove quotes from journal names (e.g. "\"Plos One\"" for "Plos one")               
                jName = jName.replace("\"", "");
                // journal/conference SJR
                String jSJR = parts[5];
                // Replace "," by "." in decimal formatting
                jSJR = jSJR.replace(",", ".");
                // For missing values: ""
                if(jSJR.equals("")){
                    jSJR = "-1";
                }
                // journal/conference h index
                String jHindex = parts[7];
//                System.out.println(jName +" "+jSJR +" "+jHindex);
                scimagoMapSJR.put(jName.toLowerCase(), Float.parseFloat(jSJR));
                scimagoMapHindex.put(jName.toLowerCase(), Integer.parseInt(jHindex));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PredicationFilter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PredicationFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
//        System.out.println(scimagoMapSJR);

        // List of MeSH prefered concept CUIs, used for fine-grained semantic indexing
        JSONObject MeSHpc = Helper.readJsonFile(s.getProperty("preferredConcepts").toString());
        JSONArray MeSHpcArr = Helper.getJSONArray("PreferredConcepts", MeSHpc);
        MeSHPreferredConcepts = new ArrayList <String> ();
        for(Object pc : MeSHpcArr){
            MeSHPreferredConcepts.add(pc.toString());
        }
//        System.out.println(MeSHPreferredConcepts);
    }
    private static Settings s;
    
    /**
     * Filter knowledge available in the Knowledge Graph
     * @param args 
     */
    public static void main(String[] args) {
        // For Unix excecution set hardcoded pathdelimiter to /
        setPathDelimiter("/");
        //Load settings from file
        String settingsFile = "." + getPathDelimiter() + "settings.yaml";
        if(args.length == 1){ // command line call
            // TO DO add checks for these values
            System.err.println(" " + new Date().toString() + " \t Creating data-set using settings file : " + args[0]);
            settingsFile = args[0];
        } 
        s = new Settings(settingsFile);       
//        proceed();
        RabbitMQConnector r  = new RabbitMQConnector(s);
        try {
            r.receiveMessages(s);
        } catch (IOException ex) {
            Logger.getLogger(PredicationFilter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(PredicationFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void proceed() {
        // Create filter : Connect to mongoDB and neo4j  
         PredicationFilter pf = new PredicationFilter(s);
         //connect to neo4j   
            pf.n4j_inputGrpah.connect();
            pf.n4j_outputGrpah.connect();            

        // 0.0 Remove Articles and Entities in output Neo4j to have a "clear" graph
        pf.deleteOutputPublication();
        pf.deleteOutputAnnotation();            
            
        // 1.0 Export all articles from the given dataset in MongoDB to neo4j graph
        Date start = new Date();
        ArrayList <String> exportedPMIDs = pf.exportArticles();
        System.out.println("Exporting " + exportedPMIDs.size() + " articles complete.");
        Date end = new Date();
        Helper.printTime(start, end," exporting " + exportedPMIDs.size() + " articles"); 

        // 2.0 Export all topic and occurrence predications (from input neo4j)  
//      Update topic and occurrence predications (from input neo4j) for articles in the list of harvested articles.
        pf.updateExportedArticles(exportedPMIDs);     
//      Update topic and occurrence predications (from input neo4j) based on the articles present in the output neo4j database (i.e. already exported).
//         pf.updateExportedArticles();     
        // Export All Topic relations present in input neo4j
//          pf.exportTopicPredications();     
        // Export All Occurrence relations present in input neo4j
//         pf.exportOccurrencePredications();

        // 2.1 Update the confidence for all Topic relations present in output neo4j
//      Update topic confidence for articles in the list of harvested articles. 
        pf.updateExportedTopicAnnotations(exportedPMIDs);
//      Update topic confidence on All the articles available in the output neo4j. 
//        For small graphs (e.g. only for updates)
//        pf.updateExportedTopicAnnotations();
//        For big graphs (e.g. the whole set of available articles in AD)
//        pf.updateExportedTopicAnnotationsPaged();

        // 4.0 Export extracted (concept-to-concept) relations
        // Make separate queries per type of information
        // Semantic types for filtering queries : selected based on Ahlers et al, after an update. For details see IASIS\AnalysisModule\AhlersPredicationTypesStudy.xlsx
            // More details on perdiation meaninig available here: https://semrep.nlm.nih.gov/SemRepSemanticType_desc.txt
        
        // Substance Semantic Types
        String substanceSemTypes = "'bdsu','chem','chvs','orch','nnon','aapp','chvf','phsu','bodm','bacs','horm','enzy','vita','imft','irda','hops','sbst','food','rcpt','antb','elii','inch','gngm','nusq'"; // Merged Union for better recall
            //Used for 1st version of WP5 sample Genetic Etiology and Substance Relation
            //String substanceSemTypes = "'aapp','antb','bacs','chem','elii','enzy','hops','horm','imft','inch','orch','phsu','rcpt','vita','gngm','nusq'";
        // Pathology Semantic Types
        String pathologySemTypes = "'patf','dsyn','mobd','comd','emod','neop','acab','anab','cgab','inpo','sosy'"; // Merged Union for better recall
            //Used for 1st version of WP5 sample Genetic Etiology and Substance Relation
            //String pathologySemTypes = "'comd','dsyn','mobd','patf','acab','anab','cgab','inpo','sosy'";
        // Anatomy Semantic Types
        String anatomySemTypes = "'anst','bpoc','cell','celc','emst','ffas','gngm','tisu'";
        // Process Semantic Types
        String processSemTypes = "'acab','anab','comd','cgab','dsyn','fndg','inpo','patf','sosy','lbtr','celf','orgf','phsf'";
        // Living Being Semantic Types
        String liningBeingSemTypes = "'anim','arch','bact','euka','fngs','humn','mamm','orgm','vtbt','virs'";

        // Genetic Etiology Perdications
        // MATCH p=(:Entity)-[r:ASSOCIATED_WITH|:PREDISPOSES|:CAUSES]->(:Entity) RETURN count(distinct r) LIMIT 25
        String GeneticEtiologyPredicates = ":ASSOCIATED_WITH|:PREDISPOSES|:CAUSES";
        pf.exportSelectedPredications("Genetic Etiology", substanceSemTypes, GeneticEtiologyPredicates, pathologySemTypes);
        // Substance Relation Perdications
        // MATCH p=(:Entity)-[r:INTERACTS_WITH|:INHIBITS|:STIMULATES]->(:Entity) RETURN count(distinct r) LIMIT 25
        String substanceRelationPredicates = ":INTERACTS_WITH|:INHIBITS|:STIMULATES";
        pf.exportSelectedPredications("Substance Relation", substanceSemTypes, substanceRelationPredicates, substanceSemTypes);            
        // Pharmacological Effects
        // MATCH p=(:Entity)-[r:AFFECTS|:DISRUPTS|:AUGMENTS]->(:Entity) RETURN count(distinct r) LIMIT 25
        String pharmacologicalEffectsPredicates = ":AFFECTS|:DISRUPTS|:AUGMENTS";
        pf.exportSelectedPredications("Pharmacological Effects", substanceSemTypes, pharmacologicalEffectsPredicates, anatomySemTypes + "," + processSemTypes);
        // Clinical Actions
        pf.exportSelectedPredications("Clinical Actions (Substance administration)", substanceSemTypes, ":ADMINISTERED_TO", liningBeingSemTypes);
        pf.exportSelectedPredications("Clinical Actions (Process manifestation)", processSemTypes, ":MANIFESTATION_OF", processSemTypes);
        pf.exportSelectedPredications("Clinical Actions (Treatment)", substanceSemTypes, ":TREATS", liningBeingSemTypes + "," + pathologySemTypes);
        // Organism Characteristics
        pf.exportSelectedPredications("Organism Characteristics (Substance location)", anatomySemTypes  + "," + liningBeingSemTypes, ":LOCATION_OF", substanceSemTypes);
        pf.exportSelectedPredications("Organism Characteristics (Organism composition)", anatomySemTypes, ":PART_OF", anatomySemTypes  + "," + liningBeingSemTypes);
            pf.exportSelectedPredications("Organism Characteristics (Organism processes)", processSemTypes, ":PROCESS_OF", liningBeingSemTypes);            
        // Co-existence
        pf.exportSelectedPredications("Co-existence (of substances)", substanceSemTypes, ":COEXISTS_WITH", substanceSemTypes);            
        pf.exportSelectedPredications("Co-existence (of processes)", processSemTypes, ":COEXISTS_WITH", processSemTypes);            

        // 6.0 Add alternative IDs to 
//          Not supproted for the online procedure. It was done adhoc for some samples        
//            pf.exportAlternativeIDs();
                
        //Disconnect from neo4j         
        pf.n4j_inputGrpah.disconect();
        pf.n4j_outputGrpah.disconect();
        pf.mongoClient.close();
    }
        
    /**
     * Export all articles from a MongoDB collection to neo4j   
     */
    public ArrayList <String> exportArticles(){
        // Get all articles in given dataset (Mongo Collection)
        ArrayList <String> pmids = new ArrayList <String> (); // A list with all axported article PMIDs to export topics and occurrences later.
        FindIterable<Document> iterable = collection.find();
//        ScopusHarvester sh = new ScopusHarvester();
        
        iterable.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                Document currentQuestion = new Document(document);
//                System.out.println(" document " +document);
                String pmid =  Neo4JControler.escapeNeo4j(document.get("pmid").toString());
                pmids.add(pmid);
                String title =  Neo4JControler.escapeNeo4j(document.get("title").toString());
                String year =  Neo4JControler.escapeNeo4j(document.get("year").toString());
                String journal =  document.get("journal").toString();
                String authors =  Neo4JControler.escapeNeo4j(document.get("authors").toString());
                ArrayList <String> authorList = (ArrayList <String>)document.get("authorList");
                ArrayList <String> ptList = (ArrayList <String>)document.get("MeshPTnames");
                ArrayList <String> AffiliationList = (ArrayList <String>)document.get("Affiliations");
                int citedByCount = (Integer)document.get("CitedByCount");
                String citedByLink = document.get("CitedByLink").toString();
                // Convert resource list in appropriate format to be imported in the Chypher query
                String authorListCypher = arrayListToCypherList(authorList);
                String ptListCypher = arrayListToCypherList(ptList);
                String AffiliationListCypher = arrayListToCypherList(AffiliationList);

                // Find Scimago Scientific Jurnal Ranking and h-index
                float SJR = -1;
                int h_index = -1;
                if(scimagoMapSJR.containsKey(journal.toLowerCase())){
                    SJR = scimagoMapSJR.get(journal.toLowerCase());
                    h_index = scimagoMapHindex.get(journal.toLowerCase());
//                    System.out.println("found!!! " + journal + " for " + pmid + " " + SJR);
                }
                
                // Add citedBy fields
//                CitedByObject citedBy = sh.getByPMID(pmid);
//                CitedByObject citedBy = new CitedByObject(0,"https://www.scopus.com/search/form.uri?display=basic&zone=header&origin=searchbasic");
                
//                
                journal = Neo4JControler.escapeNeo4j(journal);
                // Create an article node in neo4j for each article in Mongo Collection                    
                String query = "MERGE (article:Publication { pmid:'"+pmid+"'}) " // MERGE is used instead of create, so that in case of existence no duplicate articles will be created
                        + "ON CREATE SET "
                        + "article.title = '"+title+"', "
                        + "article.year = '"+year+"', "
                        + "article.journal ='"+journal+"',"
                        + "article.journal_SJR ="+SJR+","
                        + "article.journal_hIndex ="+h_index+","
                        + "article.authors = '"+authors+"',"
                        + "article.authorList = "+authorListCypher+","
                        + "article.Affiliations = "+AffiliationListCypher+","
                        + "article.citedByCount = "+citedByCount+","
                        + "article.citedByLink = '"+citedByLink+"',"
                        + "article.PublicationTypes = "+ptListCypher+"";  
                n4j_outputGrpah.query(query);
                
                // The following steps are skipped because they delay the interaction with mongo DB and cause curson timeout was caused to MongoDB
//                    Export of topics and Occurrences will be done later, indepentently

                // Export Topics for this article from neo4j
                //tmp code
//                printMessage(" " + new Date().toString() + " add topics for : " + pmid,2);
//                exportTopicPredications(pmid);
                //tmp code
//                printMessage(" " + new Date().toString() + " add occurrences : " + pmid,2);
                // Export Occurrences for this article from neo4j
//                exportOccurrencePredications(pmid);
            }
        });
        
        return pmids;
    } 

    /**
     * Export all predications complying to the conditions given from neo4j to neo4j
     *      The conditions given are the semantic types for subject and object and the types of predicates. All relations (and relation instances) not complying with ALL given conditions are ignored as invalid.
     *      All valid relations are exported into a neo4j graph.
     * 
     * @param aname             A name for the "type" of predications to be filtered and exported as a String (e.g. "Genetic Etiology")
     * @param subjSemTypes      The valid/allowed/wanted semantic types for the subject as a String adequate for Cypher queries (e.g. "'bdsu','chem','chvs','orch','nnon','aapp','chvf','phsu','bodm','bacs','horm','enzy','vita','imft','irda','hops','sbst','food','rcpt','antb','elii','inch'")
     * @param predicates        The valid/allowed/wanted predications types as a String adequate for Cypher queries (e.g. "ASSOCIATED_WITH|:PREDISPOSES|:CAUSES")
     * @param objSemTypes       The valid/allowed/wanted semantic types for the object as a String adequate for Cypher queries (e.g. "'patf','dsyn','mobd','comd','emod','neop'")
     */
    public void exportSelectedPredications(String aname, String subjSemTypes, String predicates ,String objSemTypes ) {
        boolean stop = false;
        int pageSize = 50;
        int currentPage = 0;
        while(!stop){
            //tmp code
//            printMessage(" " + new Date().toString() + " Search " + aname + " predications, page : " + currentPage ,2);
//              Infered predications and Specifications will be handled later
//            String query = "MATCH p=(subject:Entity)-[r:ASSOCIATED_WITH|:ASSOCIATED_WITH__INFER__|:ASSOCIATED_WITH__SPEC__|:PREDISPOSES|:PREDISPOSES__SPEC__|:CAUSES|:CAUSES__SPEC__]->(object:Entity)"
            String query = "MATCH p=(subject:Entity)-[r" + predicates + "]->(object:Entity)"
                    + " WHERE ANY ( rst IN r.subject_sem_type WHERE rst IN [" + subjSemTypes + "])"
                    + " AND ANY ( rot IN r.object_sem_type WHERE rot IN [" + objSemTypes + "])"
                    + " RETURN subject.id, subject.label, subject.sem_types, object.id, object.label, object.sem_types, type(r), r.object_score, r.subject_score, r.resource, r.sent_id, r.negation, r.subject_sem_type, r.object_sem_type " 
                    + " SKIP "+(currentPage*pageSize)+" LIMIT " + pageSize;
            StatementResult result = this.n4j_inputGrpah.bigQuery(query);                        
            //tmp code
//            printMessage(" " + new Date().toString() + " results taken " + query,2);
            // If the query retrieved some results
            if(result.hasNext()){
            //tmp code
//                printMessage(" " + new Date().toString() + " results exist still"  ,2);
                //Export them
                while ( result.hasNext() ){
            //tmp code
//                    printMessage(" " + new Date().toString() + " results exist still"  ,2);
                    Record record = result.next();
                    // Predication data
                    String predicate =  Neo4JControler.escapeNeo4j(record.get( "type(r)" ).asString());
                    // Handle predicate types (remove __inf__, __spec__)
//                    TODO : HAndle normalization of predicates 
//                    String predicateNormalized =  normalizePredicates(predicate);                    
                    String predicateNormalized =  predicate;                    
                    List resources = new ArrayList(record.get( "r.resource" ).asList());                    
                    List sentences = new ArrayList(record.get( "r.sent_id" ).asList());                                      
                    List negations = new ArrayList(record.get( "r.negation" ).asList());                                      
                    // Subject concept
                    List sentence_SubjSTs = new ArrayList(record.get( "r.subject_sem_type" ).asList());                                      
                    List subjScores =  new ArrayList(record.get( "r.subject_score" ).asList());
                    String subjCui =  Neo4JControler.escapeNeo4j(record.get( "subject.id" ).asString());
                    String subjLabel =  Neo4JControler.escapeNeo4j(record.get( "subject.label" ).asString());
                    List subjSem_types = new ArrayList(record.get( "subject.sem_types" ).asList());
                    // Object concept
                    List sentence_objSTs = new ArrayList(record.get( "r.object_sem_type" ).asList());                                      
                    List objScores =  new ArrayList(record.get( "r.object_score" ).asList());
                    String objCui =  Neo4JControler.escapeNeo4j(record.get( "object.id" ).asString());
                    String objLabel =  Neo4JControler.escapeNeo4j(record.get( "object.label" ).asString());
                    List objSem_types = new ArrayList(record.get( "object.sem_types" ).asList());
                    
                    // Find "unwanted semantic type" instances of the relation 
                        // The query above retrieves a relation if ANY of the instances complies with the semantic type requirements. 
                        // The "additional" instances, not having the right semantic type, should not be taken into account. Hence, they are removed before further processing
                    ArrayList<Integer> invalidInstanceIndexes = findInvalidInstancesBySemanticType(subjSemTypes, objSemTypes, sentence_SubjSTs, sentence_objSTs);
//                    System.out.print(" invalid : " + invalidInstanceIndexes);
//                    System.out.println(" for "+subjCui+" "+predicate+" "+objCui );
                    // If invalid instances found...
                    if(!invalidInstanceIndexes.isEmpty()){
                        // Remove "unwanted semantic type" instances from sentences, subjScores, objScores, sentences, sentence_SubjSTs, sentence_objSTs and negations
                        removeInstances(invalidInstanceIndexes, sentences, subjScores, objScores, sentence_SubjSTs, sentence_objSTs, negations);
                    }                   
                    /** 
                     *  TO DO
                     *  Allow empty sentences when other resources exist
                     */
                    // If any valid instances remain in the lists
                    if(!sentences.isEmpty()){                    
                        // Handle Confidence : Handle individual scores to create one score per predication
                        double confidence = findPredicationScore(subjScores,objScores,resources,predicate);

                        // Handle Negation : Handle negation existence in some relation instances to create a negation score per predication
                        double negation = findNegationScore(negations);

                        // Handle Sentences and resources : Merge list of sentences and List of resources into a list of articles and other resources in appropriate format to be imported in the Chypher query                        
                            // Remove all "text" elements from resources, only strutured resources sould remain, if any. e.g. ["text", "text", "DO"] -> ["DO"] 
                        resources.removeAll(textSource);
                            // Get articles from Sentences
                        ArrayList <String> articles = getArticlesFromSenctences(sentences);     
                            // Merge articles and the list of remaining resources
                        articles.addAll(resources);
                            // Convert resource list in appropriate format to be imported in the Chypher query
                        String resourceList = arrayListToCypherList(articles);

                        //tmp code
    //                    printMessage(" " + new Date().toString() + " Add concept nodes : " + subjCui +", "+objCui  ,2);
                        // Add subject node
                        query  = "MERGE (concept:Annotation {id:'"+subjCui+"'}) " // MERGE is used instead of create, so that in case of existence no duplicate concepts will be created
                                + "ON CREATE SET "
                                + "concept.label = '"+subjLabel+"', "
                                + "concept.semantic_types = '"+subjSem_types+"'"; 
                        this.n4j_outputGrpah.query(query);
                        // Add object node
                        query  = "MERGE (concept:Annotation {id:'"+objCui+"'}) " // MERGE is used instead of create, so that in case of existence no duplicate concepts will be created
                                + "ON CREATE SET "
                                + "concept.label = '"+objLabel+"', "
                                + "concept.semantic_types = '"+objSem_types+"'"; 
                        this.n4j_outputGrpah.query(query);
                        //tmp code
    //                    printMessage(" " + new Date().toString() + " Add " + aname + " relation : " + predicateNormalized,2);
                        // Add the relation
                        query = "MATCH (subject:Annotation {id:'"+subjCui+"'}), (object:Annotation {id:'"+objCui+"'}) " +
                                "MERGE (subject)-[r:"+predicateNormalized+"]->(object) " +
                                "ON MATCH SET r.confidence = "+confidence+", "
                                    + "r.negation = "+negation+", "
                                    + " r.resources = "+resourceList+ " " +
    //                                Why "on match" resource should be "updated instead of replaced as done with confidence?" - probably for normalized predications
    //                                + " r.resources =  r.resources + "+resourceList+ " " +
                                "ON CREATE SET r.confidence = "+confidence+", "
                                    + "r.negation = "+negation+", "
                                    + " r.resources = "+resourceList;
                        this.n4j_outputGrpah.query(query);                    
                    } else { // This relation is ignored because it hasn't any valid instances
                        printMessage(" " + new Date().toString() + aname + " relation ignored (no valid instances) : " + subjCui + " " + predicateNormalized + " " + objCui,2);                        
                    }
                }      
                // Increase current page to continue with the next one
                currentPage ++;
                printMessage(" " + new Date().toString() + " query for next " + pageSize + " predications" ,1);            
            } else {// Else, no more topics found for any article
                stop = true;
                printMessage(" " + new Date().toString() + " Export of " + aname + " precications complete! ",1);
            }
        } // end of results
    }       
    
    /**
     * Export all Genetic Etiology predications from neo4j KB 
     *      Execute the appropriate cypher query to retrieve the predications
     *      Estimate the confidence for each predication
     *      Execute the appropriate cypher query to import them 
     */
    public void exportGeneticEtiologyPredications() {
        String aname = "Genetic Etiology";
        String subjSemTypes = "'bdsu','chem','chvs','orch','nnon','aapp','chvf','phsu','bodm','bacs','horm','enzy','vita','imft','irda','hops','sbst','food','rcpt','antb','elii','inch'";
        String objSemTypes = "'patf','dsyn','mobd','comd','emod','neop'";
        String predicates = "ASSOCIATED_WITH|:PREDISPOSES|:CAUSES";
        boolean stop = false;
        int pageSize = 50;
        int currentPage = 0;
        while(!stop){
            //tmp code
//            printMessage(" " + new Date().toString() + " Search " + aname + " predications, page : " + currentPage ,2);
//              Infered predications and Specifications will be handled later
//            String query = "MATCH p=(subject:Entity)-[r:ASSOCIATED_WITH|:ASSOCIATED_WITH__INFER__|:ASSOCIATED_WITH__SPEC__|:PREDISPOSES|:PREDISPOSES__SPEC__|:CAUSES|:CAUSES__SPEC__]->(object:Entity)"
            String query = "MATCH p=(subject:Entity)-[r:" + predicates + "]->(object:Entity)"
                    + " WHERE ANY ( rst IN r.subject_sem_type WHERE rst IN [" + subjSemTypes + "])"
                    + " AND ANY ( rot IN r.object_sem_type WHERE rot IN [" + objSemTypes + "])"
                    + " RETURN subject.id, subject.label, subject.sem_types, object.id, object.label, object.sem_types, type(r), r.object_score, r.subject_score, r.resource, r.sent_id, r.negation, r.subject_sem_type, r.object_sem_type " 
                    + " SKIP "+(currentPage*pageSize)+" LIMIT " + pageSize;
            StatementResult result = this.n4j_inputGrpah.bigQuery(query);                        
            //tmp code
//            printMessage(" " + new Date().toString() + " results taken " + query,2);
            // If the query retrieved some results
            if(result.hasNext()){
            //tmp code
//                printMessage(" " + new Date().toString() + " results exist still"  ,2);
                //Export them
                while ( result.hasNext() ){
            //tmp code
//                    printMessage(" " + new Date().toString() + " results exist still"  ,2);
                    Record record = result.next();
                    // Predication data
                    String predicate =  Neo4JControler.escapeNeo4j(record.get( "type(r)" ).asString());
                    // Handle predicate types (remove __inf__, __spec__)
//                    TODO : HAndle normalization of predicates 
//                    String predicateNormalized =  normalizePredicates(predicate);                    
                    String predicateNormalized =  predicate;                    
                    List resources = new ArrayList(record.get( "r.resource" ).asList());                    
                    List sentences = new ArrayList(record.get( "r.sent_id" ).asList());                                      
                    List negations = new ArrayList(record.get( "r.negation" ).asList());                                      
                    // Subject concept
                    List sentence_SubjSTs = new ArrayList(record.get( "r.subject_sem_type" ).asList());                                      
                    List subjScores =  new ArrayList(record.get( "r.subject_score" ).asList());
                    String subjCui =  Neo4JControler.escapeNeo4j(record.get( "subject.id" ).asString());
                    String subjLabel =  Neo4JControler.escapeNeo4j(record.get( "subject.label" ).asString());
                    List subjSem_types = new ArrayList(record.get( "subject.sem_types" ).asList());
                    // Object concept
                    List sentence_objSTs = new ArrayList(record.get( "r.object_sem_type" ).asList());                                      
                    List objScores =  new ArrayList(record.get( "r.object_score" ).asList());
                    String objCui =  Neo4JControler.escapeNeo4j(record.get( "object.id" ).asString());
                    String objLabel =  Neo4JControler.escapeNeo4j(record.get( "object.label" ).asString());
                    List objSem_types = new ArrayList(record.get( "object.sem_types" ).asList());
                    
                    // Find "unwanted semantic type" instances of the relation 
                        // The query above retrieves a relation if ANY of the instances complies with the semantic type requirements. 
                        // The "additional" instances, not having the right semantic type, should not be taken into account. Hence, they are removed before further processing
                    ArrayList<Integer> invalidInstanceIndexes = findInvalidInstancesBySemanticType(subjSemTypes, objSemTypes, sentence_SubjSTs, sentence_objSTs);
//                    System.out.print(" invalid : " + invalidInstanceIndexes);
//                    System.out.println(" for "+subjCui+" "+predicate+" "+objCui );
                    // If invalid instances found...
                    if(!invalidInstanceIndexes.isEmpty()){
                        // Remove "unwanted semantic type" instances from sentences, subjScores, objScores, sentences, sentence_SubjSTs, sentence_objSTs and negations
                        removeInstances(invalidInstanceIndexes, sentences, subjScores, objScores, sentence_SubjSTs, sentence_objSTs, negations);
                    }                   
                    
                    // If any valid instances remain in the lists
                    if(!sentences.isEmpty()){                    
                        // Handle Confidence : Handle individual scores to create one score per predication
                        double confidence = findPredicationScore(subjScores,objScores,resources,predicate);

                        // Handle Negation : Handle negation existence in some relation instances to create a negation score per predication
                        double negation = findNegationScore(negations);

                        // Handle Sentences and resources : Merge list of sentences and List of resources into a list of articles and other resources in appropriate format to be imported in the Chypher query                        
                            // Remove all "text" elements from resources, only strutured resources sould remain, if any. e.g. ["text", "text", "DO"] -> ["DO"] 
                        resources.removeAll(textSource);
                            // Get articles from Sentences
                        ArrayList <String> articles = getArticlesFromSenctences(sentences);     
                            // Merge articles and the list of remaining resources
                        articles.addAll(resources);
                            // Convert resource list in appropriate format to be imported in the Chypher query
                        String resourceList = arrayListToCypherList(articles);

                        //tmp code
    //                    printMessage(" " + new Date().toString() + " Add concept nodes : " + subjCui +", "+objCui  ,2);
                        // Add subject node
                        query  = "MERGE (concept:Annotation {id:'"+subjCui+"'}) " // MERGE is used instead of create, so that in case of existence no duplicate concepts will be created
                                + "ON CREATE SET "
                                + "concept.label = '"+subjLabel+"', "
                                + "concept.semantic_types = '"+subjSem_types+"'"; 
                        this.n4j_outputGrpah.query(query);
                        // Add object node
                        query  = "MERGE (concept:Annotation {id:'"+objCui+"'}) " // MERGE is used instead of create, so that in case of existence no duplicate concepts will be created
                                + "ON CREATE SET "
                                + "concept.label = '"+objLabel+"', "
                                + "concept.semantic_types = '"+objSem_types+"'"; 
                        this.n4j_outputGrpah.query(query);
                        //tmp code
    //                    printMessage(" " + new Date().toString() + " Add " + aname + " relation : " + predicateNormalized,2);
                        // Add the relation
                        query = "MATCH (subject:Annotation {id:'"+subjCui+"'}), (object:Annotation {id:'"+objCui+"'}) " +
                                "MERGE (subject)-[r:"+predicateNormalized+"]->(object) " +
                                "ON MATCH SET r.confidence = "+confidence+", "
                                    + "r.negation = "+negation+", "
                                    + " r.resources = "+resourceList+ " " +
    //                                Why "on match" resource should be "updated instead of replaced as done with confidence?" - probably for normalized predications
    //                                + " r.resources =  r.resources + "+resourceList+ " " +
                                "ON CREATE SET r.confidence = "+confidence+", "
                                    + "r.negation = "+negation+", "
                                    + " r.resources = "+resourceList;
                        this.n4j_outputGrpah.query(query);                    
                    } else { // This relation is ignored because it hasn't any valid instances
                        printMessage(" " + new Date().toString() + aname + " relation ignored (no valid instances) : " + subjCui + " " + predicateNormalized + " " + objCui,2);                        
                    }
                }      
                // Increase current page to continue with the next one
                currentPage ++;
                printMessage(" " + new Date().toString() + " query for next " + pageSize + " predications" ,1);            
            } else {// Else, no more topics found for any article
                stop = true;
                printMessage(" " + new Date().toString() + " Export of " + aname + " precications complete! ",1);
            }
        } // end of results
    }       

    /**
     * Export all topic/occurrence predications from input neo4j KB for articles already exported in output neo4j 
     *      Execute the appropriate cypher query to retrieve exported articles
     *      Then, call appropriate functions to retrieve and export topic/occurrence predications for each article
     */
    public void updateExportedArticles(ArrayList <String> pmids) {
        printMessage(" " + new Date().toString() + " Exporting topic and occurrence precications for "+ pmids.size()+ " exported articles",1);            
        for(String pmid : pmids){
            printMessage(" " + new Date().toString() + " Export topics and occurrences for : " + pmid,2);
            exportTopicPredications(pmid);
            exportOccurrencePredications(pmid);
        }
        printMessage(" " + new Date().toString() + " Export of topic and occurrence precications for exported articles complete! ",1);            
    }     
    
    /**
     * Export all topic/occurrence predications from input neo4j KB for articles already exported in output neo4j 
     *      Execute the appropriate cypher query to retrieve exported articles
     *      Then, call appropriate functions to export retrieve and export topic/occurrence predications for each article
     */
    public void updateExportedArticles() {
        boolean stop = false;
        int pageSize = 100;
        int currentPage = 0;
        while(!stop){
            String query = "MATCH (article:Publication) RETURN article.pmid as pmid SKIP "+(currentPage*pageSize)+" LIMIT " + pageSize;
            StatementResult result = this.n4j_outputGrpah.bigQuery(query);                        
            // If the query retrieved some results
            if(result.hasNext()){
                //Export them
                while ( result.hasNext() )
                {
                    Record record = result.next();
                    String pmid =  Neo4JControler.escapeNeo4j(record.get( "pmid" ).asString());
                    printMessage(" " + new Date().toString() + " Export topics and occurrences for : " + pmid,2);
                    exportTopicPredications(pmid);
                    exportOccurrencePredications(pmid);
                }      
                // Increase current page to continue with the next one
                currentPage ++;
            } else {// Else, no more topics found for any article
                stop = true;
                printMessage(" " + new Date().toString() + " Export of topic and occurrence precications for exported articles complete! ",1);
            }
        } // end of results
    } 
    
    /**
     * Too slow for big Graphs, use the paged version below
     */
//    public void updateExportedTopicAnnotations() {
//        // Add preferred concept field in all concepts on present in MeSHPreferredConcepts
//        for(String cui : MeSHPreferredConcepts){       
//            String query = "MATCH (n:Annotation{id:\""+cui+"\"}) SET n.preferred = 1 ";
//            StatementResult result = this.n4j_outputGrpah.bigQuery(query);                        
//        }
//        // Update the confidence for all topic annotations for non-prefered concepts without occurrence into 0.5
//        String query = "MATCH p=(n:Publication)-[r:HAS_TOPIC]->(a:Annotation) WHERE (NOT (n)<-[:MENTIONED_IN]-(a)) AND (NOT EXISTS(a.preferred)) SET r.confidence = 0.5 ";
//            StatementResult result = this.n4j_outputGrpah.bigQuery(query);                        
//            
//    } 
    
    /**
     * Update topic annotation confidence for all articles in the output neo4j. 
     *      This is a paged version of the function that is better for output neo4j graphs of big size (e.g. all articles for AD)
     */
    public void updateExportedTopicAnnotationsPaged() {
        // Add preferred concept field in all concepts on present in MeSHPreferredConcepts
        for(String cui : MeSHPreferredConcepts){       
            String query = "MATCH (n:Annotation{id:\""+cui+"\"}) SET n.preferred = 1 ";
            StatementResult result = this.n4j_outputGrpah.bigQuery(query);                        
        }
        // Update the confidence for all topic annotations for non-prefered concepts without occurrence into 0.5
//        String query = "MATCH p=(n:Publication)-[r:HAS_TOPIC]->(a:Annotation) WHERE (NOT (n)<-[:MENTIONED_IN]-(a)) AND (NOT EXISTS(a.preferred)) SET r.confidence = 0.5 ";
        boolean stop = false;
        int pageSize = 1000;
        int currentPage = 0;
        while(!stop){
            String query = "MATCH p=(n:Publication)-[r:HAS_TOPIC{confidence:1}]->(a:Annotation) WHERE (NOT (n)<-[:MENTIONED_IN]-(a)) AND (NOT EXISTS(a.preferred)) SET r.confidence = 0.5 return p SKIP "+(currentPage*pageSize)+" LIMIT " + pageSize;
            StatementResult result = this.n4j_outputGrpah.bigQuery(query);   
            if(result.hasNext()){
                currentPage ++;
            } else {// Else, no more topics found for any article
                stop = true;
                printMessage(" " + new Date().toString() + " Update of Topic relatons with confidence complete! ",1);
            }
        }
    }       
    
    /**
     * Update topic annotation confidence for a given "small" set of articles, when updating the graph regularly.
     */
    public void updateExportedTopicAnnotations(ArrayList <String> pmids) {
        printMessage(" " + new Date().toString() + " Updating " + MeSHPreferredConcepts.size() + "concepts with \"preferred\" field",1);        
       // Add preferred concept field in all concepts on present in MeSHPreferredConcepts
        for(String cui : MeSHPreferredConcepts){       
            String query = "MATCH (n:Annotation{id:\""+cui+"\"}) SET n.preferred = 1 ";
            StatementResult result = this.n4j_outputGrpah.bigQuery(query);                        
        }
        printMessage(" " + new Date().toString() + " Updating of Topic relatons with confidence for " + pmids.size() + " articles",1);        
        // Update the confidence for all exported topic annotations for non-prefered concepts without occurrence into 0.5
//        String query = "MATCH p=(n:Publication)-[r:HAS_TOPIC]->(a:Annotation) WHERE (NOT (n)<-[:MENTIONED_IN]-(a)) AND (NOT EXISTS(a.preferred)) SET r.confidence = 0.5 ";
        for(String pmid : pmids){
            String query = "MATCH p=(n:Publication{pmid:\""+pmid+"\"})-[r:HAS_TOPIC{confidence:1}]->(a:Annotation) WHERE (NOT (n)<-[:MENTIONED_IN]-(a)) AND (NOT EXISTS(a.preferred)) SET r.confidence = 0.5";
            StatementResult result = this.n4j_outputGrpah.bigQuery(query);   
        }
        printMessage(" " + new Date().toString() + " Update of Topic relatons with confidence for " + pmids.size() + " articles complete!",1);
    }          
    
    /**
     * Export all alternative IDs from neo4j KB 
     *      Execute the appropriate cypher query to retrieve the IDs
     *      Then, execute the appropriate cypher query to import them 
     */
    public void exportAlternativeIDs() {
        boolean stop = false;
        int pageSize = 100;
        int currentPage = 0;
        while(!stop){
            // No relation type is defined because the only allowed relation from article to Entity is the one used for topic ("HAS_TOPIC" or HAS_MESH)
            //tmp code
//            printMessage(" " + new Date().toString() + " Search topic predications, page : " + currentPage ,2);
            String query = "MATCH (n:Entity) "
                    + "WHERE EXISTS(n.DRUGBANK) OR EXISTS(n.HGNC) OR EXISTS(n.HPO) OR EXISTS(n.SNOMEDCT_US) "
                    + "RETURN n.id, n.DRUGBANK, n.HGNC, n.HPO, n.SNOMEDCT_US SKIP "+(currentPage*pageSize)+" LIMIT " + pageSize;
            StatementResult result = this.n4j_inputGrpah.bigQuery(query);                        
            // If the query retrieved some results
            if(result.hasNext()){
                //Export them
                while ( result.hasNext() )
                {
                    ArrayList <String> altIdParts = new ArrayList <String>();
                    Record record = result.next();
                    String cui =  Neo4JControler.escapeNeo4j(record.get( "n.id" ).asString());
                    if(!record.get( "n.DRUGBANK" ).isNull()){
                        altIdParts.add("n.DRUGBANK = "+arrayListToCypherList(record.get( "n.DRUGBANK" ).asList()));
                    }
                    if(!record.get( "n.HGNC" ).isNull()){
                        altIdParts.add("n.HGNC = "+arrayListToCypherList(record.get( "n.HGNC" ).asList()));
                    }
                    if(!record.get( "n.HPO" ).isNull()){
                        altIdParts.add("n.HPO = "+arrayListToCypherList(record.get( "n.HPO" ).asList()));
                    }
                    if(!record.get( "n.SNOMEDCT_US" ).isNull()){
                        altIdParts.add("n.SNOMEDCT_US = "+arrayListToCypherList(record.get( "n.SNOMEDCT_US" ).asList()));
                    }

                    // Update Annotation node
                    if(!altIdParts.isEmpty()){                        
                        String altIds = String.join(", ", altIdParts);
                        query  = "MERGE (n:Annotation {id:'"+cui+"'}) SET " // MERGE is used instead of create, so that in case of existence no duplicate concepts will be created
                                + altIds;
                        this.n4j_outputGrpah.query(query);
                    }
                }      
                // Increase current page to continue with the next one
                currentPage ++;
            } else {// Else, no more topics found for any article
                stop = true;
                printMessage(" " + new Date().toString() + " Update of Annotations with alternative IDs complete! ",1);
            }
        } // end of results
    }  
    
    /**
     * Export all topic predications from neo4j KB 
     *      Execute the appropriate cypher query to retrieve the topics
     *      Then, execute the appropriate cypher query to import them 
     *      Estimate the confidence for each topic
     */
    public void exportTopicPredications() {
        boolean stop = false;
        int pageSize = 100;
        int currentPage = 0;
        while(!stop){
            // No relation type is defined because the only allowed relation from article to Entity is the one used for topic ("HAS_TOPIC" or HAS_MESH)
            //tmp code
//            printMessage(" " + new Date().toString() + " Search topic predications, page : " + currentPage ,2);
            String query = "MATCH (article:Article)-[r]->(concept:Entity) RETURN article.id, type(r), concept.id, concept.label, concept.sem_types SKIP "+(currentPage*pageSize)+" LIMIT " + pageSize;
            StatementResult result = this.n4j_inputGrpah.bigQuery(query);                        
            // If the query retrieved some results
            if(result.hasNext()){
                //Export them
                while ( result.hasNext() )
                {
                    Record record = result.next();
    //              Temporarily overide the HAS_MESH predicate with the HAS_TOPIC which is more correct. 
    //              In future this change will be also done in neo4j so the type(r) will be used
                    String predicate =  "HAS_TOPIC";
    //                String predicate =  Neo4JControler.escapeNeo4j(record.get( "type(r)" ).asString());
                    String pmid =  Neo4JControler.escapeNeo4j(record.get( "article.id" ).asString());
                    String cui =  Neo4JControler.escapeNeo4j(record.get( "concept.id" ).asString());
                    String label =  Neo4JControler.escapeNeo4j(record.get( "concept.label" ).asString());
                    List sem_types = record.get( "concept.sem_types" ).asList();
    //              Temporarily use fixed confidence of 1.0 for all topics
                    double confidence = 1.0;
    //              In future, also get other type of information from the neo4j graph to estimate the confidence for the topic 
                    //tmp code
//                    printMessage(" " + new Date().toString() + " Add concept node : " + cui,2);
                    // Add topic node
                    query  = "MERGE (topic:Annotation {id:'"+cui+"'}) " // MERGE is used instead of create, so that in case of existence no duplicate concepts will be created
                            + "ON CREATE SET "
                            + "topic.label = '"+label+"', "
                            + "topic.semantic_types = '"+sem_types+"'"; 
                    this.n4j_outputGrpah.query(query);
                    //tmp code
//                    printMessage(" " + new Date().toString() + " Add topics relation : " + cui,2);
                    // Add the topic relation
                    query = "MATCH (a:Publication {pmid:'"+pmid+"'}), (b:Annotation {id:'"+cui+"'}) " +
                            "MERGE (a)-[r:"+predicate+"]->(b) " +
                            "ON MATCH SET r.confidence = "+confidence+" " +
                            "ON CREATE SET r.confidence = "+confidence+"";
                    this.n4j_outputGrpah.query(query);
                    //tmp code
//                    printMessage(" " + new Date().toString() + " End topic relation : " + cui,2);                
                }      
                // Increase current page to continue with the next one
                currentPage ++;
            } else {// Else, no more topics found for any article
                stop = true;
                printMessage(" " + new Date().toString() + " Export of topic precications complete! ",1);
            }
        } // end of results
    }        
    
    /**
     * Export all occurrence predications from neo4j KB 
     *      Execute the appropriate cypher query to retrieve the predications
     *      Then, execute the appropriate cypher query to import them 
     *      Estimate the confidence for each occurrence     
     */
    public void exportOccurrencePredications() {
        boolean stop = false;
        int pageSize = 100;
        int currentPage = 0;
        while(!stop){
            // No relation type is defined because the only allowed relation from Entity to article is the one used for occurrence ("MENTIONED_IN")
           //tmp code
           printMessage(" " + new Date().toString() + " Search occurrences, page : " + currentPage ,2);
           String query = "MATCH (article:Article )<-[r]-(concept:Entity) RETURN article.id, type(r), r.score , concept.id, concept.label, concept.sem_types SKIP "+(currentPage*pageSize)+" LIMIT " + pageSize;
           StatementResult result = this.n4j_inputGrpah.bigQuery(query);
           // If the query retrieved some results
           if(result.hasNext()){
               //Export them
               while ( result.hasNext() )
               {
                   Record record = result.next();
                   String predicate =  Neo4JControler.escapeNeo4j(record.get( "type(r)" ).asString());
                   String pmid =  Neo4JControler.escapeNeo4j(record.get( "article.id" ).asString());
                   String cui =  Neo4JControler.escapeNeo4j(record.get( "concept.id" ).asString());
                   String label =  Neo4JControler.escapeNeo4j(record.get( "concept.label" ).asString());
                   List sem_types = record.get( "concept.sem_types" ).asList();
                   List occurrence_scores = record.get( "r.score" ).asList();
   //              Handle scores to create one score per predication
                   double confidence = findOccurrenceScore(occurrence_scores);
   //              In future, also getother type of information from the neo4j graph to estimate the confidence for the topic 


                   //tmp code
//                   printMessage(" " + new Date().toString() + " Add concept node : " + cui,2);
                   // Add topic node
                   query  = "MERGE (topic:Annotation {id:'"+cui+"'}) " // MERGE is used instead of create, so that in case of existence no duplicate concepts will be created
                           + "ON CREATE SET "
                           + "topic.label = '"+label+"', "
                           + "topic.semantic_types = '"+sem_types+"'"; 
                   this.n4j_outputGrpah.query(query);
                   //tmp code
//                   printMessage(" " + new Date().toString() + " Add occurrence relation : " + cui,2);                
                   // Add the topic relation
                   query = "MATCH (a:Publication {pmid:'"+pmid+"'}), (b:Annotation {id:'"+cui+"'}) " +
                           "MERGE (b)-[r:"+predicate+"]->(a) " +
                           "ON MATCH SET r.confidence = "+confidence+" " +
                           "ON CREATE SET r.confidence = "+confidence+"";
                   this.n4j_outputGrpah.query(query);
                   //tmp code
//                   printMessage(" " + new Date().toString() + " End occurrence relation : " + cui,2);                
               }   
                // Increase current page to continue with the next one
                currentPage ++;
           } else {// Else, no more occurrences found for any article
                stop = true;
                printMessage(" " + new Date().toString() + " Export of occurrence precications complete! ",1);
           }
        }// end of results
    }
    
    /**
     * Export all topic predications from neo4j KB for the given article
     *      Execute the appropriate cypher query to retrieve the topics
     *      Then, execute the appropriate cypher query to import them 
     *      Estimate the confidence for each topic
     * @param pmid  The PMID of the article the topics of which are to be exported
     */
    public void exportTopicPredications(String pmid) {
        // No relation type is defined because the only allowed relation from article to Entity is the one used for topic ("HAS_TOPIC" or HAS_MESH)
        //tmp code
//        printMessage(" " + new Date().toString() + " Search topics : " + pmid,2);
        String query = "MATCH (article:Article {id:'"+pmid+"'})-[r]->(concept:Entity) RETURN type(r), concept.id, concept.label, concept.sem_types ";
// TMP change for buggy has mesh
//        String query = "MATCH (article:Entity {id:'"+pmid+"'})-[r]->(concept:Entity) RETURN type(r), concept.id, concept.label, concept.sem_types ";
        StatementResult result = this.n4j_inputGrpah.bigQuery(query);
        // If the query retrieved some results
        if(result.hasNext()){
            //Export them
            while ( result.hasNext() )
            {
                Record record = result.next();
//              Temporarily overide the HAS_MESH predicate with the HAS_TOPIC which is more correct. 
//              In future this change will be also donw in neo4j so the type(r) will be used
                String predicate =  "HAS_TOPIC";
//                String predicate =  Neo4JControler.escapeNeo4j(record.get( "type(r)" ).asString());
                String cui =  Neo4JControler.escapeNeo4j(record.get( "concept.id" ).asString());
                String label =  Neo4JControler.escapeNeo4j(record.get( "concept.label" ).asString());
                List sem_types = record.get( "concept.sem_types" ).asList();
//              Initialize with fixed confidence of 1.0 for all topics
//                  Later in the procedure these values will be updated accordingly
                double confidence = 1.0;
//              In future, also getother type of information from the neo4j graph to estimate the confidence for the topic 
                //tmp code
//                printMessage(" " + new Date().toString() + " Add concept node : " + cui,2);
                // Add topic node
                query  = "MERGE (topic:Annotation {id:'"+cui+"'}) " // MERGE is used instead of create, so that in case of existence no duplicate concepts will be created
                        + "ON CREATE SET "
                        + "topic.label = '"+label+"', "
                        + "topic.semantic_types = '"+sem_types+"'"; 
                this.n4j_outputGrpah.query(query);
                //tmp code
//                printMessage(" " + new Date().toString() + " Add topics relation : " + cui,2);
                // Add the topic relation
                query = "MATCH (a:Publication {pmid:'"+pmid+"'}), (b:Annotation {id:'"+cui+"'}) " +
                        "CREATE (a)-[r:"+predicate+"]->(b) " +
                        "SET r.confidence = "+confidence+"";                        
//                        "MERGE (a)-[r:"+predicate+"]->(b) " +
//                        "ON MATCH SET r.confidence = "+confidence+" " +
//                        "ON CREATE SET r.confidence = "+confidence+"";
                this.n4j_outputGrpah.query(query);
                //tmp code
//                printMessage(" " + new Date().toString() + " End topic relation : " + cui,2);                
                
            }            
        } else {// Else, no topics for this article
            // This is not expected, print some warning message for this article
            printMessage(" " + new Date().toString() + " Warning : No topics found for this article : " + pmid,2);
        }
    }
    
    /**
     * Export all occurrence predications from neo4j KB for the given article
     *      Execute the appropriate cypher query to retrieve the predications
     *      Then, execute the appropriate cypher query to import them 
     *      Estimate the confidence for each occurrence
     * @param pmid  The PMID of the article for which the occurrences are to be exported
     */
    public void exportOccurrencePredications(String pmid) {
         // No relation type is defined because the only allowed relation from Entity to article is the one used for occurrence ("MENTIONED_IN")
        //tmp code
//        printMessage(" " + new Date().toString() + " Search occurrences : " + pmid,2);
        String query = "MATCH (article:Article {id:'"+pmid+"'})<-[r]-(concept:Entity) RETURN type(r), r.score , concept.id, concept.label, concept.sem_types ";
        StatementResult result = this.n4j_inputGrpah.bigQuery(query);
        // If the query retrieved some results
        if(result.hasNext()){
            //Export them
            while ( result.hasNext() )
            {
                Record record = result.next();
                String predicate =  Neo4JControler.escapeNeo4j(record.get( "type(r)" ).asString());
                String cui =  Neo4JControler.escapeNeo4j(record.get( "concept.id" ).asString());
                //TODO: Add a check here for "CUIs Excluded from MentionedIn relations"
                String label =  Neo4JControler.escapeNeo4j(record.get( "concept.label" ).asString());
                List sem_types = record.get( "concept.sem_types" ).asList();
                List occurrence_scores = record.get( "r.score" ).asList();
//              Temporarily use fixed confidence of 1.0 for all topics
                double confidence = findOccurrenceScore(occurrence_scores);
//              In future, also getother type of information from the neo4j graph to estimate the confidence for the topic 
                
                //tmp code
//                printMessage(" " + new Date().toString() + " Add concept node : " + cui,2);
                // Add topic node
                query  = "MERGE (topic:Annotation {id:'"+cui+"'}) " // MERGE is used instead of create, so that in case of existence no duplicate concepts will be created
                        + "ON CREATE SET "
                        + "topic.label = '"+label+"', "
                        + "topic.semantic_types = '"+sem_types+"'"; 
                this.n4j_outputGrpah.query(query);
                //tmp code
//                printMessage(" " + new Date().toString() + " Add occurrence relation : " + cui,2);                
                // Add the topic relation
                query = "MATCH (a:Publication {pmid:'"+pmid+"'}), (b:Annotation {id:'"+cui+"'}) " +
                        "CREATE (b)-[r:"+predicate+"]->(a) " +
                        "SET r.confidence = "+confidence+"";
//                        "MERGE (b)-[r:"+predicate+"]->(a) " +
//                        "ON MATCH SET r.confidence = "+confidence+" " +
//                        "ON CREATE SET r.confidence = "+confidence+"";
                this.n4j_outputGrpah.query(query);
                //tmp code
//                printMessage(" " + new Date().toString() + " End occurrence relation : " + cui,2);                
                
            }            
        } else {// Else, no occurrences for this article
            // This is not expected, print some warning message for this article
            printMessage(" " + new Date().toString() + " Warning : No occurrences found for this article : " + pmid,2);
        }
    }
 
    /**
     * Calculate a score for an occurrence predication
     *      Currently just an average
     * @param SemRepScores
     * @return 
     */
    public double findOccurrenceScore(List SemRepScores){
//        System.out.println(SemRepScores);
        double sum = 0;
        int length = SemRepScores.size();
            for(Object i : SemRepScores){
                long tmp = (long)i;
                sum+=tmp;
            }
        return (sum/length)/1000;
    }
    
    /**
     * Delete all article elements form neo4j database
     *      Used to "clean" the neo4j database to be used as output.
     */
    public void deleteOutputPublication(){
        boolean stop = false;
        int total = 0;
        while(!stop){
            String query = "MATCH (n:Publication) \n" +
                            "WITH n LIMIT 100\n" +
                            "DETACH DELETE n\n" +
                            "RETURN count(*);";
            StatementResult result = this.n4j_outputGrpah.bigQuery(query);
            // If the query retrieved some results
            if(result.hasNext()){
                 Record record = result.next();
                 int count = record.get( "count(*)" ).asInt();
                if(count > 0){
                    // just keep going 
                     total += count;
                     printMessage(" " + new Date().toString() + " Deleted "+total+" articles in total so far",2);  
                 } else {
                    stop = true;                     
                 }           
            } else { // no results found
                stop = true;
            }
        }
    }
    
    /**
     * Delete all Entities elements form neo4j database
     *      Used to "clean" the neo4j database to be used as output.
     */
    public void deleteOutputAnnotation(){
        boolean stop = false;
        int total = 0;
        while(!stop){
            String query = "MATCH (n:Annotation) \n" +
                            "WITH n LIMIT 100\n" +
                            "DETACH DELETE n\n" +
                            "RETURN count(*);";
            StatementResult result = this.n4j_outputGrpah.bigQuery(query);
            // If the query retrieved some results
            if(result.hasNext()){
                 Record record = result.next();
                 int count = record.get( "count(*)" ).asInt();
                 if(count > 0){
                    // just keep going 
                     total += count;
                     printMessage(" " + new Date().toString() + " Deleted "+total+" entities in total so far",2);  
                 } else {
                    stop = true;                     
                 }
            } else { // no results found
                stop = true;
            }
        }
    }

    /**
     * Get the articles from a list of sentences
     * @param   sentences, a list of the sentence ids where a predication has been extracted from (e.g. 19193610_abstract_3 or 19193610_fullText_25)
     * @return  articles, a list of the pmids of the articles where a predication has been extracted from (e.g. 19193610)
     */
    private ArrayList <String> getArticlesFromSenctences(List sentences) {
        ArrayList <String> articles = new ArrayList<>();
        int length = sentences.size();
            for(Object i : sentences){
                // e.g. 19193610_abstract_3 or 19193610_fullText_25
                String sentence_id = (String)i;
                String[] parts = sentence_id.split("_");
                if(parts.length > 0){
                String pmid = parts[0];
                    if(!articles.contains(pmid)){
                        articles.add(pmid);
                    }
                }
            }       
        return articles;
    }
    
    /**
     * Calculate a score for a concept to concept predication
     *      Currently just an average of subject and object score averages
     * @param   SemRepScores
     * @return  a score for confidence in [0,1]
     */
    private double findPredicationScore(List subjScores, List objScores, List resources, String predicate) {
        double sum = 0;
        int length = subjScores.size();
            for(Object i : subjScores){
                long tmp = (long)i;
                sum+=tmp;
            }
        double subjScore = (sum/length)/1000;
        
        sum = 0;
        length = objScores.size();
            for(Object i : objScores){
                long tmp = (long)i;
                sum+=tmp;
            }
        double objScore = (sum/length)/1000;
        
        return (subjScore+objScore)/2;
    }
    
    /**
     *  Normalize the predicate of a predication 
     *      Remove "__INFER__" and "__SPEC__" from the ending of the predicate
     * @param predicate     The predicate to be normalized
     * @return 
     */
    private String normalizePredicates(String predicate){
        predicate = predicate.replace("__INFER__", "");
        predicate = predicate.replace("__SPEC__", "");
        return predicate;
    }
    
    /**
     * Recognize the "extraction type" of a predication. 
     *      The extraction types are:
     *          Normal          : Usual extraction based on the SemRep rules
     *          Specification   : Extraction based on ISA relation
     *          Inference
     * @param predicate     The predicate of the predications (e.g. USES, USES__SPEC__ etc)
     * @return              The extraction type of the predication. One of : Normal, Specification, Inference corresponding to modes of extraction in SemRep
     */
    private String findPredicateExtractionType(String predicate){
        if(predicate.endsWith("__INFER__"))
            return "Inference";
        if(predicate.endsWith("__SPEC__"))
            return "Specification";
        return "Normal";
    }

    /**
     * Calculate a score representing the polarity of a concept to concept predication based on the negation recognized in corresponding instances
     *      Currently just an average of negative to positive instances
     * @param   negations
     * @return  a score for negation in [0,1]
     */
    private double findNegationScore(List negations) {
        double sum = 0;
        int length = negations.size();
        for(Object i : negations){
            String tmp = (String)i;
            if(i.equals(negationFlag)){
                sum++;
            }
        }
        double negation = sum/length;
        return negation;        
    }

    /**
     * Find the instances of a relation that do not comply with the wanted/valid Semantic Types (ST)
     *  
     * @param valid_subjSemTypes    String of all wanted/valid semantic types for the subject of the relation as a "list of ST abbreviations" (e.g. "'bdsu','chem','chvs','orch','nnon','aapp','chvf'")
     * @param valid_objSemTypes     String of all wanted/valid semantic types for the object of the relation as a "list of ST abbreviations" (e.g. "'bdsu','chem','chvs','orch','nnon','aapp','chvf'")
     * @param subjSem_types         A lists with the semantic types of subjects in all extracted instances
     * @param objSem_types          A lists with the semantic types of objects in all extracted instances
     * @return                      A list with the indexes of the relations instances that do not comply with the give/wanted/valid semantic types
     */
    private ArrayList<Integer> findInvalidInstancesBySemanticType(String valid_subjSemTypes, String valid_objSemTypes, List subjSem_types, List objSem_types) {
        ArrayList <Integer> InvalidInstanceIndexes = new ArrayList<>(); // The invalid indexes to be removed
        // subjSem_types and objSem_types have the same size, they are two parallel lists.
        int length = subjSem_types.size();
        int length2 = objSem_types.size();
        // For each instance of this relation
        for(int i = 0 ; i < length ; i++){
            String objST = (String)objSem_types.get(i);
            String subjST = (String)subjSem_types.get(i);
            // If the subject or the object of the instance has a non valid semantic type
            if(!valid_subjSemTypes.contains(subjST) || !valid_objSemTypes.contains(objST)){
                // add the index of the instance to the list
                InvalidInstanceIndexes.add(i);
            }
        }
        return InvalidInstanceIndexes;
    }
    
    /**
     * Remove elements with given indexes from all the given (parallel) Lists
     *      Used to remove "non valid" instances of relations retrieved from the graph, i.e. instances where the Semantic Type of the subject or the object is not wanted/valid/allowed.
     * 
     * @param invalidInstanceIndexes    ArrayList of integers : all non valid indexes to be removed (created with findInvalidInstancesBySemanticType)
     * @param sentences                 The List of sentences for this relation, retrieved from the query
     * @param subjScores                The List of subject scores for this relation, retrieved from the query
     * @param objScores                 The List of object scores for this relation, retrieved from the query
     * @param sentence_SubjSTs          The List of subject Semantic Types for this relation, retrieved from the query
     * @param sentence_objSTs           The List of object Semantic Types for this relation, retrieved from the query
     * @param negations                 The List of negations for this relation, retrieved from the query
     */
    private void removeInstances(ArrayList<Integer> invalidInstanceIndexes, List sentences, List subjScores, List objScores, List sentence_SubjSTs, List sentence_objSTs, List negations) {
        Collections.sort(invalidInstanceIndexes);
        Collections.reverse(invalidInstanceIndexes);
// tmp code         
//        System.out.println(invalidInstanceIndexes);
//        System.out.println(" before " + sentences);
        for(int i  : invalidInstanceIndexes){
            sentences.remove(i);
            subjScores.remove(i);
            objScores.remove(i);
            sentence_SubjSTs.remove(i);
            sentence_objSTs.remove(i);
            negations.remove(i);            
        }
// tmp code        
//        System.out.println(" after " + sentences);
    }
    
    /**
     * Convert a list of Strings into a String representing a list in format adequate to be used in h Cypher query
     * 
     * @param arrayList     ArrayList of String : The list of strings
     * @return              a String representing a list in format adequate to be used in h Cypher query (e.g. "['...','...']")
     */
    public String arrayListToCypherList(ArrayList <String> arrayList){
        String list = "[";
        for (String element : arrayList){
            if(!list.equals("[")){
                list +=", ";
            }
            list += "'"+Neo4JControler.escapeNeo4j(element)+"'";
        }
        list += "]";
        return list;
    }

    /**
     * Convert a list (of objects that can be cast to String) into a String representing a list in format adequate to be used in h Cypher query
     * @param list  The list of strings
     * @return  a String representing a list in format adequate to be used in h Cypher query (e.g. "['...','...']")    
     */
    public String arrayListToCypherList(List list){
        return arrayListToCypherList(ListToArrayListString(list));
    }
    
    /**
     * Convert a list of objects (actually Strings) into an ArrayList <String> 
     * @param list of object that can be casted to String
     * @return ArrayList <String> 
     */     
    public ArrayList <String> ListToArrayListString(List list){
        ArrayList <String> arrayList = new ArrayList <String>();
        for (Object element : list){
            arrayList.add((String)element);
        }
        return arrayList;
    }

}
