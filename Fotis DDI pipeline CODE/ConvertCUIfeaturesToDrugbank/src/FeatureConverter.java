//import java.io.PrintWriter;
import java.util.HashMap;

public class FeatureConverter {

	public static HashMap<String, String> DBids_CUIs = new HashMap<String, String>();
	

	public static void main (String[] args){
	    
		String dtiFolder = args[0];
		//task=DDI or DTI
		String task= args[1]; 
		
		CUIsToDrugbankIdMapper.mapIds(dtiFolder, task);
    	//Now our HashMap objects have saved all cui-Drugbank id pairs...
 	   
		FileCreator fc = new FileCreator();
		//convert to drugbank pair features for positive pairs
		fc.createFeaturesFile(dtiFolder, DBids_CUIs, task, true);
		//convert to drugbank pair features for negative pairs
		fc.createFeaturesFile(dtiFolder, DBids_CUIs, task, false);
	}
	

}
