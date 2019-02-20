package mibs.asterisk.agi.main;

import java.io.IOException;
import java.util.Optional;



public interface Action {
	
	void doCommand() throws IOException;

	
	Optional<Action> getResponce() throws IOException, AuthenticationFailedException;
	
	default AgentState getActionResult() {
		return AgentState.FAILED;
	}
}
