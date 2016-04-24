package com.nortal.ping.simple;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nortal.ping.simple.model.Report;

/**
 * @author Margus Hanni <margus.hanni@nortal.com>
 */
public class PingReport {

    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private Properties properties = new Properties();
    private Properties statuses = new Properties();

    public PingReport() throws FileNotFoundException, IOException {
        File pingPropertiesFile = new File(PingService.PING_FILE);

        if (!pingPropertiesFile.exists()) {
            LOGGER.log(Level.SEVERE, "Properties file " + pingPropertiesFile.getName() + " does not exists");
            System.exit(0);
        }
        properties.load(new FileReader(pingPropertiesFile));
    }

    public StringBuilder createReport() throws FileNotFoundException, IOException {
        return this.createReport(this.collect());
    }

    private StringBuilder createReport(List<Report> reportList) throws FileNotFoundException, IOException {

        StringBuilder builder = new StringBuilder();

        builder.append("<!DOCTYPE html>");
        builder.append("<html lang=\"et\">");
        builder.append("<head>");
        builder.append("<title>Raport</title>");
        builder.append("<meta http-equiv=\"refresh\" content=\"10\">");
        builder.append("</head>");
        builder.append("<body>");
        builder.append("<table border='1' cellspacing='0'>");

        builder.append("<thead><tr>");
        builder.append("<th>Module</th>");
        builder.append("<th>Status</th>");
        builder.append("<th>Checked</th>");
        builder.append("<th>Last responded</th>");
        builder.append("</tr></thead>");

        builder.append("<tbody>");

        for (Report report : reportList) {

            String color = "";
            if ("ERROR".equals(report.getStatus())) {
                color = " color=\"red\"";
            } else if ("unknown".equals(report.getStatus())) {
                color = " color=\"gray\"";
            }
            
            builder.append("<tr>");
            builder.append("<td><font").append(color).append(">").append(report.getModule()).append("</font></td>");
            builder.append("<td><font").append(color).append(">").append(report.getStatus()).append("</font></td>");
            builder.append("<td><font").append(color).append(">").append(report.getChecked()).append("</font></td>");
            builder.append("<td><font").append(color).append(">").append(report.getLastResponded()).append("</font></td>");
            builder.append("</tr>");

            builder.append("<tr><td>&nbsp;</td><td><font color=\"gray\">URL:</font></td>");
            builder.append("<td colspan=\"3\"><font color=\"gray\">").append(report.getUrl()).append("</font></td></tr>");
        }

        builder.append("</tbody>");
        builder.append("</table>");
        builder.append("</body>");
        builder.append("</html>");
        return builder;

    }

    public void createAndSaveReport() throws IOException {
        List<Report> reportList = this.collect();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(properties.getProperty("report.location", "report.html")));) {
            bw.write(this.createReport(reportList).toString());
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(properties.getProperty("json.location", "report.json")));) {
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            bw.write(gson.toJson(reportList));
        }
    }

    private List<Report> collect() throws FileNotFoundException, IOException {
        List<Report> reportList = new ArrayList<Report>();

        File statusFile = new File(PingService.STATUS_FILE);
        if (statusFile.exists()) {
            statuses.load(new FileReader(statusFile));
        }

        for (Object key : properties.keySet()) {
            String k = (String) key;

            if (!k.endsWith(".name")) {
                continue;
            }

            String statusKey = k.replace(".name", "");

            Report report = new Report();
            report.setModule(properties.getProperty(k));

            report.setStatus(statuses.getProperty(statusKey + ".url.status", "unknown"));
            report.setChecked(statuses.getProperty(statusKey + ".url.checked", "unknown"));
            report.setLastResponded(statuses.getProperty(statusKey + ".url.lastResponded", "unknown"));
            report.setUrl(properties.getProperty(statusKey + ".url"));

            reportList.add(report);
        }
        return reportList;
    }
}
