package mibs.asterisk.agi.main;


/*
 * 
 * agi_network: yes
line :agi_request: agi://172.16.30.49:4574
line :agi_channel: SIP/7822-0004025b
line :agi_language: en
line :agi_type: SIP
line :agi_uniqueid: 1543834122.391354
line :agi_version: 11.9.0
line :agi_callerid: 7822
line :agi_calleridname: 7822
line :agi_callingpres: 0
line :agi_callingani2: 0
line :agi_callington: 0
line :agi_callingtns: 0
line :agi_dnid: 3322
line :agi_rdnis: unknown
line :agi_context: to-pstn
line :agi_extension: 3322
line :agi_priority: 3
line :agi_enhanced: 0.0
line :agi_accountcode: 
line :agi_threadid: -1362101392
line :agi_arg_1: queue_login

 * 
 * 
 * callcenter has 2 calls (max unlimited) in 'rrmemory' strategy (22s holdtime, 68s talktime), W:0, C:40834, A:6522, SL:0.0% within 0s
   Members: 
      SIP/7855 (ringinuse disabled) (dynamic) (In use) has taken 32 calls (last was 515 secs ago)
      SIP/7859 (ringinuse disabled) (dynamic) (In use) has taken 15 calls (last was 64 secs ago)
      SIP/7850 (ringinuse disabled) (dynamic) (In use) has taken 22 calls (last was 40 secs ago)
   Callers: 
      1. DAHDI/i1/89119482197-17ad7 (wait: 0:15, prio: 0)
      2. DAHDI/i1/88126995420-17ad8 (wait: 0:13, prio: 0)

 * 
 * 
 * 
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;





public class AgiServer {

	public static final int AGI_PORT = 4574 ;
	public static final int AMI_PORT = 5038 ;
	public static final int backlog = 10; 
	private static CurrentQueue queue = null;
	
	private static boolean queueFlag;
	private static boolean memberFlag;
	
	private static BiConsumer<String, QueueContents> queueShowHandler = (b, responce) -> {
	
		if (b.matches("\\S.+") & !b.contains("--END COMMAND--") & !b.contains("Response:") & !b.contains("Privilege:")){
			queueFlag = true;
			queue = new CurrentQueue(b.split("\\s+")[0]);
		}	
		if (b.contains("Members") & queueFlag){
			memberFlag = true;
		}
		if (b.contains("Callers") & memberFlag){
			queueFlag = false ;
			memberFlag = false;
			responce.addQueueResponce(queue);
		}
		if (memberFlag & b.matches("\\s{1,6}SIP\\/\\d{1,4}.+")){
			queue.addMember( b.split("\\s+")[1] );
		}
	};

	public static Function<String, String> queueAddRemoveHandler = (s) ->{
		
		System.out.println(s);
		//if (s.contains("Unable to add interface to queue")) return "FAIL_TO_HANDLE"
		
		return "queueAddRemoveHandler";
			
	};

	public static void main(String[] args) {
		Map<String, String> agicmd = new TreeMap<>();
		
		String host = "172.16.30.49";
		System.setProperty("java.net.preferIPv4Stack" , "true");
		Logger log = Logger.getLogger(AgiServer.class.getName());
		
		ExecutorService service = null;
		try {
			service= Executors.newSingleThreadExecutor();
			service.execute(()->{
				try {
					InetAddress bindAddr =  InetAddress.getByName( host );
					try ( ServerSocket serverSocket = new ServerSocket( AGI_PORT, backlog, bindAddr )) {
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
									
									String asterisk_host ="172.16.30.48";
									int asterisk_port = 5038;
									String user="admin2";
									String password ="4568521";
									Action action;
									try(Socket clientSocket = new Socket(asterisk_host, asterisk_port))
									{
										 clientSocket.setSoTimeout(1500);
										 action = new ActionLogin(clientSocket, user, password, "vip", "SIP/7822");
										 Optional<Action> opt;
										 try {
											while((opt = action.getResponce()).isPresent()) {
												action = opt.get();
											}
										 } catch (AuthenticationFailedException | IOException e) {
											clientSocket.close();
										}
									}
									String cmd =  action.getActionResult().getActionResult();
									
									writer.write("SET VARIABLE AQMSTATUS " +  cmd + "\r\n");
									writer.flush();
									break;
								}
							}
							writer.close();
							reader.close();
							socket.close();
						}	
					} catch(IOException e) {
						log.info(e.getMessage());
						
					}
				} catch (UnknownHostException e) {
					log.info(e.getMessage());
				}
				
			});
		}finally {
			if (service != null) service.shutdown();
		}
		
	}
}
