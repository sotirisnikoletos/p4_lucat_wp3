/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package structuredHarvester;

import java.util.Date;
import yamlSettings.Settings;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rabbitmqConnector.RabbitMQConnector;

/**
 * This is a harvester for OBO ontologies and DrugBank
 *      input   : 
 *          1)  settings.yaml should be available in the project folder containing configurations for the program to run
 *              a)  An id for the specific resource     [e.g. inputResourceID: go]
 *                  i)  For "drugbank" and "doid" the specific id should be used as inputResourceID to trigger different handling of the resources.
 *                  ii) All rest resources (regardless of their inputResourceID) are handled as OBO ontologies (i.e. mesh and go)
 *              b)  The path to the input file          [e.g. inputFilePath: '/inputDir/go.obo']
 *              c)  MongDB details                      [e.g. mongodb: {host: 143.233.226.92, port: 27017, dbname: iasis, collection: go}]
 *          2)  the actual file of the resource (i.e. the .obo or .xml file)
 *      
 *      output  :   A corresponding collection will be created in MongoDB with the resource-extracted relations
 *          
 * @author tasosnent
 */
public class StructuredHarvester {
//    private static String pathDelimiter = "\\";    // The delimiter in this system (i.e. "\\" for Windows, "/" for Unix)
    private static String pathDelimiter = "/";    // The delimiter in this system (i.e. "\\" for Windows, "/" for Unix)
    private static Settings s; // The settings for the module
    
    public static void main(String[] args) {
        String settingsFile;
        if(args.length == 1){ // command line call
            // TO DO add checks for these values
            System.err.println(" " + new Date().toString() + " \t Creating data-set using settings file : " + args[0]);
            settingsFile = args[0];
        } else { // hardcoded call with default settings file named settings.yaml available in the project main folder
            settingsFile = "." + pathDelimiter + "settings.yaml";
        }
        //Load settings from file
        s = new Settings(settingsFile);
        
        RabbitMQConnector r  = new RabbitMQConnector(s);
        try {
            r.receiveMessages(s);
        } catch (IOException ex) {
            Logger.getLogger(StructuredHarvester.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(StructuredHarvester.class.getName()).log(Level.SEVERE, null, ex);
        }                
    }
}
