package edu.umd.lib.fcrepo.requests;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ProvideRolesRequestWrapper extends HttpServletRequestWrapper {
    private final Set<String> roles;

    private static final Logger logger = LoggerFactory.getLogger(ProvideRolesRequestWrapper.class);

    /**
     * Constructs a request object wrapping the given request, that returns
     * true for a specific role name when isUserInRole() is called.
     *
     * @param request request object
     * @param role additional role that returns true for isUserInRole()
     * @throws IllegalArgumentException if the request is null
     */
    public ProvideRolesRequestWrapper(HttpServletRequest request, String role) {
        this(request, new HashSet<>(Collections.singleton(role)));
    }

    /**
     * Constructs a request object wrapping the given request, that returns
     * true for any role name in the set given when isUserInRole() is called.
     *
     * @param request request object
     * @param roles additional roles that return true for isUserInRole()
     * @throws IllegalArgumentException if the request is null
     */
    public ProvideRolesRequestWrapper(HttpServletRequest request, Set<String> roles) {
        super(request);
        this.roles = roles;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (this.roles.contains(role)) {
            logger.debug("Found role {} for {}", role, this.getUserPrincipal().getName());
            return true;
        }
        // defer to the parent class
        return super.isUserInRole(role);
    }
}
