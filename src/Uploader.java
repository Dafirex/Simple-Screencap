import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;


public class Uploader {
	private enum CharType{
		UPPERCASE, LOWERCASE, NUMBER
	}
	
	final private static int nameLen = 6;
	private AmazonS3 s3;
	private String bucketName;
	private String domainName;
	private int x1;
	private int y1;
	private int x2;
	private int y2;
	
	public Uploader(int x1, int y1, int x2, int y2, BasicAWSCredentials cred, String bucket, String domain){
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(cred)).build();
		bucketName = bucket;
		domainName = domain;
	}
	
	public void upload(){	
		//Height and Width must be > 0

		if(x2-x1 <= 0|| y2-y1 <= 0){
			System.out.println("Invalid Area Size");
			return;
		}
		String fileName;
		fileName = generateName();
		Rectangle screenRect = new Rectangle(x1, y1, x2 - x1, y2 - y1);
		try{
	    	Robot robot = new Robot();
	    	BufferedImage screenFullImage = robot.createScreenCapture(screenRect);
	    	File file = new File(fileName);
	        ImageIO.write(screenFullImage, "png", file);
			s3.putObject(bucketName, fileName, file);
			s3.setObjectAcl(bucketName, fileName, CannedAccessControlList.PublicRead);
			file.delete();
			
	    }
	    catch(AWTException | IOException ex){
	    	System.err.println(ex);
	    }

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Clipboard clipboard = toolkit.getSystemClipboard();
		StringSelection strSel = new StringSelection(domainName + "/" + fileName);
		clipboard.setContents(strSel, null);
	}
	
	private String generateName(){
		String fileName = "";
		String upperCase = "BCDEFGHIJKLMNPQRSTVWXYZ";
		String lowerCase = upperCase.toLowerCase();
		String numbers = "1256789";
		CharType[] types = new CharType[nameLen];
		for(int i = 0; i < nameLen; i++){
			double x = Math.random();
			if(x < 0.33)
				types[i] = CharType.UPPERCASE;
			else if(x > 0.34 && x < .66)
				types[i] = CharType.LOWERCASE;
			else
				types[i] = CharType.NUMBER;
		}
		
		int num = x1 * x2 + y1 * y2 + (int) (1000 * Math.random());
		int index = 0;
		
		for(int i = 0; i < nameLen; i++){
			switch(types[i]){
				case UPPERCASE:
					index = num % 23;
					fileName += upperCase.charAt(index);
					break;
				case LOWERCASE:
					index = num % 23;
					fileName += lowerCase.charAt(index);
					break;
				case NUMBER:
					index = num % 7;
					fileName += numbers.charAt(index);
					break;					
				default:
					index = num % 7;
					fileName += numbers.charAt(index);
					break;
			}

			num /= 7;
		}
		fileName += ".png";
		return fileName;
	}
}
