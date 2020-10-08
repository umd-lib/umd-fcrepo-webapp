package edu.umd.lib.fcrepo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class ProvideRoleRequestWrapper extends HttpServletRequestWrapper {
    private final String role;

    /**
     * Constructs a request object wrapping the given request, that returns
     * true for a specific role name when isUserInRole() is called.
     *
     * @param request request object
     * @param role additional role that returns true for isUserInRole()
     * @throws IllegalArgumentException if the request is null
     */
    public ProvideRoleRequestWrapper(HttpServletRequest request, String role) {
        super(request);
        this.role = role;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (role.equals(this.role)) {
            return true;
        }
        // defer to the parent class
        return super.isUserInRole(role);
    }
}
