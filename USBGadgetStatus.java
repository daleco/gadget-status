
import java.io.BufferedReader;
import java.io.InputStreamReader;

// This class run shell commands in Java and analyze the result. It's checking if the usb port is used as an ethernet gadget
// and if there are any machine connected on the sub network 172.20.10.*
// The main loop run a 1hz. It's a bit slower if nothing is on the subnetwork, because it's waiting for the ping result.
public class USBGadgetStatus extends Thread {

	private boolean hostMode=false;
	private boolean remoteDeviceOnNetwork;
	boolean gadgetConnect=false;
	boolean antennaPlugged=false;
	boolean running=true;

	String gadgetConnected="gadget: high-speed config #1: CDC Ethernet (ECM)";
	String gadgetDisconnected="gadget disconnected";
	String[] commandGadgetStatus={"/bin/sh","-c","dmesg | grep -i gadget |tail -n 1"};

	public USBGadgetStatus() {
		// TODO Auto-generated constructor stub
		// Start in peripheral mode
		hostMode=false;
		remoteDeviceOnNetwork=false;
		System.out.println("Starting USB watcher...");
	} 

	public void run(){

		while(!hostMode){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			checkGadgetStatus(commandGadgetStatus);
			pingGui();
			// add code here to switch the boolean hostMode to true 
			// e.g. switch a jumper that would turn the usb port from peripheral to host
		}
		System.out.println("USBGadgetStatus - hostMode: " + hostMode);
	}

	public boolean isGadgetConnected() {
		return gadgetConnect;
	}

	public void pingGui(){
		// check if there are any machine on the sub network. If yes, the usb ethernet is working
		String output = executeCommand("ping -c 1 172.20.10.255");
		//System.out.print("Looking for 172.20.10.255 - Output = " + output);
		if (output.indexOf("time")>=0){
			remoteDeviceOnNetwork=true;
			System.out.println("Devices on network");
		}else{
			remoteDeviceOnNetwork=false;
			System.out.println ("No device(s) on network");
		}
	}

	public String checkGadgetStatus(String[] command){
		BufferedReader br=null ;
		String line;
		StringBuffer output = new StringBuffer();
		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while((line = br.readLine())!= null){
				//System.out.println("result : "  +line);
				if(isOTGConnected(line)>=0) {
					gadgetConnect=true;
					System.out.println("Gadget connected!");
				}
				if(isOTGDisconnected(line)>=0) {
					gadgetConnect=false;
					System.out.println("Gadget disconnected");
				}
				output.append(line + "\n");
			}
			if(br!=null)
				br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output.toString();
	}

	private String executeCommand(String command) {

		StringBuffer output = new StringBuffer();
		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";			
			while ((line = reader.readLine())!= null) {
				output.append(line + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output.toString();

	}

	public int isOTGConnected(String msg){
		return msg.indexOf(gadgetConnected);
	}

	public int isOTGDisconnected(String msg){
		return msg.indexOf(gadgetDisconnected);
	}

	public void free(){
		running=false;
	}

	public static void main(String[] arg){
		USBGadgetStatus usb = new USBGadgetStatus();
		usb.start();
	}

}
