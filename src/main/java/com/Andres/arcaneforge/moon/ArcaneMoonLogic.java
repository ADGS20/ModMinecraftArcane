package com.Andres.arcaneforge.moon;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.config.ArcaneServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Logica pura de la Luna Oscura/Roja: que rango de nivel le toca a cada
 * ronda, con que probabilidad se equipa un mob, y como se le aplica el
 * encantamiento. Sin eventos aqui — eso vive en event/ArcaneMoonHandler.
 *
 * IMPORTANTE: los encantamientos del pool de abajo estan confirmados como
 * agnosticos de Player (enganchan por "instanceof LivingEntity", ver
 * event/ArcaneSwordHandler.java y event/ArcaneEnchantsHandler.java linea
 * ~551 para Coraza Arcana) — por eso un mob hostil que los lleve puestos ya
 * dispara el efecto real sin necesitar logica de combate nueva.
 */
public final class ArcaneMoonLogic {

    private ArcaneMoonLogic() {}

    private static final Identifier ELITE_HEALTH_ID =
            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "moon_elite_health");
    private static final Identifier ELITE_DAMAGE_ID =
            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "moon_elite_damage");

    /** Radio (en bloques) en el que un spawn de la Luna activa busca jugador para perseguirlo de inmediato. */
    public static final double HUNT_RADIUS = Config.MOON_HUNT_RADIUS_CHUNKS * 16.0;

    /** Pool de mobs hostiles "genericos" para las oleadas de horda (ver spawnHordeWave). */
    private static final List<EntityType<? extends Monster>> HORDE_POOL = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER, EntityType.WITCH
    );

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, name));
    }

    private static ResourceKey<Enchantment> vanillaKey(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("minecraft", name));
    }

    /** Proteccion vanilla para casco/piernas/botas de un Heraldo — Coraza Arcana solo lee del peto (ver ArcaneEnchantsHandler.onCorazaArcana). */
    private static final ResourceKey<Enchantment> PROTECTION = vanillaKey("protection");

    /**
     * Sharpness/Power vanilla — probados, garantizados, y aplicables a
     * CUALQUIER espada o arco sin depender de si nuestro propio pool de
     * encantamientos (pensado para el combate cuerpo a cuerpo del jugador)
     * engancha correctamente con la fuente de daño de cada tipo de mob
     * (flecha de Esqueleto, golpe de Zombie, etc.). Es la razon real de
     * "los mobs no pegan nada": ATTACK_DAMAGE (el multiplicador de Heraldo)
     * no afecta el daño de una flecha en absoluto, asi que un Esqueleto
     * Heraldo sin esto pegaba con daño de flecha vainilla normal pese a
     * tener un enchant altisimo puesto.
     */
    private static final ResourceKey<Enchantment> SHARPNESS = vanillaKey("sharpness");
    private static final ResourceKey<Enchantment> POWER = vanillaKey("power");

    private static final ResourceKey<Enchantment> CADENA_ARCANA     = key("cadena_arcana");
    private static final ResourceKey<Enchantment> FILO_ETERNO       = key("filo_eterno");
    private static final ResourceKey<Enchantment> FILO_INSACIABLE   = key("filo_insaciable");
    private static final ResourceKey<Enchantment> CORTE_DEL_VACIO   = key("corte_del_vacio");
    private static final ResourceKey<Enchantment> MARCA_DEL_CAZADOR = key("marca_del_cazador");
    private static final ResourceKey<Enchantment> GOLPE_DIMENSIONAL = key("golpe_dimensional");
    private static final ResourceKey<Enchantment> GOLPE_SISMICO     = key("golpe_sismico");
    private static final ResourceKey<Enchantment> SANGRIA_ESPECTRAL = key("sangria_espectral");
    private static final ResourceKey<Enchantment> GOLPE_PARTIDOR    = key("golpe_partidor");
    private static final ResourceKey<Enchantment> ATAQUE_VELOZ      = key("ataque_veloz");
    private static final ResourceKey<Enchantment> ARCANE_CATACLYSM  = key("arcane_cataclysm");
    private static final ResourceKey<Enchantment> ARCANE_REPULSE    = key("arcane_repulse");
    private static final ResourceKey<Enchantment> VAMPIRO           = key("vampiro");
    private static final ResourceKey<Enchantment> CORAZA_ARCANA     = key("coraza_arcana");
    private static final ResourceKey<Enchantment> VOID_PROTECTION   = key("void_protection");

    /**
     * Pool ampliado a proposito con el mismo criterio "curado" que ya usa
     * ArcaneForgeScreen.GOLEM_MELEE_ENCHANTS para los golems — Vampiro
     * incluido: es raro ver a un mob curarse robando vida, pero el mismo
     * enganche que ya usa el jugador (LivingDamageEvent.Pre, agnostico de
     * quien golpea) hace que funcione igual de bien puesto en un Heraldo.
     */
    private static final List<ResourceKey<Enchantment>> WEAPON_POOL = List.of(
            CADENA_ARCANA, FILO_ETERNO, FILO_INSACIABLE, CORTE_DEL_VACIO,
            MARCA_DEL_CAZADOR, GOLPE_DIMENSIONAL, GOLPE_SISMICO, SANGRIA_ESPECTRAL,
            GOLPE_PARTIDOR, ATAQUE_VELOZ, ARCANE_CATACLYSM, ARCANE_REPULSE, VAMPIRO
    );

    public record LevelRange(int min, int max) {}

    public static LevelRange rangeForRound(int round) {
        return switch (Math.max(0, Math.min(round, 10))) {
            case 0 -> new LevelRange(0, 0);
            case 1 -> new LevelRange(1, 3);
            case 2 -> new LevelRange(4, 8);
            case 3 -> new LevelRange(9, 15);
            case 4 -> new LevelRange(16, 30);
            case 5 -> new LevelRange(31, 60);
            case 6 -> new LevelRange(61, 100);
            case 7 -> new LevelRange(101, 150);
            case 8 -> new LevelRange(151, 200);
            case 9 -> new LevelRange(201, 250);
            default -> new LevelRange(250, 255);
        };
    }

    public static int eliteLevelForRound(int round) {
        return Math.min(rangeForRound(round).max() + 2, 255);
    }

    public static double gearChance(int round, boolean moonActive) {
        double chance = Math.min(
                Config.MOON_BASE_GEAR_CHANCE + Config.MOON_GEAR_CHANCE_PER_ROUND * round,
                Config.MOON_MAX_GEAR_CHANCE);
        if (moonActive) chance = Math.min(chance + 0.20, 0.90);
        return chance;
    }

    public static double eliteChance(int round, boolean moonActive) {
        double base = moonActive ? Config.MOON_ACTIVE_ELITE_CHANCE : Config.MOON_BASE_ELITE_CHANCE;
        double cap = moonActive ? 0.35 : 0.15;
        return Math.min(base + 0.01 * round, cap);
    }

    private static Item swordForRound(int round) {
        if (round <= 2) return Items.WOODEN_SWORD;
        if (round <= 4) return Items.STONE_SWORD;
        if (round <= 6) return Items.IRON_SWORD;
        if (round <= 8) return Items.DIAMOND_SWORD;
        return Items.NETHERITE_SWORD;
    }

    private static Item chestplateForRound(int round) {
        if (round <= 2) return Items.LEATHER_CHESTPLATE;
        if (round <= 6) return Items.IRON_CHESTPLATE;
        if (round <= 8) return Items.DIAMOND_CHESTPLATE;
        return Items.NETHERITE_CHESTPLATE;
    }

    private static Item helmetForRound(int round) {
        if (round <= 2) return Items.LEATHER_HELMET;
        if (round <= 6) return Items.IRON_HELMET;
        if (round <= 8) return Items.DIAMOND_HELMET;
        return Items.NETHERITE_HELMET;
    }

    private static Item leggingsForRound(int round) {
        if (round <= 2) return Items.LEATHER_LEGGINGS;
        if (round <= 6) return Items.IRON_LEGGINGS;
        if (round <= 8) return Items.DIAMOND_LEGGINGS;
        return Items.NETHERITE_LEGGINGS;
    }

    private static Item bootsForRound(int round) {
        if (round <= 2) return Items.LEATHER_BOOTS;
        if (round <= 6) return Items.IRON_BOOTS;
        if (round <= 8) return Items.DIAMOND_BOOTS;
        return Items.NETHERITE_BOOTS;
    }

    /**
     * Cuanto mas alta la ronda, mas no-muertos llevan casco. En vainilla,
     * CUALQUIER item en la cabeza (RestrictSunGoal/FleeSunGoal) hace que el
     * mob deje de huir del sol y no se prenda fuego — asi que esto los hace
     * "poco a poco inmunes al sol" (progresivo, no un interruptor) sin
     * necesitar logica de inmunidad propia. Ronda 10+: garantizado.
     */
    private static void maybeGrantSunImmunity(Mob mob, RandomSource random, int round) {
        if (!(mob instanceof Zombie) && !(mob instanceof AbstractSkeleton)) return;
        if (!mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return; // ya tiene algo puesto

        double helmetChance = Math.min(0.10 * round, 1.0);
        if (random.nextDouble() < helmetChance) {
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmetForRound(round)));
        }
    }

    private static void applyEnchant(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> registry,
                                      ResourceKey<Enchantment> key, int level) {
        try {
            var opt = registry.get(key);
            if (opt.isEmpty()) return;
            ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
            mutable.set(opt.get(), level);
            stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        } catch (Exception ignored) {
            // sin registro disponible, no hay nada que aplicar
        }
    }

    private static final String HERALDO_ROUND_KEY = "arcaneforge_heraldo_round";

    /** true si el mob es un Heraldo (elite) de la Luna — ver ArcaneHeraldoDigHandler. */
    public static boolean isHeraldo(Mob mob) {
        return mob.getPersistentData().getInt(HERALDO_ROUND_KEY).orElse(0) > 0;
    }

    /** Ronda con la que se equipo este Heraldo (0 si no es Heraldo). */
    public static int heraldoRound(Mob mob) {
        return mob.getPersistentData().getInt(HERALDO_ROUND_KEY).orElse(0);
    }

    /** Punto de entrada unico: tira los dados y equipa al mob si toca. */
    public static void equipMob(Mob mob, RandomSource random, ArcaneMoonData data, ServerLevel level) {
        equipMob(mob, random, data.getRound(), data.isMoonActive(), level, false);
    }

    /**
     * Version sin ArcaneMoonData: usada por la horda automatica (probabilistica,
     * como siempre) y por /arcaneforge moon wave para poder spawnear una
     * oleada con una ronda elegida a mano por el administrador, sin tocar
     * ni leer la ronda "de verdad" guardada.
     */
    public static void equipMob(Mob mob, RandomSource random, int round, boolean moonActive, ServerLevel level) {
        equipMob(mob, random, round, moonActive, level, false);
    }

    /**
     * @param guaranteed si es true, se saltan las tiradas de gearChance/eliteChance
     *                   — el mob SIEMPRE sale armado y SIEMPRE es Heraldo.
     *                   Solo lo usa /arcaneforge moon wave: un admin invocando
     *                   una oleada a mano espera una amenaza real garantizada,
     *                   no una tirada de dados — la horda automatica de cada
     *                   noche sigue siendo probabilistica como siempre.
     */
    public static void equipMob(Mob mob, RandomSource random, int round, boolean moonActive, ServerLevel level, boolean guaranteed) {
        if (round <= 0) return;

        maybeGrantSunImmunity(mob, random, round);
        if (moonActive) {
            Player nearest = level.getNearestPlayer(mob, HUNT_RADIUS);
            if (nearest != null) mob.setTarget(nearest);
        }

        if (!guaranteed && random.nextDouble() >= gearChance(round, moonActive)) return;

        HolderLookup.RegistryLookup<Enchantment> registry;
        try {
            registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        } catch (Exception e) {
            return;
        }

        LevelRange range = rangeForRound(round);
        int enchantLevel = range.min() + random.nextInt(range.max() - range.min() + 1);

        ArcaneServerConfig serverConfig = ArcaneServerConfig.get(level);

        List<ResourceKey<Enchantment>> availableWeaponEnchants = WEAPON_POOL.stream()
                .filter(key -> !serverConfig.isEnchantmentDisabled(key.identifier()))
                .toList();
        if (!availableWeaponEnchants.isEmpty()) {
            ItemStack weapon = mob.getMainHandItem();
            if (weapon.isEmpty()) {
                weapon = new ItemStack(swordForRound(round));
            }
            ResourceKey<Enchantment> weaponEnchant = availableWeaponEnchants.get(random.nextInt(availableWeaponEnchants.size()));
            applyEnchant(weapon, registry, weaponEnchant, enchantLevel);

            // Sharpness/Power vanilla ADEMAS del enchant propio — ver el
            // comentario en la constante SHARPNESS/POWER mas arriba.
            ResourceKey<Enchantment> vanillaDamageEnchant =
                    (weapon.is(Items.BOW) || weapon.is(Items.CROSSBOW)) ? POWER : SHARPNESS;
            if (!serverConfig.isEnchantmentDisabled(vanillaDamageEnchant.identifier())) {
                applyEnchant(weapon, registry, vanillaDamageEnchant, Math.min(5 + round / 3, 50));
            }
            weapon.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }

        maybeGivePoisonArrows(mob, round);

        boolean elite = guaranteed || random.nextDouble() < eliteChance(round, moonActive);
        if (elite) {
            if (!serverConfig.isEnchantmentDisabled(CORAZA_ARCANA.identifier())) {
                ItemStack chest = mob.getItemBySlot(EquipmentSlot.CHEST);
                if (chest.isEmpty()) {
                    chest = new ItemStack(chestplateForRound(round));
                }
                applyEnchant(chest, registry, CORAZA_ARCANA, eliteLevelForRound(round));
                chest.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
                mob.setItemSlot(EquipmentSlot.CHEST, chest);
            }

            // Armadura completa a partir de cierta ronda, con Proteccion
            // vanilla de verdad (Coraza Arcana solo lee del peto) y sin
            // durabilidad — un Heraldo con armadura sin encantar y sin
            // Unbreakable se sentia igual de fragil que un mob normal pese
            // al enchant altisimo del arma.
            if (round >= Config.MOON_HERALDO_FULL_ARMOR_MIN_ROUND) {
                int protLevel = Math.min(4 + round / 2, 20);
                boolean protectionAllowed = !serverConfig.isEnchantmentDisabled(PROTECTION.identifier());
                boolean voidProtectionAllowed = !serverConfig.isEnchantmentDisabled(VOID_PROTECTION.identifier());

                ItemStack helmet = mob.getItemBySlot(EquipmentSlot.HEAD);
                if (helmet.isEmpty()) helmet = new ItemStack(helmetForRound(round));
                if (protectionAllowed) applyEnchant(helmet, registry, PROTECTION, protLevel);
                if (voidProtectionAllowed) applyEnchant(helmet, registry, VOID_PROTECTION, enchantLevel);
                helmet.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
                mob.setItemSlot(EquipmentSlot.HEAD, helmet);

                ItemStack legs = mob.getItemBySlot(EquipmentSlot.LEGS);
                if (legs.isEmpty()) legs = new ItemStack(leggingsForRound(round));
                if (protectionAllowed) applyEnchant(legs, registry, PROTECTION, protLevel);
                if (voidProtectionAllowed) applyEnchant(legs, registry, VOID_PROTECTION, enchantLevel);
                legs.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
                mob.setItemSlot(EquipmentSlot.LEGS, legs);

                ItemStack boots = mob.getItemBySlot(EquipmentSlot.FEET);
                if (boots.isEmpty()) boots = new ItemStack(bootsForRound(round));
                if (protectionAllowed) applyEnchant(boots, registry, PROTECTION, protLevel);
                if (voidProtectionAllowed) applyEnchant(boots, registry, VOID_PROTECTION, enchantLevel);
                boots.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
                mob.setItemSlot(EquipmentSlot.FEET, boots);
            }

            // Totem de la Inmortalidad a partir de cierta ronda: una
            // segunda oportunidad real, igual que tendria un jugador. NO
            // para Esqueletos: su OFFHAND ya lo ocupa la flecha con veneno
            // (ver maybeGivePoisonArrows) — un totem ahi los haria volver a
            // disparar flechas normales (getHeldProjectile mira offhand
            // primero, y un totem no cuenta como proyectil valido).
            if (round >= Config.MOON_HERALDO_TOTEM_MIN_ROUND
                    && !(mob instanceof AbstractSkeleton)
                    && mob.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
                mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }

            grantHeraldoEffects(mob, random, round);

            AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.addOrReplacePermanentModifier(new AttributeModifier(ELITE_HEALTH_ID,
                        Math.min(0.5 + 0.25 * round, Config.MOON_ELITE_HEALTH_MULT_CAP), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                mob.setHealth(mob.getMaxHealth());
            }
            AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) {
                damage.addOrReplacePermanentModifier(new AttributeModifier(ELITE_DAMAGE_ID,
                        Math.min(0.5 + 0.3 * round, Config.MOON_ELITE_DAMAGE_MULT_CAP), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            mob.setCustomName(Component.literal("§4§lHeraldo de la Luna §7· Ronda " + round));
            mob.setCustomNameVisible(true);
            mob.setGlowingTag(true);
            mob.setPersistenceRequired(); // es mas fuerte que un mob normal: no debe despawnear como uno mas
            mob.getPersistentData().putInt(HERALDO_ROUND_KEY, round); // marca consultable: isHeraldo/heraldoRound
        }
    }

    /**
     * Un Esqueleto de la Luna dispara flechas envenenadas/de daño en vez de
     * las normales de vainilla. Truco de API: Monster.getProjectile() mira
     * primero el OFFHAND del mob (ProjectileWeaponItem.getHeldProjectile) —
     * si ahi hay una Flecha con Efecto (TIPPED_ARROW con PotionContents),
     * la usa como munición real sin tocar ninguna IA ni mixin.
     */
    private static void maybeGivePoisonArrows(Mob mob, int round) {
        if (!(mob instanceof AbstractSkeleton)) return;

        Holder<Potion> potion = round >= 8 ? Potions.STRONG_HARMING : Potions.HARMING;
        ItemStack tippedArrow = new ItemStack(Items.TIPPED_ARROW);
        tippedArrow.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        mob.setItemSlot(EquipmentSlot.OFFHAND, tippedArrow);
    }

    private static final List<Holder<MobEffect>> HERALDO_EFFECT_POOL = List.of(
            MobEffects.STRENGTH, MobEffects.REGENERATION, MobEffects.RESISTANCE, MobEffects.SPEED
    );

    /**
     * "Algunos mobs tienen efectos, como super fuerza, regeneracion, etc" —
     * un Heraldo recibe 1-2 efectos al azar de este pool, con amplificador
     * segun la ronda, de duracion larga (no infinita de verdad — MobEffectInstance
     * no tiene ese concepto salvo para el Beacon — pero 1000000 ticks son
     * ~14 horas reales, mas que de sobra para cualquier pelea).
     */
    private static void grantHeraldoEffects(Mob mob, RandomSource random, int round) {
        int amplifier = Math.min(round / 4, 3);
        int effectCount = 1 + random.nextInt(2);
        List<Holder<MobEffect>> pool = new ArrayList<>(HERALDO_EFFECT_POOL);
        Collections.shuffle(pool, new Random(random.nextLong()));

        for (int i = 0; i < effectCount && i < pool.size(); i++) {
            mob.addEffect(new MobEffectInstance(pool.get(i), 1_000_000, amplifier, false, false));
        }
    }

    /**
     * Spawn manual de una oleada de mobs junto a un jugador mientras la Luna
     * esta activa. A proposito NO pasa por NaturalSpawner (el spawn natural
     * de vainilla) — EntityType.spawn(...) crea la entidad directamente, asi
     * que el limite de mobs por jugador de vainilla (~70, compartido entre
     * TODAS las categorias) no aplica aqui en absoluto. El unico tope es el
     * propio (Config.MOON_HORDE_MAX_PER_PLAYER), para no ahogar el servidor.
     */
    public static void spawnHordeWave(ServerPlayer player, ServerLevel level, RandomSource random, ArcaneMoonData data) {
        long nearby = level.getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(Config.MOON_HORDE_CAP_RADIUS), Monster::isAlive).size();
        if (nearby >= Config.MOON_HORDE_MAX_PER_PLAYER) return;

        spawnWave(player, level, random, data.getRound(), true, Config.MOON_HORDE_SPAWNS_PER_WAVE, false);
    }

    /**
     * Version para /arcaneforge moon wave: un admin elige la ronda y la
     * cantidad a mano, sin pasar por el tope de horda ni por la ronda real
     * guardada — pensado para subir la dificultad de golpe (estilo evento
     * de servidor), no para el goteo automatico de cada noche. guaranteed=true
     * para que sea una amenaza real siempre, no una tirada de dados.
     */
    public static void spawnWave(ServerPlayer player, ServerLevel level, RandomSource random,
                                  int round, boolean moonActive, int count, boolean guaranteed) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = Config.MOON_HORDE_MIN_DIST
                    + random.nextDouble() * (Config.MOON_HORDE_MAX_DIST - Config.MOON_HORDE_MIN_DIST);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;

            EntityType<? extends Monster> type = HORDE_POOL.get(random.nextInt(HORDE_POOL.size()));
            Monster mob = type.spawn(level, pos, EntitySpawnReason.EVENT);
            if (mob == null) continue;

            equipMob(mob, random, round, moonActive, level, guaranteed);
            mob.setTarget(player);
        }
    }
}
