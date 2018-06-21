package it.gov.mef.cloudify.kie.bpm;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.kie.server.client.ProcessServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import it.gov.mef.cloudify.kie.KieServiceManagerDelegate;
import it.gov.mef.cloudify.process.types.Loan;
import it.gov.mef.cloudify.process.types.LoanRequest;
import it.gov.mef.cloudify.process.types.LoanResponse;
import it.gov.mef.cloudify.process.types.ReturnRequest;
import it.gov.mef.cloudify.process.types.ReturnResponse;

@Component
@DependsOn("kieServiceManager")
public class BPMClient {

	@Autowired
	private KieServiceManagerDelegate serviceManager;
	private ProcessServicesClient processServicesClient;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@PostConstruct
	public void initProcessServicesClient() {
		
		ProcessServicesClient processServicesClient = serviceManager.getKieServicesClient().getServicesClient(ProcessServicesClient.class);
		this.processServicesClient = processServicesClient;
		
		logger.info("BPM client correctly initialized");
	}
	
	public Loan attemptLoan(String isbn) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setIsbn(isbn);
        parameters.put("loanRequest", loanRequest);
        //we are never inside a local KIE Session... 
        LoanResponse loanResponse;
//        if (appcfg.getKieSession() != null) {
//            KieSession kieSession = appcfg.getKieSession();
//            WorkflowProcessInstance procinst = (WorkflowProcessInstance)kieSession.startProcess("LibraryProcess", parameters);
//            loanResponse = (LoanResponse)procinst.getVariable("loanResponse");
//        } else {           
         Long pid = processServicesClient.startProcess("processserver-library", "LibraryProcess", parameters);
         loanResponse = (LoanResponse)processServicesClient.getProcessInstanceVariable("processserver-library", pid, "loanResponse");
        //}
        return loanResponse != null ? loanResponse.getLoan() : null;
    }

    public boolean returnLoan(Loan loan) {
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setLoan(loan);
        ReturnResponse returnResponse;
      //we are never inside a local KIE Session... 
//        if (appcfg.getKieSession() != null) {
//            KieSession kieSession = appcfg.getKieSession();
//            WorkflowProcessInstance procinst = (WorkflowProcessInstance)kieSession.getProcessInstance(loan.getId());
//            procinst.signalEvent("ReturnSignal", returnRequest);
//            returnResponse = (ReturnResponse)procinst.getVariable("returnResponse");
//        } else {
            
            processServicesClient.signalProcessInstance("processserver-library", loan.getId(), "ReturnSignal", returnRequest);
            //returnResponse = (ReturnResponse)procserv.getProcessInstanceVariable("processserver-library", loan.getId(), "returnResponse");
            returnResponse = new ReturnResponse();
            returnResponse.setAcknowledged(true);
      //  }
        return returnResponse != null ? returnResponse.isAcknowledged() : false;
    }
}
