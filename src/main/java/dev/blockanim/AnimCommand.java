package dev.blockanim;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/** /anim — open the menu. /anim <mode> — switch mode, e.g. /anim exhibition */
public class AnimCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "anim";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("animations", "blockanim");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/anim [mode]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            ClientEvents.openMenuNextTick = true;
            return;
        }
        String name = args[0];
        for (AnimMode m : AnimMode.values()) {
            if (m.name().equalsIgnoreCase(name) || m.displayName.equalsIgnoreCase(name)) {
                AnimConfig.mode = m;
                AnimConfig.enabled = true;
                AnimConfig.save();
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE
                        + "[Animations] " + EnumChatFormatting.WHITE + "Mode: " + m.displayName));
                return;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (AnimMode m : AnimMode.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(m.displayName);
        }
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Modes: " + sb));
    }
}
