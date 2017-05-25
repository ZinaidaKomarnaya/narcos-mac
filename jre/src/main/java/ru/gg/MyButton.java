package ru.gg;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MyButton extends Button {
    public MyButton(String label, ActionListener l) {
        super(label);
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        l.actionPerformed(e);
                    }
                }.start();
            }
        });
    }
}
