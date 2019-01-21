package mibs.asterisk.agi.main;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;



public class ActionQueueShow extends AbstractAction implements Action{

	public ActionQueueShow(Socket s,String queue, String peer) throws IOException {
		super(s, queue, peer);
	}
	@Override
	public void doCommand() throws IOException {
		writer.write("Action: COMMAND\r\nActionID:12345\r\ncommand: queue show\r\n\r\n");
		writer.flush();
	}
	@Override
	public Optional<Action> getResponce() throws IOException {
		doCommand();
		Action action = null;
		QueueContents content = new QueueContents();
		CurrentQueue currentQueue = null;
		boolean queueFlag = false;
		boolean memberFlag = false;
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			
			if (line.matches("\\S.+") & !line.contains("--END COMMAND--") & !line.contains("Response:") & !line.contains("Privilege:")){
				queueFlag = true;
				currentQueue = new CurrentQueue(line.split("\\s+")[0]);
			}	
			if (line.contains("Members") & queueFlag){
				memberFlag = true;
			}
			if (line.contains("Callers") & memberFlag){
				queueFlag = false ;
				memberFlag = false;
				content.addQueueResponce(currentQueue);
			}
			if (memberFlag & line.matches("\\s{1,6}SIP\\/\\d{1,4}.+")){
				currentQueue.addMember( line.split("\\s+")[1] );
			}
			if (line.contains("--END COMMAND--")) break;
		}
		action = content.isContain(queue, peer) ? new ActionRemove(socket,queue,peer) : new ActionAdd(socket,queue,peer);
		return (action != null) ? Optional.of(action) : Optional.empty();
	}

}
