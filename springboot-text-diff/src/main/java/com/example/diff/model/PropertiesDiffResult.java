package com.example.diff.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class PropertiesDiffResult {
    private Set<String> removedKeys = new HashSet<>();
    private Set<String> addedKeys = new HashSet<>();
    private Set<String> modifiedKeys = new HashSet<>();
    private Map<String, KeyValueChange> modifiedKeyChanges = new HashMap<>();

    public boolean hasChanges() {
        return !removedKeys.isEmpty() || !addedKeys.isEmpty() || !modifiedKeys.isEmpty();
    }

    public void addModifiedKey(String key, String oldValue, String newValue) {
        KeyValueChange change = new KeyValueChange();
        change.setKey(key);
        change.setOldValue(oldValue);
        change.setNewValue(newValue);
        modifiedKeyChanges.put(key, change);
    }

    @Data
    public static class KeyValueChange {
        private String key;
        private String oldValue;
        private String newValue;
    }
}
