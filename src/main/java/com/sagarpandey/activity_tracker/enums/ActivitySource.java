package com.sagarpandey.activity_tracker.enums;

/**
 * Enum to define different sources of activity creation
 */
public enum ActivitySource {
    API_SINGLE("API_SINGLE", "Single activity created via API"),
    API_BULK("API_BULK", "Activity created via bulk API"),
    IMPORT("IMPORT", "Activity imported from external source"),
    MANUAL("MANUAL", "Activity created manually by admin"),
    MOBILE_APP("MOBILE_APP", "Activity created via mobile application"),
    WEB_APP("WEB_APP", "Activity created via web application"),
    SCHEDULED("SCHEDULED", "Activity created by scheduled process"),
    MIGRATION("MIGRATION", "Activity created during data migration");
    
    private final String code;
    private final String description;
    
    ActivitySource(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return code;
    }
    
    /**
     * Get ActivitySource enum from string code
     * @param code the string code
     * @return ActivitySource enum or null if not found
     */
    public static ActivitySource fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        for (ActivitySource source : ActivitySource.values()) {
            if (source.getCode().equalsIgnoreCase(code.trim())) {
                return source;
            }
        }
        return null;
    }
}
