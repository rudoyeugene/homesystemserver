package com.rudyii.hsw.services;

import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.providers.StatsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static com.rudyii.hsw.configuration.OptionsService.COLLECT_STATISTICS;

@Slf4j
@Service
public class StatisticsService {
    private StatsProvider statsProvider;
    private ArmedStateService armedStateService;
    private OptionsService optionsService;

    @Autowired
    public StatisticsService(StatsProvider statsProvider, ArmedStateService armedStateService, OptionsService optionsService) {
        this.statsProvider = statsProvider;
        this.armedStateService = armedStateService;
        this.optionsService = optionsService;
    }

    @Async
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        if (armedStateService.isArmed() && (boolean) optionsService.getOption(COLLECT_STATISTICS)) {
            try {
                statsProvider.increaseArmedStatistic();
            } catch (Exception e) {
                log.error("StatsProvider update FAILED!", e);
            }
        }
    }
}
