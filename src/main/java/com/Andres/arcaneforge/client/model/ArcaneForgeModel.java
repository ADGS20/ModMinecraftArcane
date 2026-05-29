package com.Andres.arcaneforge.client.model;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ArcaneForgeModel extends GeoModel<ArcaneForgeBlockEntity> {

    @Override
    public ResourceLocation getModelResource(ArcaneForgeBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ArcaneForge.MODID, "geo/arcane_forge.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ArcaneForgeBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ArcaneForge.MODID, "textures/block/base.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ArcaneForgeBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ArcaneForge.MODID, "animations/arcane_forge.animation.json");
    }
}