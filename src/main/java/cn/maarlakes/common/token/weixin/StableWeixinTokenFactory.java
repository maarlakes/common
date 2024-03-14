package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.token.Tokens;
import cn.maarlakes.common.utils.StreamUtils;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.io.OutputStream;
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
public class StableWeixinTokenFactory implements WeixinTokenFactory {

    private final WeixinSecretMapper secretMapper;
    private final Executor executor;
    private final String url;
    private final boolean forceRefresh;

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper) {
        this(mapper, ForkJoinPool.commonPool());
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, boolean forceRefresh) {
        this(mapper, ForkJoinPool.commonPool(), forceRefresh);
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull Executor executor) {
        this(mapper, executor, "https://api.weixin.qq.com/cgi-bin/stable_token", false);
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull Executor executor, boolean forceRefresh) {
        this(mapper, executor, "https://api.weixin.qq.com/cgi-bin/stable_token", forceRefresh);
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull String tokenUrl) {
        this(mapper, ForkJoinPool.commonPool(), tokenUrl, false);
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull String tokenUrl, boolean forceRefresh) {
        this(mapper, ForkJoinPool.commonPool(), tokenUrl, forceRefresh);
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull Executor executor, @Nonnull String tokenUrl, boolean forceRefresh) {
        this.secretMapper = mapper;
        this.executor = executor;
        this.url = tokenUrl;
        this.forceRefresh = forceRefresh;
    }

    @Nonnull
    @Override
    public CompletionStage<WeixinToken> createToken(@Nonnull String appId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final LocalDateTime now = LocalDateTime.now();
                final URL url = new URL(this.url);
                final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.connect();
                try (OutputStream stream = connection.getOutputStream()) {
                    final JSONObject json = new JSONObject();
                    json.put("grant_type", "client_credential");
                    json.put("appid", appId);
                    json.put("secret", this.secretMapper.getSecret(appId));
                    json.put("force_refresh", this.forceRefresh);
                    stream.write(json.toJSONString().getBytes());
                }
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
