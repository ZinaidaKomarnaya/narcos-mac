package ru.gg;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ru.gg.lib.LibAll;
import ru.gg.lib_gwt.Const;
import ru.gg.lib_gwt.ILog;

public class AndroidServer {

private Server server;
public volatile boolean breakAndroid = false;
public volatile int back = 0;

public AndroidServer(ILog log) {
	try {
		server = new Server(54322);
		server.setHandler(new AbstractHandler() {
			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				response.setCharacterEncoding(Const.UTF_8);
				switch(target) {
					case "/log/info":
						log.info("android: " + LibAll.readBuffer(request.getReader()));
						break;
					case "/log/error":
						log.error("android: " + LibAll.readBuffer(request.getReader()));
						break;
					case "/break":
						for(int i = 0; i < 5; i++) {
							if(breakAndroid) {
								response.getWriter().write(Const.BREAK);
								response.getWriter().flush();
								break;
							}
							LibAll.sleep(100);
						}
						break;
					case "/back":
						for(int i = 0; i < 5; i++) {
							if(back > 0) {
								back--;
								response.getWriter().write(Const.BACK);
								response.getWriter().flush();
								break;
							}
							LibAll.sleep(100);
						}
						break;
					case "/clipboard":
						StringSelection stringSelection = new StringSelection("clipboard data");
						Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
						clpbrd.setContents(stringSelection, null);

						response.getWriter().write("yes");
						response.getWriter().flush();
						break;
				}
//				response.getWriter().write("hello from servlet handler");
//				response.getWriter().flush();
				response.getWriter().close();
			}
		});
		server.start();
	} catch(Exception ex) {
		ex.printStackTrace();
	}
}

public void stop() {
	try {
		server.stop();
	} catch(Exception e) {
		e.printStackTrace();
	}
}
}
