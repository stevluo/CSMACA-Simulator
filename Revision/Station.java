import java.util.Random;

public class Station extends Thread{
	private String name;
	private int stationNumber;
	private int sifs;
	private int difs;
	private int CWmin;
	private int CWmax;
	private int CW;
	private int slotTime;
	private AccessPoint ap;
	private int nav;
	private int backoffCounter;
	private int numberRetries = 0;
	private boolean status = true; // true = idle, false = busy
	private boolean success = false;
	Random r = new Random();
	
	public Station(String name, int sNumber, int slotTime, int sifs, int backoff, int CWmin, int CWmax, AccessPoint ap) {
		this.name = name;
		this.stationNumber = sNumber;
		this.slotTime = slotTime;
		this.sifs = sifs;
		this.difs = sifs + 2*slotTime;
		this.backoffCounter = backoff;
		this.CWmin = CWmin;
		this.CW = CWmin;
		this.CWmax = CWmax;
		this.ap = ap;
		
	}
	
	/**
	 * Starts the thread
	 */
	public void run() {
		backoffCounter = r.nextInt(CW);
		while(success) {
			
		}
	}
	
	/**
	 * When backoff counter reaches 0, send RTS, 
	 * If AP is ready, station receives CTS time and sends.
	 * When AP sends ACK, success = true;
	 * 
	 */
	public void backoffCheck() {
		if (backoffCounter <= 0) {
			System.out.println(name + " RTS");
			if(ap.rts(this)) {									//if Backoff counter = 0, send RTS.
				int cts = ap.cts(this);							//If RTS is received, retrieve CTS + SIFS + FRAME + SIFS + ACK
				try {
					Thread.sleep(cts);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				ap.ack(this);
				success = true;									//success = true will end the thread.
			}
		}
	}
	
	/**
	 * Network Allocation Vector
	 * AP calculates and sends NAV to Station after receiving RTS
	 * Station sleeps for duration of NAV
	 * @param nav
	 */
	public void navSleep(int nav) {
		try {
			this.nav = nav;
			System.out.println(name + " NAV received");
			Thread.sleep(nav);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Decrement backoff counter
	 */
	public void backoffIncrement() {
		backoffCounter -=1;
	}
	
	public void newBackoff() {
		backoffCounter = r.nextInt(CW);
	}
	
	public int getBackoffCounter() {
		return backoffCounter;
	}
	
	public int getStationNumber() {
		return stationNumber;
	}
	
	public String name() {
		return name;
	}

	/**
	 * Observes changes in AccessPoint
	 * Observable/Observer design pattern
	 * @param ap
	 */
	public void update(AccessPoint ap) {
		this.ap = ap;
		this.CW = ap.getCW();
	}
}

