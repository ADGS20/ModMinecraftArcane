package com.Andres.arcaneforge.client.renderer;

import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderState;

public class ArcaneForgeRenderer implements BlockEntityRenderer<ArcaneForgeBlockEntity, BlockEntityRenderState> {

    public ArcaneForgeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public void extractRenderState(ArcaneForgeBlockEntity blockEntity, BlockEntityRenderState renderState, float partialTick) {
        super.extractRenderState(blockEntity, renderState, partialTick);
    }

    @Override
    public void render(BlockEntityRenderState renderState, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Modelo estático renderizado por NeoForge via blockstates/arcane_forge.json
        // Añade aquí overlays o partículas extra si los necesitas en el futuro
    }
}