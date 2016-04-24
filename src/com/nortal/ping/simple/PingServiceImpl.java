package com.nortal.ping.simple;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author Margus Hanni <margus.hanni@nortal.com>
 */
public class PingServiceImpl implements PingService {

    private static final int BUFFER_SIZE = 4096;

    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final String STATUS_OK = "OK";
    private static final String STATUS_ERROR = "ERROR";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final Properties properties = new Properties();
    private final Properties statuses = new Properties();
    private final int requestConnectionTimeout;
    private final int requestReadTimeout;
    private final PingReport pingReport;
    private final boolean sendCompactReport;
    private boolean pingStatusChanged;
    private final TreeSet<String> statusChangedModules = new TreeSet<String>();
    private final PingMail pingMail;

    public PingServiceImpl() throws IOException, KeyManagementException, NoSuchAlgorithmException {

        LOGGER.log(Level.INFO, "###############################");

        File pingPropertiesFile = new File(PING_FILE);

        if (!pingPropertiesFile.exists()) {
            LOGGER.log(Level.SEVERE, "Properties file " + pingPropertiesFile.getName() + " does not exists");
            System.exit(0);
        }
        properties.load(new FileReader(pingPropertiesFile));

        File statusFile = new File(STATUS_FILE);
        if (statusFile.exists()) {
            statuses.load(new FileReader(statusFile));
        }

        this.allowAllHosts();

        requestConnectionTimeout = Integer.valueOf(properties.getProperty("request.connect-timeout"));
        LOGGER.log(Level.FINE, "Request connection timeout: " + requestConnectionTimeout);
        requestReadTimeout = Integer.valueOf(properties.getProperty("request.read-timeout"));
        LOGGER.log(Level.FINE, "Request read timeout: " + requestReadTimeout);

        pingReport = new PingReport();

        sendCompactReport = Boolean.valueOf(properties.getProperty("mail.send.compact.report", "true"));

        pingMail =
                new PingMail(properties.getProperty("mail.server.host"),
                             Integer.valueOf(properties.getProperty("mail.server.port")),
                             properties.getProperty("mail.from.address"),
                             properties.getProperty("mail.to.address").split(","),
                             properties.getProperty("mail.subject.prefix"));
    }

    @Override
    public void ping() {
        for (Object key : properties.keySet()) {
            String k = (String) key;
            String address = properties.getProperty(k);
            try {

                if (k.endsWith(".name")) {
                    continue;
                }

                if (k.startsWith("ping.")) {
                    this.pingUrl(k, address, true);
                }

                if (k.startsWith("check.")) {
                    this.pingUrl(k, address, false);
                }
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Cannot check address: " + address, ex);
            }
        }

        try {
            this.saveStatuses();
            pingReport.createAndSaveReport();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Cannot save status", ex);
        }

        if (pingStatusChanged && sendCompactReport) {
            try {
                String modules = statusChangedModules.toString().replaceAll("\\[|\\]", "");
                pingMail.send("Status changed: " + modules, pingReport.createReport().toString());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error on sending email.", ex);
            }
        }

        // clear old status
        pingStatusChanged = false;
        statusChangedModules.clear();
    }

    private void saveStatuses() throws FileNotFoundException, IOException {
        try (OutputStream output = new FileOutputStream(STATUS_FILE)) {
            statuses.store(output, null);
        }
    }

    private void pingUrl(String key, String address, boolean ping) {

        String keyName = key.substring(0, key.lastIndexOf(".")) + ".name";
        String name = properties.getProperty(keyName, address);

        String cheked = new Date().toString();
        String code = key;
        statuses.setProperty(code + ".checked", cheked);

        String statusKey = code + ".status";

        String lastRespondedKey = code + ".lastResponded";

        String oldStatus = statuses.getProperty(statusKey);

        boolean isOk = false;

        StringWriter messageWiter = new StringWriter();

        LOGGER.log(Level.INFO, "Check aadress: " + address);
        messageWiter.append("Address: ").append(address).append(LINE_SEPARATOR);

        try {
            URL url = new URL(address);

            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(requestConnectionTimeout);
            urlConnection.setReadTimeout(requestReadTimeout);

            int responseCode;

            if (address.startsWith("https")) {
                responseCode = ((HttpsURLConnection) urlConnection).getResponseCode();
            } else {
                responseCode = ((HttpURLConnection) urlConnection).getResponseCode();
            }

            String responseCodeMsg = "Response code: " + responseCode;
            messageWiter.append(responseCodeMsg).append(LINE_SEPARATOR);

            LOGGER.log(Level.INFO, responseCodeMsg + " : " + address);

            if (responseCode == 200) {
                isOk = this.readResponse(urlConnection, ping);
            } else if (responseCode == 401) {
                // teenus on siiski üleval
                isOk = true;
            }
        } catch (Exception ex) {
            messageWiter.append("Request connection timeout: ").append(String.valueOf(requestConnectionTimeout)).append(LINE_SEPARATOR);
            messageWiter.append("Request read timeout: ").append(String.valueOf(requestReadTimeout)).append(LINE_SEPARATOR);
            ex.printStackTrace(new PrintWriter(messageWiter));
            LOGGER.log(Level.SEVERE, "Ping failed: " + address, ex);
        }

        LOGGER.log(Level.INFO, isOk + " : " + address);

        if (isOk) {
            statuses.setProperty(statusKey, STATUS_OK);
            statuses.setProperty(lastRespondedKey, cheked);
            if (oldStatus != null && oldStatus.equals(STATUS_ERROR)) {
                statusChangedModules.add(name + " (" + STATUS_OK + ")");
                pingStatusChanged = true;
                if (!sendCompactReport) {
                    pingMail.send(STATUS_OK + " : " + name, messageWiter.toString());
                }
            }
        } else {
            statuses.setProperty(statusKey, STATUS_ERROR);
            if (oldStatus == null || oldStatus.equals(STATUS_OK)) {
                pingStatusChanged = true;
                statusChangedModules.add(name + " (" + STATUS_ERROR + ")");
                if (!sendCompactReport) {
                    pingMail.send(STATUS_ERROR + " : " + name, messageWiter.toString());
                }
            }
        }
    }

    private boolean readResponse(URLConnection urlConnection, boolean ping) throws IOException {

        final byte[] buffer = new byte[BUFFER_SIZE];
        final StringBuilder out = new StringBuilder();

        boolean isOk = false;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (final InputStream in = urlConnection.getInputStream()) {
            int length;
            while ((length = in.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
        }

        out.append(baos.toString("UTF-8"));
        if (ping) {
            String output = out.toString();
            if (output.contains("moodul") && output.contains("aeg")) {
                isOk = true;
            }
        } else {
            // ei kontrolli sisu
            isOk = true;
        }
        return isOk;
    }

    private void allowAllHosts() throws NoSuchAlgorithmException, KeyManagementException {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        } };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }
}
