package com.example.externalapi.infrastructure.repeat;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 防重复提交配置�? *
 * <p>配置前缀：app.repeat-submit�?/p>
 */
@ConfigurationProperties(prefix = "app.repeat-submit")
public class RepeatSubmitProperties {

    /**
     * 防重复提交总开关�?     */
    private boolean enabled = true;

    /**
     * 默认防重复窗口，单位秒。注解未指定 seconds 时使用该值�?     */
    private long defaultSeconds = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDefaultSeconds() {
        return defaultSeconds;
    }

    public void setDefaultSeconds(long defaultSeconds) {
        this.defaultSeconds = defaultSeconds;
    }
}
