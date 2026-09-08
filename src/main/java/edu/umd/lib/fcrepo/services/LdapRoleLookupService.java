package edu.umd.lib.fcrepo.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchResult;
import org.ldaptive.cache.LRUCache;
import org.ldaptive.pool.ConnectionPool;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.pool.SoftLimitConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class LdapRoleLookupService {
  private static final Logger logger = LoggerFactory.getLogger(LdapRoleLookupService.class);

  public static final String ADMIN_ROLE = "fedoraAdmin";

  public static final String USER_ROLE = "fedoraUser";

  private ConnectionFactory connectionFactory;

  private ConnectionPool connectionPool;

  private String ldapURL;

  private String bindDN;

  private String bindPassword;

  private String baseDN;

  private String memberAttribute;

  private String adminGroup;

  private String userGroup;

  private SearchExecutor searchExecutor;

  private ConnectionConfig connectionConfig;

  public LdapRoleLookupService() {}

  @PostConstruct
  public void initialize() {
    connectionPool = new SoftLimitConnectionPool(new DefaultConnectionFactory(connectionConfig));
    connectionPool.initialize();
    connectionFactory = new PooledConnectionFactory(connectionPool);

    searchExecutor = new SearchExecutor();
    searchExecutor.setSearchCache(new LRUCache<>(50, Duration.ofMinutes(10), Duration.ofMinutes(5)));
    searchExecutor.setBaseDn(baseDN);

    logger.info("Configured LDAP for user role lookup");
    logger.info("LDAP URL: {} Base DN: {} Bind DN: {}", ldapURL, baseDN, bindDN);
    logger.debug("Group {} => Role {}", adminGroup, ADMIN_ROLE);
    logger.debug("Group {} => Role {}", userGroup, USER_ROLE);
  }

  @PreDestroy
  public void teardown() {
    connectionPool.close();
  }

  /**
   * Look up the given userName in the configured LDAP directory, and return the
   * matching entry (if found).
   *
   * @param userName this should match a single uid in the directory
   * @return matching entry or null
   */
  public LdapEntry getUserEntry(final String userName) {
    try {
      final String uidFilter = "uid=" + userName;
      logger.debug("Running LDAP search with filter {}, returning attribute {}", uidFilter, memberAttribute);
      final SearchResult result = searchExecutor.search(connectionFactory, uidFilter, memberAttribute).getResult();
      logger.debug("Found {} results", result.size());
      return result.getEntry();
    } catch (LdapException e) {
      logger.error("LDAP Exception: " + e);
      e.printStackTrace();
      return null;
    }
  }

  /**
   * If the userEntry is a member of either the admin group or the user group,
   * return the appropriate role string ("fedoraAdmin" or "fedoraUser", respectively).
   * If the userEntry is null, or has neither membership relation, return null.
   * The checks for membership are done case-insensitively.
   *
   * @param userEntry LDAP entry for a user
   * @return role name string: "fedoraAdmin", "fedoraUser", or null
   */
  public String getRole(final LdapEntry userEntry) {
    final Collection<String> memberships = getMemberships(userEntry).stream().map(String::toLowerCase).collect(Collectors.toSet());
    if (memberships.contains(adminGroup.toLowerCase())) {
      return ADMIN_ROLE;
    } else if (memberships.contains(userGroup.toLowerCase())){
      return USER_ROLE;
    }
    return null;
  }

  public String getRole(final String userName) {
    return getRole(getUserEntry(userName));
  }

  /**
   * Get the set of values in the memberAttribute of the given userEntry,
   * or the empty set if the userEntry is null.
   *
   * @param userEntry LDAP entry for a user
   * @return collection of strings, or the empty set
   */
  public Collection<String> getMemberships(final LdapEntry userEntry) {
    if (userEntry == null) {
      return Collections.emptySet();
    }
    final LdapAttribute memberOfAttr = userEntry.getAttribute(memberAttribute);
    return memberOfAttr.getStringValues();
  }

  public String getLdapURL() {
    return ldapURL;
  }

  public void setLdapURL(String ldapURL) {
    this.ldapURL = ldapURL;
  }

  public String getBindDN() {
    return bindDN;
  }

  public void setBindDN(String bindDN) {
    this.bindDN = bindDN;
  }

  public String getBindPassword() {
    return bindPassword;
  }

  public void setBindPassword(String bindPassword) {
    this.bindPassword = bindPassword;
  }

  public String getBaseDN() {
    return baseDN;
  }

  public void setBaseDN(String baseDN) {
    this.baseDN = baseDN;
  }

  public String getMemberAttribute() {
    return memberAttribute;
  }

  public void setMemberAttribute(String memberAttribute) {
    this.memberAttribute = memberAttribute;
  }

  public String getAdminGroup() {
    return adminGroup;
  }

  public void setAdminGroup(String adminGroup) {
    this.adminGroup = adminGroup;
  }

  public String getUserGroup() {
    return userGroup;
  }

  public void setUserGroup(String userGroup) {
    this.userGroup = userGroup;
  }

  public ConnectionPool getConnectionPool() {
    return connectionPool;
  }

  public void setConnectionPool(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  public ConnectionConfig getConnectionConfig() {
    return connectionConfig;
  }

  public void setConnectionConfig(ConnectionConfig connectionConfig) {
    this.connectionConfig = connectionConfig;
  }
}
