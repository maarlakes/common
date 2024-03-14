package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.token.Tokens;
import cn.maarlakes.common.utils.PathUtils;
import cn.maarlakes.common.utils.StreamUtils;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * @author linjpxc
 */
public class DefaultWeixinTokenFactory implements WeixinTokenFactory {

    private final WeixinSecretMapper secretMapper;
    private final Executor executor;
    private final String url;

    public DefaultWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper) {
        this(mapper, ForkJoinPool.commonPool());
    }

    public DefaultWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull Executor executor) {
        this(mapper, executor, "https://api.weixin.qq.com/cgi-bin/token?");
    }

    public DefaultWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull String tokenUrl) {
        this(mapper, ForkJoinPool.commonPool(), tokenUrl);
    }

    public DefaultWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull Executor executor, @Nonnull String tokenUrl) {
        this.secretMapper = mapper;
        this.executor = executor;
        this.url = tokenUrl;
    }

    @Nonnull
    @Override
    public CompletionStage<WeixinToken> createToken(@Nonnull String appId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final LocalDateTime now = LocalDateTime.now();
                final URL url = new URL(PathUtils.combineWith("?", this.url, "grant_type=client_credential&appid=" + appId + "&secret=" + this.secretMapper.getSecret(appId)));
                final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.connect();
                try (InputStream stream = connection.getInputStream()) {
                    final String json = StreamUtils.readAllText(stream);
                    return WeixinTokenUtils.toWeixinToken(json, appId, now);
                }
            } catch (Exception e) {
                throw Tokens.newTokenException(e);
            }
        }, this.executor);
    }
}
