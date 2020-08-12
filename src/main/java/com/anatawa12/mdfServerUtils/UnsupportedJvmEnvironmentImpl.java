package com.anatawa12.mdfServerUtils;

import net.minecraft.command.CommandException;

public class UnsupportedJvmEnvironmentImpl implements IJvmEnvironment {
    @Override
    public void onConfig(String fileContent) {
        //noinspection NoTranslation
        throw new CommandException("your server's JVM does not support JFR. please use jdk8u262 or later");
    }

    @Override
    public void onStart(String name) {
        //noinspection NoTranslation
        throw new CommandException("your server's JVM does not support JFR. please use jdk8u262 or later");
    }

    @Override
    public byte[] onStop() {
        //noinspection NoTranslation
        throw new CommandException("your server's JVM does not support JFR. please use jdk8u262 or later");
    }

    @Override
    public boolean isSupported() {
        return false;
    }
}
