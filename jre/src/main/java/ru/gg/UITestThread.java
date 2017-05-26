package ru.gg;

import ru.gg.lib.LibAll;
import ru.gg.lib_gwt.ITerminator;

import java.io.File;

public class UITestThread extends Thread {

    private volatile boolean terminate = false;
    private volatile boolean complete = false;

    @Override
    public void run() {
        super.run();
        LibAll.nativeCmd("./gradlew app:clean").execute();
        LibAll.nativeCmd("./gradlew cAT").path(new File(System.getProperty("user.dir")).getParentFile().getPath()).terminator(new ITerminator() {
            @Override
            public boolean terminated() {
                return terminate;
            }
        }).execute();
        complete = true;
    }

    public void terminate() {
        terminate = true;
    }
    public boolean isComplete() {
        return complete;
    }
}
