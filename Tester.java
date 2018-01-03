import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

public class Tester {
	public static void main(String[] args) throws FileNotFoundException 
	{
		String num = args[0];
		Scanner sc = null;
		String token = null;
		String[] tokens = null;
		String filename = null;
		HashMap<String, int[]> tempMap = new HashMap<String, int[]>();
		HashMap<String, HashMap<String, int[]>> finalMap = new HashMap<String, HashMap<String, int[]>>();
		int numberOfNodes = Integer.parseInt(num); // /*Client.numberofNodes;*/ 5;
		System.out.println("numberOfNodes ==> " + numberOfNodes);
		System.out.println("==========================");
		for(int i=0;i<numberOfNodes;i++){
			filename = "Node-" + (i) + ".txt";
			System.out.println("opening file ==> " + filename);
			sc = new Scanner(new File(filename));
			while(sc.hasNext()){
				token = sc.nextLine();
				tokens = token.split(" ");
				if(tokens.length != numberOfNodes+1){
					System.out.println("More number of values encountered against a particular rollback. Exiting the loop!");
					System.exit(0);
				}
				int[] array = new int[tokens.length-1];
				for(int ii=1;ii<tokens.length;ii++){
					array[ii-1] = Integer.parseInt(tokens[ii]); 
				}
				tempMap.put(tokens[0], array);
			}
			finalMap.put(filename, tempMap);
			tempMap = new HashMap<String, int[]>();
			System.out.println("==========================");
			sc.close();
		}
		HashSet<String> attributes = new HashSet<String>();
		Iterator<Entry<String, HashMap<String, int[]>>> itr = finalMap.entrySet().iterator();
		while(itr.hasNext()){
			Entry<String, HashMap<String, int[]>> e = itr.next();
			System.out.println(e.getKey() + " ==> ");
			Iterator<Entry<String, int[]>> itr1 = e.getValue().entrySet().iterator();
			while(itr1.hasNext()){
				Entry<String, int[]> e1 = itr1.next();
				String key = e1.getKey();
				String s = "";
				for(int i=0;i<e1.getValue().length;i++){
					s = s + e1.getValue()[i] + "\t";
				}
				System.out.println(key + " ==> " + s);
				attributes.add(key);
			}
			System.out.println("------------------------------------------------");
		}
		for (String attribute : attributes) {
			Iterator<Entry<String, HashMap<String, int[]>>> itr1 = finalMap.entrySet().iterator();
			while(itr1.hasNext()){
				Entry<String, HashMap<String, int[]>> e1 = itr1.next();
				int[] array1 = e1.getValue().get(attribute);
				//System.out.println(e1.getKey().substring(5,6));
				int process1= Integer.parseInt(e1.getKey().substring(5,6));
				Iterator<Entry<String, HashMap<String, int[]>>> itr2 = finalMap.entrySet().iterator();
				while(itr2.hasNext()){
					Entry<String, HashMap<String, int[]>> e2 = itr2.next();
					if (!e1.getKey().equals(e2.getKey())) {
						int[] array2 = e2.getValue().get(attribute);
						boolean output = lessThanEqual(array1,process1, array2);
						if (output == true) {
							System.out.println("FAILURE!! EXITING!!");
							String s1 = "";
							for (int i = 0; i < array1.length; i++) {
								s1 = s1 + array1[i] + "\t";
							}
							String s2 = "";
							for (int i = 0; i < array2.length; i++) {
								s2 = s2 + array2[i] + "\t";
							}
							System.out.println(s1 + "\n and \n" + s2);
							System.out.println("For further humiliation, error occured in " + e1.getKey()
									+ "\n and " + e2.getKey());
							System.exit(0);
						} 
					}
				}
			}
		}
		System.out.println("Congratulations! it worked!");
	}
	
	private static boolean lessThanEqual(int[] array1, int process1, int[] array2){	
		for(int i=0;i<array1.length;i++){
			for(int j=0;j<array2.length;j++){
				if (i==process1 && j==process1) {
					if (array1[process1] > array2[process1] || (array1[process1]==0 && array2[process1]==0)) {
						return false;
					} 
				}
			}
		}
		return true;
	}
}
