package com.Andres.arcaneforge.util;

/**
 * Interfaz "pato" implementada por InfiniteChestBlockEntityMixin (sobre
 * ChestBlockEntity). Como el metodo real solo existe en el bytecode
 * transformado en tiempo de ejecucion, cualquier OTRA clase (por ejemplo
 * InfiniteChestPlaceMixin, sobre BlockItem) no puede llamarlo directamente
 * contra el tipo vainilla ChestBlockEntity — pero SI puede hacer
 * "instanceof IArcaneInfiniteChest" contra esta interfaz propia, que
 * compila normal porque es nuestra.
 *
 * Vive fuera del paquete com.Andres.arcaneforge.mixin a proposito: Mixin
 * trata ese paquete como "sandbox" (declarado como "package" en
 * Arcaneforge.mixins.json) y prohibe que codigo normal cargue directamente
 * cualquier clase ahi dentro, incluso interfaces simples sin @Mixin.
 */
public interface IArcaneInfiniteChest {
    void arcaneforge$setInfinite(boolean value);

    boolean arcaneforge$isInfinite();
}
