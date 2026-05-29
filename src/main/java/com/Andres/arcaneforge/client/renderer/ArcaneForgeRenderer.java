package com.Andres.arcaneforge.client.renderer;

import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import com.Andres.arcaneforge.client.model.ArcaneForgeModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ArcaneForgeRenderer extends GeoBlockRenderer<ArcaneForgeBlockEntity> {
    public ArcaneForgeRenderer(BlockEntityRendererProvider.Context context) {
        super(new ArcaneForgeModel());
    }
}