  import com.rabbitmq.client.*;
  import org.json.simple.*;
  import java.awt.Color;
  import org.json.simple.parser.*;
  import java.io.*;
  import java.io.IOException;
  import java.util.Properties;

  import org.json.simple.*;
  import org.json.simple.parser.*;

  public class ReceiveRequest {
    private static final String EXCHANGE_NAME = "indifi_durable";

    public static void sendResponse(String message, String key) {
     Connection connection = null;
     Channel channel = null;
     try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost("localhost");
      factory.setUsername("test");
      factory.setPassword("test");
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


  public static void main(String[] argv) throws Exception {

   /* String propertyFile = "app-" + System.getProperty("env") + ".properties";
    InputStream s = ReceiveRequest.class.getClassLoader().getResourceAsStream(propertyFile);
    Properties p = new Properties();
      p.load(s);
      System.out.println(p.getProperty("demo"));*/

    ProcessEKycRequest req = new ProcessEKycRequest();
    ConnectionFactory factory = new ConnectionFactory();
    System.out.println("trying to connec");
    factory.setHost("localhost");
    factory.setUsername("test");
    System.out.println("connected");
    factory.setPassword("test");
    factory.setAutomaticRecoveryEnabled(true);
    factory.setTopologyRecoveryEnabled(true);
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "topic",true);
    String queueName = channel.queueDeclare().getQueue();
    channel.queueBind(queueName, EXCHANGE_NAME, "ekyc_call");
    channel.queueBind(queueName, EXCHANGE_NAME, "esign_call");
    channel.queueBind(queueName, EXCHANGE_NAME, "annotate_pdf");
    channel.queueBind(queueName, EXCHANGE_NAME, "generate_pdf");
    channel.queueBind(queueName, EXCHANGE_NAME, "loan_agreement");

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope,
       AMQP.BasicProperties properties, byte[] body) throws IOException {

        System.out.println("message received");
        String message = new String(body, "UTF-8");

        if(envelope.getRoutingKey().equals("ekyc_call")) {
         ProcessEKycRequest req = new ProcessEKycRequest();
         System.out.println("called"+ message);
         String[] response = req.generateEncryptedPayload(message);
         ReceiveRequest.sendResponse(response[0],response[1]);
       } else if(envelope.getRoutingKey().equals("loan_agreement")) {
         LoanAgreement loanAgreement = new LoanAgreement();
         try {
          System.out.println("processing loan agreement");
          String[] response = loanAgreement.annotate(message);
          System.out.println("processed");
          ReceiveRequest.sendResponse(response[0],response[1]);
        }catch(Exception Ex) {
          System.out.println("accepted the error");
          System.out.println(Ex);
        }
      } else if(envelope.getRoutingKey().equals("esign_call")) {
         ProcessESignRequest req = new ProcessESignRequest();
         System.out.println("called"+ message);
         JSONObject response = req.esign(message);
	       System.out.println(response.toString());
         ReceiveRequest.sendResponse(response.toString(),(String)response.get("topic"));
      } 
      else if(envelope.getRoutingKey().equals("generate_pdf")) {
         PdfGenerator req = new PdfGenerator();
         JSONObject response = req.generatePdf(message);
         System.out.println(response.toString());
         ReceiveRequest.sendResponse(response.toString(),"process_generated_pdf");
      } 
      else {
       Annotator annotator = new Annotator();
       try {
        System.out.println("processing");
        String[] response = annotator.annotate(message);
        System.out.println("processed");
        ReceiveRequest.sendResponse(response[0],response[1]);
      }catch(Exception Ex) {
        System.out.println("accepted");
        System.out.println(Ex);
      }
    }

    System.out.println(" [x] Received '" + envelope.getRoutingKey() + "'");

  }
};
channel.basicConsume(queueName, true, consumer);
}
}
