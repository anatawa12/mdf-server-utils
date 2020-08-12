package com.anatawa12.mdfServerUtils;

import com.anatawa12.mdfServerUtils.features.LogEntityRemoves;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MdfUtilCommand extends CommandBase {
    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    @Override
    public String getCommandName() {
        return "mdf-util";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return usage;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) throw new WrongUsageException(usage);
        SubCommand cmd = subCommands.get(args[0]);
        if (cmd == null) throw new WrongUsageException(usage);
        String usage = "/mdf-util " + args[0] + " " + cmd.usage;
        if (args.length - 1 < cmd.requiredArgs) throw new WrongUsageException(usage);
        try {
            cmd.process.processCommand(sender, args);
        } catch (WrongUsageException e) {
            if (e.getMessage().equals("")) {
                throw new WrongUsageException(usage);
            } else {
                throw e;
            }
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender p_71516_1_, String[] args) {
        if (args.length == 0) {
            return null;
        } else if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(args, subCommands.keySet());
        } else {
            SubCommand cmd = subCommands.get(args[0]);
            if (cmd == null) return null;
            int index = args.length - 1 - 1;

            if (cmd.argPatterns.length <= index) return null;
            return getListOfStringsMatchingLastWord(args, cmd.argPatterns[index]);
        }
    }

    private static final String usage;
    private static final Map<String, SubCommand> subCommands = new HashMap<>();

    static {
        subCommands.put("log-entity-removes", new SubCommand(
                "[trace|file|none]",
                1,
                (sender, args) -> {
                    switch (args[1]) {
                        case "trace":
                            LogEntityRemoves.mode = LogEntityRemoves.Mode.Trace;
                            break;
                        case "file":
                            LogEntityRemoves.mode = LogEntityRemoves.Mode.File;
                            break;
                        case "none":
                            LogEntityRemoves.mode = LogEntityRemoves.Mode.None;
                            break;
                        default:
                            throw new WrongUsageException("");
                    }
                    sender.addChatMessage(new ChatComponentText("successfully change log entity remove " +
                            "state to " + args[1]));
                },
                new String[]{"trace", "file", "none"}
        ));

        usage = "/mdf-util [" + String.join("|", subCommands.keySet()) + "] <args>";
    }

    private static class SubCommand {
        public final String usage;
        public final int requiredArgs;
        public final SubCommandProcess process;
        public final String[][] argPatterns;

        private SubCommand(String usage, int requiredArgs, SubCommandProcess process, String[]... argPatterns) {
            this.usage = usage;
            this.requiredArgs = requiredArgs;
            this.process = process;
            this.argPatterns = argPatterns;
        }
    }

    private interface SubCommandProcess {
        void processCommand(ICommandSender sender, String[] args);
    }
}
