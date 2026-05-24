package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.body.AbstractByteArrayBody;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * 基于字节数组的 {@link AbstractPart} 抽象子类，委托内容操作给 {@link AbstractByteArrayBody}。
 *
 * <p>将 Part 的流式读取、长度计算和分段写入全部委托给内部延迟创建的
 * {@link AbstractByteArrayBody} 实例。子类只需实现 {@link #createContentBody()}
 * 定义具体的 Body 创建逻辑。使用双重检查锁保证 Body 实例的线程安全延迟初始化。</p>
 *
 * @param <T> Part 内容的原始类型
 * @author linjpxc
 */
public abstract class AbstractByteArrayPart<T> extends AbstractPart<T> {
    /** 延迟创建的内容 Body 实例，volatile 保证多线程可见性 */
    private volatile AbstractByteArrayBody<byte[]> body;
    /** 双重检查锁的同步监视器 */
    private final Object bodyLock = new Object();

    public AbstractByteArrayPart(@Nonnull String name, ContentType contentType, Charset charset) {
        super(name, contentType, charset);
    }

    @Override
    public InputStream getContentStream() {
        return this.getBody().getContentStream();
    }

    @Override
    public int getContentLength() {
        return this.getBody().getContentLength();
    }

    @Override
    public void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer) {
        this.getBody().writeTo(consumer);
    }

    /**
     * 子类实现此方法，创建用于处理字节数组内容的 Body 实例。
     */
    protected abstract AbstractByteArrayBody<byte[]> createContentBody();

    /**
     * 获取 Body 实例，首次访问时通过 {@link #createContentBody()} 延迟创建。
     * 使用双重检查锁保证线程安全。
     */
    private AbstractByteArrayBody<byte[]> getBody() {
        if (body == null) {
            synchronized (bodyLock) {
                if (body == null) {
                    body = createContentBody();
                }
            }
        }
        return body;
    }
}
