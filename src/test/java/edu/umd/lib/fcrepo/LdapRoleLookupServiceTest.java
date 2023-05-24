package edu.umd.lib.fcrepo;

import org.junit.Before;
import org.junit.Test;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

public class LdapRoleLookupServiceTest {
    private LdapRoleLookupService ldapService = null;

    @Before
    public void setUp() {
        ldapService = new LdapRoleLookupService();
        ldapService.setMemberAttribute("memberOf");
        ldapService.setAdminGroup("ADMIN");
        ldapService.setUserGroup("USER");
    }

    @Test
    public void testGetMembershipsNullUserEntry() {
        final Collection<String> memberships = ldapService.getMemberships(null);
        assertTrue(memberships.isEmpty());
    }

    @Test
    public void testGetMembershipsAdmin() {
        final LdapEntry userEntry = new LdapEntry();
        userEntry.addAttribute(new LdapAttribute("memberOf", "ADMIN", "other"));
        assertEquals(LdapRoleLookupService.ADMIN_ROLE, ldapService.getRole(userEntry));
    }

    @Test
    public void testGetMembershipsUser() {
        final LdapEntry userEntry = new LdapEntry();
        userEntry.addAttribute(new LdapAttribute("memberOf", "USER", "other"));
        assertEquals(LdapRoleLookupService.USER_ROLE, ldapService.getRole(userEntry));
    }

    @Test
    public void testGetMembershipsAdminCaseInsensitive() {
        final LdapEntry userEntry = new LdapEntry();
        userEntry.addAttribute(new LdapAttribute("memberOf", "admin", "other"));
        assertEquals(LdapRoleLookupService.ADMIN_ROLE, ldapService.getRole(userEntry));
    }

    @Test
    public void testGetMembershipsUserCaseInsensitive() {
        final LdapEntry userEntry = new LdapEntry();
        userEntry.addAttribute(new LdapAttribute("memberOf", "user", "other"));
        assertEquals(LdapRoleLookupService.USER_ROLE, ldapService.getRole(userEntry));
    }

    @Test
    public void testGetMembershipsNone() {
        final LdapEntry userEntry = new LdapEntry();
        userEntry.addAttribute(new LdapAttribute("memberOf", "some", "other"));
        assertNull(ldapService.getRole(userEntry));
    }
}
