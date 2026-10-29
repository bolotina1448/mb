package dev.nametags;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
        modid = NametagsMod.MODID,
        name = "Black Nametags",
        version = NametagsMod.VERSION,
        acceptedMinecraftVersions = "[1.8.9]",
        acceptableRemoteVersions = "*"
)
public class NametagsMod {

    public static final String MODID = "nametags";
    public static final String VERSION = "1.0.0";

    private static final String CAT = Configuration.CATEGORY_GENERAL;
    private static Configuration config;

    public static boolean enabled = true;
    /** Background darkness 0..100 %. */
    public static int opacity = 65;
    /** Draw the name at full brightness through blocks. */
    public static boolean throughWalls = true;
    public static boolean textShadow = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        enabled = config.getBoolean("enabled", CAT, true, "Enable custom nametags");
        opacity = config.getInt("opacity", CAT, 65, 0, 100, "Black background opacity, %");
        throughWalls = config.getBoolean("throughWalls", CAT, true, "Nametags fully visible through blocks");
        textShadow = config.getBoolean("textShadow", CAT, false, "Text shadow");
        if (config.hasChanged()) {
            config.save();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide() == Side.CLIENT) {
            ClientCommandHandler.instance.registerCommand(new NametagsCommand());
        }
    }

    public static void save() {
        config.get(CAT, "enabled", true).set(enabled);
        config.get(CAT, "opacity", 65).set(opacity);
        config.get(CAT, "throughWalls", true).set(throughWalls);
        config.get(CAT, "textShadow", false).set(textShadow);
        config.save();
    }
}
