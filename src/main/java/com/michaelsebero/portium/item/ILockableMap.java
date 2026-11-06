package com.michaelsebero.portium.item;

/** 1.14's locked maps: MapData with a lock (mixed in), which stops it drawing what the player explores. */
public interface ILockableMap {

    boolean portium$isLocked();

    void portium$setLocked(boolean locked);
}
