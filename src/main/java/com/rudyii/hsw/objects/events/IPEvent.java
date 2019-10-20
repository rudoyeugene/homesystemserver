package com.rudyii.hsw.objects.events;

import com.rudyii.hsw.enums.IPStateEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IPEvent extends EventBase {
    private final String ip;
    private final IPStateEnum state;
}
