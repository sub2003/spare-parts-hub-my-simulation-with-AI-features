package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "storage_location")
public class StorageLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Integer locationId;

    @Column(name = "location_code", length = 20)
    private String locationCode;

    @Column(name = "aisle", nullable = false, length = 30)
    private String aisle;

    @Column(name = "shelf", nullable = false, length = 30)
    private String shelf;

    @Column(name = "bin", nullable = false, length = 30)
    private String bin;

    public StorageLocation() {
    }

    public Integer getLocationId() {
        return locationId;
    }

    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getAisle() {
        return aisle;
    }

    public void setAisle(String aisle) {
        this.aisle = aisle;
    }

    public String getShelf() {
        return shelf;
    }

    public void setShelf(String shelf) {
        this.shelf = shelf;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }
}
