package com.rudyii.hsw.providers;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.rudyii.hsw.providers.StatsProvider.Action.CLEANUP;
import static com.rudyii.hsw.providers.StatsProvider.Action.INCREASE;
import static java.math.BigInteger.ZERO;

@Component
public class StatsProvider {
    private static Logger LOG = LogManager.getLogger(StatsProvider.class);
    private FirebaseDatabaseProvider firebaseDatabaseProvider;
    private OptionsService optionsService;

    @Autowired
    public StatsProvider(FirebaseDatabaseProvider firebaseDatabaseProvider, OptionsService optionsService) {
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
        this.optionsService = optionsService;
    }

    public void increaseArmedStatistic() {
        firebaseDatabaseProvider.getReference("/usageStats").addListenerForSingleValueEvent(getUsageStatsValueEventListener(INCREASE));
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupObsolete() {
        firebaseDatabaseProvider.getReference("/usageStats").addListenerForSingleValueEvent(getUsageStatsValueEventListener(CLEANUP));
    }

    private ValueEventListener getUsageStatsValueEventListener(Action action) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String today = getToday();
                Long historicalToday = calculateHistoricalToday();
                final ConcurrentHashMap<String, Long> usageStats = new ConcurrentHashMap<>((HashMap<String, Long>) dataSnapshot.getValue());

                if (usageStats.get(today) == null) {
                    LOG.info("Initialized today with ZERO");
                    usageStats.put(today, ZERO.longValue());
                }

                switch (action) {
                    case CLEANUP:
                        LOG.info("Cleaning obsolete usage stats");
                        usageStats.forEach((k, v) -> {
                            if (Long.valueOf(k) < historicalToday) {
                                usageStats.remove(k);
                            }
                        });
                        break;
                    case INCREASE:
                        Long armedTodayCount = usageStats.get(today);
                        usageStats.put(today, armedTodayCount + 1);
                        break;
                }

                firebaseDatabaseProvider.pushData("/usageStats", usageStats);
            }

            @NotNull
            private String getToday() {
                return new SimpleDateFormat("yyyyMMdd").format(new Date());
            }

            private Long calculateHistoricalToday() {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());

                calendar.add(Calendar.DATE, -((Long) optionsService.getOption("keepDays")).intValue());

                return Long.valueOf(new SimpleDateFormat("yyyyMMdd").format(calendar.getTime()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Weekly stats Firebase data!");
            }
        };
    }

    enum Action {INCREASE, CLEANUP}
}
