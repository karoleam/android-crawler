package com.nofatclips.crawler.automation.utils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import com.nofatclips.crawler.Resources;

public class AndroidConsoleSocket {

	public static void send(String cmd)
	{
		try {
			Socket s = new Socket("10.0.2.2", com.nofatclips.crawler.planning.Resources.EMULATOR_PORT);

			//scrive
			OutputStream out = s.getOutputStream();
			PrintWriter output = new PrintWriter(out);
			output.println(cmd);
			output.flush();
			
			//legge
			//BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
			//String st = input.readLine();

			s.close();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public static void callNumber(String number)
	{
		AndroidConsoleSocket.send("gsm call " + number);
	}
	
	public static void hangUp(String number)
	{
		AndroidConsoleSocket.send("gsm cancel " + number);
	}
	
	public static void sendSMS(String number, String message)
	{
		AndroidConsoleSocket.send("sms send "+number+" \""+message+"\"");
	}
}
