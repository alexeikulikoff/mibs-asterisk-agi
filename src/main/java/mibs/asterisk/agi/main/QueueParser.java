package mibs.asterisk.agi.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueueParser {

	Map<String, Set<String>> queues = new TreeMap<>();
	public QueueParser() {
		
	}
	public void addQueue(String queue, Set<String> members) {
		queues.put(queue, members);
	}
	
	public static void main(String[] args) {
		
		
		String ops = "vip has 2 calls (max unlimited) in 'rrmemory' strategy (22s holdtime, 68s talktime), W:0, C:40834, A:6522, SL:0.0% within 0s\n";
		System.out.println(ops.matches("\\S.+\\n"));
		
		
		List<String> sb = new  ArrayList<>();
		sb.add("vip has 2 calls (max unlimited) in 'rrmemory' strategy (22s holdtime, 68s talktime), W:0, C:40834, A:6522, SL:0.0% within 0s");
		sb.add("   Members:");
		sb.add("     SIP/7860 (ringinuse disabled) (dynamic) (In use) has taken 32 calls (last was 515 secs ago)");
		sb.add("     SIP/7861 (ringinuse disabled) (dynamic) (In use) has taken 15 calls (last was 64 secs ago)");
		sb.add("     SIP/7862 (ringinuse disabled) (dynamic) (In use) has taken 22 calls (last was 40 secs ago)");
		sb.add("   Callers:");
		sb.add("callcenter has 2 calls (max unlimited) in 'rrmemory' strategy (22s holdtime, 68s talktime), W:0, C:40834, A:6522, SL:0.0% within 0s");
		sb.add("   Members:");
		sb.add("     SIP/7855 (ringinuse disabled) (dynamic) (In use) has taken 32 calls (last was 515 secs ago)");
		sb.add("     SIP/7859 (ringinuse disabled) (dynamic) (In use) has taken 15 calls (last was 64 secs ago)");
		sb.add("     SIP/7850 (ringinuse disabled) (dynamic) (In use) has taken 22 calls (last was 40 secs ago)");
		sb.add("   Callers:");
		sb.add("common has 2 calls (max unlimited) in 'rrmemory' strategy (22s holdtime, 68s talktime), W:0, C:40834, A:6522, SL:0.0% within 0s");
		sb.add("   No Members");
		sb.add("   No Callers");
		boolean queueFlag = false;
		boolean memberFlag = false;
		CurrentQueue queue = null;
		QueueContents responce = new QueueContents();
		for(String b : sb) {
			if (b.toString().matches("\\S.+") & !b.contains("--END COMMAND--") & !b.contains("Response:") & !b.contains("Privilege:")){
					queueFlag = true;
					queue = new CurrentQueue(b.split("\\s+")[0]);
			}	
			if (b.contains("Members") & queueFlag){
					memberFlag = true;
			}
			if (b.contains("Callers") & memberFlag){
					queueFlag = false ;
					memberFlag = false;
					responce.addQueueResponce(queue);
			}
			if (memberFlag & b.matches("\\s{1,6}SIP\\/\\d{1,4}.+")){
					queue.addMember( b.split("\\s+")[1] );
			}
	
		}
	
		boolean res = responce.isContain("callcenter", "SIP/7859");
		
	
		
	}
}
