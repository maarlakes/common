package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.ContentTypes;
import cn.maarlakes.common.http.body.ContentBody;
import cn.maarlakes.common.http.body.ContentChannel;
import jakarta.annotation.Nonnull;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * @author linjpxc
 */
class ContentRequestBody extends RequestBody {

    private final ContentBody<?> body;
    private final ContentType contentType;

    ContentRequestBody(ContentBody<?> body) {
        this(body, body == null ? null : body.getContentType());
    }

    ContentRequestBody(ContentBody<?> body, ContentType contentType) {
        this.body = body;
        this.contentType = contentType;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        if (this.contentType == null) {
            return MediaType.parse("application/text");
        }
        return MediaType.parse(ContentTypes.toString(this.contentType));
    }

    @Override
    public void writeTo(@Nonnull BufferedSink bufferedSink) throws IOException {
        if (this.body instanceof ContentChannel) {
            ((ContentChannel) this.body).transferTo(bufferedSink);
        } else if (this.body != null) {
            this.body.writeTo(bufferedSink::write);
        }
    }
}
