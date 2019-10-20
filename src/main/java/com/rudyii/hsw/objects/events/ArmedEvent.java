package com.rudyii.hsw.objects.events;

import com.rudyii.hsw.enums.ArmedModeEnum;
import com.rudyii.hsw.enums.ArmedStateEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ArmedEvent extends EventBase {
    private ArmedModeEnum armedMode;
    private ArmedStateEnum armedState;
}
