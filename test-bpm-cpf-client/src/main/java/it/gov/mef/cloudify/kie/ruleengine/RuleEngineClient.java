package it.gov.mef.cloudify.kie.ruleengine;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.RuleServicesClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import it.gov.mef.cloudify.kie.KieServiceManagerDelegate;
import it.gov.mef.cloudify.process.types.Suggestion;
import it.gov.mef.cloudify.process.types.SuggestionRequest;
import it.gov.mef.cloudify.process.types.SuggestionResponse;

@Component
@Lazy
@DependsOn("kieServiceManager")
public class RuleEngineClient {

	@Autowired
	private KieServiceManagerDelegate serviceManager;
	
	private RuleServicesClient ruleServicesClient;
	
	@PostConstruct
	public void initRuleServicesClient() {
		
		 RuleServicesClient ruleServicesClient = serviceManager.getKieServicesClient().getServicesClient(RuleServicesClient.class);
		 this.ruleServicesClient = ruleServicesClient;
	}
	
	public Suggestion getSuggestion(String keyword) {
        SuggestionRequest suggestionRequest = new SuggestionRequest();
        suggestionRequest.setKeyword(keyword);
        suggestionRequest.setKeyword("Zombie");
        
        KieCommands commands = serviceManager.getCommands();
        
        List<Command<?>> cmds = new ArrayList<Command<?>>();
        cmds.add(commands.newInsert(suggestionRequest));
        cmds.add(commands.newFireAllRules());
        cmds.add(commands.newQuery("suggestion", "get suggestion"));
        BatchExecutionCommand batch = commands.newBatchExecution(cmds, "LibraryRuleSession");
        ExecutionResults execResults;
      //we are never inside a local KIE Session... 
//        if (appcfg.getKieSession() != null) {
//            execResults = appcfg.getKieSession().execute(batch);
//        } else {
            ServiceResponse<ExecutionResults> serviceResponse = ruleServicesClient.executeCommandsWithResults("processserver-library", batch);
            //logger.info(String.valueOf(serviceResponse));
            execResults = serviceResponse.getResult();
     //   }
        QueryResults queryResults = (QueryResults)execResults.getValue("suggestion");
        
        if (queryResults != null) {
            for (QueryResultsRow queryResult : queryResults) {
                SuggestionResponse suggestionResponse = (SuggestionResponse)queryResult.get("suggestionResponse");
                if (suggestionResponse != null) {
                    return suggestionResponse.getSuggestion();
                }
            }
        }
        return null;
    }
}
