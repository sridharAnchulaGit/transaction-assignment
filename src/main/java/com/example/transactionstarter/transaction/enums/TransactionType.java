package com.example.transactionstarter.transaction.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionType {

    CREDIT,
    DEBIT;

    @JsonCreator
    public static TransactionType fromValue(String value) {

        if (value == null) {
            return null;
        }

        try {
            return TransactionType.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid transaction type: " + value
            );
        }
    }
}