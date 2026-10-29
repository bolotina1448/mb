package dev.nametags.mixin;

import dev.nametags.NametagsMod;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the standard nametag (Render.renderLivingLabel): black background,
 * name drawn at full brightness on top of everything.
 *
 * Vanilla only calls this for names it already shows through walls
 * (not sneaking, not hidden by the server's team settings), so nothing new
 * is revealed — names just become clearly readable.
 */
@Mixin(Render.class)
public abstract class MixinRender {

    @Shadow
    @Final
    protected RenderManager renderManager;

    @Shadow
    public abstract FontRenderer getFontRendererFromRenderManager();

    @Inject(method = "renderLivingLabel", at = @At("HEAD"), cancellable = true)
    private void nametags$render(Entity entity, String str, double x, double y, double z,
                                 int maxDistance, CallbackInfo ci) {
        if (!NametagsMod.enabled || renderManager == null || renderManager.livingPlayer == null) {
            return;
        }
        ci.cancel();

        double distSq = entity.getDistanceSqToEntity(renderManager.livingPlayer);
        if (distSq > (double) (maxDistance * maxDistance)) {
            return;
        }

        FontRenderer font = getFontRendererFromRenderManager();
        float scale = 0.016666668F * 1.6F;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + entity.height + 0.5F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int yOff = "deadmau5".equals(str) ? -10 : 0;
        int half = font.getStringWidth(str) / 2;

        // Black background
        float alpha = NametagsMod.opacity / 100.0F;
        if (alpha > 0.0F) {
            GlStateManager.disableTexture2D();
            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();
            wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(-half - 2, -2 + yOff, 0.0D).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            wr.pos(-half - 2, 9 + yOff, 0.0D).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            wr.pos(half + 2, 9 + yOff, 0.0D).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            wr.pos(half + 2, -2 + yOff, 0.0D).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            tess.draw();
            GlStateManager.enableTexture2D();
        }

        if (NametagsMod.throughWalls) {
            // Full-brightness text, visible through blocks
            font.drawString(str, -half, yOff, 0xFFFFFFFF, NametagsMod.textShadow);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
        } else {
            // Vanilla style: faint through walls, bright when in plain sight
            font.drawString(str, -half, yOff, 553648127, false);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            font.drawString(str, -half, yOff, 0xFFFFFFFF, NametagsMod.textShadow);
        }

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
