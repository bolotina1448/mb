package dev.blockanim;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** All settings of the Animations menu, saved to config/blockanim.cfg. */
public final class AnimConfig {

    private static final String CAT = Configuration.CATEGORY_GENERAL;
    private static Configuration config;

    public static boolean enabled = true;
    public static AnimMode mode = AnimMode.EXHIBITION;
    public static float x = 0.0F;
    public static float y = 0.0F;
    public static float z = 0.0F;
    public static float scale = 1.0F;
    /** 1.0 = vanilla, 0.5 = twice slower swing, 2.0 = twice faster. */
    public static float swingSpeed = 1.0F;
    /** Apply X/Y/Z and scale to the item even when not blocking. */
    public static boolean alwaysOffset = false;
    /** Show arm swing when you click while blocking (like 1.7). Visual only. */
    public static boolean visualBlockhit = true;
    /** Animate the held sword while the menu is open. */
    public static boolean preview = true;

    private AnimConfig() {
    }

    public static void load(File file) {
        config = new Configuration(file);
        enabled = config.getBoolean("enabled", CAT, true, "Enable custom animations");
        mode = AnimMode.byName(config.getString("mode", CAT, AnimMode.EXHIBITION.name(), "Block-hit animation mode"));
        x = config.getFloat("x", CAT, 0.0F, -1.0F, 1.0F, "Item X offset");
        y = config.getFloat("y", CAT, 0.0F, -1.0F, 1.0F, "Item Y offset");
        z = config.getFloat("z", CAT, 0.0F, -1.0F, 1.0F, "Item Z offset");
        scale = config.getFloat("scale", CAT, 1.0F, 0.3F, 2.0F, "Item scale");
        swingSpeed = config.getFloat("swingSpeed", CAT, 1.0F, 0.2F, 2.0F, "Swing speed (1 = vanilla)");
        alwaysOffset = config.getBoolean("alwaysOffset", CAT, false, "Apply offset/scale when not blocking too");
        visualBlockhit = config.getBoolean("visualBlockhit", CAT, true, "Swing arm when clicking while blocking (visual only)");
        preview = config.getBoolean("preview", CAT, true, "Preview animation while the menu is open");
        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void save() {
        if (config == null) {
            return;
        }
        config.get(CAT, "enabled", true).set(enabled);
        config.get(CAT, "mode", AnimMode.EXHIBITION.name()).set(mode.name());
        config.get(CAT, "x", 0.0D).set(x);
        config.get(CAT, "y", 0.0D).set(y);
        config.get(CAT, "z", 0.0D).set(z);
        config.get(CAT, "scale", 1.0D).set(scale);
        config.get(CAT, "swingSpeed", 1.0D).set(swingSpeed);
        config.get(CAT, "alwaysOffset", false).set(alwaysOffset);
        config.get(CAT, "visualBlockhit", true).set(visualBlockhit);
        config.get(CAT, "preview", true).set(preview);
        config.save();
    }

    public static void reset() {
        enabled = true;
        mode = AnimMode.EXHIBITION;
        x = 0.0F;
        y = 0.0F;
        z = 0.0F;
        scale = 1.0F;
        swingSpeed = 1.0F;
        alwaysOffset = false;
        visualBlockhit = true;
        preview = true;
    }

    /** Swing length in ticks after the speed setting (vanilla is 6). */
    public static int swingTicks(int vanillaTicks) {
        return Math.max(1, Math.round(vanillaTicks / Math.max(0.05F, swingSpeed)));
    }
}
