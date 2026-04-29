package com.technogise.leave_management_system.constants;

import java.util.Map;

public class KimaiConstants {
    public static final int LEAVE_PROJECT_ID = 2;

    public static final int FULL_DAY_HOURS = 8;
    public static final int HALF_DAY_HOURS = 4;

    public static final String KIMAI_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    public static final Map<String, Integer> ACTIVITY_MAPPING = Map.of(
            LeaveConstants.ANNUAL_LEAVE, 3,
            LeaveConstants.PATERNITY_LEAVE, 4,
            LeaveConstants.MATERNITY_LEAVE, 5,
            LeaveConstants.SABBATICAL_LEAVE, 6,
            LeaveConstants.BEREAVEMENT_LEAVE, 7,
            LeaveConstants.REJUVENATION_LEAVE, 8
    );
}
