package com.anatawa12.mdfServerUtils;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import net.minecraft.command.CommandException;
import net.minecraft.command.WrongUsageException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

public class SupportedJvmEnvironmentImpl implements IJvmEnvironment {
    Configuration config = loadConfig("default");
    Recording recording;

    private Configuration loadConfig(String name) {
        try {
            return Configuration.getConfiguration(name);
        } catch (IOException | ParseException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void onConfig(String fileContent) throws ParseException {
        if (fileContent.equals("default") || fileContent.equals("profile")) {
            config = loadConfig(fileContent);
        } else {
            try {
                config = Configuration.create(new StringReader(fileContent));
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }

    @Override
    public void onStart(String name) {
        if (recording != null) throw new WrongUsageException("recording JFR now!");
        recording = new Recording(config);
        recording.setName(name);
        recording.start();
    }

    @Override
    public byte[] onStop() {
        if (recording == null) throw new WrongUsageException("it's not recording JFR now!");
        recording.stop();
        try {
            Path tempFile = Files.createTempFile(null, null);
            recording.dump(tempFile);
            byte[] result = Files.readAllBytes(tempFile);
            Files.delete(tempFile);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            //noinspection NoTranslation
            throw new CommandException("unexpected io exception occurred");
        } finally {
            recording.close();
            recording = null;
        }
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
