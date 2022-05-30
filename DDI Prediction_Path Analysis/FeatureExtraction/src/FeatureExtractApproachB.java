import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class FeatureExtractApproachB {

	final static int RELATIONS = 35; 
	final static int FEATURES = 3*RELATIONS;
	static int goldenLines=0;
	
	public static void extractFeaturesFromFile (Terms terms, String inputF, String d1, String d2, String outputF, int interaction) {
		int line=-1;
		//int featureLines=0;

		try {
			CSVWriter writer = new CSVWriter(new FileWriter(outputF, true),CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
		
			String [] outputLine = new String[FEATURES+2];
			//initialize feature with 0s in case of no path-file found between CUIs
			for (int i=0; i<outputLine.length-1; i++)
				outputLine[i]="0";
			//if the file doesnt exist, the path extraction script did not find one of the two drugs in Neo4j    
	        if (!new File(inputF).isFile()) {
				String interacts="0";
		    	// POSITIVE PAIRS: put "1" (existing interaction) in the end
				if (interaction==1)
			    	interacts="1";
			    outputLine[FEATURES]=""+interacts;    
			    outputLine[FEATURES+1]=d1+"_"+d2;
			    writer.writeNext(outputLine);
			    writer.close();
			    return;
	        }
	        CSVReader reader = new CSVReader(new FileReader(inputF));
	
			String [] nextLine;	
			String drug1=d1,drug2=d2;
			while ((nextLine = reader.readNext()) != null) { // && (i<50)) {  //limit only for the first 15 samples
				
				line++;
				List<String> lineAsList = new ArrayList<String>(Arrays.asList(nextLine));
					
				// treat smaller paths than 3...
				if (lineAsList.size()<8) { 
					int length;
					length=Integer.parseInt(lineAsList.get(lineAsList.size()-1));
					String cui2 = lineAsList.get(lineAsList.size()-2);
					//System.out.println("FOUND smaller PATH OF LENGTH="+length);
					if (length==2) {
						
						lineAsList.add(5,"0");
						lineAsList.add(6,cui2);
						lineAsList.add(7,"2");
						lineAsList=lineAsList.subList(0,8);
					}	
					else if (length==1) {
						lineAsList.add(3,"0");
						lineAsList.add(4,cui2);
						lineAsList.add(5,"0");
						lineAsList.add(6,cui2);
						lineAsList.add(7,"1");
						lineAsList=lineAsList.subList(0,8);
					}	
					else
						continue;
				}
					
				drug1 = lineAsList.get(0);
			    drug2 = lineAsList.get(6);
				    
	
			    if (line==0) 	 {
			    	//extract relation features and add to output line...
			   		outputLine = extractFeatures(outputLine, lineAsList, terms);
			    }
			    else     //still counting RELs for this existing pair...
			    	outputLine = aggregateFeatures(outputLine, lineAsList, terms);
			}
			//After finishing all path lines, save pair aggregated vector in output file
	
			String interacts="0";
		    // POSITIVE PAIRS: put "1" (existing interaction) in the end
			if (interaction==1)
				interacts="1";
			outputLine[FEATURES]=""+interacts;    
	
			outputLine[FEATURES+1]=drug1+"_"+drug2;
			writer.writeNext(outputLine);
			//featureLines++;
			    
			reader.close();
	        writer.close();
			
			//System.out.println("***************************");
			//System.out.println("Features extraction ended!");
			//System.out.println("Aggregated RELs for Drug pair: "+drug1+" - "+ drug2+":");
			//for (int i=0; i<outputLine.length;i++)
			//	System.out.print(outputLine[i]);
	    	//System.out.println("\n***************************");			
			
	    	
	    	
			
			/*count the number of positive pairs lines to equalize the negative set...
			 
			// * Then merge golden & negative files
			 //* ONLY NEEDED FOR UNBALANCED DATASETS
			 
			 if (interaction==1) {
				 goldenLines = featureLines;
			 }
			 else {
				 System.out.println("Features file for positive pairs set has:"+goldenLines +"lines");
				 System.out.println("Features file for negative pairs set has:"+featureLines +"lines");
				 reduceNegativeFileAndMerge(outputF, featureLines, goldenLines);
			 }

 			*/
			 
			 
		} catch (Exception e) {
			System.out.println("!!problem at line: "+line);
			e.printStackTrace();
			
		}
	}
	
	public static String [] extractFeatures (String [] pairLine, List<String> lineAsList, Terms terms) {
		
		
			String rel1 = lineAsList.get(1);
		    String rel2 = lineAsList.get(3);
		    String rel3 = lineAsList.get(5);
		    //String rel4 = lineAsList.get(7);
		    
		    rel1 = terms.normalizedNames.get(rel1);
		    rel2 = terms.normalizedNames.get(rel2);
		    rel3 = terms.normalizedNames.get(rel3);
		    //rel4 = terms.normalizedNames.get(rel4);
		    
		    int rel1Index =-1; 
		    int rel2Index =-1;
		    int rel3Index = -1;
		    //int rel4Index = -1;
		    
		    //System.out.println("Normalized Relation 1 name= "+rel1);
		    //System.out.println("\"1\" put in index= "+(terms.relationTypes.indexOf(rel1)+1));
		    
		    if (!rel1.equals("0"))
		    	rel1Index= terms.relationTypes.indexOf(rel1);
		    if (!rel2.equals("0"))
		    	rel2Index =terms.relationTypes.indexOf(rel2);
		    if (!rel3.equals("0"))
		    	 rel3Index = terms.relationTypes.indexOf(rel3);
		    //if (!rel4.equals("0"))
		    	// rel4Index = terms.relationTypes.indexOf(rel4);
		    
		    pairLine = new String[FEATURES+2];
		    
		    //initialize all values to 0
		    for (int j=0; j<FEATURES+1; j++)  
		    	pairLine[j]="0";
		    
		    //put 1s for existing relations
		    if (rel1Index != -1) {
		    	
		    	pairLine[rel1Index]="1";			    
		    }
		    if (rel2Index!=-1) {
		    	
		    	pairLine[RELATIONS+rel2Index]="1";					    	
		    }	
		    if (rel3Index!=-1) {

		    	pairLine[2*RELATIONS+rel3Index]="1";					    	
		    }
		    //if (rel4Index!=-1) {

		    	//pairLine[3*RELATIONS+rel4Index]="1";			
		    //}
		    
		    System.out.println("");
		return pairLine;		
	}
	
	public static String [] aggregateFeatures (String [] pairLine, List<String> lineAsList, Terms terms) {
		
		 	String rel1_original = lineAsList.get(1);
		    String rel2_original = lineAsList.get(3);
		    String rel3_original = lineAsList.get(5);
		    //String rel4_original = lineAsList.get(7);
		    
		    String rel1 = terms.normalizedNames.get(rel1_original);
		    String rel2 = terms.normalizedNames.get(rel2_original);
		    String rel3 = terms.normalizedNames.get(rel3_original);
		    //String rel4 = terms.normalizedNames.get(rel4_original);
		    
		    int rel1Index =-1; 
		    int rel2Index =-1;
		    int rel3Index = -1;
		    //int rel4Index = -1;
		    
		    //System.out.println("Normalized Relation 1 name= "+rel1);
		    //System.out.println("\"1\" put in index= "+(terms.relationTypes.indexOf(rel1)+1));
		    try {
			    if (!rel1.equals("0"))
			    	rel1Index= terms.relationTypes.indexOf(rel1);
			    if (!rel2.equals("0"))
			    	rel2Index =terms.relationTypes.indexOf(rel2);
			    if (!rel3.equals("0"))
			    	 rel3Index = terms.relationTypes.indexOf(rel3);
			    //if (!rel4.equals("0"))
			    	// rel4Index = terms.relationTypes.indexOf(rel4);
		    
		    } catch (NullPointerException e) {
		    	
		    	System.out.println("crashed...Non-normalized relations:" + rel1_original+" "+rel2_original+" "+rel3_original);
		    }
		    
		    //put 1s for existing relations
		    if (rel1Index != -1) {
		    	int rel1Counts = Integer.parseInt(pairLine[rel1Index]);
		    	rel1Counts++;
		    	pairLine[rel1Index]=""+(rel1Counts);			    
		    }
		    if (rel2Index!=-1) {
		    	int rel2Counts = Integer.parseInt(pairLine[RELATIONS+rel2Index]);
		    	rel2Counts++;
		    	pairLine[RELATIONS+rel2Index]=""+(rel2Counts);				    	
		    }	
		    if (rel3Index!=-1) {
		    	int rel3Counts = Integer.parseInt(pairLine[2*RELATIONS+rel3Index]);
		    	rel3Counts++;
		    	pairLine[2*RELATIONS+rel3Index]=""+(rel3Counts);					    	
		    }
		    //if (rel4Index!=-1) {
		    	//int rel4Counts = Integer.parseInt(pairLine[3*RELATIONS+rel4Index]);
		    //	rel4Counts++;
		    	//pairLine[3*RELATIONS+rel4Index]=""+(rel4Counts);			
		    //}

		    
		return pairLine;		
	}
	
	public static void csvFirstLine(Terms terms, String CSVfile) {
		
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(CSVfile),CSVWriter.DEFAULT_SEPARATOR,
	                CSVWriter.NO_QUOTE_CHARACTER,
	                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
	                CSVWriter.DEFAULT_LINE_END);
		
			   
			String [] relationsInPath  = {"rel1","rel2","rel3"};//,"rel4"};
			String [] firstLine = new String[FEATURES+2];
			
			for (int i=0 ; i<relationsInPath.length; i++) {   //runs 3 times for 3 relations in path
				String tag = relationsInPath[i];
	
				int j=i*RELATIONS;
	
			    for(String relationType:terms.relationTypes){
			    	firstLine[j]=tag+"_"+relationType;
			    	j++;
			    }
				
				
			}
			firstLine[FEATURES] = "INTERACTS";  //golden sample all result into DRUG-DRUG Interactions
			firstLine[FEATURES+1] = "CUI_PAIR";
			
			writer.writeNext(firstLine);
			writer.close();
			return;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void reduceNegativeFileAndMerge(String filePath, int initialLines, int resultLines) {
		
		if (initialLines>resultLines) {
			
			int line =-1;
			int skippedLines =0;
			int extraLines=initialLines-resultLines;
			int step = initialLines/extraLines;
			
			System.out.println("Reducing Features file for neagtive set to be equal with golden...step: "+ step);
			
			try {
				CSVReader reader = new CSVReader(new FileReader(filePath));
				CSVWriter writer = new CSVWriter(new FileWriter("C:\\Users\\fotis\\Desktop\\approachb-features-merged.csv"),CSVWriter.DEFAULT_SEPARATOR,
	                    CSVWriter.NO_QUOTE_CHARACTER,
	                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
	                    CSVWriter.DEFAULT_LINE_END);
			
				String [] nextLine;	
				
				
				while ((nextLine = reader.readNext()) != null) { 
					line++;
					if ((line%step)==0) {
						//System.out.println("Skip line: " +line);
						skippedLines++;
						continue;
					}
					else
						writer.writeNext(nextLine);
					
				}
				
				CSVReader goldenReader = new CSVReader(new FileReader("C:\\Users\\fotis\\Desktop\\approachb-features-golden.csv"));
				while ((nextLine = goldenReader.readNext()) != null) { 
					
				    writer.writeNext(nextLine);
					
				}
				
				//System.out.println("Reduced negative feature file  by "+skippedLines+"lines");
				reader.close();
				writer.close();
				goldenReader.close();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}	
		
	}
	

}
