import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExtractCUIs {

	public static HashMap<String, List<String>> drugCUIs = new HashMap<String, List<String>>();
	
	public static void main(String [] args) {
		
		
    	try {
    		
        	String drugPairsFolder = args[0];
        	String outputjFolder = args[1];
    		
        	//Open the output CSV file to write positive UMLS cui pairs -> input for ProcessNeo4j project
            PrintWriter writerPos= new PrintWriter(outputjFolder+"PositivePairs/drug-pairs.csv", "UTF-8");
        	//Open the output CSV file to write negative UMLS cui pairs -> input for ProcessNeo4j project
            PrintWriter writerNeg= new PrintWriter(outputjFolder+"NegativePairs/drug-pairs.csv", "UTF-8");
             	
    		//Open the CSV file with the DrugBank pairs that interact/don't interact
    		String inputLine;
    		int l=0;
    		FileReader filerdr = new FileReader(drugPairsFolder+"LC_DDIs_GroundTruth.csv");
    		BufferedReader in = new BufferedReader(filerdr);
    		while(( inputLine = in.readLine() ) != null) {
    	        String[] line = inputLine.split(",");  
    	        
    	        if (l++==0)
    	        	continue;
    	        
    	        //include only LC-related drug pairs (disease-specific approach)
    	        String LC_related = line[3];
    	        if (LC_related.equals("0"))
    	        	continue;
  
    	        String drug1 = line[0];
    	        String drug2 = line[1];
    	        
    	        List<String> drug1_cuis = drugCUIs.get(drug1);
    	        List<String> drug2_cuis = drugCUIs.get(drug2);
    	        
    	        if (drug1_cuis==null) {
    	        	drug1_cuis = getDrugCUIs(drug1, drugPairsFolder+"CACHE_DB_CUI_MAPP.tsv");
    	        	drugCUIs.put(drug1, drug1_cuis);
    	        }	
    	        if (drug2_cuis==null) {
        	        drug2_cuis = getDrugCUIs(drug2, drugPairsFolder+"CACHE_DB_CUI_MAPP.tsv");
        	    	drugCUIs.put(drug2, drug2_cuis);
    	        }
    	        for (int i = 0; i < drug1_cuis.size(); i++) {
    	        	for (int j = 0; j < drug2_cuis.size(); j++) {
		    	        //deciding interaction from the oldest Drugbank version
		    	        String interaction = line[4];
		    	        if (interaction.equals("0"))
		    	        	writerNeg.println(drug1_cuis.get(i)+","+drug2_cuis.get(j));
		    	        else if (interaction.equals("1"))
		    	        	writerPos.println(drug1_cuis.get(i)+","+drug2_cuis.get(j));
		    	        else {
		    	        	System.out.println("Interaction information is wrong for drug pair: "+drug1+"_"+drug2);
		    	        	break;
		    	        }
    	        	}
    	        }
    	        //just take 100 first drug pairs for testing now
    	        //FOT: remove this line 
    	        if (l==30)
    	        	break;
    		}
    		
    		System.out.println("Processed successfully "+l+" lines...");
    		in.close();
    		filerdr.close();
    		
    		writerPos.close();
    		writerNeg.close();
    		
        }catch(Exception e) {
        	e.printStackTrace();
        }
	}
	
	public static List<String> getDrugCUIs (String drug, String mappingTSVfile) throws Exception {
		
		List<String> cuis = new ArrayList<String>();
		
		//Open the TSV file with the DrugBank-UMLS mapping
		BufferedReader br = new BufferedReader(new FileReader(mappingTSVfile));
		String line;
		boolean foundDBid=false;
		while ((line = br.readLine()) != null ){
		        String[] values = line.split("\t");
		        
		        if(values[0].equals(drug)) {
		        	foundDBid=true;
		        	cuis.add(values[1]);
		        	System.out.println("Drug: "+drug+" , has CUI: "+values[1]);
		        }
		        else if (foundDBid)  //dont need to search the rest of the DrugBank ids....
		        	break;
		}
		br.close();
		return cuis;
	}
}
