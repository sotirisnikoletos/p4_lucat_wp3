import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import au.com.bytecode.opencsv.CSVWriter;

public class FileCreator {

	final int RELATIONS = 35; 
	final int FEATURES = 3*RELATIONS;

	public void createFeaturesFile(String dtiFolder, HashMap<String, String> DBids_CUIs, String task, boolean pos) {

		HashMap <String,int[]> outputLines = new HashMap <String,int[]>();
		try {
		   	//Open the output CSV file to write resulting DB pair features
			FileWriter fw;
			String outputPath = dtiFolder+"FeatureExtraction/approachb-"+task+"-Drugbank-features-aggregated.csv";
			if (pos)
				fw = new FileWriter(outputPath);
			else
				fw = new FileWriter(outputPath,true);
			CSVWriter writer = new CSVWriter(fw,CSVWriter.DEFAULT_SEPARATOR,
	                CSVWriter.NO_QUOTE_CHARACTER,
	                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
	                CSVWriter.DEFAULT_LINE_END);
			if (pos)
				writer.writeNext(this.csvFirstLine(new Terms("")));
				
	        //Open the existing features files to transform their features to DrugBank level
			String inputLine;
			int l=0;
			//int noCUIsFound=0;
			FileReader filerdr;
			if (pos)
				filerdr= new FileReader(dtiFolder+"FeatureExtraction/approachb-features-positivePairs.csv");
			else
				filerdr= new FileReader(dtiFolder+"FeatureExtraction/approachb-features-negativePairs.csv");
			BufferedReader in = new BufferedReader(filerdr);
			while(( inputLine = in.readLine() ) != null) {
		        if (l++==0)
		        	continue;
		        String[] line = inputLine.split(",");  
		        String[] cui_pair = line[FEATURES+1].split("_");
		        String cui1 = cui_pair[0];
		        String cui2 = cui_pair[1];
		        String DBid1 = DBids_CUIs.get(cui1);
		        String DBid2 = DBids_CUIs.get(cui2);
		        
		        int[] cuiPairFeatures = new int[FEATURES];
				//initialize output line with zeros
				if (!outputLines.containsKey(DBid1+"_"+DBid2)) {//DB_pair has not been added previously
					for (int i=0; i<cuiPairFeatures.length; i++)
						cuiPairFeatures[i]=Integer.parseInt(line[i]);
					outputLines.put(DBid1+"_"+DBid2, cuiPairFeatures);
				}	
				else  {//existing line for DB pair - must add features
					int[] dbPairFeatures = outputLines.get(DBid1+"_"+DBid2);	
					for (int i=0; i<dbPairFeatures.length; i++)
						dbPairFeatures[i]+=Integer.parseInt(line[i]);
					outputLines.put(DBid1+"_"+DBid2,dbPairFeatures);
				}
				
				//After finishing all path lines, save pair aggregated Hashmap in output file
			}
			in.close();
			filerdr.close();

			for(Entry<String, int[]> entry : outputLines.entrySet()) {
				String[] outputLine = new String[FEATURES+2];
				String DB_pair = entry.getKey();
				int [] relationCount = entry.getValue();
				outputLine[0]=DB_pair;
				// POSITIVE PAIRS: put "1" (existing interaction) in the end
				if (pos)
					outputLine[FEATURES+1]="1";
				else 
					outputLine[FEATURES+1]="0";
					
				for (int i=1; i<FEATURES+1; i++) 
				    outputLine[i]=relationCount[i-1]+"";
				    
				writer.writeNext(outputLine);
			}
			writer.close();	

		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String [] csvFirstLine(Terms terms) {
		
		try {
			String [] relationsInPath  = {"rel1","rel2","rel3"};//,"rel4"};
			String [] firstLine = new String[FEATURES+2];
			
			firstLine[0] = "DB_PAIR";
			for (int i=0 ; i<relationsInPath.length; i++) {   //runs 3 times for 3 relations in path
				String tag = relationsInPath[i];
	
				int j=i*RELATIONS;
	
			    for(String relationType:terms.relationTypes){
			    	firstLine[j+1]=tag+"_"+relationType;
			    	j++;
			    }
			}
			firstLine[FEATURES+1] = "INTERACTS";  
			return firstLine;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
