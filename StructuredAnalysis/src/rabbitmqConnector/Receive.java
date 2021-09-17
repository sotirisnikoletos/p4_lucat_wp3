/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabbitmqConnector;

/**
 *
 * @author tasosnent
 */
import com.rabbitmq.client.*;

import java.io.IOException;
/**
 * A listener waiting for the message of completion, like the orchestrator, just to test the module communication locally. 
 * @author tasosnent
 */
public class Receive {

  private final static String QUEUE_NAME = "iasis.orchestrator.queue";
  private final static String EXCHANGE_NAME = "iasis.direct";
  private final static String ROUTING_KEY = "iasis.orchestrator.routingkey";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);
    channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
          throws IOException {
        String message = new String(body, "UTF-8");
        System.out.println(" [x] Received '" + message + "'");
      }
    };
    channel.basicConsume(QUEUE_NAME, true, consumer);
  }
}