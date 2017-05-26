package com.example.android.testing.uiautomator.BasicSample;

import android.app.ActivityManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.junit.Test;
import org.junit.runner.RunWith;

import ru.gg.lib.LibAll;
import ru.gg.lib_gwt.Const;
import ru.gg.lib_gwt.ILog;
import ru.gg.lib_gwt.LibAllGwt;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RunWith(AndroidJUnit4.class)
public class AppInstallerByIntegrationUiTest {

public static boolean CONNECT_TO_JRE_SERVER = true;

public static final ILog log = new ILog() {

	@Override
	public String info(String s) {
		Log.i("GG", s);
		if(CONNECT_TO_JRE_SERVER) {
			LibAll.request(Const.ANDROID_LOG_SERVER + "/info").body(s).post();
		}
		return s;
	}

	@Override
	public void error(String s) {
		Log.e("GG", s);
		if(CONNECT_TO_JRE_SERVER) {
			LibAll.request(Const.ANDROID_LOG_SERVER + "/error").body(s).post();
		}
	}
};
private UiDevice mDevice;
List<ElementAndAction> handlers = new CopyOnWriteArrayList<>();
private boolean pause;

@Test
public void startAppInstaller() {
	log.info("startAppInstaller");
	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	StrictMode.setThreadPolicy(policy);
	mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
	new AsyncTask<Void, Void, Void>() {
		@Override
		protected Void doInBackground(Void... voids) {
			while(true) {
				LibAll.sleep(500);
				for(ElementAndAction elementAndAction : handlers) {
					AccessibilityNodeInfo element = searchUiElement(elementAndAction.id, elementAndAction.contentDesc);
					if(element != null) {
						elementAndAction.action.doAction(element);
					}
				}
				if(CONNECT_TO_JRE_SERVER) {
					if(LibAllGwt.strEquals(LibAll.request(Const.ANDROID_BREAK_SERVICE).get().str, Const.BREAK)) {
						log.info("break");
						System.exit(0);
					}
					if(LibAllGwt.strEquals(LibAll.request(Const.ANDROID_BACK_SERVICE).get().str, Const.BACK)) {
						log.info("back");
						mDevice.pressKeyCode(KeyEvent.KEYCODE_BACK);
					}
					String str = LibAll.request(Const.ANDROID_PAUSE_SERVICE).get().str;
//					log.info("pause: " + str);
					if(LibAllGwt.strEquals(str, Const.TRUE)) {
						pause = true;
					} else {
						pause = false;
					}
				}
			}
		}
	}.execute();

	while(true) {
		LibAll.sleep(5 * 1000);
		if(!pause) {
			log.info("launch application");
			launchApplication(Const.ANDROID_APP);
			LibAll.sleep(60 * 1000);
			while(!pause) {
				LibAll.sleep(2000);
				List<RelativeTouchPoint> points = new ArrayList<>();
				points.add(new ConcreteTouchPoint(30, 289));
				points.add(new ConcreteTouchPoint(302, 50));
				points.add(new ConcreteTouchPoint(23, 238));
				points.add(new ConcreteTouchPoint(302, 50));
				points.add(new ConcreteTouchPoint(588, 288));
				points.add(new ConcreteTouchPoint(588, 288));
				for(RelativeTouchPoint point : points) {
					double x = point.getX() * mDevice.getDisplayWidth() + (Math.random()-0.5)*3;
					double y = point.getY() * mDevice.getDisplayHeight() + (Math.random()-0.5)*3;
					mDevice.click((int)x, (int)y);
					LibAll.sleep(3000);
				}
			}
			log.info("kill application");
			killApplication(Const.ANDROID_APP);
			LibAll.sleep(5 * 1000);
		}
	}
}

private boolean installApplication(String appPackageName) {
	if(!isPackageExisted(appPackageName)) {
		openGooglePlayOnAppPage(appPackageName);
		click(waitUiElement("com.android.vending:id/buy_button"));
		click(waitUiElement("com.android.vending:id/continue_button"));
		long startTime = new Date().getTime();
		while(!isPackageExisted(appPackageName)) {
			if(new Date().getTime() > startTime + 60 * 60 * 1000) {
				return false;
			}
			LibAll.sleep(500);
		}
	} else {
		log.info("package " + appPackageName + " allready exist's");
		return true;
	}
	return true;
}

public boolean isPackageExisted(String targetPackage) {
	PackageManager pm = InstrumentationRegistry.getInstrumentation().getContext().getPackageManager();
	try {
		PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
	} catch(PackageManager.NameNotFoundException e) {
		return false;
	}
	return true;
}

private AccessibilityNodeInfo scrollDownToUiElement(String pid) {
	AccessibilityNodeInfo searched = searchUiElement(pid);
	if(searched != null) {
		return searched;
	}
	dragPageDown();
	LibAll.sleep(300);
	waitUiThread();
	return scrollDownToUiElement(pid);
}

private void launchApplication(String packageName) {
	killApplication(packageName);
	Intent intent = InstrumentationRegistry.getInstrumentation().getContext().getPackageManager().getLaunchIntentForPackage(packageName);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	InstrumentationRegistry.getContext().startActivity(intent);
}

public void click(AccessibilityNodeInfo element) {
	Rect outBounds = new Rect();
	element.getBoundsInScreen(outBounds);
	mDevice.click(outBounds.centerX(), outBounds.centerY());
//        LibAll.sleep(100);
//        mDevice.click(outBounds.centerX(), outBounds.centerY());
}

private void killApplication(String packageName) {
	LibAll.nativeCmd("am force-stop " + packageName).root().execute();
	ActivityManager activityManager = (ActivityManager) (InstrumentationRegistry.getTargetContext().getSystemService(Context.ACTIVITY_SERVICE));
	for(ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
		if(packageName.equals(processInfo.processName)) {
			log.error("packageName.equals(processInfo.processName)");
		}
	}
//        activityManager.killBackgroundProcesses(packageName);
}

private void openGooglePlayOnAppPage(final String appPackageName) {
	killApplication("com.android.vending");
//        try {
	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	InstrumentationRegistry.getTargetContext().startActivity(intent);
//        InstrumentationRegistry.getContext().startActivity(intent);
//        } catch (android.content.ActivityNotFoundException anfe) {
//            InstrumentationRegistry.getTargetContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
//        }
}

private void dragPageDown() {
	mDevice.drag(mDevice.getDisplayWidth() - 1, (int) (mDevice.getDisplayHeight() * 0.8), mDevice.getDisplayWidth() - 1, 0, 30);
}

private void pasteTextFromClipboard(final String text) {
	LibAll.sleep(2000);
//        for (final char c : text.toCharArray()) {
	waitUiThread(new IAction() {
		@Override
		public void doAction() {
//                    while(true) {
			LibAll.sleep(100);
			final ClipboardManager clipboard = (ClipboardManager) InstrumentationRegistry.getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
//                        String charStr = new String(new char[]{c});
//                        clipboard.setPrimaryClip(ClipData.newHtmlText("simple text", charStr, charStr));
//                        clipboard.setPrimaryClip(ClipData.newPlainText("simple text", charStr));
//                        if(clipboard.getPrimaryClip().getItemAt(0).getText() == charStr) {
//                            break;
//                        }
//                    }
			clipboard.setPrimaryClip(ClipData.newHtmlText("simple text", text, text));
		}
	});
	LibAll.sleep(100);
	mDevice.pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON);
//        }
	LibAll.sleep(2000);
//        mDevice.pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON);
//        LibAll.sleep(500);
//        mDevice.pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON);
//        LibAll.sleep(500);
}

private void waitUiThread(IAction actionInUiThread) {
	LibAll.sleep(50);
	WaitUIThreadActionRunner clipboardRunnable = new WaitUIThreadActionRunner(actionInUiThread);
	new Handler(Looper.getMainLooper()).post(clipboardRunnable);
	while(!clipboardRunnable.complete) {
		LibAll.sleep(50);
	}
}

private void waitUiThread() {
	waitUiThread(new IAction() {
		@Override
		public void doAction() {
		}
	});
}

private void removeAllHandlers() {
	handlers = new ArrayList<>();
}

private void addHandler(final ElementAndAction handler) {
	handlers.add(handler);
}

private AccessibilityNodeInfo waitUiElements(ElementAndAction[] idsAndAction, ICondition breakCondition) {
	String logStr = "wait ";
	for(ElementAndAction elements : idsAndAction) {
		logStr += " id=" + elements.id + " desc=" + elements.contentDesc;
	}
	log.info(logStr);
	LibAll.sleep(150);
	waitUiThread();
//        LibAndroid.sleep(150);
	AccessibilityNodeInfo result = null;
	while(result == null) {
		if(breakCondition != null && breakCondition.validate()) {
			log.info("break condition to be");
			return null;
		}
		for(ElementAndAction idAndAction : idsAndAction) {
			result = searchUiElement(idAndAction.id, idAndAction.contentDesc);
			if(result != null) {
				log.info("item found id = " + idAndAction.id + " desc = " + idAndAction.contentDesc);
				idAndAction.action.doAction(result);
				break;
			}
		}
		LibAll.sleep(300);
		waitUiThread();
	}
	waitUiThread();
//        LibAndroid.sleep(150);
	return result;
}

private AccessibilityNodeInfo waitUiElement(String id, ICondition breakCondition) {
	return waitUiElements(new ElementAndAction[]{new ElementAndAction(id, new IActionWithUiElement() {
		@Override
		public void doAction(AccessibilityNodeInfo uiElement) {

		}
	})}, breakCondition);
}

private AccessibilityNodeInfo waitUiElement(String id) {
	return waitUiElement(id, new ICondition() {
		@Override
		public boolean validate() {
			return false;
		}
	});
}

private AccessibilityNodeInfo searchUiElement(String pid, String contentDesc) {
	return searchUiElement(pid, contentDesc, InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow());
}

private AccessibilityNodeInfo searchUiElement(String pid) {
	return searchUiElement(pid, null);
}

private AccessibilityNodeInfo searchUiElement(String pid, String contentDesc, AccessibilityNodeInfo parent) {
	if(parent == null) {
		return null;
	}
	for(int i = 0; i < parent.getChildCount(); i++) {
		AccessibilityNodeInfo child = parent.getChild(i);
		if(child == null) {
			continue;
		}

		if(pid != null && pid.equals(child.getViewIdResourceName())) {
			if(contentDesc != null) {
				if(child.getContentDescription() != null && contentDesc.contentEquals(child.getContentDescription())) {
					return child;
				}
			} else {
				return child;
			}
		}
		if(contentDesc != null && child.getContentDescription() != null && contentDesc.contentEquals(child.getContentDescription())) {
			if(pid != null) {
				if(pid.equals(child.getViewIdResourceName())) {
					return child;
				}
			} else {
				return child;
			}
		}

		AccessibilityNodeInfo inChild = searchUiElement(pid, contentDesc, child);
		if(inChild != null) {
			return inChild;
		}
	}
	return null;
}


}

