package cn.maarlakes.common.http.body;

import cn.maarlakes.common.function.Consumer3;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 基于字节数组的 {@link ContentBody} 抽象基类，提供延迟序列化与双重检查锁缓存。
 *
 * <p>子类只需实现 {@link #contentAsBytes()} 定义如何将原始内容转换为字节数组，
 * 本类负责：线程安全的延迟缓存（volatile + synchronized 双重检查锁）、
 * 从缓存创建 {@link ByteArrayInputStream}、计算内容长度、以及分段写入。
 * 适用于内容体积可控、可完全驻留内存的请求体。</p>
 *
 * @param <T> 消息体内容的原始类型
 * @author linjpxc
 */
public abstract class  AbstractByteArrayBody<T> implements ContentBody<T> {

    /** 延迟初始化的字节缓存，volatile 保证多线程可见性 */
    private volatile byte[] buffer;
    /** 双重检查锁的同步监视器 */
    private final Object bufferLock = new Object();

    @Override
    public InputStream getContentStream() {
        return new ByteArrayInputStream(this.getBuffer());
    }

    @Override
    public int getContentLength() {
        return this.getBuffer().length;
    }

    @Override
    public void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer) {
        final byte[] bytes = this.getBuffer();
        consumer.acceptUnchecked(bytes, 0, bytes.length);
    }

    /**
     * 获取字节缓存，首次访问时通过 {@link #contentAsBytes()} 延迟初始化。
     * 使用双重检查锁保证线程安全且避免不必要的同步开销。
     */
    protected final byte[] getBuffer() {
        if (this.buffer == null) {
            synchronized (this.bufferLock) {
                if (this.buffer == null) {
                    this.buffer = this.contentAsBytes();
                }
            }
        }
        return this.buffer;
    }

    /**
     * 子类实现此方法，定义如何将原始内容转换为字节数组。
     * 此方法仅在首次访问字节缓存时调用一次。
     */
    protected abstract byte[] contentAsBytes();
}
