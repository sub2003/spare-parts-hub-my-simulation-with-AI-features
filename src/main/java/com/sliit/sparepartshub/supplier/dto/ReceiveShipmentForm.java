package com.sliit.sparepartshub.supplier.dto;

public class ReceiveShipmentForm {
    private Integer acceptedQuantity;
    private String serialNumbers;
    private String discrepancyNote;

    public Integer getAcceptedQuantity() { return acceptedQuantity; }
    public void setAcceptedQuantity(Integer acceptedQuantity) { this.acceptedQuantity = acceptedQuantity; }
    public String getSerialNumbers() { return serialNumbers; }
    public void setSerialNumbers(String serialNumbers) { this.serialNumbers = serialNumbers; }
    public String getDiscrepancyNote() { return discrepancyNote; }
    public void setDiscrepancyNote(String discrepancyNote) { this.discrepancyNote = discrepancyNote; }
}
