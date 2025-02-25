package com.example.exambuddy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty;



public class PayOSWebhookRequest {

    @JsonProperty("code")
    private String code;

    @JsonProperty("desc")
    private String desc;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("data")
    private PayOSWebhookData data; // Object chứa thông tin chi tiết

    @JsonProperty("signature")
    private String signature;

    // Getters & Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public PayOSWebhookData getData() { return data; }
    public void setData(PayOSWebhookData data) { this.data = data; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    @Override
    public String toString() {
        return "data{" +
                "code='" + code + '\'' +
                ", desc='" + desc + '\'' +
                ", success=" + success +
                ", data=" + data +
                ", signature='" + signature + '\'' +
                '}';
    }
}

