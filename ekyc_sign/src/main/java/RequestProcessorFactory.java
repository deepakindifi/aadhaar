public class RequestProcessorFactory {
    public RequestProcessor getRequestProcessor(String topic) {
    	RequestProcessor requestProcessor = null;
        switch (topic) {
            case ReceiveRequest.EKYC_QUEUE:
            	requestProcessor = new EKycRequestProcessor();
            	break;
            case ReceiveRequest.ESIGN_QUEUE:
            	requestProcessor = new ESignRequestProcessor();
            	break;
            case ReceiveRequest.GENERATE_DOCUMENTS_QUEUE:
            	requestProcessor = new AnnotateDocumentsProcessor();
            	break;
            case ReceiveRequest.LOAN_AGREEMENT_QUEUE:
            	requestProcessor = new LoanAgreementProcessor();
            	break;
        }
        return requestProcessor;
    }
}