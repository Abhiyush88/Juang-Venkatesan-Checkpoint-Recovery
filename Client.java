import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Stack;

public class Client {

	static int numberofNodes;
	static int numberofFailureEvents;
	static int maxNumber;
	static int maxPerActive;
	static int minSendDelay;
	public static HashMap<Integer, Node> info = new HashMap<Integer, Node>();
	public static HashMap<String, Integer> convert = new HashMap<String, Integer>();
	private static int port;
	private static String host = null;
	static int currentNode;
	static int totalNoOfMsgsSent = 0;
	static int[] vectorClock;
	static int[] sent;
	static int[] received;
	static ArrayList<Integer> sentList;
	static Stack<CheckPoints> storeCheckpoint = new Stack<CheckPoints>();
	// static Stack<CheckPoints> discardedCheckpoint = new Stack<CheckPoints>();
	static int checkpointSize = 0;

	public static void readConfig(String path) throws IOException {
		int countofnodes = 0;
		int sequence = 1;
		int i = 0;
		BufferedReader br = null;
		String currLine;
		br = new BufferedReader(new FileReader(path));
		while ((currLine = br.readLine()) != null) {
			if (!currLine.startsWith("#")) {
				String trimmed = currLine.trim();
				if (!trimmed.equals("")) {
					// trimmed.replace("\\s+", " ");
					String line[] = trimmed.split("\\s+");
					if (i == 0) {
						i++;
						numberofNodes = Integer.parseInt(line[0].trim());
						numberofFailureEvents = Integer.parseInt(line[1].trim());
						maxNumber = Integer.parseInt(line[2].trim());
						maxPerActive = Integer.parseInt(line[3].trim());
						minSendDelay = Integer.parseInt(line[4].trim());

					} else if (i <= numberofNodes) {
						Node nodes = new Node();
						nodes.setId(Integer.parseInt(line[0].trim()));
						nodes.setHostName(line[1].trim());
						nodes.setPort(Integer.parseInt(line[2].trim()));
						convert.put(line[1], Integer.parseInt(line[0].trim()));
						info.put(Integer.parseInt(line[0].trim()), nodes);
						i++;

					} else if (i <= (2 * numberofNodes)) {

						ArrayList<Integer> subsetOfNeighbors = new ArrayList<Integer>();
						for (int k = 0; k < line.length; k++) {
							if (line[k].trim().startsWith("#")) {
								break;
							}
							subsetOfNeighbors.add(Integer.parseInt(line[k].trim()));
						}
						Node nodes = info.get(countofnodes);
						nodes.setNeighbors(subsetOfNeighbors);
						// info.put(countofnodes, nodes);
						countofnodes++;
						i++;
					}

					else {
						info.get(Integer.parseInt(line[0].trim())).failureInfo.put(sequence,
								Integer.parseInt(line[1].trim()));
						sequence++;
					}

				}
			}
		}
		br.close();

	}

	private static int generateRandomNumber(int min, int max) {
		return min + (int) (Math.random() * ((max - min) + 1));
	}

	public static void sendApplMessage() throws InterruptedException {
		try {
			// subset of neighbors
			sentList = new ArrayList<Integer>();
			Message broad = new Message();
			broad.setSenderId(currentNode);
			broad.setMessageType(1);
			// broad.setClock(vectorClock);
			String msg = "Message from Node " + currentNode;
			broad.setMsg(msg);
			ArrayList<Integer> listofquorums = info.get(currentNode).getNeighbors();
			int sizeOfNeighbors = listofquorums.size();
			int subset;
			if (sizeOfNeighbors > maxPerActive)
				subset = generateRandomNumber(1, maxPerActive);
			else
				subset = generateRandomNumber(1, sizeOfNeighbors);
			Collections.shuffle(listofquorums);
			int subsetCount = 0;
			for (int neighid : listofquorums) {
				if (subsetCount < subset) {
					broad.setClock(vectorClock);
					sentList.add(neighid);
					
					sent[neighid]++;
					totalNoOfMsgsSent++;
					Node node1 = info.get(neighid);
					int neighPort = node1.port;
					String hosttemp = node1.hostName;

					Socket socket1 = new Socket(hosttemp, neighPort);
					ObjectOutputStream objectOutput = new ObjectOutputStream(socket1.getOutputStream());
					objectOutput.writeObject(broad);
					socket1.close();
					vectorClock[currentNode]++;
					Thread.sleep(minSendDelay);
				}
				subsetCount++;

			}

			// take checkpoint

			CheckPoints checkpoint = new CheckPoints();
			checkpoint.setVectorClock(vectorClock);
			checkpoint.setRcvd(received);
			checkpoint.setSent(sent);
			checkpoint.setSubsetOfNeighbors(sentList);
			checkpoint.setEventtrigerringMsg(Server.currentTrigerredmsg);
			checkpoint.setTotalMsgsSent(totalNoOfMsgsSent);
			storeCheckpoint.push(checkpoint);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void resendApplicationMessage() throws InterruptedException {
		try {
			//CheckPoints cp = storeCheckpoint.peek();
			//sentList = cp.getSubsetOfNeighbors();
			if(sentList !=null)
			{
			if (!sentList.isEmpty()) {
				Message broad = new Message();
				broad.setSenderId(currentNode);
				broad.setMessageType(8);
				// broad.setClock(vectorClock);
				
				String msg = "Message from Node " + currentNode;
				broad.setMsg(msg);
				for (int neighid : sentList) {
					broad.setClock(vectorClock);
					
					broad.sent = sent[neighid];
					Node node1 = info.get(neighid);
					int neighPort = node1.port;
					String hosttemp = node1.hostName;
					Socket socket1;
					socket1 = new Socket(hosttemp, neighPort);
					ObjectOutputStream objectOutput = new ObjectOutputStream(socket1.getOutputStream());
					objectOutput.writeObject(broad);
					socket1.close();
					vectorClock[currentNode]++;
					Thread.sleep(minSendDelay);
				}
			}
		}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void initiateClient() {
		try {

			Message broad = new Message();
			broad.setSenderId(currentNode);
			broad.setMessageType(6);
			Node curr = info.get(currentNode);
			ArrayList<Integer> listofneighs = curr.getNeighbors();
			for (int neighid : listofneighs) {
				Node node1 = info.get(neighid);
				int neighPort = node1.port;
				String hosttemp = node1.hostName;
				Socket socket1 = new Socket(hosttemp, neighPort);
				ObjectOutputStream objectOutput = new ObjectOutputStream(socket1.getOutputStream());
				objectOutput.writeObject(broad);

				socket1.close();

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void initiateClientType2(int senderid) {
		try {
			Node sendAck = info.get(senderid);
			int ackPort = sendAck.getPort();
			String ackHost = sendAck.getHostName();

			Message broad = new Message();
			broad.setSenderId(currentNode);
			broad.setMessageType(7);
			Socket socket1 = new Socket(ackHost, ackPort);
			ObjectOutputStream objectOutput = new ObjectOutputStream(socket1.getOutputStream());
			objectOutput.writeObject(broad);

			socket1.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void floodRecoveryMessage() {
		try {

			Message broad = new Message();
			broad.setSenderId(currentNode);
			broad.setMessageType(2);
			Node curr = info.get(currentNode);
			ArrayList<Integer> listofneighs = curr.getNeighbors();
			for (int neighid : listofneighs) {
				Node node1 = info.get(neighid);
				int neighPort = node1.port;
				String hosttemp = node1.hostName;
				Socket socket1 = new Socket(hosttemp, neighPort);
				ObjectOutputStream objectOutput = new ObjectOutputStream(socket1.getOutputStream());
				objectOutput.writeObject(broad);

				socket1.close();

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void sendUpdateMessgae() {
		try {

			Message broad = new Message();
			broad.setSenderId(currentNode);
			broad.setMessageType(3);

			Node curr = info.get(currentNode);
			ArrayList<Integer> listofneighs = curr.getNeighbors();
			for (int neighid : listofneighs) {
				int sentno = sent[neighid];
				broad.setSent(sentno);
				Node node1 = info.get(neighid);
				int neighPort = node1.port;
				String hosttemp = node1.hostName;
				Socket socket1 = new Socket(hosttemp, neighPort);
				ObjectOutputStream objectOutput = new ObjectOutputStream(socket1.getOutputStream());
				objectOutput.writeObject(broad);

				socket1.close();

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendLeafMsg() {
		try {

			Message broad = new Message();
			broad.setSenderId(currentNode);
			broad.setMessageType(4);
			broad.setUPDTD(Server.UPDTDi);
			Node node1 = info.get(Server.parentNode);
			int neighPort = node1.port;
			String hosttemp = node1.hostName;
			Socket socket1 = new Socket(hosttemp, neighPort);
			ObjectOutputStream objectOutput = new ObjectOutputStream(socket1.getOutputStream());
			objectOutput.writeObject(broad);
			socket1.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendBooleanOrMsg(boolean upd) {
		try {

			Message broad = new Message();
			broad.setSenderId(currentNode);
			broad.setMessageType(4);
			broad.setUPDTD(upd);
			Node node1 = info.get(Server.parentNode);
			int neighPort = node1.port;
			String hosttemp = node1.hostName;
			Socket socket1 = new Socket(hosttemp, neighPort);
			ObjectOutputStream objectOutput = new ObjectOutputStream(socket1.getOutputStream());
			objectOutput.writeObject(broad);
			socket1.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendBraodCastMsg(boolean upd) {
		try {

			Message broad = new Message();
			broad.setSenderId(currentNode);
			broad.setMessageType(5);
			broad.setUPDTD(upd);
			if (!Server.children.isEmpty()) {
				for (int neighid : Server.children) {
					Node node1 = info.get(neighid);
					int neighPort = node1.port;
					String hosttemp = node1.hostName;
					Socket socket1 = new Socket(hosttemp, neighPort);
					ObjectOutputStream objectOutput = new ObjectOutputStream(socket1.getOutputStream());
					objectOutput.writeObject(broad);

					socket1.close();
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void writeToFile(int[] clock) throws IOException
	{
		String name = "Node-"+currentNode + ".txt";
		File file1 = new File(name);
        BufferedWriter bfWriter = new BufferedWriter(new FileWriter(file1,true));
        bfWriter.write("Recovery"+(Server.recoverySequence-1) + " ");
        for(int i=0; i<clock.length;i++)
        {
        	bfWriter.write(clock[i] + " ");
        }
        bfWriter.write(System.lineSeparator());
        
        bfWriter.close();
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		String pathtofile = args[1];
		// System.out.println();
		readConfig(pathtofile);

		int id = 0;
		host = InetAddress.getLocalHost().getHostName().substring(0, 4);
		for (Entry<String, Integer> trav : convert.entrySet()) {
			if (trav.getKey().equalsIgnoreCase(host)) {
				id = trav.getValue();
				currentNode = id;

			}
		}

		// Initialize vector clock, sent and received vectors

		vectorClock = new int[numberofNodes];
		sent = new int[numberofNodes];
		received = new int[numberofNodes];
		for (int init = 0; init < numberofNodes; init++) {
			vectorClock[init] = 0;
			sent[init] = 0;
			received[init] = 0;
		}

		ArrayList<Integer> khali = new ArrayList<Integer>();
		CheckPoints checkpoint = new CheckPoints();
		checkpoint.setVectorClock(vectorClock);
		checkpoint.setRcvd(received);
		checkpoint.setSent(sent);
		checkpoint.setSubsetOfNeighbors(khali);
		checkpoint.setEventtrigerringMsg(Server.currentTrigerredmsg);
		checkpoint.setTotalMsgsSent(totalNoOfMsgsSent);
		storeCheckpoint.push(checkpoint);
		checkpointSize++;

		
		  String name= "Node-"+currentNode + ".txt"; 
		  File file = new File(name); 
		  file.createNewFile();
		 

		// server initiated

		Node temp = info.get(id);
		port = temp.port;
		Server sp = new Server(port);
		sp.start();

		Thread.sleep(166*numberofNodes);

		// client begins

		if (currentNode == 1) {
			// build spanning tree
			Server.isRootNode = true;
			Server.messageSeen = true;
			System.out.println("Building Spanning Tree");
			initiateClient();
		}

		Thread.sleep(1000);

		// REB protocol initiated
		if (currentNode == 1 || currentNode == 3) {
			sendApplMessage();
		}

		while (true) {

			Thread.sleep(7);
			//System.out.println();
			if (info.get(currentNode).failureInfo.containsKey(Server.recoverySequence) && !Server.recoveryMode
					&& (storeCheckpoint.size() >= info.get(currentNode).failureInfo.get(Server.recoverySequence)
							+ checkpointSize)) {
				System.out.println("Recovery Initiated");
				Server.recoveryInitiator = true;
				Server.recoveryMode = true;
				// randomly discard few checkpoints

				int discard = generateRandomNumber(1, storeCheckpoint.size() - checkpointSize);
				for (int i = 0; i < discard; i++) {
					storeCheckpoint.pop();
					// discardedCheckpoint.push(cp);
				}

				//Server.TRPi = storeCheckpoint.peek();

				// flood

				floodRecoveryMessage();

				Server.isFirstrecoverymsg = false;
				Server.broadcastMsgSeen = false;
				Server.UPDTDi = false;
				sendUpdateMessgae();
			}
			if (Server.recoverySequence > numberofFailureEvents) {
				System.out.println("Protocol Terminated");
				break;
			}

			
		}

	}

}
