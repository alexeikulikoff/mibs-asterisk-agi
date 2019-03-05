package mibs.asterisk.agi.main;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ActionAdd extends AbstractAction implements Action{
	private static final Logger logger = LogManager.getLogger(ActionAdd.class.getName());
	public ActionAdd(Socket s, String queue, String peer) throws IOException {
		super(s, queue, peer);
	}
	@Override
	public void doCommand() throws IOException {
		logger.trace("Action: COMMAND command: queue add member " + peer +" to " + queue );
		writer.write("Action: COMMAND\r\ncommand: queue add member " + peer +" to " + queue + "\r\n\r\n");
		writer.flush();
	}
	@Override
	public Optional<Action> getResponce() throws IOException, AuthenticationFailedException {
		doCommand();
		return getAddRemoveResponce();
	}

}
