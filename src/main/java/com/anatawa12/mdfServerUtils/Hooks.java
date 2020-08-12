package com.anatawa12.mdfServerUtils;

import com.anatawa12.mdfServerUtils.coremod.MdfServerUtilsTransformer;
import com.anatawa12.mdfServerUtils.features.LogEntityRemoves;
import net.minecraft.world.World;

@SuppressWarnings("unused")
public class Hooks {
    @MdfServerUtilsTransformer.HookAtFirst(owner = World.class, name = "func_72939_s", nameMcp = "updateEntities", args = {}, realArgs = {0})
    public static void onUpdateEntities(World world) {
        LogEntityRemoves.onUpdateEntities(world);
    }
}
