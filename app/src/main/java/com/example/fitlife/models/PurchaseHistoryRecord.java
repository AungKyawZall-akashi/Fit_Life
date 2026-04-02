package com.example.fitlife.models;

public class PurchaseHistoryRecord {
    private String userId;
    private String packageKey;
    private int planMonths;
    private double price;
    private String paymentMethod;
    private String phoneNumberMasked;
    private long purchasedAt;
    private long expiresAt;
    private long canceledAt;
    private String status;

    public PurchaseHistoryRecord() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPackageKey() { return packageKey; }
    public void setPackageKey(String packageKey) { this.packageKey = packageKey; }

    public int getPlanMonths() { return planMonths; }
    public void setPlanMonths(int planMonths) { this.planMonths = planMonths; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPhoneNumberMasked() { return phoneNumberMasked; }
    public void setPhoneNumberMasked(String phoneNumberMasked) { this.phoneNumberMasked = phoneNumberMasked; }

    public long getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(long purchasedAt) { this.purchasedAt = purchasedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public long getCanceledAt() { return canceledAt; }
    public void setCanceledAt(long canceledAt) { this.canceledAt = canceledAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

