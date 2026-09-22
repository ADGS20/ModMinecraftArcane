package com.Andres.arcaneforge.block;

import com.Andres.arcaneforge.ArcaneForge;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

/**
 * Le dice a GeckoLib donde buscar los 3 archivos de la estrella flotante:
 *   assets/arcaneforge/geo/block/arcane_power_block_star.geo.json
 *   assets/arcaneforge/animations/block/arcane_power_block_star.animation.json
 *   assets/arcaneforge/textures/block/arcane_power_block/estrella.png
 * Los dos primeros salen de BlockBench (proyecto "GeckoLib Animated Model");
 * el tercero es una textura normal como las demas del bloque.
 */
public class ArcanePowerBlockGeoModel extends GeoModel<ArcanePowerBlockEntity> {

    private static final Identifier MODEL =
            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "block/arcane_power_block_star");
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "textures/block/arcane_power_block/estrella.png");
    private static final Identifier ANIMATION =
            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "block/arcane_power_block_star");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(ArcanePowerBlockEntity animatable) {
        return ANIMATION;
    }
}
