package it.gov.mef.cloudify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.gov.mef.cloudify.kie.bpm.BPMClient;
import it.gov.mef.cloudify.kie.ruleengine.RuleEngineClient;
import it.gov.mef.cloudify.process.types.Loan;
import it.gov.mef.cloudify.process.types.Suggestion;

@RestController
public class ServiceController {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private BPMClient bpmClient;
	
	@Autowired
	private RuleEngineClient ruleEngineClient;
	
	@RequestMapping("/")
	public String hello() {
		return "L'inverno è arrivato, è un processo..";
	}
	
	@RequestMapping(value = "/startProcess/acquireLoan/{isbn}", 
			produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
	public ResponseEntity<Loan> acquireLoan(@PathVariable("isbn") String isbn){
		
		logger.info("invoking REST service acquireLoan, path variable: " + isbn);
		
		Loan loan = null;
		try {
			loan = bpmClient.attemptLoan(isbn);
		} catch (Exception e) {
			logger.error("error attempt loan with isbn: " + isbn + ", cause ex: ", e);
			
			Loan errorLoan = new Loan();
			errorLoan.setApproved(false);
			
			return new ResponseEntity<Loan>(errorLoan, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<Loan>(loan, HttpStatus.OK);
	}	
	
	@RequestMapping(value = "/process/returnLoan/", 
			produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	public ResponseEntity<Loan> returnLoan(@RequestBody Loan loan){
		
		logger.info("invoking REST service returnLoan, loan: " + loan);
		
		Loan acknowledgedLoan = new Loan();
		acknowledgedLoan.setBook(loan.getBook());
		acknowledgedLoan.setId(loan.getId());
		acknowledgedLoan.setNotes(loan.getNotes());
		
		boolean acknowledge = bpmClient.returnLoan(loan);
		
		try {
			if(acknowledge) {
				logger.info("Book [" + acknowledgedLoan.getBook().getIsbn() + "] retired, thanks");
				
				
				return new ResponseEntity<Loan>(loan, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error("error bpm return loan cause ex: ", e);
			return new ResponseEntity<Loan>(acknowledgedLoan, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<Loan>(loan, HttpStatus.NOT_ACCEPTABLE);
	}	
	
	@RequestMapping(value = "/executeRule/getSuggestion", 
			produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
	public ResponseEntity<Suggestion> getSuggestion(
			@RequestParam(value="keyword", required=false) String keyword){
		
		logger.info("invoking REST service getSuggestion, request params: keyword=" + keyword);
		
		Suggestion suggestion = ruleEngineClient.getSuggestion(keyword);
		
		try {
			if(suggestion != null) {
			
				return new ResponseEntity<Suggestion>(suggestion, HttpStatus.OK);
			}
			else {
				suggestion = new Suggestion();
				
			    return new ResponseEntity<Suggestion>(suggestion, HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			logger.error("error rule based suggestion cause ex: ", e);
			return new ResponseEntity<Suggestion>(new Suggestion(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}	
}
