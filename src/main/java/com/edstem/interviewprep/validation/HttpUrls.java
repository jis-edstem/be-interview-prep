package com.edstem.interviewprep.validation;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpUrls {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Pattern PORT_SUFFIX = Pattern.compile(":\\d*$");

    private HttpUrls() {}

    public static Optional<String> toAscii(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null
                    || !ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
                return Optional.empty();
            }
            URI serverBased = uri.getHost() != null ? uri : withAsciiHost(uri);
            return serverBased.getHost() == null ? Optional.empty() : Optional.of(serverBased.toASCIIString());
        } catch (URISyntaxException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static URI withAsciiHost(URI uri) throws URISyntaxException {
        String authority = uri.getRawAuthority();
        if (authority == null) {
            return uri;
        }
        int userInfoEnd = authority.lastIndexOf('@') + 1;
        String hostAndPort = authority.substring(userInfoEnd);
        Matcher port = PORT_SUFFIX.matcher(hostAndPort);
        int hostEnd = port.find() ? port.start() : hostAndPort.length();

        StringBuilder rebuilt = new StringBuilder(uri.getScheme())
                .append("://")
                .append(authority, 0, userInfoEnd)
                .append(IDN.toASCII(hostAndPort.substring(0, hostEnd), IDN.USE_STD3_ASCII_RULES))
                .append(hostAndPort.substring(hostEnd));
        if (uri.getRawPath() != null) {
            rebuilt.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            rebuilt.append('?').append(uri.getRawQuery());
        }
        if (uri.getRawFragment() != null) {
            rebuilt.append('#').append(uri.getRawFragment());
        }
        return new URI(rebuilt.toString());
    }
}
