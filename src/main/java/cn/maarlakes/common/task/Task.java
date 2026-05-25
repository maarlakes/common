package cn.maarlakes.common.task;

import java.lang.annotation.*;

/**
 * 任务执行器标记注解。
 *
 * <p>标注在 {@link TaskExecutor} 实现类上，声明该执行器所处理的任务名称。
 * 工厂在创建 {@link ScheduledTaskService} 时，会根据此注解的 {@link #value()} 过滤匹配的执行器。</p>
 *
 * <h3>匹配规则</h3>
 * <ul>
 *     <li>{@code value()} 为空数组时，该执行器参与所有任务的处理（即不进行过滤）</li>
 *     <li>{@code value()} 非空时，只有当任务名包含在数组中时，该执行器才会被纳入</li>
 * </ul>
 *
 * @author linjpxc
 * @see TaskExecutor
 * @see cn.maarlakes.common.task.AbstractScheduledTaskServiceFactory#getTaskExecutors(String)
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface Task {

    /**
     * 任务名称列表，用于匹配执行器与任务。
     *
     * <p>为空时表示该执行器处理所有任务，不进行名称过滤。</p>
     */
    String[] value() default {};
}
