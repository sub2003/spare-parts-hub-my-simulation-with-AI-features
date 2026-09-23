package com.sliit.sparepartshub.inventory.dto;

import com.sliit.sparepartshub.entity.PickTicketItem;
import com.sliit.sparepartshub.entity.SerialNumber;
import java.util.List;

public class PickTicketLineView {
    private final PickTicketItem item;
    private final boolean serialTracked;
    private final List<SerialNumber> assignedSerials;

    public PickTicketLineView(PickTicketItem item,
                              boolean serialTracked,
                              List<SerialNumber> assignedSerials) {
        this.item = item;
        this.serialTracked = serialTracked;
        this.assignedSerials = assignedSerials;
    }

    public PickTicketItem getItem() {
        return item;
    }

    public boolean isSerialTracked() {
        return serialTracked;
    }

    public List<SerialNumber> getAssignedSerials() {
        return assignedSerials;
    }
}
