package com.michaelsebero.portium.advancements;

import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Listener bookkeeping shared by Portium's advancement triggers. */
public abstract class PortiumTrigger<T extends ICriterionInstance> implements ICriterionTrigger<T> {

    private final ResourceLocation id;
    private final Map<PlayerAdvancements, Set<Listener<T>>> listeners = new HashMap<>();

    protected PortiumTrigger(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public void addListener(PlayerAdvancements advancements, Listener<T> listener) {
        this.listeners.computeIfAbsent(advancements, key -> new HashSet<>()).add(listener);
    }

    @Override
    public void removeListener(PlayerAdvancements advancements, Listener<T> listener) {
        Set<Listener<T>> set = this.listeners.get(advancements);
        if (set == null) return;
        set.remove(listener);
        if (set.isEmpty()) this.listeners.remove(advancements);
    }

    @Override
    public void removeAllListeners(PlayerAdvancements advancements) {
        this.listeners.remove(advancements);
    }

    /** Grants every criterion of this trigger the player is listening for whose conditions pass. */
    protected void trigger(EntityPlayerMP player, Predicate<T> conditions) {
        Set<Listener<T>> set = this.listeners.get(player.getAdvancements());
        if (set == null) return;
        for (Listener<T> listener : new ArrayList<>(set)) {
            if (conditions.test(listener.getCriterionInstance())) listener.grantCriterion(player.getAdvancements());
        }
    }
}
