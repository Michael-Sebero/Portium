package com.michaelsebero.portium.modern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The SRG name 1.13 code uses for a member of one of Portium's stand-ins for 1.13 Minecraft classes (package
 * {@code modern.mc}). The translator renames references to that name, and overrides declared under it, to the
 * annotated member's own name.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Srg {

    String value();
}
