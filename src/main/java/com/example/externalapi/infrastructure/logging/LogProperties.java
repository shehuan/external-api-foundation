package com.example.externalapi.infrastructure.logging;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.log")
public class LogProperties {

    private boolean beginEnabled = true;
    private boolean requestBodyEnabled = true;
    private boolean responseBodyEnabled = true;
    private boolean plainCryptoBodyEnabled = false;
    private int maxBodyLength = 3000;
    private List<String> excludePaths = new ArrayList<>();

    public boolean isBeginEnabled() {
        return beginEnabled;
    }

    public void setBeginEnabled(boolean beginEnabled) {
        this.beginEnabled = beginEnabled;
    }

    public boolean isRequestBodyEnabled() {
        return requestBodyEnabled;
    }

    public void setRequestBodyEnabled(boolean requestBodyEnabled) {
        this.requestBodyEnabled = requestBodyEnabled;
    }

    public boolean isResponseBodyEnabled() {
        return responseBodyEnabled;
    }

    public void setResponseBodyEnabled(boolean responseBodyEnabled) {
        this.responseBodyEnabled = responseBodyEnabled;
    }

    public boolean isPlainCryptoBodyEnabled() {
        return plainCryptoBodyEnabled;
    }

    public void setPlainCryptoBodyEnabled(boolean plainCryptoBodyEnabled) {
        this.plainCryptoBodyEnabled = plainCryptoBodyEnabled;
    }

    public int getMaxBodyLength() {
        return maxBodyLength;
    }

    public void setMaxBodyLength(int maxBodyLength) {
        this.maxBodyLength = maxBodyLength;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
}
