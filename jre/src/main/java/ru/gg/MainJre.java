package ru.gg;
import org.apache.log4j.Logger;

import java.awt.Label;
import java.awt.Panel;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import ru.gg.lib.LibAll;
import ru.gg.lib_gwt.Const;
import ru.gg.lib_gwt.ILog;
import ru.gg.lib_gwt.LibAllGwt;

public class MainJre {
private static final int WAIT_ANDROID = Const.ONLY_LOGIN ? 7 : 11;
private static JreParams jreParams = new JreParams();

private static final Logger logger = Logger.getLogger(MainJre.class);
public static Label statusLabel;
public static MyButton btn;

public static final ILog log = new ILog() {
	@Override
	public String info(String s) {
		logger.info(s);
		return s;
	}

	@Override
	public void error(String s) {
		logger.error(s);
	}
};
private static AndroidServer androidServer;
private static Label serverStatus;

public static void main(String[] args) {
	androidServer = new AndroidServer(log);
	JFrame frame = new JFrame("Narcos client");
	frame.setSize(600, 600);
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.setVisible(true);
	Panel panel = new Panel();
	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	frame.add(panel);
	panel.add(new JLabel(new ImageIcon("main_theme.png")));
	panel.add(new Label("1. Bluestacks уже должен быть запущен"));
	panel.add(new Label("2. Дождитесь обновления статуса"));
	statusLabel = new Label("Статус: ожидание");
	serverStatus = new Label();
	panel.add(statusLabel);
	panel.add(serverStatus);
	frame.setSize(601, 601);
	fullCycle();
}

private static void fullCycle() {
	long startTime = getUnixTimeSec();
	while(true) {
		checkAndGo();
		LibAll.sleep(15 * 1000);
	}
}

public static boolean isHerokuWorking() {
	LibAll.HttpRequest.Response response = LibAll.request(jreParams.statusUrl).log(MainJre.log).attempts(3).get();
	boolean result = response.success && "working".equals(response.str);
	serverStatus.setText("Состояние сервера narcos-mac.herokuapp.com : " + response.str);
	return result;
}

private static void checkAndGo() {
//	if(isWorking()) {
		int attempts = 0;
		while(true) {
			log.info("attempt " + (++attempts));
			boolean result = startBluestacksAndLaunchGradleUiTest();
			if(result) {
				break;
			}
		}
		stopBluestacks();
//	}
}

private static void stopBluestacks() {
	//todo
	if(false) {
		LibAll.killWinProcessByName("HD-Quit.exe");
		LibAll.killWinProcessByName("BlueStacks.exe");
		LibAll.killWinProcessByName("HD-Agent.exe");
		LibAll.killWinProcessByName("HD-BlockDevice.exe");
		LibAll.killWinProcessByName("HD-Frontend.exe");
		LibAll.killWinProcessByName("HD-Plus-Frontend.exe");
		LibAll.killWinProcessByName("HD-LogRotatorService.exe");
		LibAll.killWinProcessByName("HD-Network.exe");
		LibAll.killWinProcessByName("HD-Service.exe");
		LibAll.killWinProcessByName("HD-Plus-Service.exe");
		LibAll.killWinProcessByName("HD-UpdaterService.exe");
		LibAll.killWinProcessByName("HD-SharedFolder.exe");
		LibAll.killWinProcessByName("HD-Adb.exe");
		LibAll.killWinProcessByName("HD-ApkHandler.exe");
		LibAll.killWinProcessByName("HD-GLCheck.exe");
		LibAll.sleep(1000);
	}
}

private static boolean startBluestacksAndLaunchGradleUiTest() {
	log.info("start bluestacks with gradle ui test, wait device");
	if(true) {
//		LibAll.nativeCmd("open /Applications/BlueStacks.app/").log(log).execute();
	}
	//LibAll.nativeCmd(jreParams.adbPath + " wait-for-device").log(log).execute();
	while(!LibAllGwt.strEquals("1", LibAll.nativeCmd(jreParams.adbPath + " shell getprop sys.boot_completed").execute().resultStr.trim())) {
		try {
			Thread.sleep(2000);
			if(!androidServer.alive()) {
				statusLabel.setText("Статус: ОШИБКА!. Попробуйте перезагрузить компьютер и повторить");
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	log.info("device comming");
	UITestThread uiTestThread = new UITestThread();
	uiTestThread.start();
	long startTime = getUnixTimeSec();
	while(true) {
		LibAll.sleep(1 * 1000);
		if(uiTestThread.isComplete()) {
			uiTestThread.terminate();
			return true;
		}
		if(!isHerokuWorking()) {
//			uiTestThread.terminate();
			androidServer.pause=true;
		} else {
			androidServer.pause=false;
		}
		if(!androidServer.alive()) {
			uiTestThread.terminate();
			return false;
		}
		if(androidServer.alive() && androidServer.isConnected()) {
			statusLabel.setText("Статус: Успешно");
		}
	}
}

private static long getUnixTimeSec() {
	return new Date().getTime() / 1000;
}

}
