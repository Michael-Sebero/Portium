package net.minecraftforge.fml;

import net.minecraftforge.forgespi.language.IModInfo;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Portium's replacement for Forge 1.13's message formatting, whose mod info formats assume Forge's own launcher's mod
 * info class. Same API and formats: {@code {0,modinfo,id|name}}, {@code {0,exc,msg|cls}}, {@code {0,lower|upper}},
 * {@code {0,i18n}}, {@code {0,ornull,key}} and {@code {0,vr}}, on top of {@link MessageFormat}'s.
 */
public class ForgeI18n {

    private static final Pattern CUSTOM = Pattern.compile("\\{(\\d+),(modinfo|exc|lower|upper|i18n|ornull|vr)(?:,([^}]*))?}");

    private static volatile Map<String, String> messages = Collections.emptyMap();

    public static String getPattern(String key) {
        return messages.getOrDefault(key, key);
    }

    public static void loadLanguageData(Map<String, String> data) {
        messages = data;
    }

    public static String parseMessage(String key, Object... arguments) {
        return parseFormat(getPattern(key), arguments);
    }

    public static String parseFormat(String format, Object... arguments) {
        Matcher matcher = CUSTOM.matcher(format);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            Object argument = index < arguments.length ? arguments[index] : null;
            String text = custom(matcher.group(2), matcher.group(3), argument);
            // Quoted, so MessageFormat leaves it as it is.
            matcher.appendReplacement(resolved, Matcher.quoteReplacement("'" + text.replace("'", "''") + "'"));
        }
        matcher.appendTail(resolved);
        try {
            return MessageFormat.format(resolved.toString(), arguments);
        } catch (IllegalArgumentException e) {
            return resolved.toString();
        }
    }

    private static String custom(String type, String option, Object argument) {
        switch (type) {
            case "modinfo":
                if (!(argument instanceof IModInfo)) return String.valueOf(argument);
                IModInfo info = (IModInfo) argument;
                return "name".equals(option) ? info.getDisplayName() : info.getModId();
            case "exc":
                if (!(argument instanceof Throwable)) return String.valueOf(argument);
                Throwable throwable = (Throwable) argument;
                return "cls".equals(option) ? throwable.getClass().getName() : String.valueOf(throwable.getMessage());
            case "lower":
                return String.valueOf(argument).toLowerCase(Locale.ROOT);
            case "upper":
                return String.valueOf(argument).toUpperCase(Locale.ROOT);
            case "i18n":
                return getPattern(String.valueOf(argument));
            case "ornull":
                return argument == null ? getPattern(option) : String.valueOf(argument);
            default:
                return String.valueOf(argument);
        }
    }
}
