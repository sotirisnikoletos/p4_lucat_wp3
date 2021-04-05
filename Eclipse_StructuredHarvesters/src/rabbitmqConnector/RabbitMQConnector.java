/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabbitmqConnector;

//add _Libs\RabbitMQ java client\ *.jar
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import drugbankHarvester.DrugbankHarvester;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import oboHarvester.OBOHarvester;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import yamlSettings.Settings;

/**
 *
 * @author tasosnent
 */
public class RabbitMQConnector {
    //Hardecoded values
    private final static boolean debugMode = true; //Enables printing of messages for normal functions
    private static String pathDelimiter = "\\";    // The delimiter in this system (i.e. "\\" for Windows, "/" for Unix)
    
    private static Settings s; // The settings for the module
    
    public RabbitMQConnector(Settings s){
        this.s = s;
    }
    
    public void send(String message, Settings s) throws IOException, TimeoutException{
//        queue_name = "iasis.orchestrator.queue";
//        String exchange = "iasis.direct";
//        String routingKey = "iasis.semantic_enrichment.queue";
        String queue_name = s.getProperty("rabbitmq/queueSent").toString();
        String exchange = s.getProperty("rabbitmq/exchange").toString();
        String routingKey = s.getProperty("rabbitmq/routingkeySent").toString();
        String userName = s.getProperty("rabbitmq/username").toString();
        String password = s.getProperty("rabbitmq/password").toString();
        String hostName = s.getProperty("rabbitmq/host").toString();
        int portNumber = Integer.parseInt(s.getProperty("rabbitmq/port").toString());
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(userName);
        factory.setPassword(password);
//        factory.setVirtualHost(virtualHost);
        factory.setHost(hostName);
        factory.setPort(portNumber);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(queue_name, true, false, false, null);
        //exchange - the exchange to publish the message to
        //routingKey - the routing key
        //props - other properties for the message - routing headers etc
        //body - the message body
        channel.basicPublish(exchange, routingKey, null, message.getBytes());
//        channel.basicPublish("", queue_name, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
        channel.close();
        connection.close();
    }
    
    public void receiveMessages(Settings s) throws IOException, TimeoutException {
        String queue_name = s.getProperty("rabbitmq/queueListen").toString();
        String exchange = s.getProperty("rabbitmq/exchange").toString();
        String routingKey = s.getProperty("rabbitmq/routingkeyListen").toString();
        String userName = s.getProperty("rabbitmq/username").toString();
        String password = s.getProperty("rabbitmq/password").toString();
        String hostName = s.getProperty("rabbitmq/host").toString();
        String moduleName = s.getProperty("rabbitmq/modulename").toString();
        int portNumber = Integer.parseInt(s.getProperty("rabbitmq/port").toString());
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(userName);
        factory.setPassword(password);
//        factory.setVirtualHost(virtualHost);
        factory.setHost(hostName);
        factory.setPort(portNumber);
        
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(queue_name, true, false, false, null);
        channel.exchangeDeclare(exchange, "direct", true);
        channel.queueBind(queue_name, exchange, routingKey);
        System.out.println(" [*] Waiting for messages.");

        Consumer consumer = new DefaultConsumer(channel) {
          @Override
          public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
              throws IOException {
                  String message = new String(body, "UTF-8");
                  JSONParser parser = new JSONParser();
                    try {
                        JSONObject messageJSON =  (JSONObject) parser.parse(message);
                    
    //                  int message = new BigInteger(body).intValue();
                      System.out.println(" [x] Received '" + message + "'");

                        // The file to be harvested
                        String inputResourceID = s.getProperty("inputResourceID").toString();
                        if(inputResourceID.contains("drugbank")){
                            DrugbankHarvester.harvest(s);
                        } else {
                            OBOHarvester.harvest(s);
                        }

                        RabbitMQConnector r = new RabbitMQConnector(s);
                        JSONObject responce =  new JSONObject();
                        int jobID = ((Long) messageJSON.get("jobID")).intValue();
                        responce.put("jobID", jobID);                    
                        responce.put("componentName", moduleName);
                        responce.put("message", "OK");
                        try {
                            r.send(responce.toJSONString(), s);
                        } catch (TimeoutException ex) {
                            Logger.getLogger(RabbitMQConnector.class.getName()).log(Level.SEVERE, null, ex);
                        }    
                    } catch (ParseException ex) {
                        Logger.getLogger(RabbitMQConnector.class.getName()).log(Level.SEVERE, null, ex);
                    }

          }
        };
        channel.basicConsume(queue_name, true, consumer);
    }

}
