package com.rudyii.hsw.providers;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.rudyii.hsw.providers.StatsProvider.Action.*;
import static java.math.BigInteger.ZERO;

@Component
public class StatsProvider {
    private static Logger LOG = LogManager.getLogger(StatsProvider.class);
    private FirebaseDatabaseProvider firebaseDatabaseProvider;
    private ThreadPoolTaskExecutor hswExecutor;
    private ConcurrentHashMap<String, Long> usageStats = new ConcurrentHashMap<>();

    @Value("${statistics.keep.stats.days}")
    private Long keepDays;

    @Value("${statistics.reset.enabled}")
    private Boolean statsReset;

    @Autowired
    public StatsProvider(FirebaseDatabaseProvider firebaseDatabaseProvider, ThreadPoolTaskExecutor hswExecutor) {
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
        this.hswExecutor = hswExecutor;
    }

    public void increaseArmedStatistic() {
        firebaseDatabaseProvider.getReference("/usageStats").addListenerForSingleValueEvent(getWeeklyStatsValueEventListener(getTodayAsString(), INCREASE));
    }

    @NotNull
    private String getTodayAsString() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupObsolete() {
        LOG.info("Cleanup of obsolete usage stats started, also adding a new day into usage stats");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -keepDays.intValue());

        firebaseDatabaseProvider.getReference("/usageStats").addListenerForSingleValueEvent(getWeeklyStatsValueEventListener(getTodayAsString(), INIT,
                () -> firebaseDatabaseProvider.getReference("/usageStats").addListenerForSingleValueEvent(getWeeklyStatsValueEventListener(getTodayAsString(), CLEANUP))));
    }

    private ValueEventListener getWeeklyStatsValueEventListener(String today, Action action) {
        return getWeeklyStatsValueEventListener(today, action, null);
    }

    private ValueEventListener getWeeklyStatsValueEventListener(String today, Action action, Runnable runnable) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap<String, Long> tempUsageStats = (HashMap<String, Long>) dataSnapshot.getValue();

                if (tempUsageStats != null) {
                    usageStats.clear();
                    usageStats.putAll(tempUsageStats);
                }

                switch (action) {
                    case INIT:
                        putTodayWithZero(today);
                        break;

                    case CLEANUP:
                        doCleanup();
                        break;

                    case INCREASE:
                        putTodayWithZero(today);
                        increaseArmedCount();
                }

                if (runnable == null) {
                    firebaseDatabaseProvider.pushData("/usageStats", usageStats);
                } else {
                    firebaseDatabaseProvider.pushData("/usageStats", usageStats).addListener(runnable, hswExecutor);
                }
            }

            private void doCleanup() {
                usageStats.forEach((k, v) -> {
                    if (Long.valueOf(k) < Long.valueOf(today)) {
                        usageStats.remove(today);
                    }
                });
            }

            private void increaseArmedCount() {
                Long armedTodayCount = usageStats.get(today);
                usageStats.put(today, armedTodayCount + 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Weekly stats Firebase data!");
            }
        };
    }

    private void putTodayWithZero(String today) {
        if (usageStats.get(today) == null) {
            usageStats.put(today, ZERO.longValue());
        }
    }

    enum Action {INCREASE, CLEANUP, INIT}
}
