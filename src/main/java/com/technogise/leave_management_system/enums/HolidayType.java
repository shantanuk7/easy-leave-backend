package com.technogise.leave_management_system.enums;

public enum HolidayType {
    FIXED,
    OPTIONAL;

    public String getDisplayName() {
        String name = this.name().charAt(0) + this.name().substring(1).toLowerCase();
        return name + " Holiday";
    }
}
