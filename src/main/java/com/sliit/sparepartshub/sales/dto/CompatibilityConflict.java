package com.sliit.sparepartshub.sales.dto;
public class CompatibilityConflict { private String productA; private String productB; private String reason; public CompatibilityConflict(String a,String b,String r){productA=a;productB=b;reason=r;} public String getProductA(){return productA;} public String getProductB(){return productB;} public String getReason(){return reason;} }
