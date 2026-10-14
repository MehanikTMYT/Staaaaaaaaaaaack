package me.frankv.staaaaaaaaaaaack;

import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class StxckAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Stxck.MODID);
    // Persistence deliberately stays in the original StxckExtraItemCount NBT field.
    public static final Supplier<AttachmentType<Integer>> EXTRA = TYPES.register("extra_item_count",
            () -> AttachmentType.builder(() -> 0).sync((holder, player) -> holder instanceof PileState state && state.stxck$syncAllowed(), ByteBufCodecs.VAR_INT).build());
    private StxckAttachments() {}
}
