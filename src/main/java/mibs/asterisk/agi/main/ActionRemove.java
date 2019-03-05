package mibs.asterisk.agi.main;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class ActionRemove extends AbstractAction implements Action{
	private static final Logger logger = LogManager.getLogger(ActionRemove.class.getName());
	public ActionRemove(Socket s, String queue, String peer) throws IOException {
		super(s, queue, peer);
	}
	@Override
	public void doCommand() throws IOException {
		logger.trace("Action: COMMAND ActionID:12345 command: queue remove member " + peer +" from " + queue );
		writer.write("Action: COMMAND\r\nActionID:12345\r\ncommand: queue remove member " + peer +" from " + queue + "\r\n\r\n");
		writer.flush();
	}
	@Override
	public Optional<Action> getResponce() throws IOException, AuthenticationFailedException {
		doCommand();
		return getAddRemoveResponce();
	}

}
