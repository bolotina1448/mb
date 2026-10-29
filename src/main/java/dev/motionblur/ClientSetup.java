package dev.motionblur;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public final class ClientSetup {

    private static KeyBinding toggleKey;

    private ClientSetup() {
    }

    public static void init() {
        toggleKey = new KeyBinding("Toggle Motion Blur", Keyboard.KEY_M, "Motion Blur");
        ClientRegistry.registerKeyBinding(toggleKey);

        MinecraftForge.EVENT_BUS.register(new MotionBlurRenderer());
        MinecraftForge.EVENT_BUS.register(new ClientSetup());

        ClientCommandHandler.instance.registerCommand(new MotionBlurCommand());
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (toggleKey.isPressed()) {
            MotionBlurMod.setEnabled(!MotionBlurMod.enabled);
            sendStatus();
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (MotionBlurMod.MODID.equals(event.modID)) {
            MotionBlurMod.syncConfig();
        }
    }

    public static void sendStatus() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        String state = MotionBlurMod.enabled
                ? EnumChatFormatting.GREEN + "ON"
                : EnumChatFormatting.RED + "OFF";
        mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.AQUA + "[Motion Blur] " + state
                        + EnumChatFormatting.GRAY + " | strength: "
                        + EnumChatFormatting.WHITE + MotionBlurMod.strength));
    }
}
