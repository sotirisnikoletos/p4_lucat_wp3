import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class FeatureExtractApproachA {

	final static int RELATIONS = 35; 
	final static int FEATURES = 4*RELATIONS;
	
	public static void extractFeaturesFromFile (Terms terms, String inputF, String outputF, int interaction) {
		int i=0;

		try {
			CSVReader reader = new CSVReader(new FileReader(inputF));
			CSVWriter writer = new CSVWriter(new FileWriter(outputF),CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
		

		    writer.writeNext(csvFirstLine(terms));
		    
		   // String line=reader.readNext().toString();
		   // line=correct(line, terms);
		    
			String [] nextLine;	
			while ((nextLine = reader.readNext()) != null) { // && (i<50)) {  //limit only for the first 15 samples
				i++;
				if (i==1) 
					continue;
				
				List<String> lineAsList = new ArrayList<String>(Arrays.asList(nextLine));
			    // Add stuff using linesAsList.add(index, newValue) as many times as you need.
			    

			    if (i%500==0) {
			    	System.out.println("***************************");
			    	System.out.println("PROCESSED "+i+" lines and Counting...");
			    }
			    
			    int pathLength = Integer.parseInt(lineAsList.get(9));
			    if (pathLength>4)
					continue;

			    
			    String rel1 = lineAsList.get(1);
			    String rel2 = lineAsList.get(3);
			    String rel3 = lineAsList.get(5);
			    String rel4 = lineAsList.get(7);
			    
			    rel1 = terms.normalizedNames.get(rel1);
			    rel2 = terms.normalizedNames.get(rel2);
			    rel3 = terms.normalizedNames.get(rel3);
			    rel4 = terms.normalizedNames.get(rel4);
			    
			    int rel1Index =-1; 
			    int rel2Index =-1;
			    int rel3Index = -1;
			    int rel4Index = -1;
			    
			    //System.out.println("Normalized Relation 1 name= "+rel1);
			    //System.out.println("\"1\" put in index= "+(terms.relationTypes.indexOf(rel1)+1));
			    
			    if (!rel1.equals("0"))
			    	rel1Index= terms.relationTypes.indexOf(rel1);
			    if (!rel2.equals("0"))
			    	rel2Index =terms.relationTypes.indexOf(rel2);
			    if (!rel3.equals("0"))
			    	 rel3Index = terms.relationTypes.indexOf(rel3);
			    if (!rel4.equals("0"))
			    	 rel4Index = terms.relationTypes.indexOf(rel4);
			    
			    String [] outputLine = new String[FEATURES+1];
			    
			    //initialize all values to 0
			    for (int j=0; j<FEATURES+1; j++)  
			    	outputLine[j]="0";
			    
			    //put 1s for existing relations
			    if (rel1Index != -1) {
			    	
			    	outputLine[rel1Index]="1";			    
			    }
			    if (rel2Index!=-1) {
			    	
			    	outputLine[RELATIONS+rel2Index]="1";					    	
			    }	
			    if (rel3Index!=-1) {

			    	outputLine[2*RELATIONS+rel3Index]="1";					    	
			    }
			    if (rel4Index!=-1) {

			    	outputLine[3*RELATIONS+rel4Index]="1";			
			    }
			    
			    //golden put 1/0 (existing interaction) in the end
			    String bool="false";
			    if (interaction==1)
			    	bool="true";
			    outputLine[FEATURES]=""+bool;    
			    writer.writeNext(outputLine);
			    
			    //System.out.println("OUTPUT CSV line: "+outputLine.toString());
			}	
			reader.close();
			writer.close();
			
	    	System.out.println("***************************");			
			System.out.println("Done with features extraction!");
			
		} catch (Exception e) {
			System.out.println("!!problem at line: "+i);
			e.printStackTrace();
			
		}
	}
	
	public static String [] csvFirstLine(Terms terms) {
		String [] relationsInPath  = {"rel1","rel2","rel3","rel4"};
		String [] firstLine = new String[FEATURES+1];
		
		for (int i=0 ; i<relationsInPath.length; i++) {   //runs 4 times for 4 relations in path
			String tag = relationsInPath[i];

			int j=i*RELATIONS;

		    for(String relationType:terms.relationTypes){
		    	firstLine[j]=tag+"_"+relationType;
		    	j++;
		    }
			
			
		}
			
		firstLine[FEATURES] = "INTERACTS";  //golden sample all result into DRUG-DRUG Interactions
		return firstLine;
	}
	

}
