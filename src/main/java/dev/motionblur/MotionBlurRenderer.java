package dev.motionblur;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * Accumulation motion blur.
 *
 * After each frame is rendered (before it is shown on screen), the previous
 * accumulated image is blended on top of it:
 *
 *     result = previous * a + current * (1 - a)
 *
 * and the result is stored for the next frame. Moving things leave a smooth
 * fading trail, like the blur in video edits. The amount "a" is adjusted by
 * frame time, so the trail length looks the same at 60 and 300 FPS.
 */
public class MotionBlurRenderer {

    private static final int GL_CONSTANT_ALPHA = 0x8003;
    private static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;

    private Framebuffer history;
    private boolean hasHistory;
    private long lastFrameNanos = -1L;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        long now = System.nanoTime();
        double dt = lastFrameNanos < 0 ? 1.0 / 60.0 : (now - lastFrameNanos) / 1.0E9;
        lastFrameNanos = now;

        Minecraft mc = Minecraft.getMinecraft();
        if (!MotionBlurMod.enabled || !OpenGlHelper.isFramebufferEnabled() || mc.skipRenderWorld) {
            hasHistory = false;
            return;
        }

        Framebuffer main = mc.getFramebuffer();
        if (main == null) {
            return;
        }
        int w = main.framebufferWidth;
        int h = main.framebufferHeight;
        if (w <= 0 || h <= 0) {
            return;
        }

        if (history == null) {
            history = new Framebuffer(w, h, false);
            hasHistory = false;
        } else if (history.framebufferWidth != w || history.framebufferHeight != h) {
            history.createBindFramebuffer(w, h);
            hasHistory = false;
        }

        // How much of the previous image survives after 1/60 s (strength 10 -> 0.95)
        double retainPer60 = Math.min(0.95, MotionBlurMod.strength * 0.1);
        dt = Math.max(0.0005, Math.min(0.25, dt));
        float alpha = (float) Math.pow(retainPer60, dt * 60.0);
        alpha = Math.max(0.0F, Math.min(0.98F, alpha));

        beginDraw(w, h);

        if (hasHistory) {
            // 1) Blend the accumulated trail over the fresh frame
            main.bindFramebuffer(true);
            GlStateManager.enableBlend();
            GL14.glBlendColor(0.0F, 0.0F, 0.0F, alpha);
            GlStateManager.blendFunc(GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA);
            drawFramebuffer(history, w, h);
        }

        // 2) Save the result as the new trail
        history.bindFramebuffer(true);
        GlStateManager.disableBlend();
        drawFramebuffer(main, w, h);

        // 3) Hand the main framebuffer back to Minecraft
        main.bindFramebuffer(true);
        endDraw();

        hasHistory = true;
    }

    private static void beginDraw(int w, int h) {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, w, h, 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);

        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void endDraw() {
        GlStateManager.disableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
    }

    private static void drawFramebuffer(Framebuffer src, int w, int h) {
        float u = (float) src.framebufferWidth / (float) src.framebufferTextureWidth;
        float v = (float) src.framebufferHeight / (float) src.framebufferTextureHeight;

        src.bindFramebufferTexture();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(0.0D, h, 0.0D).tex(0.0D, 0.0D).endVertex();
        wr.pos(w, h, 0.0D).tex(u, 0.0D).endVertex();
        wr.pos(w, 0.0D, 0.0D).tex(u, v).endVertex();
        wr.pos(0.0D, 0.0D, 0.0D).tex(0.0D, v).endVertex();
        tessellator.draw();

        src.unbindFramebufferTexture();
    }
}
