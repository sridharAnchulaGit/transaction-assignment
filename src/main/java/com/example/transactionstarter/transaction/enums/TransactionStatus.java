package com.example.transactionstarter.transaction.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionStatus {

    PENDING,
    SUCCESS,
    FAILED;

    @JsonCreator
    public static TransactionStatus fromValue(String value) {
        if (value == null) {
            return null;
        }

        return TransactionStatus.valueOf(value.trim().toUpperCase());
    }
}