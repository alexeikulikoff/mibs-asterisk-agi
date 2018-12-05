package mibs.asterisk.agi.main;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;



public class ActionRemove extends AbstractAction implements Action{

	public ActionRemove(Socket s, String queue, String peer) throws IOException {
		super(s, queue, peer);
	}
	@Override
	public void doCommand() throws IOException {
		writer.write("Action: COMMAND\r\ncommand: queue remove member " + peer +" from " + queue + "\r\n\r\n");
		writer.flush();
	}
	@Override
	public Optional<Action> getResponce() throws IOException, AuthenticationFailedException {
		doCommand();
		return getAddRemoveResponce();
	}

}
