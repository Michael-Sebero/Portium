package com.michaelsebero.portium.diagnostics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.AbstractFilter;

/**
 * Keeps known harmless noise out of the log, so real problems stand out (to the player, and in Portium's reports).
 * Registrate (bundled by Create and many 1.16 mods) clears a Forge event bus cache after each one-time listener and
 * warns, with a stack trace, when the bus isn't Forge's own class; on Portium that's every one of them (hundreds per
 * start), and Registrate itself calls it "not a big deal": the listener is already unregistered.
 */
public final class LogFilters {

    public static void install() {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.getConfiguration().getRootLogger().addFilter(new AbstractFilter(Filter.Result.NEUTRAL, Filter.Result.NEUTRAL) {
                @Override
                public Result filter(LogEvent event) {
                    String logger = event.getLoggerName();
                    if (logger != null && logger.endsWith("registrate.util.OneTimeEventReceiver") && event.getMessage() != null
                            && event.getMessage().getFormattedMessage().startsWith("Failed to clear listener list")) {
                        return Result.DENY;
                    }
                    return Result.NEUTRAL;
                }
            });
            context.updateLoggers();
        } catch (RuntimeException | LinkageError e) {
            LogManager.getLogger("Portium").debug("Couldn't install Portium's log filters", e);
        }
    }

    private LogFilters() {}
}
