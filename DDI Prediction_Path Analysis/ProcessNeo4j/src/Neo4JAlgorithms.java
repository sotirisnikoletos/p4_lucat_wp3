/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
//import java.util.Iterator;

// import all jar from neo4j-community-3.3.3\lib
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

 
/**
 *
 * @author tasosnent
 */
public class Neo4JAlgorithms {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)  {
    	
    	String drugPairsFolder = args[0];
    	String neo4jFolder = args[1];
    	final int length=3;
    	int pathFilesAlreadyexisted=0;
    	
        //File databaseDirectory = new File("C:\\FOT\\neo4j-community-3.5.23\\data\\databases\\graph.db");
    	File databaseDirectory = new File(neo4jFolder);
    	
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( databaseDirectory );
        registerShutdownHook( graphDb );
        
        Node firstNode;
        Node secondNode;
        
        boolean includesLoop=false;
        
        try ( Transaction tx = graphDb.beginTx() )
        {
        	
        	try {
        		
        		String drugPair;
        		FileReader filerdr = new FileReader(drugPairsFolder+"drug-pairs.csv");
        		BufferedReader in = new BufferedReader(filerdr);
        		while(( drugPair = in.readLine() ) != null   ) {
        	        String[] drugs = drugPair.split(",");
         	
        	        //skip this drug pair, if the file with the paths already exists!
        	        File f = new File(drugPairsFolder+"ExtractedPath_Files/"+drugs[0]+"_"+drugs[1]+"_paths_"+length+".csv");
        	        if (f.exists()) {
        	        	pathFilesAlreadyexisted++;
        	        	continue;
        	        }
        	        
		          	Label l1 = Label.label("Entity"); 
		          	Label l2 = Label.label("Entity");
		            firstNode = graphDb.findNode(l1, "id", drugs[0]);
		            secondNode = graphDb.findNode(l2, "id", drugs[1]);
		            if ((firstNode==null) || (secondNode==null))
		            	continue;
		            
 		            PrintWriter writer= new PrintWriter(drugPairsFolder+"ExtractedPath_Files/"+drugs[0]+"_"+drugs[1]+"_paths_"+length+".csv", "UTF-8");
		            PathFinder<Path> finder = GraphAlgoFactory.allSimplePaths(PathExpanders.allTypesAndDirections(), length);
		            Iterable<Path> paths = finder.findAllPaths(firstNode, secondNode);
		            
		            for (Path p : paths){
		            	
		            	//includesLoop=false;
		                //System.out.println(p);
		                
		            	//edit paths , show CSV style  
		                String pathLine=drugs[0]+",";  
		            	int i=0;
		            	
			            Node currentNode = firstNode;            	
		            	Iterable<Relationship> relations = p.relationships();
		            	for (Relationship r : relations){
		            		//if (currentNode.equals(r.getOtherNode(currentNode))) {
		            			//includesLoop=true;
		            			//System.out.println("FOUND LOOP!");
		            			//break;
		            		//}	
		            		i++;
		            		if (i!=length) {
		            			currentNode = r.getOtherNode(currentNode);
		            			pathLine+=r.getType()+","+currentNode.getProperty("id")+",";		            			
		            		}
		            		else
		            			pathLine+=r.getType()+",";
		            	}
		            	//if (includesLoop) 
		            		//continue;	
		            	pathLine+=drugs[1]+","+i;
		            	writer.println(pathLine);
		                
		            }
		            writer.close();
		            tx.success();
		            
		        }
	            in.close();
	            filerdr.close();
        		
	         }catch(Exception e) {
	            	e.printStackTrace();
	         }
	    }
	
	    System.out.println("ALL PATHS RETRIEVED");
	    System.out.println("Skipped "+pathFilesAlreadyexisted+ " drug pairs that their paths were already retrieved in a file...");
        
        graphDb.shutdown();
    }
    
   
    
    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }  

    
}
