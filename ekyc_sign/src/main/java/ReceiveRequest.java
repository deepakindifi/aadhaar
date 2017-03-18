import com.rabbitmq.client.*;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class ReceiveRequest {

    public static final String EKYC_QUEUE = "ekyc_call";
    public static final String ESIGN_QUEUE = "esign_call";
    public static final String GENERATE_DOCUMENTS_QUEUE = "annotate_documents_pdf";
    public static final String LOAN_AGREEMENT_QUEUE = "loan_agreement";

    public static String NODE_MACHINE_ADDRESS;
    public static String RABBITMQ_HOST;
    public static String RABBITMQ_USERNAME;
    public static String RABBITMQ_PASSWORD;
    public static String RABBITMQ_ADDRESS;
    public static String S3_BUCKET;
    public static String AWS_ACCESS_KEY;
    public static String AWS_SECRET_KEY;


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
            channel.basicQos(10);

            initializeConsumer(channel, EKYC_QUEUE);
            initializeConsumer(channel, ESIGN_QUEUE);
            initializeConsumer(channel, GENERATE_DOCUMENTS_QUEUE);
            initializeConsumer(channel, LOAN_AGREEMENT_QUEUE);

            System.out.println("---- LISTENING ------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initializeConsumer(Channel channel, String queueName) throws IOException {
        channel.queueDeclare(queueName, true, false, false, null);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(properties.getCorrelationId())
                        .build();

                System.out.println("Message Received");
                String result = "";
                try {
                    String message = new String(body,"UTF-8");
                    RequestProcessorFactory requestProcessorFactory = new RequestProcessorFactory();
                    RequestProcessor requestProcessor = requestProcessorFactory.getRequestProcessor(queueName);
                    if(requestProcessor != null) {
                        JSONObject response = requestProcessor.processRequest(message);
                        result = (String)response.get("message");
                    }
                }
                catch (RuntimeException e){
                    System.out.println(" [.] " + e.toString());
                }
                finally {
                    channel.basicPublish("", properties.getReplyTo(), replyProps, result.getBytes("UTF-8"));
                    System.out.println("Message Processed " + result);
                }
            }
        };

        channel.basicConsume(queueName, true, consumer);
    }

    public static void main(String[] argv) throws Exception {

        String propertyFile = "app-" + System.getProperty("env") + ".properties";
        System.out.println(propertyFile);
        InputStream s = ReceiveRequest.class.getClassLoader().getResourceAsStream(propertyFile);
        Properties p = new Properties();
        p.load(s);
        System.out.println("HERE");
        NODE_MACHINE_ADDRESS = p.getProperty("node_machine_address");
        RABBITMQ_HOST = p.getProperty("rabbitmq_host");
        RABBITMQ_USERNAME = p.getProperty("rabbitmq_username");
        RABBITMQ_PASSWORD = p.getProperty("rabbitmq_password");
        S3_BUCKET = p.getProperty("s3_bucket");
        AWS_ACCESS_KEY = p.getProperty("aws_access_key");
        AWS_SECRET_KEY = p.getProperty("aws_secret_key");

        initializeChannel();
        
    }
}
