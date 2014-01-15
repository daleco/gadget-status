
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CheckSubNetwork extends Thread {

	private boolean hostMode=false;
	private boolean remoteDeviceOnNetwork;
	boolean gadgetConnect=false;
	boolean antennaPlugged=false;
	boolean running=true;

	String gadgetConnected="gadget: high-speed config #1: CDC Ethernet (ECM)";
	String gadgetDisconnected="musb g_ether gadget disconnected";
	String[] commandGadgetStatus={"/bin/sh","-c","dmesg | grep -i gadget |tail -n 1"};

	public static String fakeMsg="Mar 21 02:53:01 phyCORE-AM335x daemon.err inetd[708]: /etc/inetd.conf: No such file or directory" +
			"Mar 21 02:53:04 phyCORE-AM335x auth.info login[713]: root login on 'ttyO0'"+
			"Mar 21 02:53:22 phyCORE-AM335x user.info kernel: [   29.808502]  gadget: high-speed config #1: CDC Ethernet (ECM)";


	public CheckSubNetwork() {
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
			/*
			if( !gadgetConnect || !remoteDeviceOnNetwork){
				// turn from peripheral to host
				try {
					Thread.sleep(1000);
					System.out.println("check for antenna....");
					startWifiAntenna();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				hostMode=true;
			}*/

		}
		System.out.println("USBGadgetStatus - hostMode: " + hostMode);
	}

	public void startWifiAntenna(){
		Thread antenna = new Thread(){
			public void run(){
				while(running){
					checkWifiPlugged();
					if(antennaPlugged)
						try {
							// run the script that init wifi antenna with wpa_supplicant
							System.out.println("run wifi.sh");
							Runtime.getRuntime().exec("/home/checkWiFi2.sh");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		antenna.start();
	}

	public boolean isGadgetConnected() {
		return gadgetConnect;
	}

	public void checkWifiPlugged(){
		BufferedReader br=null ;
		String line;
		try {
			// the wifi device will alway be plugged in that ports
			br = new BufferedReader(new FileReader("/sys/bus/usb/devices/2-1/product"));
			line = br.readLine();
			antennaPlugged=false;
			while(line!=null){
				if(line.indexOf("WifiStationEXT")>=0) {
					antennaPlugged=true;
					System.out.println("Wifi plugged ! ");
				}
				line = br.readLine();
			}
			if(br!=null)
				br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}


	public void pingGui(){
		// check if reachable
		//String output = executeCommand("ping -c 1 192.168.7.255");
		//  System.out.print("Looking for 192.168.7.255 - ");
		String output = executeCommand("ping -c 1 172.20.10.255");
		System.out.print("Looking for 172.20.10.255 - Output = " + output);
		if (output.indexOf("time")>=0){
			remoteDeviceOnNetwork=true;
			System.out.println("Device on network");
		}else{
			remoteDeviceOnNetwork=false;
			System.out.println ("No device on network");
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
				System.out.println("result : "  +line);
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
		CheckSubNetwork usb = new CheckSubNetwork();
		//usb.start();
		//System.out.println("looking for match " + usb.isOTGConnected(fakeMsg));
		//System.out.println("looking for match " + usb.isOTGConnected("Bla blab al"));

		String[] command={"/bin/sh","-c","dmesg | grep -i gadget |tail -n 1"};
		//String[] command={"/bin/sh","-c","ping -c 1 172.20.10.8"};
		//for osx 
	//	String[] command={"/sbin","-c","ping -c 1 172.20.10.255"};
		System.out.println("ping...");
		String output = usb.checkGadgetStatus(command);

		System.out.println("result : " + output);

	}

}
