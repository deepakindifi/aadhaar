public class RequestProcessorFactory {
    public RequestProcessor getRequestProcessor(String topic) {
    	RequestProcessor requestProcessor = null;
        switch (topic) {
            case ReceiveRequest.EKYC_TOPIC:
            	requestProcessor = new EKycRequestProcessor();
            	break;
            case ReceiveRequest.ESIGN_TOPIC:
            	requestProcessor = new ESignRequestProcessor();
            	break;
            case ReceiveRequest.GENERATE_DOCUMENTS_TOPIC:
            	requestProcessor = new DocumentsRequestProcessor();
            	break;
        }
        return requestProcessor;
    }
}