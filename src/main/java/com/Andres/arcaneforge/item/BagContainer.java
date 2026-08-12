package com.Andres.arcaneforge.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Almacen del Saco Dimensional: vista de UNA pagina (54 espacios completos).
 *
 * IMPORTANTE: NO usamos el componente CONTAINER (limite duro de 256 espacios).
 * Guardamos TODO el contenido del saco en NBT propio dentro de CUSTOM_DATA,
 * bajo la clave "bag_items", como una lista de {s:indiceGlobal, item:<itemNBT>}.
 * Asi no hay limite de 256 y podemos tener muchas paginas.
 *
 * Para serializar cada ItemStack usamos ItemStack.CODEC con RegistryOps(NbtOps),
 * que es la forma estable en esta version de Minecraft/NeoForge.
 *
 * Cada espacio apila hasta 99.
 */
public class BagContainer extends SimpleContainer {

    public static final int PAGE_SIZE  = 54;            // 6 filas completas
    public static final int MAX_STACK  = 1_000_000_000; // sin limite practico
    public static final String ITEMS_KEY = "bag_items";

    private final ItemStack bag;
    private int pageOffset;
    private final RegistryOps<Tag> ops;

    public BagContainer(ItemStack bag, int page, HolderLookup.Provider provider) {
        super(PAGE_SIZE);
        this.bag = bag;
        this.pageOffset = page * PAGE_SIZE;
        this.ops = RegistryOps.create(NbtOps.INSTANCE, provider);
        loadFromBag();
    }

    /** Carga en los 54 slots el contenido de la pagina actual (pageOffset). */
    private void loadFromBag() {
        // Limpiar primero.
        for (int i = 0; i < PAGE_SIZE; i++) {
            this.getItems().set(i, ItemStack.EMPTY);
        }
        CompoundTag tag = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = tag.getList(ITEMS_KEY).orElseGet(ListTag::new);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i).orElseGet(CompoundTag::new);
            int globalIndex = entry.getInt("s").orElse(-1);
            if (globalIndex >= pageOffset && globalIndex < pageOffset + PAGE_SIZE) {
                Tag itemTag = entry.get("item");
                if (itemTag != null) {
                    ItemStack stack = ItemStack.CODEC.parse(ops, itemTag).result().orElse(ItemStack.EMPTY);
                    this.getItems().set(globalIndex - pageOffset, stack);
                }
            }
        }
    }

    /** Guarda la pagina actual y carga otra distinta, SIN reabrir la ventana. */
    public void loadPage(int newPage) {
        save();                      // guarda lo que hubiera en la pagina actual
        this.pageOffset = newPage * PAGE_SIZE;
        loadFromBag();               // carga la nueva pagina en los mismos slots
    }

    @Override
    public int getMaxStackSize() {
        return MAX_STACK;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        save();
    }

    public void saveNow() {
        save();
    }

    private void save() {
        if (bag.isEmpty()) return;

        CompoundTag tag = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag oldList = tag.getList(ITEMS_KEY).orElseGet(ListTag::new);

        // Conservar todas las entradas que NO son de esta pagina.
        ListTag newList = new ListTag();
        for (int i = 0; i < oldList.size(); i++) {
            CompoundTag entry = oldList.getCompound(i).orElseGet(CompoundTag::new);
            int globalIndex = entry.getInt("s").orElse(-1);
            if (globalIndex < pageOffset || globalIndex >= pageOffset + PAGE_SIZE) {
                newList.add(entry);
            }
        }

        // Anadir los items actuales de esta pagina (solo los no vacios).
        for (int i = 0; i < PAGE_SIZE; i++) {
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty()) {
                Tag itemTag = ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);
                if (itemTag != null) {
                    CompoundTag entry = new CompoundTag();
                    entry.putInt("s", pageOffset + i);
                    entry.put("item", itemTag);
                    newList.add(entry);
                }
            }
        }

        tag.put(ITEMS_KEY, newList);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
