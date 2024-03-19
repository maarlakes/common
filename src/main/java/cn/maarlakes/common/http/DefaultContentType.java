package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author linjpxc
 */
final class DefaultContentType implements ContentType {
    private static final long serialVersionUID = -3562691153207914795L;

    private final String mediaType;
    private final String charset;
    private final List<NameValuePair> parameters;

    DefaultContentType(@Nonnull String mediaType, String charset, @Nonnull Collection<? extends NameValuePair> parameters) {
        this.mediaType = mediaType;
        this.charset = charset;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }

    @Nonnull
    @Override
    public String getMediaType() {
        return this.mediaType;
    }

    @Override
    public String getCharset() {
        return this.charset;
    }

    @Override
    public Collection<NameValuePair> getParameters() {
        return this.parameters;
    }

    @Override
    public String getParameter(@Nonnull String name) {
        for (NameValuePair parameter : this.parameters) {
            if (name.equalsIgnoreCase(parameter.getName())) {
                return parameter.getValue();
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public ContentType withCharset(String charset) {
        if (Objects.equals(this.charset, charset)) {
            return this;
        }
        return new DefaultContentType(this.mediaType, charset, this.parameters);
    }

    @Nonnull
    @Override
    public ContentType withParameter(@Nonnull NameValuePair... params) {
        final Map<String, NameValuePair> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (NameValuePair parameter : this.parameters) {
            map.put(parameter.getName(), parameter);
        }
        for (NameValuePair parameter : params) {
            map.put(parameter.getName(), parameter);
        }
        return new DefaultContentType(this.mediaType, this.charset, map.values());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ContentType) {
            final ContentType that = (ContentType) obj;
            return this.mediaType.equalsIgnoreCase(that.getMediaType()) && Objects.equals(this.charset, that.getCharset())
                    && this.parameters.equals(that.getParameters());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.mediaType, this.charset, this.parameters);
    }

    @Override
    public String toString() {
        return this.toHeader().toString();
    }
}
