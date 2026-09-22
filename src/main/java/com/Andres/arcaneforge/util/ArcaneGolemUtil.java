package com.Andres.arcaneforge.util;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.UUID;

/**
 * Datos y helpers compartidos del Golem Arcano (creado con el Cetro Arcano
 * del Golem). Guarda TODOS los encantamientos que tenia el cetro en el
 * momento de vincularlo, no solo los que el mod sabe usar en combate — asi,
 * si en el futuro se conecta un encantamiento nuevo a este sistema, los
 * golems ya vinculados no pierden ese dato por no haberlo guardado.
 *
 * Se guarda en getPersistentData() (mismo mecanismo que BindingWand usa para
 * el jugador) en vez de inventar un AttachmentType nuevo para un solo flag +
 * un mapa pequeño de enchant->nivel.
 *
 * Funciona sobre CUALQUIER AbstractGolem (IronGolem y SnowGolem son las dos
 * subclases vanilla hoy) en vez de estar atado solo a IronGolem: el mismo
 * Cetro vincula cualquiera de los dos, y este util no necesita saber cual es
 * cual — el comportamiento especifico por tipo vive en GolemBindingRod y en
 * los handlers de evento (ArcaneGolemHandler / ArcaneSnowGolemHandler).
 */
public final class ArcaneGolemUtil {
    private ArcaneGolemUtil() {}

    private static final String FLAG_KEY = "arcane_golem";
    private static final String ENCHANTS_KEY = "arcane_golem_enchants";
    private static final String OWNER_KEY = "arcane_golem_owner";

    private static final Identifier SCALE_ID = Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "golem_arcano_scale");
    private static final Identifier SCALE_LEAL_ID = Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "golem_arcano_scale_leal");
    private static final Identifier HEALTH_ID = Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "golem_arcano_health");
    private static final Identifier DAMAGE_ID = Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "golem_arcano_damage");

    private static final Identifier SNOW_HEALTH_ID = Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "golem_arcano_nieve_health");
    private static final Identifier SNOW_SPEED_ID = Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "golem_arcano_nieve_speed");
    private static final Identifier SNOW_SCALE_ID = Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "golem_arcano_nieve_scale");
    private static final Identifier SNOW_SCALE_LEAL_ID = Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "golem_arcano_nieve_scale_leal");

    /** Lealtad (vainilla, la del tridente) vinculada al golem: lo hace seguir a su dueño como un perro. */
    public static final ResourceKey<Enchantment> LOYALTY_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("minecraft", "loyalty"));

    public static boolean isArcaneGolem(LivingEntity entity) {
        if (!(entity instanceof AbstractGolem)) return false;
        return entity.getPersistentData().getBoolean(FLAG_KEY).orElse(false);
    }

    /** Nivel de un encantamiento vinculado a este golem (0 si no es Golem Arcano o no lo tiene). */
    public static int golemEnchantLevel(LivingEntity entity, ResourceKey<Enchantment> key) {
        if (!isArcaneGolem(entity)) return 0;
        CompoundTag enchants = entity.getPersistentData().getCompound(ENCHANTS_KEY).orElse(new CompoundTag());
        return enchants.getInt(key.identifier().toString()).orElse(0);
    }

    /** Vincula TODOS los encantamientos del cetro a un golem (de hierro o de nieve). */
    public static void bindWandEnchantsToGolem(AbstractGolem golem, ItemStack wand) {
        ItemEnchantments wandEnchants = wand.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        CompoundTag stored = new CompoundTag();
        for (var entry : wandEnchants.entrySet()) {
            var keyOpt = entry.getKey().unwrapKey();
            if (keyOpt.isEmpty()) continue;
            stored.putInt(keyOpt.get().identifier().toString(), entry.getIntValue());
        }
        golem.getPersistentData().putBoolean(FLAG_KEY, true);
        golem.getPersistentData().put(ENCHANTS_KEY, stored);
    }

    /** Suma de todos los niveles vinculados, usada para escalar tamaño/vida/daño del golem. */
    public static int totalBoundLevels(AbstractGolem golem) {
        if (!isArcaneGolem(golem)) return 0;
        CompoundTag enchants = golem.getPersistentData().getCompound(ENCHANTS_KEY).orElse(new CompoundTag());
        int total = 0;
        for (String k : enchants.keySet()) {
            total += enchants.getInt(k).orElse(0);
        }
        return total;
    }

    /** true si el golem tiene Lealtad vinculada (persigue a su dueño como un perro). */
    public static boolean isLoyal(AbstractGolem golem) {
        return golemEnchantLevel(golem, LOYALTY_KEY) > 0;
    }

    /** Guarda quien vinculo el golem (para que Lealtad sepa a quien seguir). */
    public static void setOwner(AbstractGolem golem, UUID owner) {
        golem.getPersistentData().putString(OWNER_KEY, owner.toString());
    }

    /** Dueño del golem, o null si nunca se guardo (golem vinculado antes de esta funcion, por ejemplo, o dato invalido). */
    public static UUID getOwner(AbstractGolem golem) {
        String raw = golem.getPersistentData().getString(OWNER_KEY).orElse("");
        if (raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Tamaño, vida y daño extra escalan con la suma de todos los niveles
     * vinculados (totalBoundLevels). El tamaño se limita a 1.5x como mucho;
     * si ademas lleva Lealtad vinculada (persigue al dueño, ver
     * ArcaneGolemHandler), se reduce a la MITAD de ese resultado — un golem
     * grande no cabe siguiendo al jugador por huecos de 2 bloques, uno
     * pequeño si. Usado tanto por GolemBindingRod (vinculo manual) como por
     * el Generador Arcano de Golems (spawns automaticos con el mismo
     * template de encantamientos).
     */
    public static void applyGolemBonuses(IronGolem golem) {
        int totalLevels = totalBoundLevels(golem);

        AttributeInstance scale = golem.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.addOrReplacePermanentModifier(new AttributeModifier(SCALE_ID,
                    Math.min(0.005 * totalLevels, 0.5), AttributeModifier.Operation.ADD_VALUE));
            if (isLoyal(golem)) {
                scale.addOrReplacePermanentModifier(new AttributeModifier(SCALE_LEAL_ID,
                        -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }

        AttributeInstance health = golem.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_ID,
                    Math.min(2.0 * totalLevels, 2000.0), AttributeModifier.Operation.ADD_VALUE));
            golem.setHealth(golem.getMaxHealth());
        }

        AttributeInstance damage = golem.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.addOrReplacePermanentModifier(new AttributeModifier(DAMAGE_ID,
                    Math.min(0.3 * totalLevels, 150.0), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /**
     * Version para golem de nieve: sin ATTACK_DAMAGE (vainilla no se lo da,
     * ataca de rango con bolas de nieve — el daño real lo escala
     * ArcaneSnowGolemHandler leyendo totalBoundLevels), asi que aqui se
     * escala vida, velocidad y tamaño en su lugar. Mismo tope de escala 1.5x
     * y misma reduccion a la mitad con Lealtad vinculada que el de hierro.
     */
    public static void applyGolemBonuses(SnowGolem golem) {
        int totalLevels = totalBoundLevels(golem);

        AttributeInstance health = golem.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addOrReplacePermanentModifier(new AttributeModifier(SNOW_HEALTH_ID,
                    Math.min(0.5 * totalLevels, 400.0), AttributeModifier.Operation.ADD_VALUE));
            golem.setHealth(golem.getMaxHealth());
        }

        AttributeInstance speed = golem.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrReplacePermanentModifier(new AttributeModifier(SNOW_SPEED_ID,
                    Math.min(0.001 * totalLevels, 0.4), AttributeModifier.Operation.ADD_VALUE));
        }

        AttributeInstance scale = golem.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.addOrReplacePermanentModifier(new AttributeModifier(SNOW_SCALE_ID,
                    Math.min(0.005 * totalLevels, 0.5), AttributeModifier.Operation.ADD_VALUE));
            if (isLoyal(golem)) {
                scale.addOrReplacePermanentModifier(new AttributeModifier(SNOW_SCALE_LEAL_ID,
                        -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }
}
