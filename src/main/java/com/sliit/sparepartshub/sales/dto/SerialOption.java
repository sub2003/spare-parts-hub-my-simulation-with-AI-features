package com.sliit.sparepartshub.sales.dto;

/**
 * Lightweight serial-number option exposed to the POS UI.
 * Keeps the JPA entity out of the session/UI model.
 */
public class SerialOption {
    private final Integer serialId;
    private final String serialValue;

    public SerialOption(Integer serialId, String serialValue) {
        this.serialId = serialId;
        this.serialValue = serialValue;
    }

    public Integer getSerialId() {
        return serialId;
    }

    public String getSerialValue() {
        return serialValue;
    }
}
