package com.example.android.testing.uiautomator.BasicSample;

import android.os.Environment;
import ru.gg.lib.LibAll;

import java.io.File;

public class LibAndroid {

    public static String readTextFile(boolean sdCard, String file) {
        return LibAll.textFile(getAbsolutePath(sdCard, file)).waitInfinity().read();
    }

    public static void writeTextFile(boolean sdCard, String file, String text) {
        LibAll.textFile(getAbsolutePath(sdCard, file)).write(text);
    }

    public static String getAbsolutePath(boolean sdCard, String file) {
        File f;
        if(sdCard) {
            f = new File(Environment.getExternalStorageDirectory(), file);
        } else {
            f = new File(file);
        }
        return f.getAbsolutePath();
    }

}
