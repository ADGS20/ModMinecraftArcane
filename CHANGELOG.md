# Changelog — Luna Oscura/Roja y Generador Arcano de Gólems

Este documento cubre la evolución de dos sistemas grandes del mod (la Luna
Oscura/Roja y el Generador Arcano de Gólems) y los ajustes de esta sesión.
No es un historial completo de todo el mod — solo de lo que se ha ido
construyendo e iterando en estas conversaciones.

## Luna Oscura/Roja — evento mundial escalable

**Base del sistema:**
- Cada `MOON_INTERVAL_DAYS` días de juego (10 por defecto) sube una ronda
  permanente y arranca una "Luna" activa de `MOON_DURATION_TICKS` (24000
  ticks, 1 día completo).
- Cada ronda define un rango de nivel de encantamiento (`ArcaneMoonLogic.rangeForRound`,
  de 1-3 en la ronda 1 hasta 250-255 en ronda 10+) que se le puede aplicar
  al arma de un mob hostil al spawnear.
- Probabilidad de que un mob salga "equipado" y de que sea un **Heraldo**
  (élite con nombre propio, brillo, más vida y más daño) — ambas suben con
  la ronda y con si la Luna está activa esa noche.
- Los no-muertos (Zombie/Skeleton) ganan cascos progresivamente con la
  ronda, lo que los hace inmunes al sol por un efecto secundario de la IA
  vanilla (`RestrictSunGoal`/`FleeSunGoal` ignoran el sol si el mob lleva
  algo en la cabeza).
- Ítem consumible **Fragmento Lunar**: adelanta la próxima Luna con cooldown propio.

**Esta sesión — combate y detección:**
- Los Golems Arcanos ahora SÍ pueden atacar Creepers (`ArcaneGolemCreeperTargetMixin`
  + búsqueda activa cada 20 ticks) — antes vainilla los excluía a propósito.
- Dos Golems Arcanos ya NUNCA se hacen daño real entre sí (`onGolemFriendlyFire`,
  daño puesto a 0) **y** dejan de tenerse como objetivo apenas ocurre
  (`onGolemNoFriendlyTarget`) — el primer arreglo por sí solo no bastaba
  porque vainilla registra "quién me golpeó" antes de que el daño se anule,
  así que un roce (una bola de nieve, un rebote de Cadena Arcana) los dejaba
  peleándose igual aunque sin hacerse daño.
- Radio de detección/caza subido de 64 bloques a **15 chunks (240 bloques)**,
  y ahora se re-fuerza cada 1.5s sobre TODO mob hostil en ese radio — no
  solo a los recién spawneados, así los que ya estaban ahí antes de que
  empezara la Luna también se suman a la cacería.
- Nuevo **sistema de horda**: cada 5 segundos mientras la Luna está activa,
  aparece una oleada de mobs (4 por jugador) creada directamente por código
  — como no pasa por el spawn natural de vainilla, el límite de mobs de
  vainilla (~70, compartido entre categorías) no le aplica. Tope propio de
  seguridad: 40 mobs cerca de cada jugador.
- **Cielo rojo**: mientras la Luna está activa, la niebla se tiñe de rojo
  intenso en el cliente (`ArcaneMoonClientHandler`, evento `ViewportEvent.ComputeFogColor`),
  sincronizado por red (`S2CMoonStateSync`) incluyendo a quien se conecta
  en medio de una Luna ya activa.

**Nueva sesión — más drástico todavía:**
- **Niebla roja más agresiva**: además del tinte de color, la distancia de
  niebla se acerca a ~55% de lo normal mientras la Luna está activa
  (`ViewportEvent.RenderFog`), así el rojo domina mucho más de lo que se ve
  — sin esto, con distancias de render típicas el tinte casi no se notaba.
  El domo del cielo en sí (no solo la niebla) necesitaría un mixin al
  renderer o una textura de luna recoloreada — no se tocó por riesgo/tiempo,
  queda como posible mejora futura si el efecto de niebla no basta.
- **La ventana de peligro dura el doble** (`MOON_DURATION_TICKS`: 24000 →
  48000 ticks = 2 días completos). Importante: NO se tocó el ciclo real de
  día/noche de Minecraft — al investigar el código fuente resultó que esta
  versión reemplazó el sistema clásico por uno de "WorldClock" totalmente
  nuevo y en desarrollo (`net.minecraft.world.clock`), demasiado nuevo y
  frágil para tocarlo a ciegas sin arriesgar romper el sueño/spawneo en
  todo el mod. En vez de eso se duplicó cuánto dura la ventana de peligro
  propia de la Luna (caza + horda + cielo rojo), que ya estaba
  desacoplada del ciclo real de sol por diseño.
- **Heraldos de ronda alta rompen terreno natural**: a partir de
  `MOON_HERALDO_DIG_MIN_ROUND` (4), un Heraldo sin línea de visión a su
  objetivo intenta romper el bloque que lo bloquea (`ArcaneHeraldoDigHandler`).
  Solo rompe bloques de la tag de datapack `#arcaneforge:heraldo_diggable`
  (piedra, tierra, arena, troncos, hojas, hielo, nieve, etc. — terreno
  natural) — nunca tablones, ladrillos, hormigón ni nada fabricado por el
  jugador, así que una casa de verdad sigue siendo refugio. Un servidor
  puede editar esa tag para ajustar qué se puede romper.
- **`/arcaneforge moon wave`**: un admin puede invocar de golpe una oleada
  de mobs equipados a la ronda que elija, junto a un jugador — sin tocar la
  ronda real ni el temporizador de la Luna. Pensado para eventos tipo
  "sube la dificultad ya" en vivo.

**Balance tras la primera prueba real (los Heraldos morían muy fácil):**
- **Armadura completa**: a partir de `MOON_HERALDO_FULL_ARMOR_MIN_ROUND` (3),
  un Heraldo sale con casco+peto+piernas+botas, no solo el peto — antes el
  encantamiento le subía el daño que HACE, pero sin puntos de armadura de
  verdad moría igual de rápido que un mob normal.
- **Totem de la Inmortalidad**: a partir de `MOON_HERALDO_TOTEM_MIN_ROUND` (6),
  el Heraldo lleva uno en la mano secundaria — segunda oportunidad real,
  igual que tendría un jugador (vainilla ya deja que CUALQUIER entidad viva
  se salve con un totem si lo lleva puesto, no hace falta lógica nueva).
- **Vida y daño ya no se topan en la ronda 10**: antes el multiplicador de
  vida/daño llegaba a su tope justo en la ronda 10 (mismo punto donde el
  encantamiento llega a 255) y ya no subía más — así que una ronda más alta
  invocada a mano no se sentía más peligrosa de verdad. Ahora sigue
  escalando hasta `MOON_ELITE_HEALTH_MULT_CAP` (12x) / `MOON_ELITE_DAMAGE_MULT_CAP` (10x).
- **`/arcaneforge moon wave` acepta rondas hasta 50** (antes 10) y **hasta
  300 mobs por oleada** (antes 60).
**Segunda ronda de balance (la horda invocada seguía sin hacer daño real):**
- **Causa real encontrada**: el multiplicador de daño de un Heraldo
  (`ELITE_DAMAGE_ID`) escala el atributo `ATTACK_DAMAGE` — pero ese atributo
  SOLO se usa en el golpe cuerpo a cuerpo. El daño de una flecha de
  Esqueleto, la explosión de un Creeper o la poción de una Bruja no leen
  `ATTACK_DAMAGE` en absoluto, así que 3 de los 5 tipos de la horda
  (Esqueleto, Creeper, Bruja) seguían pegando con su daño de vainilla
  normal pese a tener un enchant altísimo puesto en el arma.
- **Sharpness/Power vanilla garantizados**: ahora, además del encantamiento
  propio del mod, toda arma de un mob equipado recibe Sharpness (cuerpo a
  cuerpo) o Power (arco), escalando con la ronda — son encantamientos
  vanilla probados que sí afectan el tipo de daño real de cada mob.
- **Armadura con Proteccion de verdad**: casco/piernas/botas ahora llevan
  Proteccion vanilla (antes salían sin ningún encantamiento).
- **Equipo irrompible**: arma y armadura de un Heraldo ya no se pueden
  romper por durabilidad (`DataComponents.UNBREAKABLE`) — el "encantamiento
  infinito" pedido, aplicado como propiedad del ítem.
- **`/arcaneforge moon wave` ahora es GARANTIZADO**: antes, invocar una
  oleada seguía tirando los mismos dados de probabilidad que un spawn
  natural (~75% de sacar algo de gear, ~28-35% de ser Heraldo) — de 200
  mobs pedidos, la mayoría podían salir sin nada. Ahora, todo mob de una
  oleada invocada por comando sale SIEMPRE armado y SIEMPRE es Heraldo
  (con armadura completa + Proteccion + Totem si la ronda lo permite). La
  horda automática de cada noche sigue siendo probabilística, sin cambios.
**Tercera ronda de balance (mas encantamientos del mod, efectos y flechas venenosas):**
- **Pool de armas ampliado con encantamientos del mod**: además de los 8
  originales, ahora también pueden salir Golpe Partidor, Ataque Veloz,
  Cataclismo Arcano, Repulsión Arcana y **Vampiro** (sí, un mob curándose
  al golpear — el mismo enganche agnóstico de quién ataca que ya usa el
  jugador, tomado de la lista curada `GOLEM_MELEE_ENCHANTS` que ya existía
  para los golems).
- **Protección del Vacío en la armadura**: casco/piernas/botas de un
  Heraldo ahora también llevan este encantamiento propio del mod, además
  de la Protección vanilla.
- **Efectos de poción**: un Heraldo recibe 1-2 efectos al azar (Fuerza,
  Regeneración, Resistencia o Velocidad) con amplificador según la ronda,
  de duración larguísima (~14h reales).
- **Los Esqueletos de la Luna disparan flechas envenenadas/dañinas** en vez
  de las normales — flecha con Daño Instantáneo (Daño Instantáneo Fuerte
  desde ronda 8). Truco de API sin mixin: `Monster.getProjectile()` mira
  primero la mano secundaria del mob, así que basta con ponerle ahí una
  Flecha con Efecto. Nota: por esto mismo, los Esqueletos ya NO llevan
  Totem (la mano secundaria la ocupa la flecha).
- **Los mobs de la Luna ya no se pelean entre ellos por accidente**
  (`ArcaneMonsterFriendlyFireHandler`) — una flecha perdida, una explosión
  de Creeper o una poción de Bruja que salpica a otro mob cercano ya no
  desata una pelea interna que se autodestruye la horda antes de llegar al
  jugador. Acotado a mobs que cazan al mismo jugador (no toca dinámicas
  vanilla reales como Piglin vs Hoglin).

## Generador Arcano de Gólems

- Se encanta un **Núcleo** una vez en la Forja (costo normal de XP +
  combustible mágico), se inserta en el bloque generador (clic derecho) y
  se le vinculan cofres con la Vara de Vinculación.
- Cada `GOLEM_GENERATOR_INTERVAL_TICKS` (6000 = 5 min reales), si los
  cofres tienen el doble de la receta vainilla (8 Hierro + 2 Calabaza
  Tallada, o 4 Nieve + 2 Calabaza Tallada), retira los materiales y
  spawnea un gólem nuevo con exactamente los mismos encantamientos que el
  Núcleo — sin volver a gastar XP del jugador nunca más.
- **Esta sesión**: si el cofre tiene AMBOS materiales a la vez (hierro Y
  nieve), el generador elige al azar entre gólem de hierro o de nieve en
  vez de priorizar siempre hierro — antes, mientras hubiera hierro de
  sobra, nunca salía un gólem de nieve aunque también hubiera nieve. El
  mensaje al insertar el Núcleo ahora también explica las dos recetas.
- Tope anti-spam: no produce más si ya hay `GOLEM_GENERATOR_MAX_NEARBY`
  gólems en un radio de `GOLEM_GENERATOR_NEARBY_RADIUS` bloques.

## Comandos de administración (`/arcaneforge`, nivel de operador)

```
/arcaneforge enchant disable <namespace:path>   prohíbe conseguir ESE encantamiento nuevo
/arcaneforge enchant enable  <namespace:path>   lo vuelve a permitir
/arcaneforge enchant list                        lista los prohibidos ahora mismo

/arcaneforge moon enable                         enciende la Luna Oscura/Roja
/arcaneforge moon disable                        la apaga (corta una activa al instante)
/arcaneforge moon status                         ronda actual + si está activa ahora
/arcaneforge moon wave <ronda> [cantidad] [jugador]   invoca una oleada de golpe a esa ronda
```

## Constantes ajustables (`Config.java`)

Todo lo numérico de ambos sistemas vive en `Config.java` — radios, tiempos,
probabilidades y costes de materiales — para poder afinarlos sin tocar la
lógica.
