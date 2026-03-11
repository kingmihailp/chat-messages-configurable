package com.kingmihailp.broadcastmod.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a message string with inline color / formatting codes into a
 * {@link Component} that Minecraft can render.
 *
 * <h2>Supported syntax</h2>
 * <pre>
 * &0-&9, &a-&f  – legacy Minecraft color codes
 * &k             – obfuscated (random glyphs)
 * &l             – bold
 * &m             – strikethrough
 * &n             – underline
 * &o             – italic
 * &r             – reset all formatting
 * &#RRGGBB       – full 24-bit hex color  (e.g. &#FF5500)
 * &#RGB          – 3-digit shorthand hex   (e.g. &#F80  →  #FF8800)
 * </pre>
 *
 * <h2>Example message</h2>
 * <pre>
 * "&6Welcome to the server! &r&#FF4444Danger &r&azone &r&#FFFFFFahead."
 * </pre>
 */
public final class MessageParser {

    // Matches  &#RRGGBB, &#RGB, or &X  (X = 0-9 a-f k-o r, case-insensitive)
    private static final Pattern CODE_PATTERN =
            Pattern.compile("&(#[0-9A-Fa-f]{3,6}|[0-9a-fk-orA-FK-OR])");

    private MessageParser() {}

    /**
     * Parse {@code raw} and return a fully styled {@link Component}.
     */
    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();

        MutableComponent result = Component.empty();
        Matcher matcher = CODE_PATTERN.matcher(raw);

        int cursor = 0;
        Style style = Style.EMPTY;

        while (matcher.find()) {
            // Append plain text between the last code and this one
            if (matcher.start() > cursor) {
                result.append(Component.literal(raw.substring(cursor, matcher.start()))
                        .withStyle(style));
            }

            style  = applyCode(style, matcher.group(1));
            cursor = matcher.end();
        }

        // Append any remaining text after the last code
        if (cursor < raw.length()) {
            result.append(Component.literal(raw.substring(cursor)).withStyle(style));
        }

        return result;
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    private static Style applyCode(Style base, String code) {
        // ── Hex color ────────────────────────────────────────────────────────────
        if (code.startsWith("#")) {
            String hex = code.substring(1);

            // Expand 3-digit shorthand: RGB → RRGGBB
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0)
                         + hex.charAt(1) + hex.charAt(1)
                         + hex.charAt(2) + hex.charAt(2);
            }

            if (hex.length() == 6) {
                try {
                    return base.withColor(TextColor.fromRgb(Integer.parseInt(hex, 16)));
                } catch (NumberFormatException ignored) {}
            }
            return base; // Malformed hex – leave style unchanged
        }

        // ── Legacy code ──────────────────────────────────────────────────────────
        return switch (Character.toLowerCase(code.charAt(0))) {
            // Colors
            case '0' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.BLACK));
            case '1' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_BLUE));
            case '2' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN));
            case '3' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_AQUA));
            case '4' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_RED));
            case '5' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE));
            case '6' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.GOLD));
            case '7' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY));
            case '8' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY));
            case '9' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.BLUE));
            case 'a' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.GREEN));
            case 'b' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.AQUA));
            case 'c' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED));
            case 'd' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE));
            case 'e' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW));
            case 'f' -> base.withColor(TextColor.fromLegacyFormat(ChatFormatting.WHITE));
            // Formatting
            case 'k' -> base.withObfuscated(true);
            case 'l' -> base.withBold(true);
            case 'm' -> base.withStrikethrough(true);
            case 'n' -> base.withUnderlined(true);
            case 'o' -> base.withItalic(true);
            // Reset
            case 'r' -> Style.EMPTY;
            default   -> base;
        };
    }
}
