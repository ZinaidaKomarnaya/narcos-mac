package com.example.android.testing.uiautomator.BasicSample;

public class WaitUIThreadActionRunner implements Runnable {
    public volatile boolean complete = false;
    private IAction action;
    public WaitUIThreadActionRunner(IAction action) {
        this.action = action;
    }
    @Override
    public void run() {
        action.doAction();
        complete = true;
    }
}
