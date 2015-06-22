package com.maffiou.kidCraft;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HtmlGen {
	private Logger myLog;
	private HttpServer server;
	private String pageHeader;
	private String pageFooter;
	private String tableEntry;

	KidCraft myPlugIn;

	HtmlGen(KidCraft javaPlugin) {
		myLog = Bukkit.getLogger();
		myPlugIn = javaPlugin;

		/* Some default settings for the web server */
		myPlugIn.config.addDefault("pageTemplate", "default");
		myPlugIn.config.addDefault("port", "8000");
		myPlugIn.config.addDefault("admin", "password");

		prepareWebPage();
		start();
	}

	public void prepareWebPage() {
		String webpage = null;
		try {

			String pageTemplatePath = myPlugIn.config.getString("pageTemplate");
			if(pageTemplatePath == null || pageTemplatePath.equals("default")) {

				myLog.info("Using default page template");
				InputStream input = getClass().getResourceAsStream("PageTemplate.html");

				try {
					byte[] data = new byte[input.available()];

					input.read(data);
					input.close();
					webpage= new String(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				String webpagePath = myPlugIn.getDataFolder().getPath()+"//"+pageTemplatePath;
				myLog.info("Using local template file: "+pageTemplatePath+"...");
				File file = new File(webpagePath);
				FileReader reader;

				reader = new FileReader(file);

				char[] chars = new char[(int) file.length()];
				reader.read(chars);
				reader.close();

				webpage = new String(chars);
			}
			String[] a = webpage.split("%TEMPLATE%");

			pageHeader = a[0];
			pageFooter = a[2];
			tableEntry = a[1];

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void start() {
		int port = Integer.parseInt(myPlugIn.config.getString("port"));
		myLog.info("Starting server on port: "+port);
		try {
			server = HttpServer.create(new InetSocketAddress(port), 0);
			server.createContext("/test",new MyHandler("/test"));
			server.createContext("/ajax", new MyHandler("/ajax"));
			server.setExecutor(null);
			server.start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			server.stop(1);
		} catch (Exception e){
			
		}
	}

	class MyHandler implements HttpHandler {
		String myContext;
		MyHandler(String context) {
			myContext = context;
		}

		void sendResponse(HttpExchange t, String response) {
			try {
				t.sendResponseHeaders(200, response.length());

				OutputStream os = t.getResponseBody();

				os.write(response.getBytes());
				os.flush();
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			if(myContext=="/test") {
				String webpage = pageHeader;
				for(String player: myPlugIn.pm.getPlayerList()) {
					webpage+=tableEntry.replace("%NAME%", player).replace("%TIME%", ""+myPlugIn.pm.getPlayTime(player)/60);
				}
				webpage+=pageFooter;
				sendResponse(t,webpage);

			} else if (myContext =="/ajax") {
				String uri =t.getRequestURI().toString();
				myLog.info(uri);
				Pattern p1 = Pattern.compile("^.*time_plus_(.*).*$");
				Pattern p2 = Pattern.compile("^.*time_minus_(.*).*$");
				Pattern p3 = Pattern.compile("^.*gift_(.*).*$");
				Pattern p4 = Pattern.compile("^.*updateAll.*$");
				Pattern p5 = Pattern.compile("^.*status_(\\d*)_(.*).*$");

				Matcher m1 = p1.matcher(uri);
				Matcher m2 = p2.matcher(uri);
				Matcher m3 = p3.matcher(uri);
				Matcher m4 = p4.matcher(uri);
				Matcher m5 = p5.matcher(uri);

				if(m1.matches()) {
					String player = m1.group(1);
					myPlugIn.pm.setPlayTime(player, myPlugIn.pm.getPlayTime(player)+60);
					sendResponse(t,"[{\"time_"+player+"\":\"&nbsp;"+(myPlugIn.pm.getPlayTime(player)/60)+" min&nbsp;\"}]");
				}
				else if(m2.matches()) {
					String player = m2.group(1);
					myPlugIn.pm.setPlayTime(player, myPlugIn.pm.getPlayTime(player)-60);
					sendResponse(t,"[{\"time_"+player+"\":\"&nbsp;"+(myPlugIn.pm.getPlayTime(player)/60)+" min&nbsp;\"}]");
				}
				else if(m3.matches()) {
					String player = m3.group(1);
					myPlugIn.pm.setGiftState(player, !myPlugIn.pm.getGiftState(player));
					sendResponse(t,"[{\"gift_"+player+"\":"+myPlugIn.pm.getGiftState(player)+"}]");
				}
				else if(m4.matches()) {
					String response =new String("[");
					for(String player: myPlugIn.pm.getPlayerList()) {
						response+="{\"gift_"+player+"\":"+myPlugIn.pm.getGiftState(player)+"},";
						response+="{\"time_"+player+"\":\"&nbsp;"+myPlugIn.pm.getPlayTime(player)/60+" min&nbsp;\"},";
						response+="{\"status_"+myPlugIn.pm.getPlayerStatus(player)+"_"+player+"\":true},";
					}
					response+="{\"\":\"\"}]";
					sendResponse(t, response);
				}
				else if(m5.matches()) {
					String player = m5.group(2);
					int newStatus = Integer.parseInt(m5.group(1));
					myPlugIn.updatePlayerStatus(player, newStatus);
					sendResponse(t,"{\"status_"+myPlugIn.pm.getPlayerStatus(player)+"_"+player+"\":true}");
				}
				else {
					sendResponse(t,"[]");
				}
			}
		}
	}
}
