package com.michaelsebero.portium.world;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

/**
 * Deaths from beds exploding in the Nether or End are credited to "[Intentional Game Design]" with a link to the
 * bug report, as in 1.16. The bed mixin marks its explosion while it runs; the explosion mixin swaps the damage source.
 */
public final class BedExplosions {

    private static final String BUG_URL = "https://bugs.mojang.com/browse/MCPE-28723";
    private static boolean active;

    public static void begin() {
        active = true;
    }

    public static void end() {
        active = false;
    }

    public static boolean isActive() {
        return active && PortiumConfig.mechanics.intentionalGameDesign;
    }

    public static DamageSource damageSource() {
        return new BadRespawnPoint().setDifficultyScaled().setExplosion();
    }

    private static final class BadRespawnPoint extends DamageSource {

        BadRespawnPoint() {
            super(Tags.MOD_ID + ".bad_respawn_point");
        }

        @Override
        public ITextComponent getDeathMessage(EntityLivingBase victim) {
            ITextComponent link = new TextComponentString("[").appendSibling(new TextComponentTranslation("death.attack.portium.bad_respawn_point.link")).appendText("]");
            link.getStyle()
                    .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, BUG_URL))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("MCPE-28723")));
            return new TextComponentTranslation("death.attack.portium.bad_respawn_point.message", victim.getDisplayName(), link);
        }
    }

    private BedExplosions() {}
}
