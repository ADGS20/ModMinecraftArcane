package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Al entrar por primera vez a una partida:
 *   - Entrega el Libro Guia Arcano al jugador.
 *   - Desbloquea TODAS las recetas del mod en el libro de recetas.
 * Marca una bandera en los datos persistentes del jugador para no repetir la entrega.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneStartupHandler {

    private static final String TAG_GIVEN = "arcaneforge_guide_given";

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // getPersistentData() del jugador sobrevive en disco entre sesiones.
        CompoundTag persistent = player.getPersistentData();
        boolean alreadyGiven = persistent.getBoolean(TAG_GIVEN).orElse(false);

        // Siempre aseguramos que las recetas del mod esten desbloqueadas.
        unlockArcaneRecipes(player);

        if (!alreadyGiven) {
            ItemStack book = new ItemStack(com.Andres.arcaneforge.registry.ModItems.ARCANE_GUIDE_BOOK.get());
            if (!player.addItem(book)) {
                player.drop(book, false);
            }
            persistent.putBoolean(TAG_GIVEN, true);
        }
    }

    /** Desbloquea en el libro de recetas todas las recetas de crafteo del mod. */
    public static void unlockArcaneRecipes(ServerPlayer player) {
        try {
            if (!(player.level() instanceof ServerLevel level)) return;
            List<RecipeHolder<?>> ours = new ArrayList<>();
            for (RecipeHolder<?> holder : level.recipeAccess().recipeMap().byType(RecipeType.CRAFTING)) {
                // holder.id() puede devolver un ResourceKey o un Identifier segun la version;
                // comparamos por texto para ser robustos: "arcaneforge:..."
                String idText = String.valueOf(holder.id());
                if (idText.contains(ArcaneForge.MODID + ":")) {
                    ours.add(holder);
                }
            }
            if (!ours.isEmpty()) {
                player.awardRecipes(ours);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.error("No se pudieron desbloquear las recetas del mod: ", e);
        }
    }
}
