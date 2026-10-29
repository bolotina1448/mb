package dev.blockanim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

/** First-person item transforms for every block-hit mode. */
public final class Animations {

    private static final float PI = (float) Math.PI;

    /** Set by the ItemRenderer mixin at the start of each hand render. */
    public static float partialTicks;

    private Animations() {
    }

    // ------------------------------------------------------------- entry points

    /** Full transform for a sword while blocking (replaces vanilla). */
    public static void applyBlock(float equip, float swing) {
        GlStateManager.translate(AnimConfig.x, AnimConfig.y, AnimConfig.z);

        float s = MathHelper.sin(MathHelper.sqrt_float(swing) * PI);

        switch (AnimConfig.mode) {
            case VANILLA:
                transform(equip, 0.0F);
                block();
                break;

            case OLD:
                transform(equip, swing);
                block();
                break;

            case EXHIBITION:
                transform(equip / 2.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
                GlStateManager.rotate(-s * 31.0F, 1.0F, 0.0F, 2.0F);
                GlStateManager.rotate(-s * 33.0F, 1.5F, s / 1.1F, 0.0F);
                block();
                break;

            case SWANK:
                transform(equip / 2.0F, swing);
                GlStateManager.rotate(s * 30.0F / 2.0F, -s, 0.0F, 9.0F);
                GlStateManager.rotate(s * 40.0F, 1.0F, -s / 2.0F, 0.0F);
                block();
                break;

            case SWING:
                transform(equip / 2.0F, swing);
                GlStateManager.rotate(-s * 40.0F / 2.0F, s / 2.0F, 0.0F, 9.0F);
                GlStateManager.rotate(-s * 30.0F, 1.0F, s / 2.0F, 0.0F);
                block();
                break;

            case SIGMA:
                transform(equip * 0.5F, 0.0F);
                GlStateManager.rotate(-s * 55.0F / 2.0F, -8.0F, 0.0F, 9.0F);
                GlStateManager.rotate(-s * 45.0F, 1.0F, s / 2.0F, 0.0F);
                block();
                break;

            case PUSH:
                GlStateManager.translate(-s * 0.05F, s * 0.04F, -s * 0.25F);
                transform(equip, 0.0F);
                block();
                break;

            case SLIDE:
                GlStateManager.translate(-s * 0.15F, -s * 0.03F, 0.0F);
                transform(equip, 0.0F);
                GlStateManager.rotate(-s * 20.0F, 0.0F, 0.0F, 1.0F);
                block();
                break;

            case TAP:
                GlStateManager.translate(0.0F, -s * 0.12F, 0.0F);
                transform(equip, 0.0F);
                GlStateManager.rotate(-s * 15.0F, 1.0F, 0.0F, 0.0F);
                block();
                break;

            case AVATAR:
                avatar(equip, swing);
                block();
                break;

            case SPIN:
                transform(equip, 0.0F);
                float angle = (System.currentTimeMillis() % 1000L) / 1000.0F * 360.0F;
                GlStateManager.rotate(angle, 0.0F, 0.0F, -1.0F);
                block();
                break;

            default:
                transform(equip, 0.0F);
                block();
                break;
        }

        scale();
    }

    /** Normal (not blocking) item: vanilla transform plus optional offset/scale. */
    public static void applyNormal(float equip, float swing) {
        if (AnimConfig.enabled && AnimConfig.alwaysOffset) {
            GlStateManager.translate(AnimConfig.x, AnimConfig.y, AnimConfig.z);
            transform(equip, swing);
            scale();
        } else {
            transform(equip, swing);
        }
    }

    // ------------------------------------------------------------- state

    public static boolean isBlockItem(ItemStack stack) {
        return stack != null && stack.getItemUseAction() == EnumAction.BLOCK;
    }

    /** Menu is open with preview on and a sword in hand. */
    public static boolean isPreviewing(ItemStack held) {
        Minecraft mc = Minecraft.getMinecraft();
        return AnimConfig.enabled && AnimConfig.preview
                && mc.currentScreen instanceof AnimGui
                && isBlockItem(held);
    }

    /** Fake block-hit swing for the menu preview. */
    public static float previewSwing() {
        long period = Math.max(100L, (long) (AnimConfig.swingTicks(6) * 50L * 1.6F));
        float t = (System.currentTimeMillis() % period) / (float) period;
        return Math.min(1.0F, t * 1.6F);
    }

    // ------------------------------------------------------------- vanilla copies

    /** Copy of ItemRenderer.transformFirstPersonItem (1.8.9). */
    public static void transform(float equipProgress, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    /** Copy of ItemRenderer.doBlockTransformations (1.8.9). */
    public static void block() {
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
    }

    private static void avatar(float equipProgress, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -40.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    private static void scale() {
        float s = AnimConfig.scale;
        if (s != 1.0F) {
            GlStateManager.scale(s, s, s);
        }
    }
}
