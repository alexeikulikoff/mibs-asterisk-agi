package mibs.asterisk.agi.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
	public static final String CONFIG_NAME = "application.properties";
	public static final int backlog = 10;

	private String user;
	private String password;
	private String asterisk_host;
	private int asterisk_port;
	private String agi_host;
	private int agi_port;
	private String dbhost;
	private String dbname;
	private String dbuser;
	private String dbpassword;
	
	private String control_dbhost;
	private String control_dbname;
	private String control_dbuser;
	private String control_dbpassword;

	public String getGreeting() {
		return "Hello world.";
	}

	private Properties prop;;

	public App(String file) {
		prop = new Properties();
		FileInputStream fis;
		
		try {
//			input = getClass().getClassLoader().getResourceAsStream(CONFIG_NAME);
		//	input = getClass().getClassLoader().getResourceAsStream(file);
			fis = new FileInputStream( file );
			prop.load( fis );
			user = prop.getProperty("asterisk_user");
			password = prop.getProperty("asterisk_password");
			asterisk_host = prop.getProperty("asterisk_host");
			asterisk_port = Integer.parseInt(prop.getProperty("asterisk_port"));
			agi_host = prop.getProperty("agi_host");
			agi_port = Integer.parseInt(prop.getProperty("agi_port"));
			dbhost = prop.getProperty("dbhost");
			dbname = prop.getProperty("dbname");
			dbuser = prop.getProperty("dbuser");
			dbpassword = prop.getProperty("dbpassword");
			
			control_dbhost = prop.getProperty("control_dbhost");
			control_dbname = prop.getProperty("control_dbname");
			control_dbuser = prop.getProperty("control_dbuser");
			control_dbpassword = prop.getProperty("control_dbpassword");
			
		} catch (Exception e1) {
			logger.error("Configuration file: " + CONFIG_NAME + "  is not found!");
		}
	}

	private String dsURL() {
		return "jdbc:mysql://" + dbhost + ":3306/" + dbname + "?useUnicode=yes&characterEncoding=UTF-8";
	}
	private String dsControlURL() {
		return "jdbc:mysql://" + control_dbhost + ":3306/" + control_dbname + "?useUnicode=yes&characterEncoding=UTF-8";
	}
	private Optional<Сentconf> getСentconf(String extension) {
	
		Сentconf result = null;
		ResultSet rs = null;
		try (Connection connect = DriverManager.getConnection(dsURL(), dbuser, dbpassword);
				Statement statement = connect.createStatement()) {
			String sql = "select agentid, queueid, penalty from centerconfig where extension = '" + extension + "'";
			rs = statement.executeQuery(sql);
			if (rs.next()) {
				result = new Сentconf();
				result.setAgentid(rs.getLong("agentid"));
				result.setQueueid(rs.getLong("queueid"));
				result.setPenalty(rs.getInt("penalty"));
			}else {
				logger.error("Error! Agentid, queueid, penalty not found for extension: " + extension);
			}
		} catch (Exception e) {
			logger.error("Error! Config not found for extension: " + extension);
		}finally {
			try {
				rs.close();
			} catch (SQLException e) {
				logger.error("Error! Cannot close result set for extension " + extension);
			}
		}
		return (result != null) ? Optional.of(result) : Optional.empty();
	}

	private Optional<String> getQueueNameById(Long id) {
		
		String result = null;
		ResultSet rs = null;
		try (Connection connect = DriverManager.getConnection(dsURL(), dbuser, dbpassword);
				Statement statement = connect.createStatement()) {
			String sql = "select name from queues where id = " + id;
			rs = statement.executeQuery(sql);
			if (rs.next()) {
				result = new String(rs.getString("name"));
			}else {
				logger.error("Error! Cannot fina name from queues for id " + id);
			}
		} catch (Exception e) {
			logger.error("Error! Queue is not found for Id:  " + id);
		}finally {
		try {
			rs.close();
		} catch (SQLException e) {
			logger.error("Error! Cannot close result set in getQueueNameById for" + id);
		}
	}
		return (result != null) ? Optional.of(result) : Optional.empty();
	}

	private Optional<Long> getPeerIdByName(String name) {
		
		Long result = null;
		ResultSet rs = null;
		try (Connection connect = DriverManager.getConnection(dsURL(), dbuser, dbpassword);
				Statement statement = connect.createStatement()) {
			String sql = "select id from peers where name like '%" + name + "'";
			rs = statement.executeQuery(sql);
			if (rs.next()) {
				result = Long.valueOf(rs.getLong("id"));	
			}else {
				logger.error("Error! Cannot select id from peer for name:  " + name);
			}
		} catch (Exception e) {
			logger.error("Error! Peer is not found for name: " + name);
		}
		finally {
			try {
				rs.close();
			} catch (SQLException e) {
				logger.error("Error! Cannot close result set in getPeerIdByName for name" + name);
			}
		}	
		return (result != null) ? Optional.of(result) : Optional.empty();
	}

	private Optional<String> handleQueue(String queueName, String peerName) {

		Action action = null;
		
		if (!peerName.startsWith("SIP")) peerName = "SIP/" + peerName;
		
		try (Socket clientSocket = new Socket(asterisk_host, asterisk_port)) {
		
			action = new ActionLogin(clientSocket, user, password, queueName, peerName);
			Optional<Action> opt;
			try {
				while ((opt = action.getResponce()).isPresent()) {
					action = opt.get();
				}
			} catch (AuthenticationFailedException | IOException e) {
				clientSocket.close();
			}
		} catch (Exception e1) {

			logger.error("Error! Wrong queue handling for queue name:  " + queueName + " and peer name: " + peerName);
		}
		return (action != null) ? Optional.of(action.getActionResult().getActionResult()) : Optional.empty();
	}

	private void error(Writer writer, String msg) throws IOException {
		writer.write("SET VARIABLE AQMSTATUS " + msg + "\n");
		writer.flush();

	}

	private void saveMemberAction(Сentconf conf, Long peerId, String command) {
		if (command.equals("FAILED")) {
			logger.error("Save member action get Fail command.");
			return;
		}
		LocalDateTime ld = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		try (Connection connect = DriverManager.getConnection(dsURL(), dbuser, dbpassword);
				Statement statement = connect.createStatement()) {
			String sql = "insert into members(queueid, agentid, peerid, time, event) values (" + conf.getQueueid() + ","
					+ conf.getAgentid() + "," + peerId + ",'" + ld.format(formatter) + "','" + command + "')";
			
			statement.executeUpdate(sql);
			String info = "Command: " + command + " id:" + conf.getAgentid() + " time: " + ld.format(formatter);
			
			logger.trace( info );
			
		} catch (Exception e) {
			logger.error("Error! Member is not saved. peerid: " + peerId );
		}

	}

	private Optional<String> getInboundRecordCommand(String ext) {
		String result= null;
		ResultSet rs = null;
		try (Connection connect = DriverManager.getConnection(dsControlURL(), control_dbuser, control_dbpassword);
				Statement statement = connect.createStatement()) {
			String sql = "select recordin from equipments where phone='" + ext + "'";
			rs = statement.executeQuery(sql);
			if (rs.next()) {
				result = new String(rs.getString("recordin"));
			}else {
				logger.error("Error! getInboundRecordCommand not found: " + ext );
			}
			
		} catch (Exception e) {
			logger.error("Error! Inbound record command not found: " + ext );
		}
		finally {
			try {
				rs.close();
			} catch (SQLException e) {
				logger.error("Error! Cannot close result set in getInboundRecordCommand for extension: " + ext);
			}
		}	
		return (result != null) ? Optional.of(result) : Optional.empty();
	}
	private Optional<String> getOutboundRecordCommand(String phone) {
		String result= null;
		ResultSet rs = null;
		try (Connection connect = DriverManager.getConnection(dsControlURL(), control_dbuser, control_dbpassword);
				Statement statement = connect.createStatement()) {
			String sql = "select recordout from equipments where phone='" + phone + "'";
			rs = statement.executeQuery(sql);
			if(rs.next()) {
				result = new String(rs.getString("recordout"));
			}
			else {
				logger.error("Error! getOutboundRecordCommand not found: " + phone );
			}
			
		} catch (Exception e) {
			logger.error("Error! Outbound record command not found: " + phone );
		}
		finally {
			try {
				rs.close();
			} catch (SQLException e) {
				logger.error("Error! Cannot close result set in getOutboundRecordCommand for phone: " + phone);
			}
		}	
		return (result != null) ? Optional.of(result) : Optional.empty();
	}
	private Optional<String> getExternal(String callerid) {
		String result= null;
		ResultSet rs = null;
		try (Connection connect = DriverManager.getConnection(dsControlURL(), control_dbuser, control_dbpassword);
				Statement statement = connect.createStatement()) {
			String sql = "select external from equipments where phone='" + callerid + "'";
			rs = statement.executeQuery(sql);
			if (rs.next()) {
				result = new String(rs.getString("external"));
			}
			else {
				logger.error("Error! get External not found: " + callerid );
			}
		
		} catch (Exception e) {
			logger.error("Error! External not found: " + callerid );
		}
		finally {
			try {
				rs.close();
			} catch (SQLException e) {
				logger.error("Error! Cannot close result set in getExternal for callerid: " + callerid);
			}
		}	
		return (result != null) ? Optional.of(result) : Optional.empty();
	}
	public void run() {
		System.setProperty("java.net.preferIPv4Stack", "true");
		Map<String, String> agicmd = new TreeMap<>();
		ExecutorService service = null;
		try {
			service = Executors.newSingleThreadExecutor();
			service.execute(() -> {
				try {
					InetAddress bindAddr = InetAddress.getByName(agi_host);
					try (ServerSocket serverSocket = new ServerSocket(agi_port, backlog, bindAddr)) {
						while (true) {
							Socket socket = serverSocket.accept();
							OutputStream out = socket.getOutputStream();
							Writer writer = new OutputStreamWriter(out);
							BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
							String line = null;
							while ((line = reader.readLine()) != null) {
								System.out.println(line);
								String key = line.split(":")[0].trim();
								String value = line.split(":")[1].trim();
								agicmd.put(key, value);
								if (line.contains("agi_arg_1")) {

									if (value.equals("record_inbound")) {
										String extension = agicmd.get("agi_extension");
										if (extension == null || extension.length() == 0 ) {
											error(writer, "ERROR_WRONG_EXTENSION");
											break;
										}
										extension = extension.trim();
										Optional<String> cmd  = getInboundRecordCommand(extension);
										if (cmd.isPresent()) {
											writer.write("SET VARIABLE RECORD_INBOUND " + cmd.get() +"\n");
											writer.flush();
										}
										break;
									}
									if (value.equals("record_outbound")) {
									
										String channel = agicmd.get("agi_channel").trim();
										String phone =  channel.substring(channel.indexOf("/") + 1,channel.indexOf("-"));
									
										if (phone == null || phone.length() == 0 ) {
											error(writer, "ERROR_WRONG_EXTENSION");
											break;
										}
										Optional<String> cmd  = getOutboundRecordCommand(phone);
										if (cmd.isPresent()) {
											
											writer.write("SET VARIABLE RECORD_OUTBOUND " + cmd.get() +"\n");
											writer.flush();
										}
										break;
									}
									if (value.equals("call_outside")) {
										String callerid = agicmd.get("agi_callerid");
										if (callerid == null || callerid.length() == 0 ) {
											error(writer, "ERROR_WRONG_CALLID");
											break;
										}
										callerid = callerid.trim();
										Optional<String> cmd  = getExternal(callerid);
										if (cmd.isPresent()) {
											writer.write("SET VARIABLE EXTERNAL_CALLID " + cmd.get() +"\n");
											writer.flush();
										}
										break;
									}	
									if (value.equals("queue_login")) {
										String extension = agicmd.get("agi_extension").trim();
										Optional<Сentconf> OptCent = getСentconf(extension);
										if (!OptCent.isPresent()) {
											error(writer, "ERROR_WRONG_CONFIG");
											break;
										}
										Сentconf contConf = OptCent.get();
										Optional<String> OptQueue = getQueueNameById(contConf.getQueueid());
										if (!OptQueue.isPresent()) {
											error(writer, "ERROR_WRONG_QUEUE_ID");
											break;
										}
										String queueName = OptQueue.get();
										String peerName = agicmd.get("agi_callerid").trim();
										//peerName = peerName.substring(0, peerName.indexOf("-")).trim();
										if (peerName == null || peerName.length() == 0) {
											error(writer, "ERROR_WRONG_CHANNEL");
											break;
										}
										Optional<Long> optPeerid = getPeerIdByName(peerName);
										if (!optPeerid.isPresent()) {
											error(writer, "ERROR_PEER_ID_NOT_FOUND");
											break;
										}
										Long peerId = optPeerid.get();
										Optional<String> optHandle = handleQueue(queueName, peerName);
										if (optHandle.isPresent()) {
											String command = optHandle.get();
											saveMemberAction(contConf, peerId, command);
											writer.write("SET VARIABLE AQMSTATUS " + command + "\n");
											writer.flush();
										}
									}
									break;
								}
							}
							writer.close();
							reader.close();
							socket.close();
						}
					} catch (IOException e) {
						logger.error("Error open Server Socketet: "  +  e.getMessage());

					}
				} catch (UnknownHostException e) {
					logger.error("Error bind address: " +e.getMessage());
				}

			});
		} finally {
			if (service != null)
				service.shutdown();
		}
	}

	public static void main(String[] args) {

	//	new App(CONFIG_NAME).run();
		if (args != null && args.length > 0 ) {
			new App(args[0]).run();
		}else {
			logger.error("Error! No configuration file provided!");
		}
		

	}
}
