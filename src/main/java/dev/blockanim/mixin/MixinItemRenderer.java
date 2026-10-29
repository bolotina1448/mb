package dev.blockanim.mixin;

import dev.blockanim.AnimConfig;
import dev.blockanim.Animations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla first-person rendering (1.8.9) calls, while blocking:
 *     transformFirstPersonItem(equip, 0); doBlockTransformations();
 * and otherwise:
 *     doItemUsedTransformations(swing); transformFirstPersonItem(equip, swing);
 *
 * We remember the equip progress in the first call and apply the selected
 * animation in place of doBlockTransformations.
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Shadow
    private ItemStack itemToRender;

    private float blockanim$equip;
    private boolean blockanim$pending;

    @Shadow
    private void transformFirstPersonItem(float equipProgress, float swingProgress) {
    }

    @Shadow
    private void doBlockTransformations() {
    }

    @Shadow
    private void doItemUsedTransformations(float swingProgress) {
    }

    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"))
    private void blockanim$onRenderStart(float partialTicks, CallbackInfo ci) {
        Animations.partialTicks = partialTicks;
        blockanim$pending = false;
    }

    @Redirect(method = "renderItemInFirstPerson", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V"))
    private void blockanim$transform(ItemRenderer self, float equip, float swing) {
        if (!AnimConfig.enabled) {
            transformFirstPersonItem(equip, swing);
            return;
        }

        if (blockanim$isBlocking()) {
            // doBlockTransformations comes right after: do everything there
            blockanim$equip = equip;
            blockanim$pending = true;
            return;
        }

        if (Animations.isPreviewing(itemToRender)) {
            Animations.applyBlock(equip, Animations.previewSwing());
            return;
        }

        Animations.applyNormal(equip, swing);
    }

    @Redirect(method = "renderItemInFirstPerson", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemRenderer;doBlockTransformations()V"))
    private void blockanim$block(ItemRenderer self) {
        if (blockanim$pending) {
            blockanim$pending = false;
            EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
            float swing = player != null ? player.getSwingProgress(Animations.partialTicks) : 0.0F;
            Animations.applyBlock(blockanim$equip, swing);
            return;
        }
        doBlockTransformations();
    }

    @Redirect(method = "renderItemInFirstPerson", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemRenderer;doItemUsedTransformations(F)V"))
    private void blockanim$itemUsed(ItemRenderer self, float swing) {
        if (Animations.isPreviewing(itemToRender)) {
            return;
        }
        doItemUsedTransformations(swing);
    }

    private boolean blockanim$isBlocking() {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        return player != null
                && player.getItemInUseCount() > 0
                && Animations.isBlockItem(itemToRender);
    }
}
