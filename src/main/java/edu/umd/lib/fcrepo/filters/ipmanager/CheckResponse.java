package edu.umd.lib.fcrepo.filters.ipmanager;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CheckResponse {
  private String id;
  private String ip;
  private List<Check> checks;

  @JsonProperty("@id")
  public String getId() {
    return id;
  }

  @JsonProperty("@id")
  public void setId(String id) {
    this.id = id;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public List<Check> getChecks() {
    return checks;
  }

  public void setChecks(List<Check> checks) {
    this.checks = checks;
  }
}
