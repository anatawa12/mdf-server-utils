package com.anatawa12.mdfServerUtils;

import java.text.ParseException;

public interface IJvmEnvironment {
    void onConfig(String fileContent) throws ParseException;

    void onStart(String name);

    byte[] onStop();

    boolean isSupported();
}
