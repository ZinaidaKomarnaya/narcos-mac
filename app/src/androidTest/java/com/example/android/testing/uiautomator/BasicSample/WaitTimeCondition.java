package com.example.android.testing.uiautomator.BasicSample;

public class WaitTimeCondition implements ICondition {

    private final long deadlineTime;

    public WaitTimeCondition(int ms) {
        deadlineTime = System.currentTimeMillis() + ms;
    }

    @Override
    public boolean validate() {
        return System.currentTimeMillis() > deadlineTime;
    }
}
