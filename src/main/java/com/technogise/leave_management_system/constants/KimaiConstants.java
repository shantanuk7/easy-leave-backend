package com.technogise.leave_management_system.constants;

import java.util.Map;

public class KimaiConstants {
    public static final int LEAVE_PROJECT_ID = 2;

    public static final Map<String, Integer> ACTIVITY_MAPPING = Map.of(
            "Annual Leave", 3,
            "Paternity Leave", 4,
            "Maternity Leave", 5,
            "Sabbatical Leave", 6,
            "Bereavement Leave", 7,
            "5 Year Rejuvenation Leave", 8
    );
}
