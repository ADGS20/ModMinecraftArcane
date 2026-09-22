package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.moon.ArcaneMoonLogic;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Los mobs de la Luna (Heraldos y la horda en general) nunca deben pelear
 * entre ellos por un roce accidental — una flecha de Esqueleto que se pasa
 * de largo, una explosion de Creeper, una pocion arrojadiza de Bruja que
 * salpica a otro mob cercano, etc. Sin esto, una horda de 20-60 mobs
 * apretados terminaba autodestruyendose antes de que llegaran al jugador.
 *
 * Alcance a proposito ACOTADO para no tocar comportamiento vanilla real
 * (Piglin vs Hoglin, Piglin vs Zombified Piglin, etc. siguen intactos):
 *  - El daño solo se anula si AMBOS (victima y atacante) son Monster Y
 *    ademas ambos estan cazando exactamente al MISMO jugador ahora mismo
 *    — asi que dos mobs de bandos "vanilla" distintos, que no comparten
 *    objetivo, no se ven afectados.
 *  - El objetivo solo se limpia (para que HurtByTargetGoal no los deje
 *    peleando pese al daño anulado, mismo problema ya resuelto para los
 *    Golems Arcanos en ArcaneGolemHandler) cuando al menos uno de los dos
 *    es un Heraldo — el subconjunto que de verdad vale la pena proteger.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneMonsterFriendlyFireHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMonsterFriendlyFire(LivingDamageEvent.Pre event) {
        try {
            if (!(event.getEntity() instanceof Monster monsterTarget)) return;

            Entity attacker = event.getSource().getEntity();
            if (!(attacker instanceof Monster monsterAttacker) || monsterAttacker == monsterTarget) return;

            LivingEntity huntedByTarget = monsterTarget.getTarget();
            LivingEntity huntedByAttacker = monsterAttacker.getTarget();
            if (!(huntedByTarget instanceof Player) || huntedByTarget != huntedByAttacker) return;

            event.setNewDamage(0);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Monster friendly-fire guard error: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onMonsterTick(EntityTickEvent.Post event) {
        try {
            if (!(event.getEntity() instanceof Monster monster)) return;
            if (!(monster.getTarget() instanceof Monster otherMonster)) return;

            if (ArcaneMoonLogic.isHeraldo(monster) || ArcaneMoonLogic.isHeraldo(otherMonster)) {
                monster.setTarget(null);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Monster friendly-fire target-clear error: {}", e.getMessage());
        }
    }
}
