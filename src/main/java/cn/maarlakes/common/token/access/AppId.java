package cn.maarlakes.common.token.access;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

public interface AppId extends Serializable {

    @Nonnull
    String getAppId();
}
