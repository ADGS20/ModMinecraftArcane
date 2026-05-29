package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.client.renderer.ArcaneForgeRenderer;
import com.Andres.arcaneforge.menu.ArcaneForgeScreen;
import com.Andres.arcaneforge.registry.ModBlockEntities;
import com.Andres.arcaneforge.registry.ModMenuTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ArcaneForge.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ARCANE_FORGE_MENU.get(), ArcaneForgeScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_FORGE_BE.get(), new BlockEntityRendererProvider<com.Andres.arcaneforge.block.ArcaneForgeBlockEntity>() {
            @Override
            public BlockEntityRenderer<com.Andres.arcaneforge.block.ArcaneForgeBlockEntity> create(BlockEntityRendererProvider.Context context) {
                return new ArcaneForgeRenderer(context);
            }
        });
    }
}