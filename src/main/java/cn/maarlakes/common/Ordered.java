package cn.maarlakes.common;

/**
 * @author linjpxc
 */
public interface Ordered {

    int HIGHEST = Integer.MIN_VALUE;

    int LOWEST = Integer.MAX_VALUE;

    int order();
}
