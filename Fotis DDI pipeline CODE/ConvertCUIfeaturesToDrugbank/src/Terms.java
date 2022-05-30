import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

public class Terms {

	public List<String>  drugs = new ArrayList<String>();
	public List<String> relationTypes = new ArrayList<String>();
	public HashMap<String,String> normalizedNames= new HashMap<String, String>();
	
	public Terms(String drugsListFile) {
		//initializeEncodings

		 AddRelationCodes();
		 addNormalizedNames();
		 
		 //currently not needed...
		// AddDrugs(drugsListFile);
		 
	}
	
	
	
	void AddDrugs(String drugsListF) {
	
		
		System.out.println("***************************");
		try {
			CSVReader reader = new CSVReader(new FileReader(drugsListF));
			
			int i=0;
			String [] nextLine;	
			while ((nextLine = reader.readNext()) != null) { 
				
				if (i++==0)
					continue;
				List<String> lineAsList = new ArrayList<String>(Arrays.asList(nextLine));
			    String exists_in_literature = lineAsList.get(1);
	
			    if (exists_in_literature.equals("1")) {
			    	String drug = lineAsList.get(0);
			    	System.out.println("Add drug:  "+drug);
			    	drugs.add(drug);
			    
			    }	
		
			}	
			reader.close();

			System.out.println("Finished drug additions!");
	    	System.out.println("***************************");			
			
		} catch (Exception e) {
			e.printStackTrace();
			
		}
	}
	
	void AddRelationCodes() {
			
		relationTypes.add("ADMINISTERED_TO");
		relationTypes.add("AFFECTS");
		relationTypes.add("ASSOCIATED_WITH");
		relationTypes.add("AUGMENTS");
		relationTypes.add("CAUSES");
		relationTypes.add("COEXISTS_WITH");
		relationTypes.add("compared_with");
		relationTypes.add("COMPLICATES");
		relationTypes.add("CONVERTS_TO");
		relationTypes.add("DIAGNOSES");
		relationTypes.add("different_from");
		relationTypes.add("different_than");
		relationTypes.add("DISRUPTS");
		relationTypes.add("higher_than");
		relationTypes.add("INHIBITS");
		relationTypes.add("INTERACTS_WITH");
		relationTypes.add("IS_A");
		relationTypes.add("ISA");
		relationTypes.add("LOCATION_OF");
		relationTypes.add("lower_than");
		relationTypes.add("MANIFESTATION_OF");
		relationTypes.add("METHOD_OF");
		relationTypes.add("OCCURS_IN");
		relationTypes.add("PART_OF");
		relationTypes.add("PRECEDES");
		relationTypes.add("PREDISPOSES");
		relationTypes.add("PREVENTS");
		relationTypes.add("PROCESS_OF");
		relationTypes.add("PRODUCES");
		relationTypes.add("same_as");
		relationTypes.add("STIMULATES");
		relationTypes.add("TREATS");
		relationTypes.add("USES");
		relationTypes.add("MENTIONED_IN");
		relationTypes.add("HAS_MESH");
		
	}
	
	void addNormalizedNames() {
		normalizedNames.put("0","0");
		normalizedNames.put("ADMINISTERED_TO","ADMINISTERED_TO");
		normalizedNames.put("ADMINISTERED_TO__SPEC__", "ADMINISTERED_TO");
		normalizedNames.put("AFFECTS", "AFFECTS");
		normalizedNames.put("AFFECTS__SPEC__", "AFFECTS");
		normalizedNames.put("ASSOCIATED_WITH", "ASSOCIATED_WITH");
		normalizedNames.put("ASSOCIATED_WITH__INFER__", "ASSOCIATED_WITH");
		normalizedNames.put("ASSOCIATED_WITH__SPEC__", "ASSOCIATED_WITH");
		normalizedNames.put("AUGMENTS", "AUGMENTS");
		normalizedNames.put("AUGMENTS__SPEC__", "AUGMENTS");
		normalizedNames.put("CAUSES", "CAUSES");
		normalizedNames.put("CAUSES__SPEC__", "CAUSES");
		normalizedNames.put("COEXISTS_WITH", "COEXISTS_WITH");
		normalizedNames.put("COEXISTS_WITH__SPEC__", "COEXISTS_WITH");
		normalizedNames.put("compared_with", "compared_with");
		normalizedNames.put("compared_with__SPEC__", "compared_with");
		normalizedNames.put("COMPLICATES", "COMPLICATES");
		normalizedNames.put("COMPLICATES__SPEC__", "COMPLICATES");
		normalizedNames.put("CONVERTS_TO", "CONVERTS_TO");
		normalizedNames.put("CONVERTS_TO__SPEC__", "CONVERTS_TO");
		normalizedNames.put("DIAGNOSES", "DIAGNOSES");
		normalizedNames.put("DIAGNOSES__SPEC__", "DIAGNOSES");
		normalizedNames.put("different_from", "different_from");
		normalizedNames.put("different_from__SPEC__", "different_from");
		normalizedNames.put("different_than", "different_than");
		normalizedNames.put("different_than__SPEC__", "different_than");
		normalizedNames.put("DISRUPTS", "DISRUPTS");
		normalizedNames.put("DISRUPTS__SPEC__", "DISRUPTS");
		normalizedNames.put("higher_than", "higher_than");
		normalizedNames.put("higher_than__SPEC__", "higher_than");
		normalizedNames.put("INHIBITS", "INHIBITS");
		normalizedNames.put("INHIBITS__SPEC__", "INHIBITS");
		normalizedNames.put("INTERACTS_WITH", "INTERACTS_WITH");
		normalizedNames.put("INTERACTS_WITH__INFER__", "INTERACTS_WITH");
		normalizedNames.put("INTERACTS_WITH__SPEC__", "INTERACTS_WITH");
		normalizedNames.put("IS_A", "IS_A");
		normalizedNames.put("ISA", "ISA");
		normalizedNames.put("LOCATION_OF", "LOCATION_OF");
		normalizedNames.put("LOCATION_OF__SPEC__", "LOCATION_OF");
		normalizedNames.put("lower_than", "lower_than");
		normalizedNames.put("lower_than__SPEC__", "lower_than");
		normalizedNames.put("MANIFESTATION_OF", "MANIFESTATION_OF");
		normalizedNames.put("MANIFESTATION_OF__SPEC__", "MANIFESTATION_OF");
		normalizedNames.put("METHOD_OF", "METHOD_OF");
		normalizedNames.put("METHOD_OF__SPEC__", "METHOD_OF");
		normalizedNames.put("OCCURS_IN", "OCCURS_IN");
		normalizedNames.put("OCCURS_IN__SPEC__", "OCCURS_IN");
		normalizedNames.put("PART_OF", "PART_OF");
		normalizedNames.put("PART_OF__SPEC__", "PART_OF");
		normalizedNames.put("PRECEDES", "PRECEDES");
		normalizedNames.put("PRECEDES__SPEC__", "PRECEDES");
		normalizedNames.put("PREDISPOSES", "PREDISPOSES");
		normalizedNames.put("PREDISPOSES__SPEC__", "PREDISPOSES");
		normalizedNames.put("PREVENTS", "PREVENTS");
		normalizedNames.put("PREVENTS__SPEC__", "PREVENTS");
		normalizedNames.put("PROCESS_OF", "PROCESS_OF");
		normalizedNames.put("PROCESS_OF__SPEC__", "PROCESS_OF");
		normalizedNames.put("PRODUCES", "PRODUCES");
		normalizedNames.put("PRODUCES__SPEC__", "PRODUCES");
		normalizedNames.put("same_as", "same_as");
		normalizedNames.put("same_as__SPEC__", "same_as");
		normalizedNames.put("STIMULATES", "STIMULATES");
		normalizedNames.put("STIMULATES__SPEC__", "STIMULATES");
		normalizedNames.put("TREATS", "TREATS");
		normalizedNames.put("TREATS__INFER__", "TREATS");
		normalizedNames.put("TREATS__SPEC__", "TREATS");
		normalizedNames.put("USES", "USES");
		normalizedNames.put("USES__SPEC__", "USES");
		normalizedNames.put("MENTIONED_IN", "MENTIONED_IN");
		normalizedNames.put("HAS_MESH", "HAS_MESH");
		
	}
}
