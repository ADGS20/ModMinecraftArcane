package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.network.S2CInfiniteChestSync;
import com.Andres.arcaneforge.util.IArcaneInfiniteChest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Se probo primero con un @SubscribeEvent en PlayerContainerEvent.Open, pero
 * ese evento se dispara DESPUES de que ServerPlayer.openMenu() ya llamo a
 * initMenu(menu), y initMenu() hace container.addSlotListener(this.containerListener)
 * — lo que dispara INMEDIATAMENTE el envio del contenido completo al
 * cliente. Como SimpleContainer.setItem() (el contenedor "de mentira" que
 * usa el cliente solo para dibujar, ver SimpleContainerCapacityMixin)
 * recorta cada stack a 64/99 en el momento que lo recibe, para cuando
 * llegaba nuestro paquete avisando "este cofre es infinito" los stacks ya
 * habian sido recortados de forma permanente en el cliente — de ahi que
 * funcionara la primera vez (cofre recien colocado, vacio) pero no al
 * reabrir un cofre que ya tenia mas de 64/99 guardados.
 *
 * La solucion: engancharse en initMenu() mismo, ANTES de addSlotListener,
 * para que nuestro paquete de aviso llegue al cliente antes que la
 * sincronizacion de contenido completa (misma conexion, orden garantizado).
 */
@Mixin(ServerPlayer.class)
public abstract class InfiniteChestMenuOpenMixin {

    @Inject(method = "initMenu", at = @At("HEAD"))
    private void arcaneforge$syncInfiniteChestBeforeContentSync(AbstractContainerMenu container, CallbackInfo ci) {
        if (container instanceof ChestMenu chestMenu
                && chestMenu.getContainer() instanceof IArcaneInfiniteChest chest
                && chest.arcaneforge$isInfinite()) {
            PacketDistributor.sendToPlayer((ServerPlayer) (Object) this, new S2CInfiniteChestSync(chestMenu.containerId));
        }
    }
}
