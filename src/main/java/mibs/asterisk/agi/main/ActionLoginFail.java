package mibs.asterisk.agi.main;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

public class ActionLoginFail extends AbstractAction implements Action{

	public ActionLoginFail(Socket s) throws IOException {
		super(s);
	}
	@Override
	public void doCommand() throws IOException {
		
	}
	@Override
	public Optional<Action> getResponce() {
		return Optional.empty();
	}


}
