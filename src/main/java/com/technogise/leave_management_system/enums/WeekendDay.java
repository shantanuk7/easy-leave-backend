package com.technogise.leave_management_system.enums;

import lombok.Getter;

import java.time.DayOfWeek;

@Getter
public enum WeekendDay {
    SATURDAY(DayOfWeek.SATURDAY),
    SUNDAY(DayOfWeek.SUNDAY);

    private final DayOfWeek dayOfWeek;

    WeekendDay(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
}
