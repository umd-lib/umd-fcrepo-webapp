package edu.umd.lib.fcrepo.filters.ipmanager;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Group {
  private String id;
  private String key;
  private String name;

  @JsonProperty("@id")
  public String getId() {
    return id;
  }

  @JsonProperty("@id")
  public void setId(String id) {
    this.id = id;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
