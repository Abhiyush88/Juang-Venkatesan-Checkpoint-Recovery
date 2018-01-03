import java.util.ArrayList;
import java.util.HashMap;

public class Node {
int id;


String hostName;
int port;
ArrayList<Integer> neighbors = new ArrayList<Integer>();
HashMap<Integer, Integer> failureInfo = new HashMap<Integer, Integer>();
public int getId() {
	return id;
}
public void setId(int id) {
	this.id = id;
}
public String getHostName() {
	return hostName;
}
public void setHostName(String hostName) {
	this.hostName = hostName;
}
public int getPort() {
	return port;
}
public void setPort(int port) {
	this.port = port;
}
public ArrayList<Integer> getNeighbors() {
	return neighbors;
}
public void setNeighbors(ArrayList<Integer> neighbor) {
	this.neighbors = neighbor;
}
}
