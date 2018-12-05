package mibs.asterisk.agi.main;

public enum AgentState {

	REMOVED("AGENT_REMOVED"), ADDED("AGENT_ADDED"), FAILED("FAILED");
	private String actionResult;
	private AgentState(String s) {
		actionResult = s;
	}
	public String getActionResult() {
		return actionResult;
	}
	
}
