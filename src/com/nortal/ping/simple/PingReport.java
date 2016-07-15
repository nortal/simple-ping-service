package com.nortal.ping.simple;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    private boolean reportWithAudio;
    private String reportWithAudioSoundError;
    private String reportWithAudioSoundReturn;
    private boolean reportShowExaminedAddress;

    public PingReport() throws FileNotFoundException, IOException {
        File pingPropertiesFile = new File(PingService.PING_FILE);

        if (!pingPropertiesFile.exists()) {
            LOGGER.log(Level.SEVERE, "Properties file " + pingPropertiesFile.getName() + " does not exists");
            System.exit(0);
        }
        properties.load(new FileReader(pingPropertiesFile));

        reportWithAudio = Boolean.valueOf(properties.getProperty("report.with.audio", "false"));
        reportShowExaminedAddress = Boolean.valueOf(properties.getProperty("report.show.examined.address", "true"));
        reportWithAudioSoundError = properties.getProperty("report.with.audio.sound.error", "error.wav");
        reportWithAudioSoundReturn = properties.getProperty("report.with.audio.sound.return", "return.wav");
    }

    public StringBuilder createReport(boolean withAudioScript) throws FileNotFoundException, IOException {
        return this.createReport(this.collect());
    }

    private StringBuilder createReport(List<Report> reportList) throws FileNotFoundException, IOException {

        StringBuilder builder = new StringBuilder();

        builder.append("<!DOCTYPE html>");
        builder.append("<html lang=\"et\">");
        builder.append("<head>");
        builder.append("<title>Raport</title>");
        builder.append("<meta http-equiv=\"refresh\" content=\"10\">");
        builder.append("<meta http-equiv=\"cache-control\" content=\"no-cache\">");
        builder.append("</head>");
        builder.append("<body>");
        builder.append("<table border='1' cellspacing='0' id='table'>");

        builder.append("<thead><tr>");
        builder.append("<th>Module</th>");
        builder.append("<th>Status</th>");
        builder.append("<th>Checked</th>");
        builder.append("<th>Last responded</th>");
        builder.append("</tr></thead>");

        builder.append("<tbody>");

        for (Report report : reportList) {

            String rowColor = "";
            boolean isError = "ERROR".equals(report.getStatus());
            if (isError) {
                rowColor = " bgcolor=\"darkred\"";
            } else {
                rowColor = " bgcolor=\"white\"";
            }

            String color = "";
            if (isError) {
                color = " color=\"white\"";
            } else {
                color = " color=\"black\"";
            }

            builder.append("<tr").append(rowColor).append(">");
            builder.append("<td><font").append(color).append(">").append(report.getModule()).append("</font></td>");
            builder.append("<td><font").append(color).append(">").append(report.getStatus()).append("</font></td>");
            builder.append("<td><font").append(color).append(">").append(report.getChecked()).append("</font></td>");
            builder.append("<td><font").append(color).append(">").append(report.getLastResponded()).append("</font></td>");
            builder.append("</tr>");

            if (reportShowExaminedAddress) {
                builder.append("<tr><td><font color=\"gray\">URL:</font></td>");
                builder.append("<td colspan=\"3\"><font color=\"gray\">").append(report.getUrl()).append("</font></td></tr>");
            }
        }

        builder.append("</tbody>");
        builder.append("</table>");

        if (this.reportWithAudio) {
            builder.append("<script type='text/javascript'>");
            builder.append("var source = window.document.getElementById('source');");
            builder.append("var error = table && table.innerHTML.indexOf('ERROR') != -1;");

            builder.append("if(!sessionStorage.hasError){");
            builder.append("sessionStorage.hasError = 0;");
            builder.append("}");

            builder.append("if(error && sessionStorage.hasError && sessionStorage.hasError !== '1'){");
            builder.append("sessionStorage.hasError = '1';");
            builder.append("var audio = new Audio('").append(this.reportWithAudioSoundError).append("');");
            builder.append("audio.play();");
            builder.append("}");

            builder.append("if(!error && sessionStorage.hasError && sessionStorage.hasError !== '0'){");
            builder.append("sessionStorage.hasError = '0';");
            builder.append("var audio = new Audio('").append(this.reportWithAudioSoundReturn).append("');");
            builder.append("audio.play();");
            builder.append("}");
            builder.append("</script>");
        }

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
        Collections.sort(reportList);
        return reportList;
    }
}
