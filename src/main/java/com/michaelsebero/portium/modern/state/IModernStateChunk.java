package com.michaelsebero.portium.modern.state;

/** Implemented by chunks, which carry the full states of their 1.13 blocks with more than 16 states. */
public interface IModernStateChunk {

    ModernStateData portium$getModernStates();
}
