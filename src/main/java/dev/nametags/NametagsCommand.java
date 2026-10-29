package dev.nametags;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/** /nametags [on|off|walls|shadow|opacity <0-100>] (alias /nt) */
public class NametagsCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "nametags";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("nt");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/nt <on|off|walls|shadow|opacity 0-100>";
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
        if (args.length > 0) {
            String a = args[0].toLowerCase();
            if (a.equals("on")) {
                NametagsMod.enabled = true;
            } else if (a.equals("off")) {
                NametagsMod.enabled = false;
            } else if (a.equals("walls")) {
                NametagsMod.throughWalls = !NametagsMod.throughWalls;
            } else if (a.equals("shadow")) {
                NametagsMod.textShadow = !NametagsMod.textShadow;
            } else if (a.equals("opacity") && args.length > 1) {
                try {
                    NametagsMod.opacity = Math.max(0, Math.min(100, Integer.parseInt(args[1])));
                } catch (NumberFormatException e) {
                    usage(sender);
                    return;
                }
            } else {
                usage(sender);
                return;
            }
            NametagsMod.save();
        }
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GRAY + "[Nametags] "
                + EnumChatFormatting.GRAY + "enabled: " + on(NametagsMod.enabled)
                + EnumChatFormatting.GRAY + " | walls: " + on(NametagsMod.throughWalls)
                + EnumChatFormatting.GRAY + " | shadow: " + on(NametagsMod.textShadow)
                + EnumChatFormatting.GRAY + " | opacity: " + EnumChatFormatting.WHITE + NametagsMod.opacity + "%"));
    }

    private void usage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: " + getCommandUsage(sender)));
    }

    private static String on(boolean v) {
        return v ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF";
    }
}
