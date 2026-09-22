package edu.umd.lib.fcrepo.filters.ipmanager;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Check {
  private String id;
  private String ip;
  private Group group;
  private Boolean contained;

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

  public Group getGroup() {
    return group;
  }

  public void setGroup(Group group) {
    this.group = group;
  }

  public Boolean isContained() {
    return contained;
  }

  public void setContained(Boolean contained) {
    this.contained = contained;
  }
}
