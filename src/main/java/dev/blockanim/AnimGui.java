package dev.blockanim;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiSlider;

/**
 * "Animations" tab: a compact panel on the left so the sword on the right
 * stays visible and shows the animation live (preview).
 */
public class AnimGui extends GuiScreen implements GuiSlider.ISlider {

    private static final int PANEL_X = 10;
    private static final int PANEL_W = 160;
    private static final int ROW = 22;

    private GuiButton enabledBtn;
    private GuiButton modeBtn;
    private GuiButton alwaysBtn;
    private GuiButton blockhitBtn;
    private GuiButton previewBtn;

    private GuiSlider xSlider;
    private GuiSlider ySlider;
    private GuiSlider zSlider;
    private GuiSlider scaleSlider;
    private GuiSlider speedSlider;

    private int panelTop;
    private int panelBottom;

    @Override
    public void initGui() {
        buttonList.clear();
        int x = PANEL_X;
        int w = PANEL_W;
        int totalHeight = 12 * ROW + 16;
        panelTop = Math.max(6, (height - totalHeight) / 2);
        int y = panelTop + 18;

        enabledBtn = add(new GuiButton(0, x, y, w, 20, ""));
        y += ROW;
        add(new GuiButton(1, x, y, 20, 20, "<"));
        modeBtn = add(new GuiButton(2, x + 22, y, w - 44, 20, ""));
        add(new GuiButton(3, x + w - 20, y, 20, 20, ">"));
        y += ROW;

        xSlider = slider(10, x, y, "X: ", -1.0, 1.0, AnimConfig.x);
        y += ROW;
        ySlider = slider(11, x, y, "Y: ", -1.0, 1.0, AnimConfig.y);
        y += ROW;
        zSlider = slider(12, x, y, "Z: ", -1.0, 1.0, AnimConfig.z);
        y += ROW;
        scaleSlider = slider(13, x, y, "Scale: ", 0.3, 2.0, AnimConfig.scale);
        y += ROW;
        speedSlider = slider(14, x, y, "Swing speed: ", 0.2, 2.0, AnimConfig.swingSpeed);
        y += ROW;

        alwaysBtn = add(new GuiButton(4, x, y, w, 20, ""));
        y += ROW;
        blockhitBtn = add(new GuiButton(5, x, y, w, 20, ""));
        y += ROW;
        previewBtn = add(new GuiButton(6, x, y, w, 20, ""));
        y += ROW;
        add(new GuiButton(7, x, y, w / 2 - 1, 20, "Reset"));
        add(new GuiButton(8, x + w / 2 + 1, y, w / 2 - 1, 20, "Done"));
        y += ROW;

        panelBottom = y + 2;
        refreshLabels();
    }

    private <T extends GuiButton> T add(T button) {
        buttonList.add(button);
        return button;
    }

    private GuiSlider slider(int id, int x, int y, String prefix, double min, double max, double value) {
        GuiSlider s = new GuiSlider(id, x, y, PANEL_W, 20, prefix, "", min, max, value, true, true, this);
        s.precision = 2;
        s.updateSlider();
        buttonList.add(s);
        return s;
    }

    private void refreshLabels() {
        enabledBtn.displayString = "Animations: " + onOff(AnimConfig.enabled);
        modeBtn.displayString = AnimConfig.mode.displayName;
        alwaysBtn.displayString = "Offset always: " + onOff(AnimConfig.alwaysOffset);
        blockhitBtn.displayString = "Visual blockhit: " + onOff(AnimConfig.visualBlockhit);
        previewBtn.displayString = "Preview: " + onOff(AnimConfig.preview);
    }

    private static String onOff(boolean v) {
        return v ? "§aON" : "§cOFF";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                AnimConfig.enabled = !AnimConfig.enabled;
                break;
            case 1:
                AnimConfig.mode = AnimConfig.mode.prev();
                break;
            case 2:
            case 3:
                AnimConfig.mode = AnimConfig.mode.next();
                break;
            case 4:
                AnimConfig.alwaysOffset = !AnimConfig.alwaysOffset;
                break;
            case 5:
                AnimConfig.visualBlockhit = !AnimConfig.visualBlockhit;
                break;
            case 6:
                AnimConfig.preview = !AnimConfig.preview;
                break;
            case 7:
                AnimConfig.reset();
                initGui();
                return;
            case 8:
                mc.displayGuiScreen(null);
                return;
            default:
                return;
        }
        refreshLabels();
    }

    @Override
    public void onChangeSliderValue(GuiSlider slider) {
        float v = (float) slider.getValue();
        if (slider == xSlider) {
            AnimConfig.x = v;
        } else if (slider == ySlider) {
            AnimConfig.y = v;
        } else if (slider == zSlider) {
            AnimConfig.z = v;
        } else if (slider == scaleSlider) {
            AnimConfig.scale = v;
        } else if (slider == speedSlider) {
            AnimConfig.swingSpeed = v;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // No dark background: the sword on the right must stay visible
        drawRect(PANEL_X - 5, panelTop - 4, PANEL_X + PANEL_W + 5, panelBottom, 0xB0101014);
        drawRect(PANEL_X - 5, panelTop - 4, PANEL_X + PANEL_W + 5, panelTop - 3, 0xFF7B5CFF);
        drawCenteredString(fontRendererObj, "Animations", PANEL_X + PANEL_W / 2, panelTop + 4, 0xFFFFFF);

        if (AnimConfig.preview && !Animations.isBlockItem(mc.thePlayer != null ? mc.thePlayer.getHeldItem() : null)) {
            String hint = "Take a sword to see the preview";
            drawString(fontRendererObj, hint, width - fontRendererObj.getStringWidth(hint) - 8, height - 14, 0xAAAAAA);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        AnimConfig.save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
