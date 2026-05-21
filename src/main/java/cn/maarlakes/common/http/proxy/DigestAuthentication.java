package cn.maarlakes.common.http.proxy;

import jakarta.annotation.Nonnull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class DigestAuthentication implements ProxyAuthentication {
    private static final long serialVersionUID = 4982801167289082558L;

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String username;
    private final String password;

    public DigestAuthentication(@Nonnull String username, @Nonnull String password) {
        this.username = username;
        this.password = password;
    }

    @Nonnull
    public String getPassword() {
        return password;
    }

    @Nonnull
    public String getUsername() {
        return username;
    }

    public String toAuthorization(@Nonnull String digestAuthenticate, String method, String uri) {
        final Map<String, String> challenge = parseChallenge(digestAuthenticate);
        final String realm = challenge.get("realm");
        final String nonce = challenge.get("nonce");
        if (realm == null || nonce == null) {
            return null;
        }
        final String cnonce = generateNonce();
        final String nc = "00000001";
        String qop = challenge.get("qop");
        String selectedQop = null;
        if (qop != null) {
            for (String part : qop.split(",")) {
                if ("auth".equals(part.trim())) {
                    selectedQop = "auth";
                    break;
                }
            }
            if (selectedQop == null && qop.contains("auth-int")) {
                selectedQop = "auth-int";
            }
        }
        final String responseValue = computeResponse(
                this.username, this.password, realm,
                method, uri, nonce, nc, cnonce, selectedQop
        );
        final StringBuilder sb = new StringBuilder("Digest ");
        sb.append("username=\"").append(this.username).append("\", ");
        sb.append("realm=\"").append(realm).append("\", ");
        sb.append("nonce=\"").append(nonce).append("\", ");
        sb.append("uri=\"").append(uri).append("\", ");
        if (selectedQop != null) {
            sb.append("qop=").append(selectedQop).append(", ");
            sb.append("nc=").append(nc).append(", ");
            sb.append("cnonce=\"").append(cnonce).append("\", ");
        }
        sb.append("response=\"").append(responseValue).append("\"");
        final String opaque = challenge.get("opaque");
        if (opaque != null) {
            sb.append(", opaque=\"").append(opaque).append("\"");
        }
        return sb.toString();
    }

    private static Map<String, String> parseChallenge(@Nonnull String headerValue) {
        final Map<String, String> result = new HashMap<>();
        final String trimmed = headerValue.trim();
        if (!trimmed.regionMatches(true, 0, "Digest", 0, 6)) {
            return result;
        }
        String rest = trimmed.substring(6).trim();
        if (rest.startsWith(" ")) {
            rest = rest.substring(1);
        }

        int i = 0;
        while (i < rest.length()) {
            // Skip whitespace and commas
            while (i < rest.length() && (rest.charAt(i) == ' ' || rest.charAt(i) == ',')) {
                i++;
            }
            if (i >= rest.length()) {
                break;
            }

            // Parse key
            final int keyStart = i;
            while (i < rest.length() && rest.charAt(i) != '=' && rest.charAt(i) != ' ' && rest.charAt(i) != ',') {
                i++;
            }
            final String key = rest.substring(keyStart, i).trim().toLowerCase();

            // Skip whitespace
            while (i < rest.length() && rest.charAt(i) == ' ') {
                i++;
            }

            if (i >= rest.length() || rest.charAt(i) != '=') {
                continue;
            }

            // Skip whitespace
            do {
                i++;
            } while (i < rest.length() && rest.charAt(i) == ' ');

            if (i >= rest.length()) {
                break;
            }

            // Parse value
            String value;
            if (rest.charAt(i) == '"') {
                i++; // skip opening quote
                final StringBuilder sb = new StringBuilder();
                while (i < rest.length()) {
                    final char c = rest.charAt(i);
                    if (c == '"') {
                        i++;
                        break;
                    }
                    if (c == '\\' && i + 1 < rest.length()) {
                        i++;
                        sb.append(rest.charAt(i));
                    } else {
                        sb.append(c);
                    }
                    i++;
                }
                value = sb.toString();
            } else {
                final int valueStart = i;
                while (i < rest.length() && rest.charAt(i) != ' ' && rest.charAt(i) != ',') {
                    i++;
                }
                value = rest.substring(valueStart, i);
            }

            result.put(key, value);
        }

        return result;
    }

    @Nonnull
    private static String computeResponse(
            @Nonnull String username, @Nonnull String password, @Nonnull String realm,
            @Nonnull String method, @Nonnull String uri, @Nonnull String nonce,
            @Nonnull String nc, @Nonnull String cnonce, String qop
    ) {
        final String ha1 = md5Hex(username + ":" + realm + ":" + password);
        final String ha2 = md5Hex(method + ":" + uri);
        if (("auth".equals(qop) || "auth-int".equals(qop))) {
            return md5Hex(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2);
        }
        return md5Hex(ha1 + ":" + nonce + ":" + ha2);
    }

    @Nonnull
    private static String md5Hex(@Nonnull String input) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            return toHexString(md5.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    @Nonnull
    public static String generateNonce() {
        final byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return toHexString(bytes);
    }

    @Nonnull
    private static String toHexString(@Nonnull byte[] bytes) {
        final char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            final int v = bytes[i] & 0xFF;
            chars[i * 2] = HEX_CHARS[v >>> 4];
            chars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(chars);
    }
}
