package com.example.jarconflict.model;

import java.util.List;

public class ScanResult {
    private List<JarInfo> jars;
    private List<ConflictInfo> conflicts;
    private ScanSummary summary;
    private long scanTimeMs;
    private String scanMode;
    
    public ScanResult() {}

    public List<JarInfo> getJars() {
        return jars;
    }

    public void setJars(List<JarInfo> jars) {
        this.jars = jars;
    }

    public List<ConflictInfo> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<ConflictInfo> conflicts) {
        this.conflicts = conflicts;
    }

    public ScanSummary getSummary() {
        return summary;
    }

    public void setSummary(ScanSummary summary) {
        this.summary = summary;
    }

    public long getScanTimeMs() {
        return scanTimeMs;
    }

    public void setScanTimeMs(long scanTimeMs) {
        this.scanTimeMs = scanTimeMs;
    }

    public String getScanMode() {
        return scanMode;
    }

    public void setScanMode(String scanMode) {
        this.scanMode = scanMode;
    }

    public static class ScanSummary {
        private int totalJars;
        private int totalClasses;
        private int conflictCount;
        private int criticalConflicts;
        private int highConflicts;
        private int mediumConflicts;
        private int lowConflicts;

        public int getTotalJars() {
            return totalJars;
        }

        public void setTotalJars(int totalJars) {
            this.totalJars = totalJars;
        }

        public int getTotalClasses() {
            return totalClasses;
        }

        public void setTotalClasses(int totalClasses) {
            this.totalClasses = totalClasses;
        }

        public int getConflictCount() {
            return conflictCount;
        }

        public void setConflictCount(int conflictCount) {
            this.conflictCount = conflictCount;
        }

        public int getCriticalConflicts() {
            return criticalConflicts;
        }

        public void setCriticalConflicts(int criticalConflicts) {
            this.criticalConflicts = criticalConflicts;
        }

        public int getHighConflicts() {
            return highConflicts;
        }

        public void setHighConflicts(int highConflicts) {
            this.highConflicts = highConflicts;
        }

        public int getMediumConflicts() {
            return mediumConflicts;
        }

        public void setMediumConflicts(int mediumConflicts) {
            this.mediumConflicts = mediumConflicts;
        }

        public int getLowConflicts() {
            return lowConflicts;
        }

        public void setLowConflicts(int lowConflicts) {
            this.lowConflicts = lowConflicts;
        }
    }
}