/* Message Formats:
 * Type 1: Application Message
 */

import java.io.Serializable;

public class Message implements Serializable {
private static final long serialVersionUID = 3884398797680355305L;
int messageType;
int senderId;
int[] clock;
String msg;
int sent;
boolean UPDTD;
public boolean isUPDTD() {
	return UPDTD;
}
public void setUPDTD(boolean uPDTD) {
	UPDTD = uPDTD;
}
public int getSent() {
	return sent;
}
public void setSent(int sent) {
	this.sent = sent;
}
public String getMsg() {
	return msg;
}
public void setMsg(String msg) {
	this.msg = msg;
}
public int[] getClock() {
	return clock;
}
public void setClock(int[] clock) {
	this.clock = clock;
}
public int getMessageType() {
	return messageType;
}
public void setMessageType(int messageType) {
	this.messageType = messageType;
}
public int getSenderId() {
	return senderId;
}
public void setSenderId(int senderId) {
	this.senderId = senderId;
}


}
