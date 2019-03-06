package mibs.asterisk.agi.main;

import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class ActionQueueShow extends AbstractAction implements Action{
	private static final Logger logger = LogManager.getLogger(ActionQueueShow.class.getName());
	private final Pattern pt = Pattern.compile("SIP/\\d+");
	
	public ActionQueueShow(Socket s,String queue, String peer) throws IOException {
		super(s, queue, peer);
	}
	@Override
	public void doCommand() throws IOException {
		writer.write("Action: COMMAND\r\nActionID:12345\r\ncommand: queue show " + queue + "\r\n\r\n");
		writer.flush();
	}
	@Override
	public Optional<Action> getResponce() throws IOException {
		doCommand();
		Action action = null;
		QueueContents content = new QueueContents();
		CurrentQueue currentQueue =  new CurrentQueue( queue );
		boolean memberFlag = false;
		while (true) {
			String line = reader.readLine();
			logger.trace(line);
			if (line.contains("Members") ){
				memberFlag = true;
			}
			if (memberFlag) {
				if (!line.contains("Members:") & !line.contains("No Members") & !line.contains("No Callers"))
				{	
				  Matcher m = pt.matcher(line);
				  if (m.find()) currentQueue.addMember( m.group(0) );
				}	
			}
			if (line.contains("Callers") & memberFlag){
				memberFlag = false;
				content.addQueueResponce(currentQueue);
			}
			if (line.contains("No Callers") | line.contains("Callers:")  ) break;
		
		}
		action = content.isContain(queue, peer) ? new ActionRemove(socket,queue,peer) : new ActionAdd(socket,queue,peer);
		return (action != null) ? Optional.of(action) : Optional.empty();
	}

}
