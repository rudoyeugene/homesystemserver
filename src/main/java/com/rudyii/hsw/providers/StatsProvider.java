package com.rudyii.hsw.providers;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.services.ArmedStateService;
import com.rudyii.hsw.services.firebase.FirebaseGlobalSettingsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.USAGE_STATS_ROOT;

@Slf4j
@Service
@AllArgsConstructor
public class StatsProvider {
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private final FirebaseGlobalSettingsService globalSettingsService;
    private final ArmedStateService armedStateService;

    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        if (armedStateService.isArmed() && globalSettingsService.getGlobalSettings().isGatherStats()) {
            increaseArmedStatistic();
        }
    }

    public void increaseArmedStatistic() {
        firebaseDatabaseProvider.getRootReference().child(USAGE_STATS_ROOT).child(getToday()).addListenerForSingleValueEvent(getUsageStatsIncreaseValueEventListener(getToday()));
    }

    private ValueEventListener getUsageStatsIncreaseValueEventListener(String today) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int minutes = 0;
                if (dataSnapshot.exists()) {
                    minutes = (int) dataSnapshot.getValue();
                }
                minutes++;
                firebaseDatabaseProvider.getRootReference().child(USAGE_STATS_ROOT).child(today).setValueAsync(minutes);
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
