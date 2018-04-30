package com.rudyii.hsw.services;

import com.rudyii.hsw.providers.StatsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    private static Logger LOG = LogManager.getLogger(StatisticsService.class);

    @Value("${statistics.collect.enabled}")
    private Boolean collectStats;

    private StatsProvider statsProvider;
    private ArmedStateService armedStateService;

    @Autowired
    public StatisticsService(StatsProvider statsProvider, ArmedStateService armedStateService) {
        this.statsProvider = statsProvider;
        this.armedStateService = armedStateService;
    }

    @Async
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        if (armedStateService.isArmed() && collectStats) {
            try {
                statsProvider.increaseArmedStatistic();
            } catch (Exception e) {
                LOG.error("StatsProvider update FAILED!", e);
            }
        }
    }
}
