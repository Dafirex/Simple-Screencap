import java.awt.AWTException;
import java.awt.Color;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JFrame;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;

import com.amazonaws.auth.BasicAWSCredentials;


public class ScreenCapMain implements NativeKeyListener, NativeMouseListener{
	private enum State{
		WAITING, CAPTURING, UPLOADING
	};
	
	final private static String accessKey = "[ACCESSKEY HERE]";
	final private static String secretKey = "[SECRETKEY HERE]";
	final private static String bucketName = "[BUCKETNAME HERE]";
	final private static String domainName = "[DOMAINNAME HERE]";

	private static State currentState;
	private static int x1 = 0;
	private static int y1 = 0;
	private static int x2 = 0;
	private static int y2 = 0;
	
	private static int imgScale = 1;
	
	private static boolean comboKey = false;
	private static boolean comboKey2 = false;

	private static TrayIcon trayIcon;
	
	public static void main(String[] args) {
		boolean running = true;
		currentState = State.WAITING;
		if(!SystemTray.isSupported()){
			System.out.println("SystemTray not supported");
			return;
		}
		
		try {
			GlobalScreen.registerNativeHook();
		}
		catch (NativeHookException ex) {
			System.err.println("There was a problem registering the native hook.");
			System.err.println(ex.getMessage());
			System.exit(0);
		}
		
		ScreenCapMain main = new ScreenCapMain();
		
		BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		Uploader imageUpload = new Uploader(credentials, bucketName, domainName);
		//Global Key Listener
		GlobalScreen.addNativeKeyListener(main);
		GlobalScreen.addNativeMouseListener(main);
		
		//Disable Logging
		LogManager.getLogManager().reset();
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.OFF);


		//Creates System Tray Icons
		SystemTray tray = SystemTray.getSystemTray();
		Image image = Toolkit.getDefaultToolkit().getImage("resources/trayicon.png");

		PopupMenu trayPopup = new PopupMenu();
		MenuItem close = new MenuItem("Close");
		
		ActionListener exitListen = new ActionListener(){
			public void actionPerformed(ActionEvent e){
				System.exit(0);
			}
		};
		
		close.addActionListener(exitListen);
		trayPopup.add(close);
	    trayIcon = new TrayIcon(image, "Dafirex's Screencap Tool", trayPopup);
	    trayIcon.setImageAutoSize(true);

	    try{
	        tray.add(trayIcon);
	    }catch(AWTException awtException){
	        awtException.printStackTrace();
	    }
	    
	    //JFrame for rectangle selection visual
	    JFrame selection = new JFrame("Selection");
	    selection.setUndecorated(true);
	    selection.setExtendedState(JFrame.MAXIMIZED_BOTH);
        selection.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        selection.pack();
        selection.setLocationRelativeTo(null);
        selection.setBackground(new Color(.5f, .5f, .5f, .3f));
        selection.setAlwaysOnTop(true);
        selection.setVisible(false);
        
        
        
		while(running){
			switch(currentState){

				case WAITING:
					selection.setVisible(false);
					try{
						Thread.sleep(1000);
					}
					catch(InterruptedException e){
						
					}
					
					break;
				case CAPTURING:
					try{
						Thread.sleep(10);
					}
					catch(InterruptedException e){
						
					}
					selection.setVisible(true);
					x2 = MouseInfo.getPointerInfo().getLocation().x;
					y2 = MouseInfo.getPointerInfo().getLocation().y;
					
					int originX;
					int endX;
					int originY;
					int endY;
					
					//Rectangles will not render with negative dimensions
					//This is done to change where the rectangle draws from
					originX = Math.min(x1, x2);
					endX = Math.max(x1, x2);
					originY = Math.min(y1, y2);
					endY = Math.max(y1, y2);
					
					selection.setLocation(originX, originY);
					selection.setSize(endX - originX, endY - originY);
					
					break;
				case UPLOADING:
					selection.setVisible(false);
					//Rectangles will not render with negative dimensions
					//This is done to change where the rectangle draws from
					originX = Math.min(x1, x2);
					endX = Math.max(x1, x2);
					originY = Math.min(y1, y2);
					endY = Math.max(y1, y2);
					imageUpload.uploadImage(originX, originY, endX, endY, imgScale);
					currentState = State.WAITING;
					trayIcon.displayMessage("Screencap Success", imageUpload.getLink(), MessageType.INFO);
					System.out.println("Screencap Success");
					imgScale = 1;
					System.gc();
					break;
				default:
					break;
			}
		}

	}

	@Override
	public void nativeKeyPressed(NativeKeyEvent e) {
		// TODO Auto-generated method stub
		if(e.getKeyCode() == NativeKeyEvent.VC_CONTROL && currentState == State.WAITING){
			comboKey = true;
		}
		else if(e.getKeyCode() == NativeKeyEvent.VC_SHIFT && comboKey && currentState == State.WAITING){
			comboKey2 = true;
		}
		else if(e.getKeyCode() == NativeKeyEvent.VC_A && comboKey && comboKey2 && currentState == State.WAITING){
			comboKey = false;
			comboKey2 = false;
			currentState = State.CAPTURING;
			x1 = MouseInfo.getPointerInfo().getLocation().x;
			y1 = MouseInfo.getPointerInfo().getLocation().y;
			System.out.println("Capture Started");
		}
		
		if(e.getKeyCode() == NativeKeyEvent.VC_ESCAPE && currentState == State.CAPTURING){
			currentState = State.WAITING;
			System.out.println("Capture Canceled");
			imgScale = 1;
		}
		
		//Numbers 1 to 4 for scaling up to 4x
		if(currentState == State.CAPTURING &&( NativeKeyEvent.VC_1 - 1 <= e.getKeyCode() && e.getKeyCode() <= NativeKeyEvent.VC_5 - 1)){
			imgScale = e.getKeyCode() - 1;
			trayIcon.displayMessage("Screencap Scaling", e.getKeyCode() - 1 + "x", MessageType.INFO);
		}
		
	}

	@Override
	public void nativeKeyReleased(NativeKeyEvent e) {
		// TODO Auto-generated method stub
		if(e.getKeyCode() == NativeKeyEvent.VC_CONTROL)
			comboKey = false;
		if(e.getKeyCode() == NativeKeyEvent.VC_SHIFT)
			comboKey2 = false;
		
	}

	@Override
	public void nativeKeyTyped(NativeKeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void nativeMouseClicked(NativeMouseEvent e) {
		switch(currentState){
			case CAPTURING:
				if(e.getButton() == NativeMouseEvent.BUTTON1){
					currentState = State.UPLOADING;
					break;
				}
				else{
					currentState = State.WAITING;
					imgScale = 1;
				}
				break;
			default:
				break;
		}
		
	}

	@Override
	public void nativeMousePressed(NativeMouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void nativeMouseReleased(NativeMouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}
