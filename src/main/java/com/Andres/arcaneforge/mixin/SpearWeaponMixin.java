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
                // ANTES leiamos player.getUseItem()/getMainHandItem() para sacar el nivel
                // del encantamiento. En SUPERVIVENCIA, TridentItem.releaseUsing() ya
                // consumio el triente de la mano (itemStack.consumeAndReturn(1, player))
                // ANTES de que este inject se ejecute, asi que ambas llamadas devolvian
                // vacio y el bloque entero se saltaba en silencio — por eso "solo
                // funcionaba en creativo" (hasInfiniteMaterials() evita que se consuma).
                // El proyectil (ThrownTrident extends AbstractArrow) siempre guarda su
                // propia copia del item lanzado via getPickupItemStackOrigin(), con
                // encantamientos incluidos, sin importar el modo de juego — es la fuente
                // fiable.
                ItemStack itemEnMano = ItemStack.EMPTY;
                if (projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow) {
                    itemEnMano = arrow.getPickupItemStackOrigin();
                }

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

                            // ANTES usabamos 1.5^nivel (exponencial). Aun topando el nivel
                            // usado en la formula a 10, el multiplicador resultante (~x57.7)
                            // llevaba la velocidad base del tridente (~2.5 bloques/tick) a
                            // ~145 bloques/tick — unos 2900 bloques/segundo. A esa velocidad
                            // el tridente prácticamente desaparece en el mismo tick en que se
                            // lanza: no se ve volar, y si tiene Lealtad puede salir del área
                            // cargada antes de que la Lealtad logre traerlo de vuelta. Por
                            // eso "no hacia nada" y "no regresaba a la mano" — el efecto SI
                            // se aplicaba (confirmado por los logs), pero era demasiado
                            // extremo para ser jugable. Cambiamos a escala lineal con techo
                            // real en nivel 10: hasta x3.0 de velocidad extra, que en la
                            // practica ya es un tridente brutalmente rapido (~7.5 bloques/tick,
                            // ~150 bloques/seg) pero todavia visible y compatible con Lealtad.
                            int nivelTopado = Math.min(nivel, 10);
                            float multiplicadorContinuo = Math.min(1.0f + 0.2f * nivelTopado, 3.0f);

                            Vec3 movimientoOriginal = projectile.getDeltaMovement();
                            // ANTES: el impulso extra se calculaba a partir de la velocidad
                            // HORIZONTAL DEL JUGADOR (velJugador), no del tridente. Si el jugador
                            // esta quieto apuntando (lo normal al cargar un lanzamiento en
                            // supervivencia), esa velocidad es ~0 y el bono tambien, asi que el
                            // encantamiento no se notaba — solo funcionaba "por accidente" si
                            // ademas te estabas moviendo/volando al soltar el tiro (como en
                            // creativo). Ahora escalamos la velocidad PROPIA del tridente ya
                            // lanzado, asi el bono es siempre fiable sin importar si te moviste.
                            double velocidadTridente = movimientoOriginal.length();
                            if (velocidadTridente > 0.01) {
                                double impulsoExtra = velocidadTridente * (multiplicadorContinuo - 1.0f);
                                Vec3 direccionTiro = movimientoOriginal.normalize();
                                Vec3 nuevoMovimiento = movimientoOriginal.add(direccionTiro.scale(impulsoExtra));

                                // Aplicamos el nuevo vector físico ultraveloz al proyectil
                                projectile.setDeltaMovement(nuevoMovimiento);
                                projectile.hurtMarked = true; // Sincroniza la velocidad de inmediato con los clientes

                                ArcaneForge.LOGGER.debug("[ETHEREAL-LAUNCH] nivel={} multiplicador={} velAntes={} velDespues={}",
                                        nivel, multiplicadorContinuo, velocidadTridente, nuevoMovimiento.length());
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