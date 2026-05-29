package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(net.minecraft.world.entity.projectile.Projectile.class)
public abstract class SpearWeaponMixin {

    @Unique
    private static final ResourceKey<Enchantment> AF_ETHEREAL_LAUNCH_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "ethereal_launch"));

    @Inject(
            method = "shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V",
            at = @At("TAIL")
    )
    private void onProjectileLaunch(Entity shooter, float x, float y, float z, float velocity, float inaccuracy, CallbackInfo ci) {
        Entity projectile = (Entity) (Object) this;

        // Ejecución exclusiva en el servidor para evitar desincronizaciones físicas
        if (projectile.level().isClientSide()) return;

        // Filtro: Nos aseguramos de que el proyectil sea exactamente un tridente o lanza vanilla
        if (projectile.getType().getDescriptionId().equals("entity.minecraft.trident")) {

            if (shooter instanceof Player player) {
                // Obtenemos de forma limpia el ítem que el jugador está usando/soltando para disparar
                ItemStack itemEnMano = player.getUseItem();

                // Si por algún motivo el useItem ya se vació en ese tick, usamos la mano principal como respaldo
                if (itemEnMano.isEmpty()) {
                    itemEnMano = player.getMainHandItem();
                }

                if (!itemEnMano.isEmpty()) {
                    try {
                        var registry = projectile.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                        ItemEnchantments enchantments = itemEnMano.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                        Optional<net.minecraft.core.Holder.Reference<Enchantment>> launchOpt = registry.get(AF_ETHEREAL_LAUNCH_KEY);

                        if (launchOpt.isPresent() && enchantments.getLevel(launchOpt.get()) > 0) {
                            int nivel = enchantments.getLevel(launchOpt.get());

                            // Capturamos el vector de movimiento horizontal del jugador en este tick
                            Vec3 velJugador = player.getDeltaMovement();
                            double velocidadHorizontal = Math.sqrt(velJugador.x * velJugador.x + velJugador.z * velJugador.z);

                            // Si el jugador se está moviendo en cualquier dirección (carrera, saltos, strafe...)
                            if (velocidadHorizontal > 0.01) {
                                // Tu fórmula: multiplicación continua (1.5 ^ Nivel)
                                float multiplicadorContinuo = (float) Math.pow(1.5, nivel);
                                double impulsoExtra = velocidadHorizontal * multiplicadorContinuo;

                                Vec3 movimientoOriginal = projectile.getDeltaMovement();
                                Vec3 direccionTiro = movimientoOriginal.normalize();
                                Vec3 nuevoMovimiento = movimientoOriginal.add(direccionTiro.scale(impulsoExtra));

                                // Aplicamos el nuevo vector físico ultraveloz al proyectil
                                projectile.setDeltaMovement(nuevoMovimiento);
                                projectile.hurtMarked = true; // Sincroniza la velocidad de inmediato con los clientes
                            }
                        }
                    } catch (Exception e) {
                        ArcaneForge.LOGGER.error("Error aplicando el multiplicador al proyectil: {}", e.getMessage());
                    }
                }
            }
        }
    }
}