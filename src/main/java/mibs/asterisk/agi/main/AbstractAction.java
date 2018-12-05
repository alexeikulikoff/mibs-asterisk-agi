package mibs.asterisk.agi.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Optional;

public abstract class AbstractAction {
	protected String queue;
	protected String peer;
	protected Socket socket;
	protected Writer writer; 
	protected BufferedReader reader;
	protected AgentState state;
	public AbstractAction(Socket s) throws IOException {
		socket = s;
		writer = new OutputStreamWriter(socket.getOutputStream());
		reader = new BufferedReader( new InputStreamReader(socket.getInputStream()));
	}
	public AbstractAction(Socket s, AgentState rs) throws IOException {
		this(s);
		state = rs;
	}
	public AbstractAction(Socket s, String q, String a) throws IOException {
		this(s);
		queue = q;
		peer = a;
	}
	
	public Socket getSocket() {
		return socket;
	}
	protected Optional<Action> getAddRemoveResponce() throws IOException{
		Action action = null;
		boolean flag = false;
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			if (line.contains("Follows") & !flag) {
				flag = true;
			}
			if (flag) {
				if (line.contains("Removed interface")) {
					action = new ActionLogOff(socket,AgentState.REMOVED);
					break;
				}
				if (line.contains("Added interface")) {
					action = new ActionLogOff(socket,AgentState.ADDED);
					break;
				}
				if (line.contains("Unable to add interface")) {
					action = new ActionLogOff(socket,AgentState.FAILED);
					break;
				}
			}
			if (line.contains("--END COMMAND--")) {
				break;
			}
		}	 
		return (action!=null) ? Optional.ofNullable(action) : Optional.empty();
	}
}
