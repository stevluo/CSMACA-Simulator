import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class CSMACA extends Thread implements Observable{

	private static ArrayList<Observer> station = new ArrayList<Observer>();
	private static int stations;
	private static int slotTime;
	private static int sifs;
	private static int CWmin;
	private static int status = 0; 								//0 = idle, 1 = busy
	
	public void run() {
		Scanner s = new Scanner(System.in);
		
		//Input # of stations
		System.out.println("Enter the number of stations (at least 4):");
		stations = s.nextInt();
		
		//Input slot time
		System.out.println("Enter slot time:");
		slotTime = s.nextInt();
		
		//Input SIFS
		System.out.println("Enter SIFS time");
		sifs = s.nextInt();
		
		//Input CWmin
		System.out.println("Enter CWmin time");
		CWmin = s.nextInt();
		
		//Close scanner
		s.close();
		
		//Create unique randoms for each station
		ArrayList<Integer> list = new ArrayList<Integer>(stations);
		for (int i = CWmin; i < 1023; i++) {					//chooses number from CWmin to CWmax
		list.add(i);
		}
		Collections.shuffle(list);
		
		//Initialize variables for each station
		for (int i = 0; i<stations; i++) {
			station.add((Observer) new Station("Station "+(i+1), i, slotTime, sifs, list.get(i), CWmin));
		}
		notifyObserver();
		
		//run each station and change backoff after every loop
		while(stations > 0) {
			sleep(sifs + 2*slotTime, " DIFS sleep + wait for remaining backoff");		//DIFS
			checkBackOff(this);
		}		
	}
	
	public void sleep(int time, String action) {
		try {
			sleep(time);
		} catch (InterruptedException e) {
			System.err.println("Interrupted: sleep");
		}
		System.out.println(action);
	}
	
	//Calculate backoff timer for stations
	public void checkBackOff(CSMACA c) {
		for (int i=0; i<station.size(); i++) {
			((Station) station.get(i)).backoff();
		}
	}

	public int getStatus() {
		return status;
	}
	
	public void setStatus(int i) {
		status = i;
		notifyObserver();
	}
	
	public void addObserver(Observer o) {
		station.add(o);
	}

	public void notifyObserver() {
		for(Observer o: station) {
			o.update(this);
		}
	}

	public void rts(Station s) {
		sleep(sifs + sifs + slotTime + sifs + slotTime, s.name() + " RTS");
		
	}

	public void ack(Station s) {
		sleep(slotTime, "ACK for " + s.name());
	}

	public void removeObserver(Station s) {
		station.remove(s);
		stations--;
	}
}
