package Test;

import httpServer.booter;
import nlogger.nlogger;

public class TestWeb {
	public static void main(String[] args) {
		booter booter = new booter();
		try {
			System.out.println("GrapeWebInfo");
			System.setProperty("AppName", "GrapeWebInfo");
			booter.start(1006);
		} catch (Exception e) {
			nlogger.logout(e);
		}
	}
}
