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

    private RestrictionBatchEntry[] restrictions;

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

    public RestrictionBatchEntry[] getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(RestrictionBatchEntry[] restrictions) {
        this.restrictions = restrictions;
    }

    public static RestrictionBatchEntry convertToBatchEntry(RestrictionEntry entry) {
        final RestrictionBatchEntry batchEntry = new RestrictionBatchEntry();
        batchEntry.setType(entry.getType());
        batchEntry.setKey(entry.getKey());
        batchEntry.setTitle(entry.getTitle());
        batchEntry.setDescription(entry.getDescription());
        batchEntry.setChoiceEntries(entry.getChoiceEntries());
        batchEntry.setChoiceValues(entry.getChoiceValues());
        batchEntry.setCurrentValue(entry.getSelectedString());
        batchEntry.setCurrentValues(entry.getAllSelectedStrings());

        return batchEntry;
    }

    public static RestrictionEntry convertToEntry(RestrictionBatchEntry batchEntry) {
        final RestrictionEntry entry = new RestrictionEntry(batchEntry.getType(), batchEntry.getKey());
        entry.setTitle(batchEntry.getTitle());
        entry.setDescription(batchEntry.getDescription());
        entry.setChoiceEntries(batchEntry.getChoiceEntries());
        entry.setChoiceValues(batchEntry.getChoiceValues());
        entry.setSelectedString(batchEntry.getCurrentValue());
        entry.setAllSelectedStrings(batchEntry.getCurrentValues());

        return entry;
    }
}
