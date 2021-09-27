package com.rudyii.hsw.objects.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IspEvent extends EventBase {
    private String externalIp;
    private String ispName;
}
