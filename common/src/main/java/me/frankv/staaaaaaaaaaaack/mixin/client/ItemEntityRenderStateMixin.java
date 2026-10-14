package me.frankv.staaaaaaaaaaaack.mixin.client;
import me.frankv.staaaaaaaaaaaack.client.CountRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements CountRenderState {
    @Unique private String stxck$text;
    public String stxck$text() { return stxck$text; }
    public void stxck$text(String text) { stxck$text = text; }
}
