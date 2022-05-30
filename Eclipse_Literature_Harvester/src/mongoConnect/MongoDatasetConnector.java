/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mongoConnect;
// add javaMongoDriver/*.jar
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import java.util.Arrays;

import org.bson.Document;
import com.mongodb.client.MongoCollection;

/**
 * Basic Connection to MongoDB to write a dataset 
 * 
 * @author tasosnent
 */
public class MongoDatasetConnector {

//  The mongo installation to use
    private MongoClient mongoClient;
// The database (iASiS test DB)       
    private MongoDatabase db;
//  The collection to be used
    private MongoCollection collection;
//  The dataset ID
    private String dataSetId;

    /**
     * Create a collection in MongoDB to store a dataset and provide the functionality to add JSON object in this collection
     * @param ip                The MongoDB IP
     * @param port              The MongoDB port
     * @param dataBaseName      The MongoDB name
     * @param dataSetName       The DataSet Name (to be used to create an homonymous collection in the database e.g. AD_20180214)
     */
    public MongoDatasetConnector(String ip, int port, String dataBaseName,String dataSetName){
        mongoClient = new MongoClient(ip, port);
        db = mongoClient.getDatabase(dataBaseName);
        dataSetId = dataSetName;
        collection = this.createCollection(dataSetName);
        // Delete previous content if any
        BasicDBObject document = new BasicDBObject();
        collection.deleteMany(document);
    }

    public MongoDatasetConnector(String ip, int port, String dataBaseName,String dataSetName, String username, String password){
       // mongoClient = new MongoClient(ip, port);

        // FOT ADDITION: Creating Credentials 
        MongoCredential credential; 
        credential = MongoCredential.createCredential(username, "admin", password.toCharArray()); 
        System.out.println("Created MongoDB credentials");  
        mongoClient = new MongoClient(new ServerAddress(ip,port),Arrays.asList(credential));
        
        System.out.println("Connected to the database successfully");
        
        db = mongoClient.getDatabase(dataBaseName);
        
        dataSetId = dataSetName;
        collection = this.createCollection(dataSetName);
        System.out.println("created MongoDB collection "+dataSetName);
        // Delete previous content if any
        BasicDBObject document = new BasicDBObject();
        collection.deleteMany(document);
        System.out.println("Deleted previous MongoDB collections...");
    }

    /**
     * Creates a collection in MongoDB with the given ID
     * @param datasetId     The ID to be used as name of the collection
     * @return              The MongoCollection element just created
     */
    public MongoCollection createCollection(String datasetId){
        MongoCollection collection = db.getCollection(datasetId);
        return collection;
    }
 
    /**
     * Add a JSON object to MongoDB in the predefined collection for the dataset
     * @param jsonObject 
     */
    public void add(String jsonObject){
        Document doc = Document.parse(jsonObject);
        collection.insertOne(doc);
    }

    /**
     * @return the dataSetId
     */
    public String getDataSetId() {
        return dataSetId;
    }

    /**
     * @param dataSetId the dataSetId to set
     */
    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }
}
