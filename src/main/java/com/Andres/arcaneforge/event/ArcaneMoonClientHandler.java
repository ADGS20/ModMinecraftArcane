package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.client.ClientMoonState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Mientras la Luna Oscura/Roja esta activa (ClientMoonState, sincronizado
 * por S2CMoonStateSync), tiñe la niebla de rojo intenso — vainilla no deja
 * repintar el domo del cielo sin un mixin al renderer, pero la niebla ya
 * cubre casi todo el horizonte visible y consigue el mismo efecto de
 * "cielo rojo apocaliptico" sin tocar el pipeline de render.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID, value = Dist.CLIENT)
public class ArcaneMoonClientHandler {

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!ClientMoonState.isActive()) return;

        event.setRed(Math.max(event.getRed(), 0.55f));
        event.setGreen(event.getGreen() * 0.12f);
        event.setBlue(event.getBlue() * 0.12f);
    }

    /**
     * Acerca la niebla (mas o menos a la mitad de lo normal) mientras la
     * Luna esta activa — sin esto, con la distancia de renderizado tipica
     * el rojo de arriba apenas se nota porque el cielo/horizonte visible es
     * enorme. Acercandola, el rojo domina gran parte de lo que se ve, mucho
     * mas "apocaliptico" sin tocar el domo del cielo en si (eso necesitaria
     * un mixin al renderer, mas arriesgado).
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!ClientMoonState.isActive()) return;

        event.scaleFarPlaneDistance(0.55f);
        event.scaleNearPlaneDistance(0.55f);
    }
}
