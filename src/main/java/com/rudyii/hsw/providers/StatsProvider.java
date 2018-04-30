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
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

import static java.math.BigInteger.ZERO;

@Component
public class StatsProvider {
    private static Logger LOG = LogManager.getLogger(StatsProvider.class);

    private FirebaseDatabaseProvider firebaseDatabaseProvider;
    private Map<String, Long> weeklyStats;

    @Value("${statistics.keep.stats.days}")
    private Long keepDays;

    @Value("${statistics.reset.enabled}")
    private Boolean statsReset;

    @Autowired
    public StatsProvider(FirebaseDatabaseProvider firebaseDatabaseProvider) {
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
    }

    public void increaseArmedStatistic() {
        String today = getTodayAsString();

        firebaseDatabaseProvider.getReference("/weeklyStats").addListenerForSingleValueEvent(getWeeklyStatsValueEventListener(today, true));
    }

    @NotNull
    private String getTodayAsString() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    public void reset() {
        if (statsReset) {
            firebaseDatabaseProvider.pushData("/weeklyStats", null);
        }
    }

    public ArrayList<String> generateReportBody() throws Exception {
        ArrayList<String> report = new ArrayList<>();
        report.add("Will be moved to Client application");

        reset();

        return report;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupObsolete() {
        initStatsMapIfNullForToday();

        LOG.info("Cleanup of obsolete statistics started");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -keepDays.intValue());

        String today = new SimpleDateFormat("yyyyMMdd").format(calendar.getTime());

        firebaseDatabaseProvider.getReference("/weeklyStats").addListenerForSingleValueEvent(getWeeklyStatsValueEventListener(today, false));
    }

    private ValueEventListener getWeeklyStatsValueEventListener(String today, boolean doIncrease) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                weeklyStats = (Map<String, Long>) dataSnapshot.getValue();
                initStatsMapIfNullForToday();

                if (doIncrease) {
                    increaseArmedCount();
                } else {
                    doCleanup();
                }

                firebaseDatabaseProvider.pushData("/weeklyStats", weeklyStats);
            }

            private void doCleanup() {
                weeklyStats.forEach((k, v) -> {
                    if (Long.valueOf(k) < Long.valueOf(today)) {
                        weeklyStats.remove(today);
                    }
                });
            }

            private void increaseArmedCount() {
                Long armedTodayCount = weeklyStats.get(today);
                weeklyStats.put(today, armedTodayCount + 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Weekly stats Firebase data!");
            }
        };
    }

    private void initStatsMapIfNullForToday() {
        String today = getTodayAsString();

        if (weeklyStats == null) {
            weeklyStats = new HashMap<>();
        } else if (weeklyStats.get(today) == null) {
            weeklyStats.put(today, ZERO.longValue());
        }
    }
}
