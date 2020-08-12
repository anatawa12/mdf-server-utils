package com.anatawa12.mdfServerUtils;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

import java.io.File;
import java.util.Map;

@Mod(modid = MdfServerUtils.MODID)
public class MdfServerUtils {
    @SuppressWarnings("unused")
    @Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new MdfUtilCommand());
    }

    @SuppressWarnings("unused")
    @NetworkCheckHandler
    public boolean networkHandler(Map<String, String> map, Side side) {
        return true;
    }

    @SuppressWarnings("unused")
    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        //noinspection ResultOfMethodCallIgnored
        logDir.mkdirs();
    }

    public static final String MODID = "mdf-utils";

    public static SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

    public static File logDir = new File("mdf-utils-log");
}
