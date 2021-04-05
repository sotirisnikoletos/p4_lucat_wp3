/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabbitmqConnector;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import java.math.BigInteger;
import java.util.concurrent.TimeoutException;
import org.json.simple.JSONObject;
/**
 * A Messenger sending a message with a jobID, like the orchestrator, just to test the module communication locally. 
 * @author tasosnent
 */
public class Send {

//    Literature Harvester
  private final static String QUEUE_NAME = "iasis.literature_harvester.queue";
  private final static String EXCHANGE_NAME = "iasis.direct";
  private final static String ROUTING_KEY = "iasis.literature_harvester.routingkey";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws java.io.IOException, TimeoutException {
        // TODO code application logic here
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
//        String message = "jobID 02";
//        byte[] message =  BigInteger.valueOf(2).toByteArray();
        JSONObject responce =  new JSONObject();
        responce.put("jobID",  new Integer(17)); 
        String message = responce.toJSONString();
//        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
        channel.close();
        connection.close();
    }
    
}
