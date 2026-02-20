package com.onewhohears.tacview.init;

import com.google.common.collect.ImmutableSet;
import com.onewhohears.tacview.TacViewMod;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlagSet;

public class TVModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            TacViewMod.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<TacViewEntity>> TAC_VIEW = ENTITIES.register("tacview",
            () -> createEntityType(TacViewEntity::new, EntityDimensions.fixed(1, 1)));

    private static <T extends Entity> EntityType<T> createEntityType(EntityType.EntityFactory<T> factory, EntityDimensions size) {
        return new EntityType<>(factory, MobCategory.MISC, true, true, false,
                true, ImmutableSet.of(), size, 16, 3, FeatureFlagSet.of());
    }

    public static void register() {
        ENTITIES.register();
    }
}
