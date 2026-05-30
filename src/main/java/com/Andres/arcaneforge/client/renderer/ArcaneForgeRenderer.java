package com.Andres.arcaneforge.client.renderer;

import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class ArcaneForgeRenderer implements BlockEntityRenderer<ArcaneForgeBlockEntity, BlockEntityRenderState> {

    public ArcaneForgeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public void submit(BlockEntityRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Modelo estático — NeoForge lo renderiza via blockstates/arcane_forge.json
    }
}
