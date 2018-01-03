import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;



public class Server extends Thread {

	private int port;
	Client clientObj;
	ServerSocket serverSocket= null;
	Socket clientSocket = null;
	//static Boolean active = false;
	static int currentTrigerredmsg=-1;
	static boolean recoveryMode= false;
	static int recoverySequence=1;
	//static int myNodeNo;
	static boolean UPDTDi=false;
	//static CheckPoints TRPi;
	static boolean isRootNode=false;
	static boolean isFirstrecoverymsg = true;
	static boolean recoveryInitiator = false;
	static Boolean messageSeen = false;
	static int parentNode;
	static ArrayList<Integer> children = new ArrayList<Integer>();
	static int checknieghborCount=0;
	static int convergecastNieghborCount=0;
	static List<Boolean> booleanList = new ArrayList<Boolean>();
	static ArrayList<Message> bufferList = new ArrayList<Message>();
	static boolean broadcastMsgSeen = false;
	static boolean firstRound = true;
	static ArrayList<Message> resendBufferList = new ArrayList<Message>();
	public Server(int port)
	{
		this.port = port;
		clientObj  = new Client();
	}

	public void run()
	{
		try {

			serverSocket = new ServerSocket(port);
			//myNodeNo = Client.currentNode;


		} catch (IOException e1) {
			e1 .printStackTrace();
		}	
		while(true) {

			try {

				clientSocket = serverSocket.accept();
				ObjectInputStream objectInput = new ObjectInputStream(clientSocket.getInputStream());
				Message objRec = (Message) objectInput.readObject();
				if(objRec.getMessageType()==1)
				{
					if(!recoveryMode)
					{
						currentTrigerredmsg = objRec.getSenderId();
						//update vector clock

						int[] localClock = objRec.getClock();
						for(int i=0; i<Client.numberofNodes;i++)
						{
							if(Client.vectorClock[i]< localClock[i])
								Client.vectorClock[i]= localClock[i];
						}
						Client.vectorClock[Client.currentNode]++;

						//update received vector

						Client.received[objRec.getSenderId()]++;

						if(Client.maxNumber > Client.totalNoOfMsgsSent)
						{
							Client.sendApplMessage();
						}
						else
						{
							//System.out.println("Total messages count exceeded" +Client.totalNoOfMsgsSent);
							// take checkpoint

							ArrayList<Integer> khali = new ArrayList<Integer>();
							CheckPoints checkpoint = new CheckPoints();
							checkpoint.setVectorClock(Client.vectorClock);
							checkpoint.setRcvd(Client.received);
							checkpoint.setSent(Client.sent);
							checkpoint.setSubsetOfNeighbors(khali);
							checkpoint.setEventtrigerringMsg(Server.currentTrigerredmsg);
							checkpoint.setTotalMsgsSent(Client.totalNoOfMsgsSent);
							Client.storeCheckpoint.push(checkpoint);
						}
					}

				}
				else if(objRec.getMessageType()==2) //recovery flooding msg
				{
					recoveryMode= true;
					//flood recovery message
					if(Server.recoveryMode && Server.isFirstrecoverymsg)
					{
						Client.floodRecoveryMessage();
						// TRPi= Client.storeCheckpoint.peek();
						broadcastMsgSeen = false;
						isFirstrecoverymsg = false;
						UPDTDi = false;
						Client.sendUpdateMessgae();  // send update messages to neighbors

					}

				}

				else if(objRec.getMessageType()==3)  // check if needs to rollback
				{
					checknieghborCount++;

					if(!firstRound && !broadcastMsgSeen && !isRootNode)
					{
						bufferList.add(objRec);
						//System.out.println("buffering");
					}
					else
					{
						broadcastMsgSeen = false;
						// check with each neighbor
						int id = objRec.getSenderId();
						if(Client.received[id] > objRec.getSent())
						{
							UPDTDi = true;
							Client.storeCheckpoint.pop();
							boolean temp =true;
							while(temp)
							{
								CheckPoints TRPi = Client.storeCheckpoint.peek();
								if(TRPi.rcvd[id] == objRec.getSent())
								{
									temp=false;
									Client.sent = TRPi.sent;
									Client.received = TRPi.rcvd;
									Client.vectorClock = TRPi.vectorClock;
									Client.sentList = TRPi.getSubsetOfNeighbors();
									currentTrigerredmsg = TRPi.eventtrigerringMsg;
									Client.totalNoOfMsgsSent = TRPi.totalMsgsSent;
								}
								else
								{
									Client.storeCheckpoint.pop();
								}
							}
						}

						// leaf will message the neighbors

						if(checknieghborCount==Client.info.get(Client.currentNode).neighbors.size())
						{
							firstRound = false;
							checknieghborCount = 0;
							if(children.isEmpty())
							{
								// start converge cast
								Client.sendLeafMsg();
							}
							else
							{
								booleanList.add(UPDTDi);
								if(booleanList.size()>children.size()){

									convergecastNieghborCount=0;
									if(!isRootNode)
									{
										if(booleanList.contains(true))
										{
											Client.sendBooleanOrMsg(true);
										}
										else
											Client.sendBooleanOrMsg(false);
										booleanList.clear();
									}
									else  // root node starts broad cast
									{
										if(booleanList.contains(true))
										{
											// next iteration/round
											Client.sendBraodCastMsg(true);

											// start next round
											//TRPi= Client.storeCheckpoint.peek();
											// send update messages to neighbors
											UPDTDi = false;
											Client.sendUpdateMessgae();
										}
										else
										{
											// terminate
											Client.sendBraodCastMsg(false);
											isFirstrecoverymsg = true;
											recoveryMode= false;
											firstRound = true;
											System.out.println("Recovery Sequence"+recoverySequence);
											recoverySequence++;
											Client.checkpointSize = Client.storeCheckpoint.size();

											if(recoveryInitiator)
											{
												recoveryInitiator = false;
											}
											int[] finalClock= Client.vectorClock;
											Client.writeToFile(finalClock);
											Client.resendApplicationMessage();
											//System.out.println("sent resend message " + Client.currentNode);
										}
										booleanList.clear();
									}


								}

							}
						}
					}

				}
				else if(objRec.getMessageType()==4)  // convergeCast
				{
					convergecastNieghborCount++;
					booleanList.add(objRec.isUPDTD());


					if(convergecastNieghborCount==children.size() && booleanList.size()>convergecastNieghborCount)
					{
						convergecastNieghborCount=0;
						if(!isRootNode)
						{
							if(booleanList.contains(true))
							{
								Client.sendBooleanOrMsg(true);
							}
							else
								Client.sendBooleanOrMsg(false);
							booleanList.clear();
						}
						else  // root node starts broad cast
						{
							if(booleanList.contains(true))
							{
								// next iteration/round
								Client.sendBraodCastMsg(true);
								//Thread.sleep(1000);

								// start next round, send update messages to neighbors
								UPDTDi = false;
								Client.sendUpdateMessgae();
							}
							else
							{
								// terminate
								Client.sendBraodCastMsg(false);
								//Thread.sleep(1000);
								isFirstrecoverymsg = true;
								recoveryMode= false;
								firstRound = true;
								System.out.println("Recovery Sequence"+recoverySequence);
								recoverySequence++;
								Client.checkpointSize = Client.storeCheckpoint.size();

								if(recoveryInitiator)
								{
									recoveryInitiator = false;
								}
								int[] finalClock= Client.vectorClock;
								Client.writeToFile(finalClock);
								Client.resendApplicationMessage();
								//System.out.println("sent resend message "+ Client.currentNode);
								
							}
							booleanList.clear();
						}

					}

				}
				else if(objRec.getMessageType()==5) // broadcast
				{
					
					broadcastMsgSeen=  true;
					// if not a termination msg, next round nextRound = true;
					if(objRec.isUPDTD())
					{
						Client.sendBraodCastMsg(true);
						//Thread.sleep(1000);

						// send update messages to neighbors
						UPDTDi = false;
						Client.sendUpdateMessgae();

						// if received update message before broadcast message
						if(!bufferList.isEmpty())
						{
							for(Message msg: bufferList)
							{
								// check with each neighbor
								int id = msg.getSenderId();
								if(Client.received[id] > msg.getSent())
								{
									UPDTDi = true;
									Client.storeCheckpoint.pop();
									boolean temp =true;
									while(temp)
									{
										CheckPoints TRPi = Client.storeCheckpoint.peek();
										if(TRPi.rcvd[id] == msg.getSent())
										{
											temp=false;
											Client.sent = TRPi.sent;
											Client.received = TRPi.rcvd;
											Client.vectorClock = TRPi.vectorClock;
											Client.sentList = TRPi.getSubsetOfNeighbors();
											currentTrigerredmsg = TRPi.eventtrigerringMsg;
											Client.totalNoOfMsgsSent = TRPi.totalMsgsSent;
										}
										else
										{
											Client.storeCheckpoint.pop();
										}
									}
								}


							}
							bufferList.clear();

							// leaf will message the neighbors

							if(checknieghborCount==Client.info.get(Client.currentNode).neighbors.size())
							{
								Thread.sleep(20);
								broadcastMsgSeen = false;
								firstRound = false;
								checknieghborCount = 0;
								if(children.isEmpty())
								{
									// start converge cast
									Client.sendLeafMsg();
								}
								else
								{
									booleanList.add(UPDTDi);
									if(booleanList.size()>children.size()){

										convergecastNieghborCount=0;
										if(!isRootNode)
										{
											if(booleanList.contains(true))
											{
												Client.sendBooleanOrMsg(true);
											}
											else
												Client.sendBooleanOrMsg(false);
											booleanList.clear();
										}
									}


								}

							}
						}
					}

							//if terminated 
							else
							{
								Client.sendBraodCastMsg(false);
								//Thread.sleep(1000);

								isFirstrecoverymsg = true;
								recoveryMode= false;
								firstRound = true;
								recoverySequence++;
								Client.checkpointSize = Client.storeCheckpoint.size();// + Client.discardedCheckpoint.size();
								if(recoveryInitiator)
								{
									recoveryInitiator = false;

								}
								int[] finalClock= Client.vectorClock;
								Client.writeToFile(finalClock);
								Client.resendApplicationMessage();
								
								// if resend message is received before broadcast
								if(!resendBufferList.isEmpty())
								{
									for(Message msg: resendBufferList)
									{
									int senderid= msg.getSenderId();

									//update vector clock

									int[] localClock = msg.getClock();
									for(int i=0; i<Client.numberofNodes;i++)
									{
										if(Client.vectorClock[i]< localClock[i])
											Client.vectorClock[i]= localClock[i];
									}
									Client.vectorClock[Client.currentNode]++;
									//Client.vectorClock[Client.currentNode] = Client.vectorClock[Client.currentNode]+1;
									if(Client.received[senderid]< msg.getSent())
									{
										currentTrigerredmsg = msg.getSenderId();
										//update received vector

										Client.received[msg.getSenderId()]++;

										if(Client.maxNumber > Client.totalNoOfMsgsSent)
										{
											Client.sendApplMessage();
										}

									}
								}
									resendBufferList.clear();	
								}
							}


						
				}
				else if(objRec.getMessageType()==6 && messageSeen)  // spanning tree message
				{
					// no need of nack, just ignore

				}
				//when message received for the first time
				else if(objRec.getMessageType()==6 && !messageSeen)
				{
					messageSeen= true;
					parentNode= objRec.getSenderId();
					Client.initiateClientType2(parentNode); // send ack
					Client.initiateClient();// send msgs to neighbors

				}
				else if(objRec.getMessageType()==7)
				{
					int senderid= objRec.getSenderId();
					children.add(senderid);
				}
				else if(objRec.getMessageType()==8)
				{
					if(broadcastMsgSeen || isRootNode)
					{
						//System.out.println("resend message is received after broadcast");
					int senderid= objRec.getSenderId();

					//update vector clock

					int[] localClock = objRec.getClock();
					for(int i=0; i<Client.numberofNodes;i++)
					{
						if(Client.vectorClock[i]< localClock[i])
							Client.vectorClock[i]= localClock[i];
					}
					Client.vectorClock[Client.currentNode]++;
					//Client.vectorClock[Client.currentNode] = Client.vectorClock[Client.currentNode]+1;
					if(Client.received[senderid]< objRec.getSent())
					{
						currentTrigerredmsg = objRec.getSenderId();
						//update received vector

						Client.received[objRec.getSenderId()]++;

						if(Client.maxNumber > Client.totalNoOfMsgsSent)
						{
							Client.sendApplMessage();
						}

					}
					
					}
					else
					{
						//System.out.println("adding resend message to resend bufferList");
						resendBufferList.add(objRec);
					}
				}


			}
			catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


	}
}
