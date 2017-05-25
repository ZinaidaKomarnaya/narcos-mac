package com.example.android.testing.uiautomator.BasicSample;

public class ElementAndAction {
    public String id;
    public String contentDesc;
    public IActionWithUiElement action;

    public ElementAndAction(String id, IActionWithUiElement action) {
        this.id = id;
        this.action = action;
    }
    public ElementAndAction(String id, String contentDesc, IActionWithUiElement action) {
        this.id = id;
        this.contentDesc = contentDesc;
        this.action = action;
    }
}
