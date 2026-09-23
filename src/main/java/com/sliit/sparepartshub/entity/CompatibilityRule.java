package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

/**
 * Drives the "PC Builder Lite" compatibility warning logic used at checkout
 * (e.g. flagging a DDR5 RAM stick against a DDR4 motherboard).
 */
@Entity
@Table(name = "compatibility_rule")
public class CompatibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer ruleId;

    @Column(name = "spec_type", nullable = false, length = 30)
    private String specType;

    @Column(name = "value_a", nullable = false, length = 30)
    private String valueA;

    @Column(name = "value_b", nullable = false, length = 30)
    private String valueB;

    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    public CompatibilityRule() {
    }

    public Integer getRuleId() {
        return ruleId;
    }

    public void setRuleId(Integer ruleId) {
        this.ruleId = ruleId;
    }

    public String getSpecType() {
        return specType;
    }

    public void setSpecType(String specType) {
        this.specType = specType;
    }

    public String getValueA() {
        return valueA;
    }

    public void setValueA(String valueA) {
        this.valueA = valueA;
    }

    public String getValueB() {
        return valueB;
    }

    public void setValueB(String valueB) {
        this.valueB = valueB;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
