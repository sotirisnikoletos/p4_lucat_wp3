/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package neo4JControler;

import java.util.ArrayList;
//Add neo4j-java-driver-1.2.1.jar 
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import yamlSettings.Settings;
import static org.apache.commons.lang.StringEscapeUtils.escapeJavaScript;

/**
 *
 * @author tasosnent
 */
public class Neo4JControler {
        // Neo4j variables
    String host = "bolt://localhost:7687";
//    String host = "bolt://127.0.0.1:7474";
    String username = "neo4j";
    String password = "testpass1";
    Driver driver;
    private Session session;
   
    
    /**
     * Default constructor using hard-coded credentials
     */
    public Neo4JControler(){
        System.out.println("Default credentials for DB are used!");
    }
    
    /**
     * Constructor using credentials
     * @param host          neo4j DB address
     * @param username      neo4j DB user name
     * @param password      neo4j DB pass
     */
    public Neo4JControler(String host,String username, String password ){
        this.host = host;
        this.username = username;
        this.password = password;
        
    }
    
    /**
     * Constructor using Settings and default setting names: neo4j/host, neo4j/port, neo4j/user, neo4j/password
     * @param s     Settings variable (from YAML file) including : neo4j/host, neo4j/port, neo4j/user, neo4j/password
     */
    public Neo4JControler(Settings s ){
        this.host = "bolt://" + s.getProperty("neo4j/host") + ":" + s.getProperty("neo4j/port");
        this.username = (String)s.getProperty("neo4j/user");
        this.password = (String)s.getProperty("neo4j/password");
    }
  
    /**
     * Constructor using Settings and default setting names
     * @param s             Settings variable (from YAML file) including : X/host, X/port, X/user, X/password
     * @param neo4jField   The settings field under which neo4j settings (i.e. host, port, user, password) can be found (e.g. neo4j)
     */
    public Neo4JControler(Settings s, String neo4jField ){
        this.host = "bolt://" + s.getProperty(neo4jField+"/host") + ":" + s.getProperty(neo4jField+"/port");
        this.username = (String)s.getProperty(neo4jField+"/user");
        this.password = (String)s.getProperty(neo4jField+"/password");
        
    }
    
    /**
     * Execute given query to Neo4j database
     * @param cypherQuery   The query to be executed
     * @return      A list of the matching records 
     */
    public StatementResult bigQuery(String cypherQuery){
        ArrayList <Record> records = new ArrayList<>();
        StatementResult result = getSession().run( cypherQuery );
        return result;
    }
    
    /**
     * Execute given query to Neo4j database
     * @param cypherQuery   The query to be executed
     * @return      A list of the matching records 
     */
    public ArrayList <Record> query(String cypherQuery){
//        tmp code    
//        System.out.println( " in query()");
        ArrayList <Record> records = new ArrayList<>();
//        System.out.println( " before run " +  getSession().isOpen());
        StatementResult result = getSession().run( cypherQuery );
//        System.out.println( " after run");
        while ( result.hasNext() )
        {
            Record record = result.next();
            records.add(record);
//            System.out.println( record.asMap());
    //        log.append(record.get( "cui" ).asString() + " " + record.get( "sem_types" ).size() );
//            System.out.println(record.get( "count" ).asString() + " " + record.get( "article.pmcid" ).asString() );
        }
        return records;
    }
    /**
     * Execute hard-coded sample query to Neo4j database for testing
     */
    public void sampleQuery(){
        System.out.println( "Executing sample query" );

        StatementResult result = getSession().run( "MATCH (n:Entity) RETURN n.cui as cui, n.sem_types as sem_types LIMIT 5 " );
        while ( result.hasNext() )
        {
        Record record = result.next();
        System.out.println( record.fields().size() );
//        log.append(record.get( "cui" ).asString() + " " + record.get( "sem_types" ).size() );
        System.out.println(record.get( "cui" ).asString() + " " + record.get( "sem_types" ).size() );
        }

    }

    
    /**
     *  Connection to Neo4j
     *      Called in this Class constructor
     */
    public void connect(){
        // Connecto to neo4j Graph DB
        driver = GraphDatabase.driver(this.host, AuthTokens.basic(this.username, this.password ) );
        setSession(driver.session());
//        log.append(" Connected to Database." + newline);
        System.out.println(" Connected to Database " + host + " as " + username + "." );
    }
    
    /**
     * Disconnected from Neo4j
     *      Called when window is closed
     */
    public void disconect(){
        getSession().close();
        driver.close();
//        log.append(" Disconnected from Database." + newline);
         System.out.println(" Disconnected from Database : " + host + " as " + username + ".");
    }
    
    public static String escapeNeo4j(String s){
//        s = s.replace("\\", "\\\\");
//        s = s.replace("'", "\'");
//        s = s.replace("\"", "\\\"");
        s = escapeJavaScript(s);
        s = s.replace("\\/", "/");
        
        return s;
    }

    /**
     * @return the session
     */
    public Session getSession() {
        return session;
    }

    /**
     * @param session the session to set
     */
    public void setSession(Session session) {
        this.session = session;
    }
}
