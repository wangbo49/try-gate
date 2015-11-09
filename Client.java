

import java.net.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.*;



public class Client {
	
	private ServerSocket welcomeSocket;
	private int clientPortNumber;
	BufferedReader inFromUser;
	DataOutputStream outToServer;
	boolean login_flag = false;
	String serverName_local;
	int heartbeatTime = 30;
	int port_local;
	HashMap<String, Integer> username_portNumber = new HashMap<String,Integer>();
	HashMap<String,String> username_IP = new HashMap<String,String>();

	public Client(String serverName, int port) throws IOException {
		serverName_local = serverName;
		port_local = port;
		//System.out.println("Connecting to " + serverName_local + " on " + port_local);//Used for debug
		/*construct the welcomSocket at the client side*/
		welcomeSocket = new ServerSocket(0); 
		clientPortNumber = welcomeSocket.getLocalPort();
		
		/*construct the socket at the client side to connect the server */
		Socket connectionSocket_client = new Socket(serverName_local, port_local);
		
		/*After connected to the server, send the port number(identifier of the client) to the server*/
		outToServer = new DataOutputStream(connectionSocket_client.getOutputStream());
		outToServer.writeBytes(Integer.toString(clientPortNumber)+"\n");
		outToServer.flush();
		
		
		
		//System.out.println("Just connected to "+ connectionSocket_client.getRemoteSocketAddress());//Used for debug

		// TODO if the server is closed
		
		
		new fromServerThread(connectionSocket_client);
		new fromUserThread(connectionSocket_client);
		new Heartbeat();
		while(true){
			Socket clientSocket = welcomeSocket.accept();
			//System.out.println("Message is going to send to receiver");//Used for debug
			new fromServerThread(clientSocket);
			
		}
		
		}
		
		
	/*****************************************************
	 * Create fromServerThread
	 * read stream from the server 
	 * execute actions or display information
	 *****************************************************/
	class fromServerThread extends Thread {
		Socket connectionSocket_client;
		DataInputStream inFromServer;
		DataOutputStream outToServer;
		
		/*Constructor*/
		public fromServerThread(Socket connectionSocket_client) throws IOException {
			this.connectionSocket_client = connectionSocket_client;
			inFromServer = new DataInputStream(connectionSocket_client.getInputStream());
			//MSystem.out.println("New Connection from: "+ connectionSocket_client.getRemoteSocketAddress());//Used for debug
			start();
		}
		
		
		public void run(){
			while(true){
				try{
					    
						if(inFromServer.available()>0){
							String lines = inFromServer.readUTF();
							if(lines.equals("Logout Successfully ")){
								System.out.println(lines);
								System.exit(0);
							}else if(lines.equals("logout")){
								System.out.println("You are logged out! ");
								System.exit(0);
							}else if(lines.split(" ")[0].equals("portNumber:")){
								System.out.println("Test 2 Part");/////
								String portNumber = lines.split(" ")[1];
								int portNum = Integer.parseInt(portNumber);
								System.out.println("portNumber: "+portNumber);/////
								String IP = lines.split(" ")[3].substring(1);
								System.out.println("IP: "+IP);/////
								String username = lines.split(" ")[5];
								System.out.println("Username: "+username);/////
								if(username_portNumber.containsKey(username)){
									username_portNumber.remove(username);
									username_portNumber.put(username, portNum);
								}else{
									username_portNumber.put(username, portNum);
								}
								if(username_IP.containsKey(username)){
									username_IP.remove(username);
									username_IP.put(username, IP);
								}else{
									username_IP.put(username, IP);
								}
									
								}else{
								System.out.println(lines);
							}
							
						}else{
							
							continue;

						}
					
					}catch (Exception e) {
					e.printStackTrace();
					System.out.println("Login suspended");
					break;
				}
			}
			
		}
		
		
	}
	
	
	/*****************************************************
	 * Create fromUserThread
	 * Read in stream from keyboard
	 * 
	 *****************************************************/
	class fromUserThread extends Thread{
		Socket connectionSocket_client;
		BufferedReader inFromUser;
		DataInputStream inFromServer;
		DataOutputStream outToServer;
		
		/*Constructor*/
		public fromUserThread(Socket connectionSocket_client) throws IOException{
			this.connectionSocket_client = connectionSocket_client;
			inFromUser = new BufferedReader(new InputStreamReader(System.in));
			inFromServer = new DataInputStream(connectionSocket_client.getInputStream());
			outToServer = new DataOutputStream(connectionSocket_client.getOutputStream());
			start();
		}
		
		public void run(){
			while(true){
				String lines = null;
				try {
						/*Wait for the input from user*/
						lines = inFromUser.readLine();
						
						
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				try{
						
							
							String username = null;
							String message = null;
						if(lines.split(" ")[0].equals("private")){
							
							username = lines.split(" ")[1];
							message = lines.substring(9+username.length());
							if(username_portNumber.containsKey(username)){
								int portNumber = username_portNumber.get(username);
								String IP = username_IP.get(username);
								int portNum = username_portNumber.get(username);
								Socket privateSocket = new Socket(IP, portNum);
								DataOutputStream outToReceiver = new DataOutputStream(privateSocket.getOutputStream());
								outToReceiver.writeUTF(message);
								outToReceiver.flush();
								outToReceiver.close();
								privateSocket.close();
							}else{
								System.out.println("Request Failed! Do the getaddress command first !");
							}
							
							
							
							
							
						}else{
							outToServer.writeBytes(lines+"\n");
							outToServer.flush();
						}
						
						
					}catch(IOException e){
					try{
						/*The remote socket at the server side has been disclosed*/
						/*re-construct a socket at the client side to connect the server*/
						Socket connectionSocket_client_new = new Socket(serverName_local, port_local);
						DataOutputStream outToServer_new = new DataOutputStream(connectionSocket_client_new.getOutputStream());
						
						/*After connected to the server, send the port number(identifier of the client) to the server*/
						outToServer_new.writeBytes(Integer.toString(clientPortNumber)+"\n");
						outToServer_new.writeBytes(lines+"\n");
						outToServer_new.flush();
					}catch(UnknownHostException e1){
						
					}catch(IOException e2){
						
					}
					
				}
			}
		}
	}
	
	/*****************************************************
	 * HeartBeat Thread
	 *****************************************************/
	public class Heartbeat extends Thread{
		public Heartbeat() {
			ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();
			heartbeat.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						System.out.println("%%%%%%");
						Socket socket = new Socket(serverName_local, port_local);
						DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
						outToServer.writeUTF("LIVE");
						outToServer.flush();
						
					} catch (UnknownHostException e) {
						System.out.println("UnknownHost");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}, 0, heartbeatTime, TimeUnit.SECONDS);
		}
		
		
		
		
		
		
		
		
	}
	public static void main(String[] args) {

		String serverName = args[0];
		int port = Integer.parseInt(args[1]);

		try {
			new Client(serverName, port);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

}

