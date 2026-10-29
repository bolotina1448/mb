package dev.blockanim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public final class ClientEvents {

    private static KeyBinding menuKey;
    /** Open the menu on the next tick (chat closes itself after a command). */
    public static boolean openMenuNextTick;

    private ClientEvents() {
    }

    public static void init() {
        menuKey = new KeyBinding("Animations menu", Keyboard.KEY_J, "Block Animations");
        ClientRegistry.registerKeyBinding(menuKey);
        MinecraftForge.EVENT_BUS.register(new Handler());
        ClientCommandHandler.instance.registerCommand(new AnimCommand());
    }

    public static class Handler {

        @SubscribeEvent
        public void onKey(InputEvent.KeyInputEvent event) {
            if (menuKey.isPressed()) {
                Minecraft.getMinecraft().displayGuiScreen(new AnimGui());
            }
        }

        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();

            if (openMenuNextTick) {
                openMenuNextTick = false;
                mc.displayGuiScreen(new AnimGui());
                return;
            }

            // Visual block-hit: swing the arm when clicking while blocking.
            // Client-side only, nothing is sent to the server.
            EntityPlayerSP p = mc.thePlayer;
            if (!AnimConfig.enabled || !AnimConfig.visualBlockhit || p == null || mc.currentScreen != null) {
                return;
            }
            if (p.isUsingItem() && Animations.isBlockItem(p.getItemInUse())
                    && mc.gameSettings.keyBindAttack.isKeyDown()) {
                int half = AnimConfig.swingTicks(6) / 2;
                if (!p.isSwingInProgress || p.swingProgressInt >= half || p.swingProgressInt < 0) {
                    p.swingProgressInt = -1;
                    p.isSwingInProgress = true;
                }
            }
        }
    }
}
