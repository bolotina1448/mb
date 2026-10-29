package dev.motionblur;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/** /motionblur (alias /mb): on | off | toggle | 1..10 */
public class MotionBlurCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "motionblur";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("mb");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/mb <on|off|1-10> | /mb camera <0-10>";
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
            ClientSetup.sendStatus();
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GRAY + "Usage: " + getCommandUsage(sender)));
            return;
        }

        String arg = args[0].toLowerCase();
        if (arg.equals("on")) {
            MotionBlurMod.setEnabled(true);
        } else if (arg.equals("off")) {
            MotionBlurMod.setEnabled(false);
        } else if (arg.equals("camera") || arg.equals("cam")) {
            if (args.length < 2) {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.GRAY + "Camera blur: " + MotionBlurMod.cameraBlur
                                + " (0-10). Usage: /mb camera <0-10>"));
                return;
            }
            try {
                MotionBlurMod.setCameraBlur(Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.RED + "Usage: /mb camera <0-10>"));
                return;
            }
        } else if (arg.equals("toggle")) {
            MotionBlurMod.setEnabled(!MotionBlurMod.enabled);
        } else {
            int value;
            try {
                value = Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.RED + "Usage: " + getCommandUsage(sender)));
                return;
            }
            MotionBlurMod.setStrength(value);
            MotionBlurMod.setEnabled(true);
        }
        ClientSetup.sendStatus();
    }
}
