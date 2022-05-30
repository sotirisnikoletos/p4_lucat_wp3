import java.io.BufferedReader;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.List;

public class CUIsToDrugbankIdMapper {

	public static void mapIds(String dtiFolder, String task) {
		try {
						
			//Open the CSV file with the DrugBank DTI pairs that interact/don't interact
	        //in order to save all DB-CUI mappings in 2 objects (drug_cuis , target_cuis )
			String inputLine;
			int l=0;
			//int noCUIsFound=0;
			FileReader filerdr = new FileReader(dtiFolder+"LC_"+task+"s_GroundTruth.csv");
			BufferedReader in = new BufferedReader(filerdr);
			while(( inputLine = in.readLine() ) != null) {
		        String[] line = inputLine.split(",");  
		        if (l++==0)
		        	continue;
		        
		        //include only LC-related drug pairs (disease-specific approach)
		        String LC_related = line[3];
		        if (LC_related.equals("0"))
		        	continue;
	
		        String drug = line[0];
		        String target = line[1];
		        //System.out.println("Tranform ids of pair: "+drug+"-"+target);
		        
		        List<String> drug_cuis;
		        List<String> target_cuis;
		        
		        if (!FeatureConverter.DBids_CUIs.containsValue(drug)) {
		        	drug_cuis = getDrugCUIs(drug, dtiFolder);
		        	if (drug_cuis.isEmpty()) {
		        		//noCUIsFound++;
		        		continue;
		        	}	
		        	else {
		        		for (String cui : drug_cuis) 
		        			FeatureConverter.DBids_CUIs.put(cui, drug);    	        		
		        	}
		        	
		        }	

		   	    if (!FeatureConverter.DBids_CUIs.containsValue(target)) {
			        if (task.equals("DDI"))
			        		target_cuis = getDrugCUIs(target, dtiFolder);
			        else		
			        		target_cuis = getTargetCUIs(target, dtiFolder);
	    	       	if (target_cuis.isEmpty()) {
	    	       		//noCUIsFound++;
	    	       		continue;
	    	       	}	
	    	       	else {
	    	       		for (String cui : target_cuis) 
	    	       			FeatureConverter.DBids_CUIs.put(cui, target);    	        		
	    	       		
	    	       	}
	    	    }	
	    	}
		  	in.close();
		  	filerdr.close();
	
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//get all LC taerget CUIs from a cached file
	public static List<String> getTargetCUIs (String target, String dtiFolder) throws Exception {
					
		List<String> cuis = new ArrayList<String>();
			
		//Open the CSV file with the DrugBank-UMLS mapping for targets
		BufferedReader br = new BufferedReader(new FileReader(dtiFolder+"LCtargets.csv"));
		String line;
		boolean foundDBid=false;
		while ((line = br.readLine()) != null ){
		     String[] values = line.split(",");
		        
		     if(values[2].equals(target)) {
		      	foundDBid=true;
		       	cuis.add(values[1]);
//		       	System.out.println("Target: "+target+" , has CUI: "+values[1]);
		     }
		     else if (foundDBid)  //dont need to search the rest of the DrugBank ids....
		        	break;
		}
		br.close();
		return cuis;
		
	}
	
	//get all LC drug CUIs from a cached file
	public static List<String> getDrugCUIs (String drug, String dtiFolder) throws Exception {
		
		List<String> cuis = new ArrayList<String>();
			
		//Open the CSV file with the DrugBank-UMLS mapping for targets
		BufferedReader br = new BufferedReader(new FileReader(dtiFolder+"CACHE_DB_CUI_MAPP.tsv"));
		String line;
		boolean foundDBid=false;
		while ((line = br.readLine()) != null ){
		     String[] values = line.split("\t");
		        
		     if(values[0].equals(drug)) {
		      	foundDBid=true;
		       	cuis.add(values[1]);
		       	//System.out.println("Drug: "+drug+" , has CUI: "+values[1]);
		     }
		     else if (foundDBid)  //dont need to search the rest of the DrugBank ids....
		        	break;
		}
		br.close();
		return cuis;
		
	}

}
