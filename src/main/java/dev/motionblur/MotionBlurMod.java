package dev.motionblur;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
        modid = MotionBlurMod.MODID,
        name = "Motion Blur",
        version = MotionBlurMod.VERSION,
        acceptedMinecraftVersions = "[1.8.9]",
        acceptableRemoteVersions = "*",
        guiFactory = "dev.motionblur.GuiFactory"
)
public class MotionBlurMod {

    public static final String MODID = "motionblur";
    public static final String VERSION = "1.1.0";

    public static Configuration config;

    /** Включён ли эффект. */
    public static boolean enabled = true;
    /** Сила размытия 1..10. */
    public static int strength = 6;
    /** Размытие при повороте камеры 0..10 (5 = как настоящая камера). */
    public static int cameraBlur = 6;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        syncConfig();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide() == Side.CLIENT) {
            ClientSetup.init();
        }
    }

    public static void syncConfig() {
        enabled = config.getBoolean("enabled", Configuration.CATEGORY_GENERAL, true,
                "Enable motion blur");
        strength = config.getInt("strength", Configuration.CATEGORY_GENERAL, 6, 1, 10,
                "Blur strength: 1 = barely visible, 10 = very heavy trails");
        cameraBlur = config.getInt("cameraBlur", Configuration.CATEGORY_GENERAL, 6, 0, 10,
                "Smooth streak when turning the camera: 0 = off, 5 = real camera, 10 = exaggerated");
        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        config.get(Configuration.CATEGORY_GENERAL, "enabled", true).set(value);
        config.save();
    }

    public static void setStrength(int value) {
        strength = Math.max(1, Math.min(10, value));
        config.get(Configuration.CATEGORY_GENERAL, "strength", 6).set(strength);
        config.save();
    }

    public static void setCameraBlur(int value) {
        cameraBlur = Math.max(0, Math.min(10, value));
        config.get(Configuration.CATEGORY_GENERAL, "cameraBlur", 6).set(cameraBlur);
        config.save();
    }
}
