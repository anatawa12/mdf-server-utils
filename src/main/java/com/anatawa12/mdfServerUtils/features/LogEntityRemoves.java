package com.anatawa12.mdfServerUtils.features;

import com.anatawa12.mdfServerUtils.MdfServerUtils;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

public class LogEntityRemoves {
    public static Mode mode = Mode.None;

    public static void onUpdateEntities(World world) {
        switch (mode) {
            case None:
                break;
            case Trace:
                logger.trace("unloadedEntityList: " + world.unloadedEntityList);
                logger.trace("unloadedTileEntityList: " + world.field_147483_b);
                break;
            case File:
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true)))) {
                    writer.append("log at ").append(String.valueOf(new Date())).append(System.lineSeparator());
                    writer.append("unloadedEntityList: ").append(String.valueOf(world.unloadedEntityList)).append(System.lineSeparator());
                    writer.append("unloadedTileEntityList: ").append(String.valueOf(world.field_147483_b)).append(System.lineSeparator());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public enum Mode {
        None,
        Trace,
        File,
    }

    private static Logger logger = LogManager.getLogger();
    private static File logFile = new File(MdfServerUtils.logDir, "log-entity-removes.txt");
}
