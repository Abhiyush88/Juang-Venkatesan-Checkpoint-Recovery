import java.util.ArrayList;

public class CheckPoints {
int totalMsgsSent;
int[] sent;
int[] rcvd;
int[] vectorClock;
int eventtrigerringMsg;
ArrayList<Integer> subsetOfNeighbors = new ArrayList<Integer>();

public int getTotalMsgsSent() {
	return totalMsgsSent;
}
public void setTotalMsgsSent(int totalMsgsSent) {
	this.totalMsgsSent = totalMsgsSent;
}
public int[] getSent() {
	return sent;
}
public void setSent(int[] sent) {
	this.sent = sent;
}
public int[] getRcvd() {
	return rcvd;
}
public void setRcvd(int[] rcvd) {
	this.rcvd = rcvd;
}
public int[] getVectorClock() {
	return vectorClock;
}
public void setVectorClock(int[] vectorClock) {
	this.vectorClock = vectorClock;
}
public int getEventtrigerringMsg() {
	return eventtrigerringMsg;
}
public void setEventtrigerringMsg(int eventtrigerringMsg) {
	this.eventtrigerringMsg = eventtrigerringMsg;
}
public ArrayList<Integer> getSubsetOfNeighbors() {
	return subsetOfNeighbors;
}
public void setSubsetOfNeighbors(ArrayList<Integer> subsetOfNeighbors) {
	this.subsetOfNeighbors = subsetOfNeighbors;
}


}
