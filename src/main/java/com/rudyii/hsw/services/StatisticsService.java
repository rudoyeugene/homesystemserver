package com.rudyii.hsw.services;

import com.rudyii.hsw.configuration.Options;
import com.rudyii.hsw.providers.StatsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    private static Logger LOG = LogManager.getLogger(StatisticsService.class);

    private StatsProvider statsProvider;
    private ArmedStateService armedStateService;
    private Options options;

    @Autowired
    public StatisticsService(StatsProvider statsProvider, ArmedStateService armedStateService, Options options) {
        this.statsProvider = statsProvider;
        this.armedStateService = armedStateService;
        this.options = options;
    }

    @Async
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        if (armedStateService.isArmed() && (boolean) options.getOption("collectStatistics")) {
            try {
                statsProvider.increaseArmedStatistic();
            } catch (Exception e) {
                LOG.error("StatsProvider update FAILED!", e);
            }
        }
    }
}
