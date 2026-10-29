package dev.blockanim.mixin;

import dev.blockanim.AnimConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swing speed setting: changes only how long your own arm swing looks. */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    @Inject(method = "getArmSwingAnimationEnd", at = @At("RETURN"), cancellable = true)
    private void blockanim$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        if (!AnimConfig.enabled || AnimConfig.swingSpeed == 1.0F) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && (Object) this == mc.thePlayer) {
            cir.setReturnValue(AnimConfig.swingTicks(cir.getReturnValue()));
        }
    }
}
