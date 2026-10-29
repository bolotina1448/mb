package dev.motionblur;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

/**
 * Smooth motion blur, tuned to look good at low frame rates (60-80 FPS).
 *
 * Two parts work together:
 *  1. Camera blur: every frame is smeared along the direction the camera
 *     turned since the previous frame, so a fast turn becomes one smooth
 *     streak instead of separate "ghost copies".
 *  2. Accumulation: the previous result is blended in, so everything that
 *     moves leaves a soft fading trail. Trail length is the same at any FPS.
 *
 * The blur is applied right after the world is drawn and before the HUD, so
 * the crosshair, hotbar and chat stay sharp.
 */
public class MotionBlurRenderer {

    private static final int GL_CONSTANT_ALPHA = 0x8003;
    private static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
    private static final int MAX_SAMPLES = 48;

    private static final String VERTEX_SHADER =
            "#version 120\n"
            + "varying vec2 uv;\n"
            + "void main() {\n"
            + "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n"
            + "    uv = gl_MultiTexCoord0.xy;\n"
            + "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 120\n"
            + "uniform sampler2D cur;\n"
            + "uniform sampler2D hist;\n"
            + "uniform vec2 dir;\n"
            + "uniform vec2 uvMax;\n"
            + "uniform float blend;\n"
            + "uniform int samples;\n"
            + "varying vec2 uv;\n"
            + "void main() {\n"
            + "    vec3 acc = vec3(0.0);\n"
            + "    float n = float(samples);\n"
            + "    for (int i = 0; i < " + MAX_SAMPLES + "; i++) {\n"
            + "        if (i >= samples) break;\n"
            + "        float t = (float(i) + 0.5) / n - 0.5;\n"
            + "        vec2 p = clamp(uv + dir * t, vec2(0.0), uvMax);\n"
            + "        acc += texture2D(cur, p).rgb;\n"
            + "    }\n"
            + "    acc /= n;\n"
            + "    vec3 h = texture2D(hist, uv).rgb;\n"
            + "    gl_FragColor = vec4(mix(acc, h, blend), 1.0);\n"
            + "}\n";

    private Framebuffer histA;
    private Framebuffer histB;
    private boolean hasHistory;

    private int program = -1;
    private boolean shaderFailed;
    private int locCur, locHist, locDir, locUvMax, locBlend, locSamples;

    private long lastFrameNanos = -1L;
    private boolean appliedThisFrame;
    private boolean hasLastRotation;
    private float lastYaw, lastPitch;

    // ------------------------------------------------------------------ hooks

    /** Main path: after the world, before the HUD. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        apply(event.partialTicks);
    }

    /** Fallback (F1 / HUD hidden): apply at the end of the frame. */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            appliedThisFrame = false;
            return;
        }
        if (!appliedThisFrame) {
            apply(event.renderTickTime);
        }
    }

    // ------------------------------------------------------------------ core

    private void apply(float partialTicks) {
        appliedThisFrame = true;

        long now = System.nanoTime();
        double dt = lastFrameNanos < 0 ? 1.0 / 60.0 : (now - lastFrameNanos) / 1.0E9;
        lastFrameNanos = now;

        Minecraft mc = Minecraft.getMinecraft();
        if (!MotionBlurMod.enabled || !OpenGlHelper.isFramebufferEnabled()
                || mc.skipRenderWorld || mc.theWorld == null) {
            reset();
            return;
        }

        Framebuffer main = mc.getFramebuffer();
        if (main == null || main.framebufferWidth <= 0 || main.framebufferHeight <= 0) {
            return;
        }
        int w = main.framebufferWidth;
        int h = main.framebufferHeight;
        ensureBuffers(w, h);

        // Trail: how much of the previous image survives after 1/60 s
        double retainPer60 = Math.min(0.95, MotionBlurMod.strength * 0.1);
        dt = Math.max(0.0005, Math.min(0.25, dt));
        float blend = (float) Math.pow(retainPer60, dt * 60.0);
        blend = Math.max(0.0F, Math.min(0.98F, blend));
        if (!hasHistory) {
            blend = 0.0F;
        }

        float[] dir = cameraMotion(mc, partialTicks, w, h);

        beginDraw(w, h);
        if (useShader()) {
            drawShaderPass(main, w, h, blend, dir);
        } else {
            drawFallbackPass(main, w, h, blend);
        }
        main.bindFramebuffer(true);
        endDraw();

        hasHistory = true;
    }

    /** Screen-space shift (in texture UV) caused by the camera turn this frame. */
    private float[] cameraMotion(Minecraft mc, float partialTicks, int w, int h) {
        float[] none = {0.0F, 0.0F};
        Entity view = mc.getRenderViewEntity();
        if (view == null || mc.currentScreen != null || MotionBlurMod.cameraBlur <= 0) {
            hasLastRotation = false;
            return none;
        }
        float yaw = view.prevRotationYaw + (view.rotationYaw - view.prevRotationYaw) * partialTicks;
        float pitch = view.prevRotationPitch + (view.rotationPitch - view.prevRotationPitch) * partialTicks;
        if (!hasLastRotation) {
            lastYaw = yaw;
            lastPitch = pitch;
            hasLastRotation = true;
            return none;
        }
        float dYaw = yaw - lastYaw;
        float dPitch = pitch - lastPitch;
        lastYaw = yaw;
        lastPitch = pitch;

        // Teleport / respawn / huge snap: don't smear the whole screen
        if (Math.abs(dYaw) > 45.0F || Math.abs(dPitch) > 45.0F) {
            return none;
        }

        double vFov = Math.toRadians(Math.max(30.0F, Math.min(110.0F, mc.gameSettings.fovSetting)));
        double tanV = Math.tan(vFov / 2.0);
        double tanH = tanV * ((double) w / (double) h);

        double du = Math.tan(Math.toRadians(dYaw)) / (2.0 * tanH);
        double dv = Math.tan(Math.toRadians(dPitch)) / (2.0 * tanV);

        // cameraBlur 5 = smear over the whole time between frames (real camera)
        double shutter = MotionBlurMod.cameraBlur / 5.0;
        du *= shutter;
        dv *= shutter;

        // Never smear more than 25% of the screen in one frame
        double len = Math.sqrt(du * du + dv * dv);
        if (len > 0.25) {
            du *= 0.25 / len;
            dv *= 0.25 / len;
        }
        return new float[]{(float) du, (float) dv};
    }

    // ------------------------------------------------------------------ passes

    private void drawShaderPass(Framebuffer main, int w, int h, float blend, float[] dir) {
        float uScale = (float) main.framebufferWidth / main.framebufferTextureWidth;
        float vScale = (float) main.framebufferHeight / main.framebufferTextureHeight;

        // How many samples: about one per 1.5 pixels of streak, so it is gap-free
        float pixels = (float) Math.sqrt((dir[0] * w) * (dir[0] * w) + (dir[1] * h) * (dir[1] * h));
        int samples = Math.max(1, Math.min(MAX_SAMPLES, (int) Math.ceil(pixels / 1.5F)));

        // 1) cur (smeared) + histA  ->  histB
        histB.bindFramebuffer(true);
        GlStateManager.disableBlend();

        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.bindTexture(histA.framebufferTexture);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.bindTexture(main.framebufferTexture);
        // smooth sampling between pixels for the streak
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL20.glUseProgram(program);
        GL20.glUniform1i(locCur, 0);
        GL20.glUniform1i(locHist, 1);
        GL20.glUniform2f(locDir, dir[0] * uScale, dir[1] * vScale);
        GL20.glUniform2f(locUvMax,
                uScale - 0.5F / main.framebufferTextureWidth,
                vScale - 0.5F / main.framebufferTextureHeight);
        GL20.glUniform1f(locBlend, blend);
        GL20.glUniform1i(locSamples, samples);

        quad(w, h, uScale, vScale);

        GL20.glUseProgram(0);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.bindTexture(0);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

        // 2) histB -> main (what the player sees)
        main.bindFramebuffer(true);
        drawFramebuffer(histB, w, h);

        // 3) swap: histB becomes the trail for the next frame
        Framebuffer tmp = histA;
        histA = histB;
        histB = tmp;
    }

    /** Old path for very old GPUs without shaders: accumulation only. */
    private void drawFallbackPass(Framebuffer main, int w, int h, float blend) {
        if (blend > 0.0F) {
            main.bindFramebuffer(true);
            GlStateManager.enableBlend();
            GL14.glBlendColor(0.0F, 0.0F, 0.0F, blend);
            GlStateManager.blendFunc(GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA);
            drawFramebuffer(histA, w, h);
            GlStateManager.disableBlend();
        }
        histA.bindFramebuffer(true);
        GlStateManager.disableBlend();
        drawFramebuffer(main, w, h);
    }

    // ------------------------------------------------------------------ setup

    private void ensureBuffers(int w, int h) {
        if (histA == null) {
            histA = new Framebuffer(w, h, false);
            histB = new Framebuffer(w, h, false);
            hasHistory = false;
        } else if (histA.framebufferWidth != w || histA.framebufferHeight != h) {
            histA.createBindFramebuffer(w, h);
            histB.createBindFramebuffer(w, h);
            hasHistory = false;
        }
    }

    private boolean useShader() {
        if (shaderFailed || !OpenGlHelper.shadersSupported) {
            return false;
        }
        if (program == -1) {
            program = buildProgram();
            if (program <= 0) {
                shaderFailed = true;
                return false;
            }
            locCur = GL20.glGetUniformLocation(program, "cur");
            locHist = GL20.glGetUniformLocation(program, "hist");
            locDir = GL20.glGetUniformLocation(program, "dir");
            locUvMax = GL20.glGetUniformLocation(program, "uvMax");
            locBlend = GL20.glGetUniformLocation(program, "blend");
            locSamples = GL20.glGetUniformLocation(program, "samples");
        }
        return true;
    }

    private static int buildProgram() {
        try {
            int vs = compile(GL20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fs = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            if (vs == 0 || fs == 0) {
                return 0;
            }
            int prog = GL20.glCreateProgram();
            GL20.glAttachShader(prog, vs);
            GL20.glAttachShader(prog, fs);
            GL20.glLinkProgram(prog);
            if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                System.err.println("[MotionBlur] shader link failed: "
                        + GL20.glGetProgramInfoLog(prog, 4096));
                return 0;
            }
            return prog;
        } catch (Throwable t) {
            System.err.println("[MotionBlur] shaders unavailable: " + t);
            return 0;
        }
    }

    private static int compile(int type, String src) {
        int sh = GL20.glCreateShader(type);
        GL20.glShaderSource(sh, src);
        GL20.glCompileShader(sh);
        if (GL20.glGetShaderi(sh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("[MotionBlur] shader compile failed: "
                    + GL20.glGetShaderInfoLog(sh, 4096));
            return 0;
        }
        return sh;
    }

    private void reset() {
        hasHistory = false;
        hasLastRotation = false;
    }

    // ------------------------------------------------------------------ GL helpers

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
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
    }

    private static void drawFramebuffer(Framebuffer src, int w, int h) {
        float u = (float) src.framebufferWidth / (float) src.framebufferTextureWidth;
        float v = (float) src.framebufferHeight / (float) src.framebufferTextureHeight;
        GlStateManager.bindTexture(src.framebufferTexture);
        quad(w, h, u, v);
    }

    private static void quad(int w, int h, float u, float v) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(0.0D, h, 0.0D).tex(0.0D, 0.0D).endVertex();
        wr.pos(w, h, 0.0D).tex(u, 0.0D).endVertex();
        wr.pos(w, 0.0D, 0.0D).tex(u, v).endVertex();
        wr.pos(0.0D, 0.0D, 0.0D).tex(0.0D, v).endVertex();
        tessellator.draw();
    }
}
