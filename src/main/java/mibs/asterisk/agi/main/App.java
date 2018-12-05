package mibs.asterisk.agi.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
	private static final Logger logger = LogManager.getLogger(App.class.getName());
	public static final String CONFIG_NAME="application.properties";
	public static final int backlog = 10; 

	private String user;
	private String password;
	private String asterisk_host;
	private int asterisk_port;
	private String agi_host;
	private  int agi_port;
	private String dbhost;
	private String dbname;
	private String dbuser;
	private String dbpassword;
	
	
    public String getGreeting() {
        return "Hello world.";
    }

    private Properties prop;;
    
    public App(String file) {
    	prop = new Properties();
    	InputStream input;
    	try {
			input = getClass().getClassLoader().getResourceAsStream(CONFIG_NAME);
			prop.load(input);
			user = prop.getProperty("asterisk_user");
			password= prop.getProperty("asterisk_password");
			asterisk_host = prop.getProperty("asterisk_host");
			asterisk_port = Integer.parseInt( prop.getProperty("asterisk_port") ) ;
			agi_host = prop.getProperty("agi_host");
			agi_port = Integer.parseInt( prop.getProperty("agi_port") ) ;
			dbhost =  prop.getProperty("dbhost");
			dbname =  prop.getProperty("dbname");
			dbuser =  prop.getProperty("dbuser");
			dbpassword =  prop.getProperty("dbpassword");
		} catch (Exception e1) {
			logger.error("Configuration file: " + CONFIG_NAME + "  is not found!");
		}
    }
  
    private Optional<Сentconf> getСentconf( String extension ) {
		String dsURL = "jdbc:mysql://" + dbhost + ":3306/" + dbname + "?useUnicode=yes&characterEncoding=UTF-8"	;
		Сentconf result = null;
    	try(Connection connect = DriverManager.getConnection(dsURL, dbuser, dbpassword);
    		Statement statement = connect.createStatement())
    		{
    			String sql = "select agentid, queueid, penalty from centerconfig where extension = '" + extension + "'";
    			ResultSet rs = statement.executeQuery( sql );
    			rs.next();
    			result = new Сentconf();
    			result.setAgentid(rs.getLong("agentid"));
    			result.setQueueid(rs.getLong("queueid"));
    			result.setPenalty(rs.getInt("penalty"));
    			rs.close();
    		}catch(Exception e) {
    			logger.error(e.getMessage());
    		}
    	return (result!=null) ? Optional.of(result) : Optional.empty();
    }
    private Optional<String> getQueueNameById( Long id ){
    	String dsURL = "jdbc:mysql://" + dbhost + ":3306/" + dbname + "?useUnicode=yes&characterEncoding=UTF-8"	;
		String result = null;
    	try(Connection connect = DriverManager.getConnection(dsURL, dbuser, dbpassword);
    		Statement statement = connect.createStatement())
    		{
    			String sql = "select name from queues where id = " + id ;
    			ResultSet rs = statement.executeQuery( sql );
    			rs.next();
    			result = new String( rs.getString("name"));
    			rs.close();
    		}catch(Exception e) {
    			logger.error(e.getMessage());
    		}
    	return (result!=null) ? Optional.of(result) : Optional.empty();
    }
    private Optional<Long> getPeerIdByName( String name){
    	String dsURL = "jdbc:mysql://" + dbhost + ":3306/" + dbname + "?useUnicode=yes&characterEncoding=UTF-8"	;
		Long result = null;
    	try(Connection connect = DriverManager.getConnection(dsURL, dbuser, dbpassword);
    		Statement statement = connect.createStatement())
    		{
    			String sql = "select id from peers where name = '" + name + "'" ;
    			ResultSet rs = statement.executeQuery( sql );
    			rs.next();
    			result = Long.valueOf(rs.getLong("id"));
    			rs.close();
    		}catch(Exception e) {
    			logger.error(e.getMessage());
    		}
    	return (result!=null) ? Optional.of(result) : Optional.empty();
    }
    private Optional<String> handleQueue(String queueName, String peerName){
    
    	Action action = null;
		try(Socket clientSocket = new Socket(asterisk_host, asterisk_port))
		{
			 clientSocket.setSoTimeout(1500);
			 action = new ActionLogin(clientSocket, user, password, queueName, peerName);
			 Optional<Action> opt;
			 try {
				while((opt = action.getResponce()).isPresent()) {
					action = opt.get();
				}
			 } catch (AuthenticationFailedException | IOException e) {
				clientSocket.close();
			}
		} catch (Exception e1) {
			
			logger.error(e1.getMessage());
		}
		return (action != null) ? Optional.of( action.getActionResult().getActionResult()) :  Optional.empty();	
    }
    private void error(Writer writer, String msg) throws IOException {
    	writer.write("SET VARIABLE AQMSTATUS " +  msg + "\r\n");
		writer.flush();
    }
    private void saveMemberAction(Сentconf conf, Long peerId, String actionState) {
    	String dsURL = "jdbc:mysql://" + dbhost + ":3306/" + dbname + "?useUnicode=yes&characterEncoding=UTF-8"	;
		String result = null;
    	try(Connection connect = DriverManager.getConnection(dsURL, dbuser, dbpassword);
    		Statement statement = connect.createStatement())
    		{
    			String sql = "insert into members(queueid, agentid, peerid, time, event) values (" + conf.getQueueid() +"," + conf.getAgentid() + "," + peerId +    ;
    			ResultSet rs = statement.executeQuery( sql );
    			rs.next();
    			result = new String( rs.getString("name"));
    			rs.close();
    		}catch(Exception e) {
    			logger.error(e.getMessage());
    		}
    	return (result!=null) ? Optional.of(result) : Optional.empty();
    }
    public void run() {
    	System.setProperty("java.net.preferIPv4Stack" , "true");
		Map<String, String> agicmd = new TreeMap<>();
		ExecutorService service = null;
		try {
			service= Executors.newSingleThreadExecutor();
			service.execute(()->{
				try {
					InetAddress bindAddr =  InetAddress.getByName( agi_host );
					try ( ServerSocket serverSocket = new ServerSocket( agi_port, backlog, bindAddr )) {
						while (true) {
							Socket socket = serverSocket.accept();
							OutputStream out = socket.getOutputStream();
							Writer writer = new OutputStreamWriter(out);
							BufferedReader reader = new BufferedReader( new InputStreamReader(socket.getInputStream()));
							String line = null;
							while((line = reader.readLine())!= null) {
								String key = line.split(":")[0].trim();
								String value = line.split(":")[1].trim();
								agicmd.put(key, value);
								if (line.contains("agi_arg_1")) {
									
									String extension = agicmd.get("agi_extension").trim();
									Optional<Сentconf> OptCent = getСentconf(extension);
									if (!OptCent.isPresent()) error(writer, "ERROR_WRONG_CONFIG"); 
									Optional<String> OptQueue = getQueueNameById(OptCent.get().getQueueid());
									if (!OptQueue.isPresent()) error(writer, "ERROR_WRONG_QUEUE_ID"); 
									
									String peer = agicmd.get("agi_channel");
									if (peer == null) error(writer, "ERROR_WRONG_CHANNEL"); 
									peer = peer.substring(0, peer.indexOf("-")).trim();
									
									Optional<String> opt =  handleQueue(OptQueue.get(), peer);
									
									if (opt.isPresent()) {
										saveMemberAction(OptCent.get(), opt.get());
										writer.write("SET VARIABLE AQMSTATUS " +  opt.get() + "\r\n");
										writer.flush();
									}
									break;
								}
							}
							writer.close();
							reader.close();
							socket.close();
						}	
					} catch(IOException e) {
						logger.error(e.getMessage());
						
					}
				} catch (UnknownHostException e) {
					logger.error(e.getMessage());
				}
				
			});
		}finally {
			if (service != null) service.shutdown();
		}
    }
    
    
    public static void main(String[] args) {
    	
    	new App(CONFIG_NAME).run();;
        
    }
}
