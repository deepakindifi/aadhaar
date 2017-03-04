import com.rabbitmq.client.*;
import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.IOException;


public class ReceiveRequest {

    private static final String EXCHANGE_NAME = "indifi_durable";
    private static final String EKYC_TOPIC = "ekyc_call";
    private static final String ESIGN_TOPIC = "esign_call";
    private static final String GENERATE_DOCUMENTS_TOPIC = "generate_pdf";

    public static String NODE_MACHINE_ADDRESS;
    public static String RABBITMQ_HOST;
    public static String RABBITMQ_USERNAME;
    public static String RABBITMQ_PASSWORD;

    public static void sendResponse(String message, String key) {
        Connection connection = null;
        Channel channel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(RABBITMQ_HOST);
            factory.setUsername(RABBITMQ_USERNAME);
            factory.setPassword(RABBITMQ_PASSWORD);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setTopologyRecoveryEnabled(true);
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic",true);

            channel.basicPublish(EXCHANGE_NAME, key, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + key + "':'" + message + "'");

        }
        catch  (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (connection != null) {
                try {
                    connection.close();
                }
                catch (Exception ignore) {}
            }
        }
    }

    private static void initializeChannel() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(RABBITMQ_HOST);
        connectionFactory.setUsername(RABBITMQ_USERNAME);
        connectionFactory.setPassword(RABBITMQ_PASSWORD);
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setTopologyRecoveryEnabled(true);
        Channel channel;
        try {
            Connection connection = connectionFactory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NAME, EKYC_TOPIC);
            channel.queueBind(queueName, EXCHANGE_NAME, ESIGN_TOPIC);
            channel.queueBind(queueName, EXCHANGE_NAME, GENERATE_DOCUMENTS_TOPIC);
            Consumer consumer = initializeConsumer(channel);
            channel.basicConsume(queueName, true, consumer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    private static Consumer initializeConsumer(Channel channel) {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                System.out.println("message received");
                String message = new String(body, "UTF-8");

                RequestProcessorFactory requestProcessorFactory = new RequestProcessorFactory();
                requestProcessor = requestProcessorFactory.getRequestProcessor(envelope.getRoutingKey());
                if(requestProcessor != null) {
                    JSONObject response = requestProcessor.processRequest(message);
                    ReceiveRequest.sendResponse((String)response.get("message"), (String)response.get("topic"));
                }

                System.out.println(" [x] Received '" + envelope.getRoutingKey() + "'");

            }
        };
    }


    public static void main(String[] argv) throws Exception {

        String propertyFile = "app-" + System.getProperty("env") + ".properties";
        InputStream s = ReceiveRequest.class.getClassLoader().getResourceAsStream(propertyFile);
        Properties p = new Properties();
        p.load(s);
        NODE_MACHINE_ADDRESS = p.getProperty("node_machine_address");
        RABBITMQ_HOST = p.getProperty("rabbitmq_host");
        RABBITMQ_USERNAME = p.getProperty("rabbitmq_username");
        RABBITMQ_PASSWORD = p.getProperty("rabbitmq_password");

        initializeChannel();
        
    }
}
