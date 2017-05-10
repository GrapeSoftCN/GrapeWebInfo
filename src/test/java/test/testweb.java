package test;

import httpServer.booter;

public class testweb {
	public static void main(String[] args) {
		booter booter = new booter();
		 try {
		 System.out.println("GrapeWeb!");
		 System.setProperty("AppName", "GrapeWeb");
		 booter.start(1005);
		} catch (Exception e) {
		}
	}
}
