package com.afwsamples.testdpc.models;

import android.content.RestrictionEntry;

import java.io.Serializable;
import java.util.List;

public class RestrictionBatchEntry implements Serializable {

    private int type;

    private String key;

    private String title;

    private String description;

    private String[] choiceEntries;

    private String[] choiceValues;

    private String currentValue;

    private String[] currentValues;

    private RestrictionEntry[] restrictions;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getChoiceEntries() {
        return choiceEntries;
    }

    public void setChoiceEntries(String[] choiceEntries) {
        this.choiceEntries = choiceEntries;
    }

    public String[] getChoiceValues() {
        return choiceValues;
    }

    public void setChoiceValues(String[] choiceValues) {
        this.choiceValues = choiceValues;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public String[] getCurrentValues() {
        return currentValues;
    }

    public void setCurrentValues(String[] currentValues) {
        this.currentValues = currentValues;
    }

    public RestrictionEntry[] getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(RestrictionEntry[] restrictions) {
        this.restrictions = restrictions;
    }
}
