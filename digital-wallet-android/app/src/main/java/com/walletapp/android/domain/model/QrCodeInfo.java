package com.walletapp.android.domain.model;

import java.math.BigDecimal;

public class QrCodeInfo {
    private final String rawPayload;
    private final String poiMethod;
    private final String acquirerId;
    private final String walletId;
    private final BigDecimal amount;
    private final String description;
    private final boolean isDynamic;
    private final boolean isValid;

    public QrCodeInfo(String rawPayload, String poiMethod, String acquirerId,
                      String walletId, BigDecimal amount, String description,
                      boolean isDynamic, boolean isValid) {
        this.rawPayload = rawPayload;
        this.poiMethod = poiMethod;
        this.acquirerId = acquirerId;
        this.walletId = walletId;
        this.amount = amount;
        this.description = description;
        this.isDynamic = isDynamic;
        this.isValid = isValid;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String getPoiMethod() {
        return poiMethod;
    }

    public String getAcquirerId() {
        return acquirerId;
    }

    public String getWalletId() {
        return walletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public boolean isValid() {
        return isValid;
    }
}
