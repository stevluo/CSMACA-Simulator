
public class Station extends Thread implements Observer{
	
	private String name;
	private int stationNumber;
	private int sifs;
	public static int status = 0; 				// 0 = idle, 1 = busy
	private int difs;
	private int cwMin;							//cwMin is also used as backoff
	private int cwMax = 1023;
	private int slotTime;
	private CSMACA ap;
	private int backoffCounter;

	
	public Station(String name, int number, int slotTime, int sifs, int backoff, int CWmin) {
		this.name = name;
		this.sifs = sifs;
		this.cwMin = CWmin;
		this.slotTime = slotTime;
		this.stationNumber = number;
		this.backoffCounter = backoff;
		difs = sifs + 2*slotTime;
		
	}
	
	/**
	 * Decrements backoff
	 * If backoffCounter is 0 and AP is idle, send rts, else exponentially increase backoff timer
	 */
	public void backoff() {
		backoffCounter -= slotTime;							
		if (backoffCounter <= 0) {							
			int s = ap.getStatus();
			if(s == 0) {		//if ap is idle
				ap.setStatus(1);
				ap.rts(this);
				ap.sleep(slotTime, "CTS to " + name);
				ap.sleep(sifs, name + " SIFS");
				ap.sleep(slotTime, name +" Frame");
				ap.ack(this);
				System.out.println(name + " transmission complete");
				
				ap.setStatus(0);
				ap.removeObserver(this);
			} else {
				backoffCounter = Math.min(2*cwMin, cwMax);
			}
		}
	}
	
	public String name() {
		return name;
	}
	public void update(Observable o) {
		ap = (CSMACA)o;
	}
}
