package com.nortal.ping.simple.model;

import java.io.Serializable;

/**
 * @author Margus Hanni <margus.hanni@nortal.com>
 */
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    private String module;
    private String status;
    private String url;
    private String checked;
    private String lastResponded;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getChecked() {
        return checked;
    }

    public void setChecked(String checked) {
        this.checked = checked;
    }

    public String getLastResponded() {
        return lastResponded;
    }

    public void setLastResponded(String lastResponded) {
        this.lastResponded = lastResponded;
    }

}
