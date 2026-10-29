package dev.blockanim;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
        modid = BlockAnimMod.MODID,
        name = "Block Animations",
        version = BlockAnimMod.VERSION,
        acceptedMinecraftVersions = "[1.8.9]",
        acceptableRemoteVersions = "*"
)
public class BlockAnimMod {

    public static final String MODID = "blockanim";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        AnimConfig.load(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide() == Side.CLIENT) {
            ClientEvents.init();
        }
    }
}
