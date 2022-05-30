import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


public class ProcessCSV {
	
	public static final int max_length = 3;
	
	
	public static void main(String args []) {
	
		String DDIfolder = args[0];

		String inputFolderNeg = DDIfolder+"ProcessNeo4j/NegativePairs/";//ExtractedPath_Files/C0023791_C0033148_paths_3.csv";
		String inputFolder = DDIfolder+"ProcessNeo4j/PositivePairs/";//ExtractedPath_Files/C0085272_C0008845_paths_3.csv";
		
		//CURRENTLY NOT USED! - only useful to map DB ids to UMLS cuis
		String drugsListFile ="/media/fotis/OS/FOT/Research/Graph analysis @dimokritos/experiments/DrugsFromLiterature_v2.csv";
			
		String outputFile_ApproachB =DDIfolder+"FeatureExtraction/approachb-features-positivePairs.csv";
		String outputFileNeg_ApproachB =DDIfolder+"FeatureExtraction/approachb-features-negativePairs.csv";
		
		
		//String outputFile_ApproachC ="C:\\Users\\Fot!\\Desktop\\approachc-features-golden.csv";
		//String outputFileNeg_ApproachC ="C:\\Users\\Fot!\\Desktop\\approachc-features-negatives.csv";
		
		
		Terms terms = new Terms(drugsListFile);
		
		
		//ADDED CODE FOR OPENING DRUG PAIR FILES (POSITIVE +NEGATIVE) AND RUNNING FEATURE EXTRACTION PER PAIR
    	try {
    		
    		//write first line of Features CSV file (Positive pairs)
    		FeatureExtractApproachB.csvFirstLine(terms, outputFile_ApproachB);
    		
    		String drugPair;
    		FileReader filerdr = new FileReader(inputFolder+"drug-pairs.csv");
    		BufferedReader in = new BufferedReader(filerdr);
    		while(( drugPair = in.readLine() ) != null   ) {
    	        String[] drugs = drugPair.split(",");  
    	        String drugPairFile = inputFolder+"ExtractedPath_Files/"+drugs[0]+"_"+drugs[1]+"_paths_"+max_length+".csv";

    	        System.out.println("Examining positive Drug pair: "+drugs[0]+" - "+ drugs[1]);
			//training features building with golden/negative  samples  --Approach B
    	        FeatureExtractApproachB.extractFeaturesFromFile(terms, drugPairFile, drugs[0], drugs[1], outputFile_ApproachB, 1);
    		}
    		
    		//write first line of Features CSV file (Negative pairs)
    		FeatureExtractApproachB.csvFirstLine(terms, outputFileNeg_ApproachB);

    		filerdr = new FileReader(inputFolderNeg+"drug-pairs.csv");
    		in = new BufferedReader(filerdr);
    		while(( drugPair = in.readLine() ) != null   ) {
    	        String[] drugs = drugPair.split(",");
      	        String drugPairFile = inputFolderNeg+"ExtractedPath_Files/"+drugs[0]+"_"+drugs[1]+"_paths_"+max_length+".csv";

    	        //if the file doesnt exist, the path extraction script did not find one of the two drugs in Neo4j    
    	        if (!new File(drugPairFile).isFile())
    	        	continue;
    	        System.out.println("Examining negative Drug pair: "+drugs[0]+" - "+ drugs[1]);
			//training features building with golden/negative  samples  --Approach B
    	        FeatureExtractApproachB.extractFeaturesFromFile(terms, drugPairFile, drugs[0], drugs[1], outputFileNeg_ApproachB, 0);
    		}
    		in.close();
    		filerdr.close();
    		
    		
        }catch(Exception e) {
        	e.printStackTrace();
        }
		 
		//training features building with golden/negative  samples  --Approach C
		
		//FeatureExtractApproachC.defineFeaturesFromFrequentPaths("C:\\FOT\\Research\\Graph analysis @dimokritos\\experiments\\features extraction\\approach c - frequent paths in golden\\IMPORTANT_PATHS.csv");
		//FeatureExtractApproachC.extractFeaturesFromFile(terms, inputFile, outputFile_ApproachC, 1);
		//FeatureExtractApproachC.extractFeaturesFromFile(terms, inputFileNeg, outputFileNeg_ApproachC, 0);
				 	
		
	}
	

	public static String CSVfileCorrect(String line, Terms terms) {
		
	    for(String relationType:terms.relationTypes){
	    	if (line.contains(relationType+"-"))
	    			line.replace(relationType+"-", relationType+",");
	    }
		
		return line;
	}

}
