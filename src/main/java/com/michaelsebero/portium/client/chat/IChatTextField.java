package com.michaelsebero.portium.client.chat;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/** What 1.13 added to text fields for the chat screen, mixed into {@code GuiTextField}. */
public interface IChatTextField {

    /** Recolours the visible text, given it and the index in the full text it starts at. */
    void portium$setFormatter(@Nullable BiFunction<String, Integer, String> formatter);

    /** Gray text shown after the cursor when it's at the end: the rest of the selected suggestion. */
    void portium$setSuggestion(@Nullable String suggestion);

    /** Called whenever the text changes. */
    void portium$setListener(@Nullable Runnable listener);
}
