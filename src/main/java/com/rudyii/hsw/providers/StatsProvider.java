package com.rudyii.hsw.providers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;

@Component
public class StatsProvider {
    private static Logger LOG = LogManager.getLogger(StatsProvider.class);

    private Connection connection;

    @Value("${statistics.keep.stats.days}")
    private Long keepDays;

    @Value("${statistics.reset.enabled}")
    private Boolean statsReset;

    @Autowired
    public StatsProvider(Connection connection) {
        this.connection = connection;
    }

    public void increaseArmedStatistic() throws Exception {
        Integer date = Integer.valueOf(new SimpleDateFormat("yyyyMMdd").format(new Date()));
        Integer currentMinutes = 0;

        PreparedStatement selectStatement = connection.prepareStatement("SELECT MINUTES_COUNT FROM ARMED_STATE_STATS WHERE STATS_DATE = ?");
        selectStatement.setInt(1, date);
        ResultSet rs = selectStatement.executeQuery();
        if (rs.next()) {
            currentMinutes = rs.getInt(1);
        }

        PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO ARMED_STATE_STATS (STATS_DATE, MINUTES_COUNT) VALUES (?,?)");
        statement.setInt(1, date);
        statement.setInt(2, currentMinutes + 1);
        statement.execute();
    }

    public void reset() {
        if (statsReset) {
            try {
                connection.createStatement().executeQuery("DELETE FROM ARMED_STATE_STATS");
            } catch (SQLException e) {
                LOG.warn("Nothing removed from Armed state stats while reset requested");
            }
            try {
                connection.createStatement().executeQuery("VACUUM");
            } catch (SQLException e) {
                LOG.warn("Vacuum failed");
            }
        }
    }

    public ArrayList<String> generateReportBody() throws Exception {
        ArrayList<String> report = new ArrayList<>();

        Statement weeklyStats = connection.createStatement();
        ResultSet rsDay = weeklyStats.executeQuery("SELECT STATS_DATE, MINUTES_COUNT FROM ARMED_STATE_STATS ORDER BY STATS_DATE");

        report.add("<ul>");
        while (rsDay.next()) {
            report.add("<li><b>" + rsDay.getString(1) + "</b> - " + rsDay.getInt(2) / 60 + " hours " + rsDay.getInt(2) % 60 + " minutes;");
        }
        report.add("</ul>");

        Statement totalStats = connection.createStatement();
        ResultSet rsTotal = totalStats.executeQuery("SELECT SUM(MINUTES_COUNT) FROM ARMED_STATE_STATS");

        report.add("Total in <b>" + ARMED + "</b> state: "
                + TimeUnit.MINUTES.toDays(rsTotal.getInt(1)) + " days "
                + rsTotal.getInt(1) / 60 % 24 + " hours "
                + rsTotal.getInt(1) % 60 + " minutes");

        reset();

        return report;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupObsolete() {
        LOG.info("Cleanup of obsolete statistics started");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -keepDays.intValue());

        String date = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
        try {
            connection.createStatement().executeQuery("DELETE FROM ARMED_STATE_STATS where MODIFIED_DATE < date('" + date + "')");
        } catch (SQLException e) {
            LOG.warn("Nothing removed from Armed state stats");
        }
        try {
            connection.createStatement().executeQuery("DELETE FROM ARMED_STATE_HIST where MODIFIED_DATE < date('" + date + "')");
        } catch (SQLException e) {
            LOG.warn("Nothing removed from Armed state history");
        }
        LOG.info("Cleanup of obsolete statistics completed");
    }
}
