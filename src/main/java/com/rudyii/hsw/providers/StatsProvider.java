package com.rudyii.hsw.providers;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.services.SystemModeAndStateService;
import com.rudyii.hsw.services.firebase.FirebaseGlobalSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.USAGE_STATS_ROOT;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsProvider {
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private final FirebaseGlobalSettingsService globalSettingsService;
    private final SystemModeAndStateService systemModeAndStateService;
    private final ThreadPoolTaskExecutor hswExecutor;
    private String today;

    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        if (systemModeAndStateService.isArmed() && globalSettingsService.getGlobalSettings().isGatherStats()) {
            today = getToday();
            increaseArmedStatistic();
        }
    }

    public void increaseArmedStatistic() {
        firebaseDatabaseProvider.getRootReference().child(USAGE_STATS_ROOT).child(today).addListenerForSingleValueEvent(getUsageStatsIncreaseValueEventListener());
    }

    private ValueEventListener getUsageStatsIncreaseValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long minutes = 0;
                if (dataSnapshot.exists()) {
                    minutes = (long) dataSnapshot.getValue();
                }
                long finalMinutes = minutes + 1;
                hswExecutor.submit(() -> {
                    firebaseDatabaseProvider.getRootReference().child(USAGE_STATS_ROOT).child(today).setValueAsync(finalMinutes);
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private String getToday() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }
}
