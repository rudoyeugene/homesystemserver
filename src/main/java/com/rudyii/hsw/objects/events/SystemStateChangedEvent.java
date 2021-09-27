package com.rudyii.hsw.objects.events;

import com.rudyii.hs.common.type.SystemModeType;
import com.rudyii.hs.common.type.SystemStateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SystemStateChangedEvent extends EventBase {
    private SystemModeType systemMode;
    private SystemStateType systemState;
    private String by;
}
