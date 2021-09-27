package com.rudyii.hsw.objects.events;

import com.rudyii.hs.common.objects.settings.GlobalSettings;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SettingsUpdatedEvent extends EventBase {
    private GlobalSettings globalSettings;
}
