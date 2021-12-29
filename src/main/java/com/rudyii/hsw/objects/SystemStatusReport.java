package com.rudyii.hsw.objects;

import com.rudyii.hs.common.type.MonitoringModeType;
import com.rudyii.hs.common.type.SystemModeType;
import com.rudyii.hs.common.type.SystemStateType;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
@Builder
public class SystemStatusReport {
    private SystemStateType systemState;
    private SystemModeType systemMode;
    private Map<String, Boolean> runningDetectors;
    private Map<String, MonitoringModeType> monitoringModes;
    @Builder.Default
    private Date timestamp = new Date();
}
