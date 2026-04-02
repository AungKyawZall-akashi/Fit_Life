package com.example.fitlife.models;

public class PackagePurchase {
    private String userId;
    private String packageKey;
    private long purchasedAt;
    private long expiresAt;

    public PackagePurchase() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPackageKey() { return packageKey; }
    public void setPackageKey(String packageKey) { this.packageKey = packageKey; }

    public long getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(long purchasedAt) { this.purchasedAt = purchasedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
}

