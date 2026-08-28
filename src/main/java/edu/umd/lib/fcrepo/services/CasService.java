package edu.umd.lib.fcrepo.services;

public class CasService {
  private String casUrlPrefix;

  public CasService() {}

  public String getCasLogoutUrl() {
    return casUrlPrefix + "/logout";
  }

  public String getCasUrlPrefix() {
    return casUrlPrefix;
  }

  public void setCasUrlPrefix(String casUrlPrefix) {
    this.casUrlPrefix = casUrlPrefix;
  }
}
