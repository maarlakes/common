package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 默认的事件发布器工厂，每次调用 {@link #createPublisher} 创建新的 {@link DefaultEventPublisher} 实例。
 *
 * @author linjpxc
 */
public class DefaultEventPublisherFactory implements EventPublisherFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventPublisherFactory.class);

    @Nonnull
    @Override
    public EventPublisher createPublisher(@Nonnull EventDispatcher dispatcher, @Nonnull List<? extends EventInvoker> invokers) {
        if (log.isDebugEnabled()) {
            log.debug("创建事件发布器, 监听器数量: {}", invokers.size());
        }
        return new DefaultEventPublisher(dispatcher, invokers);
    }
}
