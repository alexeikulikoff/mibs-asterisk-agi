package mibs.asterisk.agi.main;

public class Сentconf {
	private Long agentid;
	private Long queueid;
	private int penalty;
	public Long getAgentid() {
		return agentid;
	}
	public void setAgentid(Long agentid) {
		this.agentid = agentid;
	}
	public Long getQueueid() {
		return queueid;
	}
	public void setQueueid(Long queueid) {
		this.queueid = queueid;
	}
	public int getPenalty() {
		return penalty;
	}
	public void setPenalty(int penalty) {
		this.penalty = penalty;
	}
	
}
