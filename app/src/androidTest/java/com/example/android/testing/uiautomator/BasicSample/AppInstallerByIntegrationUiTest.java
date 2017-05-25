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
import android.provider.Settings;
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
    //todo internet connection lost
    private UiDevice mDevice;
    private long lastActionMillis;
    private AndroidDevice androidDevice;
    List<ElementAndAction> handlers = new CopyOnWriteArrayList<>();

    @Test
    public void startAppInstaller() {
        log.info("startAppInstaller");
        lastActionMillis = System.currentTimeMillis();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if(false) {
            String absolutePath = LibAndroid.getAbsolutePath(true, "build.prop.temp");
            LibAllGwt.Properties properties = LibAllGwt.readProperties(LibAndroid.readTextFile(false, "/system/build.prop"));
            properties.setProperty("ro.product.model", "LG-D802");
            properties.setProperty("ro.product.brand", "lge");
            properties.setProperty("ro.product.name", "g2_tmo_com");
            properties.setProperty("ro.product.device", "g2");
            properties.setProperty("ro.product.manufacturer", "LGE");
            properties.setProperty("ro.build.fingerprint", "lge/g2_tmo_com/g2:4.4.2/KOT49I.D80220b/D80220b.1394112765:user/release-keys");
            LibAndroid.writeTextFile(false, absolutePath, properties.toString());
            LibAll.nativeCmd("mount -o remount,rw -t yaffs2 /dev/block/mtdblock4 /system").root().execute();
            LibAll.nativeCmd("mv -f /system/build.prop /system/build.prop.bak").root().execute();
            LibAll.nativeCmd("busybox cp -f " + absolutePath + " /system/build.prop").root().execute();
            LibAll.nativeCmd("chmod 644 /system/build.prop").root().execute();
            log.info("after write");
            System.exit(0);
        }

        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        androidDevice = LibAll.JSON.toObj(LibAndroid.readTextFile(true, "windows/BstSharedFolder/" + Const.DEVICE_JSON), AndroidDevice.class);
        final AndroidSmallTask androidSmallTask = LibAll.JSON.toObj(LibAndroid.readTextFile(true, "windows/BstSharedFolder/" + Const.RUN_PARAMS_JSON), AndroidSmallTask.class);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                while(true) {
                    LibAll.sleep(500);
                    for (ElementAndAction elementAndAction : handlers) {
                        AccessibilityNodeInfo element = searchUiElement(elementAndAction.id, elementAndAction.contentDesc);
                        if (element != null) {
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
                        String clipboardStr = LibAll.request(Const.CLIPBOARD_SERVICE).get().str;
                        if(clipboardStr.length() > 0) {
                            log.info("clipboard");
                            String[] split = clipboardStr.split("-!-");
                            for (String s : split) {
                                pasteTextFromClipboard(s);
                            }

//                            pasteTextFromClipboard(androidSmallTask.rate.desc);
//                            pasteTextFromClipboard(androidSmallTask.rate.title);
//                            pasteTextFromClipboard(clipboardStr);
                        }
                        if(LibAll.request(Const.CLIPBOARD_SERVICE_FROM_FILE).get().str.contains("yes")) {
                            log.info("clipboard from file");
                            String text = LibAndroid.readTextFile(true, "windows/BstSharedFolder/" + "clipboard.txt");
                            pasteTextFromClipboard(text);
                        }
                    }
                    if(!androidDevice.registered && (System.currentTimeMillis() - lastActionMillis)/1000 > 1*60) {
                        registerAction();
                        mDevice.pressKeyCode(KeyEvent.KEYCODE_BACK);
                    }
                }
            }
        }.execute();

        AccessibilityNodeInfo cancelGps = waitUiElement("android:id/button2", new WaitTimeCondition(30 * 1000));
        if(cancelGps != null) {
            click(cancelGps);
        }

        if (!androidDevice.registered) {
            androidDevice.registered = registerGooglePlayAccount(androidDevice);
        }
        if(Const.ONLY_LOGIN) {
            androidSmallTask.success = androidDevice.registered;
            log.info("before write run_params file");
            LibAndroid.writeTextFile(true, "windows/BstSharedFolder/" + Const.RUN_PARAMS_JSON, LibAll.JSON.toStr(androidSmallTask));
            log.info("after write run_params file");
            LibAll.sleep(3000);

        }
        else {
            LibAndroid.writeTextFile(true, "windows/BstSharedFolder/" + Const.DEVICE_JSON, LibAll.JSON.toStr(androidDevice));
            if (androidDevice.registered) {
                if (installApplication(androidSmallTask.bigTask.appPackage)) {
                    launchApplication(androidSmallTask.bigTask.appPackage);
                    LibAll.sleep(30 * 1000);
                    openGooglePlayOnAppPage(androidSmallTask.bigTask.appPackage);

                    if (androidSmallTask.rate != null) {
                        waitUiElement("com.android.vending:id/launch_button");
                        LibAll.sleep(2000);
                        click(scrollDownToUiElement("com.android.vending:id/star" + androidSmallTask.rate.rating));
                        if(false) {
                            if(androidSmallTask.rate.title != null && androidSmallTask.rate.title.length() > 3) {
//                            waitUiElement("com.android.vending:id/review_title").setText(androidSmallTask.rate.title);
                                click(waitUiElement("com.android.vending:id/review_title"));
                                if("yes".equals(LibAll.request(Const.CLIPBOARD_COMMENT_TITLE).get().str)) {
                                    LibAll.sleep(500);
                                    mDevice.pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON);
                                }
//                            pasteTextFromClipboard(androidSmallTask.rate.title);
                            }
                        }
                        if(androidSmallTask.rate.desc != null && androidSmallTask.rate.desc.length() > 3) {
//                            waitUiElement("com.android.vending:id/review_comment").setText(androidSmallTask.rate.desc);
                            click(waitUiElement("com.android.vending:id/review_comment"));
                            if("yes".equals(LibAll.request(Const.CLIPBOARD_COMMENT_DESC).get().str)) {
                                LibAll.sleep(500);
                                mDevice.pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON);
                            }
//                            pasteTextFromClipboard(androidSmallTask.rate.desc);
                        }
                        for(int i=3; i>0; i--) {
                            log.info("wait for comment approve " + i*10 + " sec");
                            LibAll.sleep(10*1000);
                        }
                        waitUiElements(new ElementAndAction[]{
                                new ElementAndAction("com.android.vending:id/positive_button", new IActionWithUiElement() {
                                    @Override
                                    public void doAction(AccessibilityNodeInfo uiElement) {
                                        click(uiElement);
                                    }
                                }),
                                new ElementAndAction("com.android.vending:id/review_submit_button", new IActionWithUiElement() {
                                    @Override
                                    public void doAction(AccessibilityNodeInfo uiElement) {
                                        click(uiElement);
                                    }
                                })
                        }, null);
                    }
                }
                log.info("Android task success completed :)");
                androidSmallTask.success = true;
                LibAndroid.writeTextFile(true, "windows/BstSharedFolder/" + Const.RUN_PARAMS_JSON, LibAll.JSON.toStr(androidSmallTask));
            } else {
                //Происходит перезагрузка bluestacks
            }
        }
    }

    private boolean installApplication(String appPackageName) {
        if (!isPackageExisted(appPackageName)) {
            openGooglePlayOnAppPage(appPackageName);
            click(waitUiElement("com.android.vending:id/buy_button"));
            click(waitUiElement("com.android.vending:id/continue_button"));
            long startTime = new Date().getTime();
            while (!isPackageExisted(appPackageName)) {
                if (new Date().getTime() > startTime + 60 * 60 * 1000) {
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

    private boolean registerGooglePlayAccount(final AndroidDevice device) {
        registerAction();
        pasteAndroidId();
//        Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        InstrumentationRegistry.getInstrumentation().getContext().startActivity(intent);
        final String testedApplicationPackage = "com.onto.notepad";
        openGooglePlayOnAppPage(testedApplicationPackage);
        click(waitUiElement("com.google.android.gsf.login:id/next_button"));
        addHandler(new ElementAndAction("com.google.android.gsf.login:id/username_edit", new IActionWithUiElement() {
            @Override
            public void doAction(AccessibilityNodeInfo uiElement) {
                log.info("1");
                click(uiElement);
                pasteTextFromClipboard(device.account.mail);
                click(searchUiElement("com.google.android.gsf.login:id/password_edit"));
                pasteTextFromClipboard(device.account.password);
                click(searchUiElement("com.google.android.gsf.login:id/next_button"));
                click(waitUiElement("android:id/button1"));
            }
        }));
        addHandler(new ElementAndAction("com.google.android.gsf.login:id/next_button", new IActionWithUiElement() {
                    @Override
                    public void doAction(AccessibilityNodeInfo uiElement) {
                        log.info("2");
                        click(uiElement);
                    }
                })
        );
        ElementAndAction usageReportHandler = new ElementAndAction("com.google.android.gms:id/usage_reporting_checkbox", new IActionWithUiElement() {//запретить автоматическую отправку статистики
            @Override
            public void doAction(AccessibilityNodeInfo uiElement) {
                log.info("3");
                click(uiElement);
                click(waitUiElement("android:id/button1"));
            }
        });
        addHandler(usageReportHandler);
        addHandler(new ElementAndAction("com.android.vending:id/not_now_button", new IActionWithUiElement() {
            @Override
            public void doAction(AccessibilityNodeInfo uiElement) {
                log.info("4");
                click(uiElement);
            }
        }));
        addHandler(new ElementAndAction(null, "Подтвердите резервный адрес электронной почты", new IActionWithUiElement() {
                    @Override
                    public void doAction(AccessibilityNodeInfo uiElement) {
                        log.info("5");
                        click(uiElement);
                        log.info("click on Подтвердите резервный адрес электронной почты");
                        LibAll.sleep(2 * 1000);
                        pasteTextFromClipboard(device.account.reserveMail);
                        mDevice.pressKeyCode(KeyEvent.KEYCODE_ENTER);
                    }
                })
        );
        addHandler(new ElementAndAction(null, "Your account is disabled Heading", new IActionWithUiElement() {
                    @Override
                    public void doAction(AccessibilityNodeInfo uiElement) {
                        log.info("account disabled");
                        System.exit(0);
                    }
                })
        );
//        waitUiElements(new ElementAndAction[]{new ElementAndAction("com.google.android.gsf.login:id/next_button", new IActionWithUiElement() {
//            @Override
//            public void doAction(AccessibilityNodeInfo uiElement) {
//                click(uiElement);
//            }
//        })}, shopEnableCondition);
        addHandler(new ElementAndAction("com.android.vending:id/positive_button", new IActionWithUiElement() {
            @Override
            public void doAction(AccessibilityNodeInfo uiElement) {
                log.info("6");
                click(uiElement);
            }
        }));

        if(!Const.ONLY_LOGIN) {
            for (int i = 0; i < 10; i++) {
                LibAll.sleep(2000);
                final ICondition breakCondition = new ICondition() {
                    @Override
                    public boolean validate() {
                        return isPackageExisted(testedApplicationPackage);
                    }
                };
                click(waitUiElement("com.android.vending:id/buy_button", breakCondition));
                click(waitUiElement("com.android.vending:id/continue_button", breakCondition));

                if (waitUiElement("com.android.vending:id/alertTitle", new ICondition() {
                    final int initTime = Calendar.getInstance().get(Calendar.SECOND);

                    @Override
                    public boolean validate() {
                        return searchUiElement("com.android.vending:id/buy_button") != null || breakCondition.validate();
//                    AccessibilityNodeInfo accessibilityNodeInfo = searchUiElement("com.android.vending:id/buy_button");
//                    return (Calendar.getInstance().get(Calendar.SECOND) > initTime + 5);
                    }
                }) != null) {
                    click(waitUiElement("android:id/button1"));
                }

                LibAll.sleep(3000);
                if (breakCondition.validate()) {
                    removeAllHandlers();
                    addHandler(usageReportHandler);
                    return true;
                }
            }
            return false;
        } else {
            waitUiElement("com.android.vending:id/buy_button");
            removeAllHandlers();
            addHandler(usageReportHandler);
            return true;
        }
    }

    public boolean isPackageExisted(String targetPackage) {
        PackageManager pm = InstrumentationRegistry.getInstrumentation().getContext().getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private void pasteAndroidId() {
        String bsid = LibAndroid.readTextFile(true, "windows/BstSharedFolder/current_bsid.txt");
        String androidId = bsid.replaceAll("-", "").substring(0, 16);
        ContentResolver contentResolver = InstrumentationRegistry.getTargetContext().getContentResolver();
        Settings.Secure.putString(contentResolver, Settings.Secure.ANDROID_ID, androidId);
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
    }

    private AccessibilityNodeInfo scrollDownToUiElement(String pid) {
        AccessibilityNodeInfo searched = searchUiElement(pid);
        if (searched != null) {
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

    private void registerAction() {
        lastActionMillis = System.currentTimeMillis();
    }

    public void click(AccessibilityNodeInfo element) {
        registerAction();
        Rect outBounds = new Rect();
        element.getBoundsInScreen(outBounds);
        mDevice.click(outBounds.centerX(), outBounds.centerY());
//        LibAll.sleep(100);
//        mDevice.click(outBounds.centerX(), outBounds.centerY());
    }

    private void killApplication(String packageName) {
        LibAll.nativeCmd("am force-stop " + packageName).root().execute();
        ActivityManager activityManager = (ActivityManager) (InstrumentationRegistry.getTargetContext().getSystemService(Context.ACTIVITY_SERVICE));
        for (ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
            if (packageName.equals(processInfo.processName)) {
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
        while (!clipboardRunnable.complete) {
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
        for (ElementAndAction elements : idsAndAction) {
            logStr += " id=" + elements.id + " desc=" + elements.contentDesc;
        }
        log.info(logStr);
        LibAll.sleep(150);
        waitUiThread();
//        LibAndroid.sleep(150);
        AccessibilityNodeInfo result = null;
        while (result == null) {
            if (breakCondition != null && breakCondition.validate()) {
                log.info("break condition to be");
                return null;
            }
            for (ElementAndAction idAndAction : idsAndAction) {
                result = searchUiElement(idAndAction.id, idAndAction.contentDesc);
                if (result != null) {
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
        if (parent == null) {
            return null;
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child == null) {
                continue;
            }

            if (pid != null && pid.equals(child.getViewIdResourceName())) {//todo bad
                if (contentDesc != null) {
                    if (child.getContentDescription() != null && contentDesc.contentEquals(child.getContentDescription())) {
                        return child;
                    }
                } else {
                    return child;
                }
            }
            if (contentDesc != null && child.getContentDescription() != null && contentDesc.contentEquals(child.getContentDescription())) {//todo bad
                if (pid != null) {
                    if (pid.equals(child.getViewIdResourceName())) {
                        return child;
                    }
                } else {
                    return child;
                }
            }

            AccessibilityNodeInfo inChild = searchUiElement(pid, contentDesc, child);
            if (inChild != null) {
                return inChild;
            }
        }
        return null;
    }


}

