package com.sliit.sparepartshub.inventory.dto;
public class PickLineForm {
    private Integer itemId;
    private Integer pickedQuantity;
    public Integer getItemId(){return itemId;}
    public void setItemId(Integer itemId){this.itemId=itemId;}
    public Integer getPickedQuantity(){return pickedQuantity;}
    public void setPickedQuantity(Integer pickedQuantity){this.pickedQuantity=pickedQuantity;}
}
