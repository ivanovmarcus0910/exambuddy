package com.example.exambuddy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PayOSWebhookData {
    @JsonProperty("orderCode")
    private Long orderCode;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("description")
    private String description;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("transactionDateTime")
    private String transactionDateTime;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("paymentLinkId")
    private String paymentLinkId;

    @JsonProperty("code")
    private String code;

    @JsonProperty("desc")
    private String desc;

    @JsonProperty("counterAccountBankId")
    private String counterAccountBankId;

    @JsonProperty("counterAccountBankName")
    private String counterAccountBankName;

    @JsonProperty("counterAccountName")
    private String counterAccountName;

    @JsonProperty("counterAccountNumber")
    private String counterAccountNumber;

    @JsonProperty("virtualAccountName")
    private String virtualAccountName;

    @JsonProperty("virtualAccountNumber")
    private String virtualAccountNumber;

    // Getters & Setters
    public Long getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(Long orderCode) {
        this.orderCode = orderCode;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(String transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentLinkId() {
        return paymentLinkId;
    }

    public void setPaymentLinkId(String paymentLinkId) {
        this.paymentLinkId = paymentLinkId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getCounterAccountBankId() {
        return counterAccountBankId;
    }

    public void setCounterAccountBankId(String counterAccountBankId) {
        this.counterAccountBankId = counterAccountBankId;
    }

    public String getCounterAccountBankName() {
        return counterAccountBankName;
    }

    public void setCounterAccountBankName(String counterAccountBankName) {
        this.counterAccountBankName = counterAccountBankName;
    }

    public String getCounterAccountName() {
        return counterAccountName;
    }

    public void setCounterAccountName(String counterAccountName) {
        this.counterAccountName = counterAccountName;
    }

    public String getCounterAccountNumber() {
        return counterAccountNumber;
    }

    public void setCounterAccountNumber(String counterAccountNumber) {
        this.counterAccountNumber = counterAccountNumber;
    }

    public String getVirtualAccountName() {
        return virtualAccountName;
    }

    public void setVirtualAccountName(String virtualAccountName) {
        this.virtualAccountName = virtualAccountName;
    }

    public String getVirtualAccountNumber() {
        return virtualAccountNumber;
    }

    public void setVirtualAccountNumber(String virtualAccountNumber) {
        this.virtualAccountNumber = virtualAccountNumber;
    }

    @Override
    public String toString() {
        return "{" +
                "'orderCode':" + orderCode + "," +
                "'amount':" + amount + "," +
                "'description':'" + description + "'," +
                "'accountNumber':'" + accountNumber + "'," +
                "'reference':'" + reference + "'," +
                "'transactionDateTime':'" + transactionDateTime + "'," +
                "'currency':'" + currency + "'," +
                "'paymentLinkId':'" + paymentLinkId + "'," +
                "'code':'" + code + "'," +
                "'desc':'" + desc + "'," +
                "'counterAccountBankId':'" + counterAccountBankId + "'," +
                "'counterAccountBankName':'" + counterAccountBankName + "'," +
                "'counterAccountName':'" + counterAccountName + "'," +
                "'counterAccountNumber':'" + counterAccountNumber + "'," +
                "'virtualAccountName':'" + virtualAccountName + "'," +
                "'virtualAccountNumber':'" + virtualAccountNumber + "'" +
                "}";
    }

}