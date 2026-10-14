package me.frankv.staaaaaaaaaaaack.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import me.frankv.staaaaaaaaaaaack.Stxck;
import me.frankv.staaaaaaaaaaaack.client.CountRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void stxck$extract(ItemEntity entity, ItemEntityRenderState state, float partial, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        int distance = Stxck.clientConfig.getMinItemCountRenderDistance();
        String text = state.distanceToCameraSq <= (double) distance * distance
                && mc.player != null && !mc.player.isShiftKeyDown()
                ? Stxck.getOverlayText(entity).orElse(null) : null;
        ((CountRenderState) state).stxck$text(text);
    }
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    private void stxck$submit(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        String text = ((CountRenderState) state).stxck$text();
        if (text == null) return;
        var mc = Minecraft.getInstance();
        float scale = 0.025f * (float) Stxck.clientConfig.getOverlaySizeMultiplier();
        pose.pushPose();
        pose.translate(0, state.boundingBoxHeight + 0.75f, 0);
        pose.mulPose(camera.orientation);
        pose.scale(scale, -scale, scale);
        collector.submitText(pose, -mc.font.width(text) / 2f, 0, Component.literal(text).getVisualOrderText(),
                false, Font.DisplayMode.NORMAL, state.lightCoords, -1,
                (int) (mc.options.getBackgroundOpacity(0.25f) * 255) << 24, 0);
        pose.popPose();
    }
}
