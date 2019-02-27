package mibs.asterisk.agi.main;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;


public class ActionLogin extends AbstractAction implements Action{
	private String user;
	private String password;
	public ActionLogin(Socket s,  String user, String password, String queue, String peer) throws IOException {
		super(s, queue, peer);
		this.user = user;
		this.password = password;
	}
	@Override
	public void doCommand() throws IOException {
		writer.write("Action: Login\r\nActionID:12345\r\nUsername: " + user + "\r\nSecret: " + password + "\r\n\r\n");
		writer.flush();
	}
	@Override
	public Optional<Action> getResponce() throws IOException, AuthenticationFailedException {
	
		doCommand();
		
		Action responce = null;
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
		
			
			if (line.contains(" Authentication failed")) {
				 writer.close();
				 reader.close();
				 throw new AuthenticationFailedException("Authentication failed");
			}
			if (line.contains("Authentication accepted")) {
				responce = new ActionQueueShow(socket, queue, peer);
				break;
			}
		
		}
		return (responce != null) ? Optional.of(responce): Optional.empty();
	}

}
