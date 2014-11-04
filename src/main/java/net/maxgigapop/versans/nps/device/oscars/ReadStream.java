package net.maxgigapop.versans.nps.device.oscars;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ReadStream implements Runnable {
    String name;
    String stream;
    InputStream is;
    Thread thread;      
    public ReadStream(String name, InputStream is) {
        this.name = name;
        this.stream = "";
        this.is = is;
    }       
    public void start () {
        thread = new Thread (this);
        thread.start ();
    }       
    public void run () {
        try {
            InputStreamReader isr = new InputStreamReader (is);
            BufferedReader br = new BufferedReader (isr);   
            while (true) {
                String s = br.readLine ();
                if (s != null)
                	stream += s;
            }
        } catch (Exception ex) {
        	;
        } finally {
        	try {
        		is.close();
        	} catch (Exception ex) {
        		;
        	}
        }
        //System.out.println("ReadStream end "+this.name);
    }
    public void interrupt() {
    	if (!Thread.interrupted()) {
    		thread.interrupt();
    	}
    }
    public String getString() {
    	return stream;
    }
}
