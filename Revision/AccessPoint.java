import java.util.ArrayList;
import java.util.Scanner;

public class AccessPoint extends Thread {
	private int sifs;
	private int difs;
	private int slotTime;
	private int CWmin;
	private int CWmax = 1023;									//CWmax is set a 1023 because all typical parameters have CWmax = 1023
	private int CW;
	private int nav;
	private int numberOfStations;
	private boolean status = true;								//true = idle, false = busy;
	private ArrayList<Station> s = new ArrayList<Station>();	//contains list of stations
	
	public void run() {
		
		//List of typical parameters
		System.out.println("Typical Parameter Values:");
		System.out.println("DS PHY: Slot time = 20, SIFS = 10, CWmin = 31, CWmax = 1023");
		System.out.println("FH PHY: Slot time = 50, SIFS = 28, CWmin = 15, CWmax = 1023");
		System.out.println("11a: Slot time = 9, SIFS = 16, CWmin = 15, CWmax = 1023");
		System.out.println("11b: Slot time = 20, SIFS = 10, CWmin = 31, CWmax = 1023");
		System.out.println("11g: Slot time = 20 or 9, SIFS = 10, CWmin = 15 or 31, CWmax = 1023");

		Scanner scan = new Scanner(System.in);
		
		//Input # of stations
		System.out.println("Enter the number of stations (at least 4):");
		numberOfStations = scan.nextInt();
		
		//Input slot time
		System.out.println("Enter slot time:");
		slotTime = scan.nextInt();
		
		//Input SIFS
		System.out.println("Enter SIFS time");
		sifs = scan.nextInt();
		
		//Input CWmin
		System.out.println("Enter CWmin time");
		CWmin = scan.nextInt();
		CW = CWmin;
		
		//Close scanner
		scan.close();
		
		/**
		 * Creates and adds number of stations to ArrayList<Station> s
		 * Station: Station name, station number, slot time, sifs, backoff = 0, cwmin, cwmax, this instance)
		 */
		for (int i=0; i<numberOfStations; i++) {
			s.add(new Station("Station " + (i+1), i, slotTime, sifs, 0, CWmin, CWmax, this));
		}
		
		/**
		 * Starts each station
		 */
		for (int i=0; i<numberOfStations; i++) {
			s.get(i).run();
		}
		
		/**
		 * Decrements the backoff for each station.
		 */
		while(numberOfStations > 0) {
			printBackoff();
			decrementBackoff();
			backoffCheck();
			System.out.println("AP idle");
		}
		System.out.println("Program end.");
	}
	
	/**
	 * Persistence strategy
	 * Goes through stations to decrement backoff
	 */
	private void decrementBackoff() {
		
		try {										//Wait DIFS
			Thread.sleep(difs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		for(int i = 0; i < numberOfStations; i++) {	//increments backoff for each station
			s.get(i).backoffIncrement();
		}
	}
	
	private void backoffCheck() {
		for(int i = 0; i < numberOfStations; i++) {	//station checks if RTS
			s.get(i).backoffCheck();
		}
	}
	
	/**
	 * Station sends RTS to AP and AP sends NAV to all other stations
	 * @param s
	 */
	public boolean rts(Station s) {
		try {
			Thread.sleep(sifs);						//wait SIFS
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (status) {								//if AP is idle
			if(!checkDuplicateBackoff(s)) {			//handle collisions, continue on if none.
				status = false;
				notifyObserver();
				nav(s.getStationNumber());			//Other stations receive NAV
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Detects collision. 
	 * If collision is found, both stations stop transmission and rescheduled according to exponential backoff
	 * Returns true if there are duplicates
	 * @return
	 */
	public boolean checkDuplicateBackoff(Station st) {
		int[] backoff = new int[numberOfStations];						//holds backoff value for each station
		boolean[] duplicates = new boolean[numberOfStations];			//Array value is true if it is a duplicate
		boolean duplicate = false;
		String str = "Stations ";
		for (int i = 0; i<numberOfStations; i++) {
			backoff[i] = s.get(i).getBackoffCounter();
		}
		for (int i = 0; i<numberOfStations; i++) {
			if (st.getStationNumber() != s.get(i).getStationNumber()) {
				if (st.getBackoffCounter() == backoff[i]) {
					duplicates[i] = true;
					for (int j = 0; j < numberOfStations; j++) {
						if(st.getBackoffCounter() == backoff[j]) {
							duplicates[j] = true;
						}
					}
					duplicate = true;
				}
			}
		}
		if (duplicate) cwUnsuccessful();								//If there is a duplicate, trigger unsuccessful transmission method to increment CW
		for (int i = 0; i < numberOfStations; i++) {					
			if(duplicates[i]) {
				s.get(i).newBackoff();									//Obtain new backoffs for duplicates
				str = str + " " + (s.get(i).getStationNumber()+1);
			}
		}
		if(duplicate) {
			System.out.println(str + " collision detected");
			for (int i = 0; i < numberOfStations; i++) {
				System.out.println(s.get(i).name() + " Backoff: " + s.get(i).getBackoffCounter());
			}
		}
		
		return duplicate;
	}
	
	/**
	 * Send CTS packet after SIFS interval
	 * @param s
	 */
	public int cts(Station s) {
		try {
			Thread.sleep(sifs);							//Wait SIFS
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println(s.name() + " CTS");			//Send CTS
		// cts = SIFS + Frame + SIFS
		int cts = sifs + 2*slotTime + sifs;
		return cts;
	}
	
	/**
	 * Send ACK to Station, reset CW to CWmin, set status to idle
	 * @param s
	 */
	public void ack(Station s) {
		System.out.println(s.name() + " ACK");			//Send ACK
		System.out.println(s.name()+ " Success");
		if (CW != CWmin) {
			CW = CWmin;
			notifyObserver();
			System.out.println("Contention Window Reset: " + CW);	//After successful transmission, reset CW to CWmin
		}
		this.s.remove(s);								//remove station from ArrayList
		numberOfStations--;
		status = true;
		notifyObserver();
	}
	
	/**
	 * For every unsuccessful transmission attempt, double CW + 1 with a maximum of CWmax
	 * This is reset when there is a successful transmission i.e. ACK is called
	 */
	public void cwUnsuccessful() {
		CW = Math.min(2*CW + 1, CWmax);
		notifyObserver();
		System.out.println("Unsuccessful transmission, CW = " + CW);
	}
	
	/**
	 * Sends NAV to all other stations and makes them sleep
	 * @param stationNumber
	 */
	public void nav(int stationNumber) {
		//nav = RTS + SIFS + CTS + SIFS + DATA + SIFS + ACK;
		nav = slotTime + sifs + slotTime + sifs + 2*slotTime + sifs + slotTime;
		for (int i=0; i<numberOfStations; i++) {
			if (i != stationNumber) {
				s.get(i).navSleep(nav);
			}
		}
	}
	
	/**
	 * Returns Contention Window size
	 * @return
	 */
	public int getCW() {
		return CW;
	}
	
	/**
	 * notifies Stations of changes in AccessPoint
	 * Observable/Observer design pattern
	 */
	public void notifyObserver() {
		for(Station s: s) {
			s.update(this);
		}
	}
	
	public void printBackoff() {
		for (int i = 0; i < numberOfStations; i++) {
			System.out.println(s.get(i).name() + " Backoff: " + s.get(i).getBackoffCounter());
		}
	}
}
