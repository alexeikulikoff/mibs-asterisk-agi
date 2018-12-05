package mibs.asterisk.agi.main;

import java.util.Set;
import java.util.TreeSet;

public class QueueContents {

	private Set<CurrentQueue> queuesResponce;;
	public QueueContents() {
		queuesResponce = new TreeSet<>();
	}
	public void addQueueResponce(CurrentQueue qr) {
		queuesResponce.add(qr);
	}
	@Override
	public String toString() {
		return "QueuesResponce [" + queuesResponce + "]";
	}
	public boolean isContain(String queueName, String memberName) {
		for(CurrentQueue qr : queuesResponce ) {
			if (qr.getQueue().equals(queueName)) {
				return qr.isContainMember(memberName);
			}
		}
		return false;
	}
	
}
