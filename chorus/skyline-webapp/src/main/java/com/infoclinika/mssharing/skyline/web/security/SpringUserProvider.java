package com.infoclinika.mssharing.skyline.web.security;

import com.google.common.base.Function;
import com.infoclinika.mssharing.model.helper.SecurityHelper;
import com.infoclinika.mssharing.platform.web.security.RichUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pavel Kaplin
 *         date: 14.05.2014
 */
@Component
public class SpringUserProvider implements UserDetailsService{

    private static final String ROLE_USER = "ROLE_user";
    private static final String ROLE_ADMIN = "ROLE_admin";
    private static final SimpleGrantedAuthority[] ADMIN_ROLES = new SimpleGrantedAuthority[]
            {new SimpleGrantedAuthority(ROLE_ADMIN), new SimpleGrantedAuthority(ROLE_USER)};
    private static final SimpleGrantedAuthority[] USER_ROLES = new SimpleGrantedAuthority[]{new SimpleGrantedAuthority(ROLE_USER)};

    @Inject
    private SecurityHelper securityHelper;

    private static final Function<SecurityHelper.UserDetails, UserDetails> USER_DETAILS_TRANSFORMER = new Function<SecurityHelper.UserDetails, UserDetails>() {
        @Override
        public UserDetails apply(SecurityHelper.UserDetails input) {
            return new RichUser(input.id, input.email, input.password, getRoles(input.admin), input.firstName, input.lastName, input.emailVerified, input.labs);
        }
    };

    private static List<SimpleGrantedAuthority> getRoles(boolean isAdmin) {
        if (isAdmin) {
            return Arrays.asList(ADMIN_ROLES);
        }
        return Arrays.asList(USER_ROLES);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SecurityHelper.UserDetails userDetails = securityHelper.getUserDetailsByEmail(username);
        if (userDetails == null) {
            throw new UsernameNotFoundException("User with email " + username + " not found");
        }
        return USER_DETAILS_TRANSFORMER.apply(userDetails);
    }

    public UserDetails getUserDetails(long id) {
        SecurityHelper.UserDetails userDetails = securityHelper.getUserDetails(id);
        return USER_DETAILS_TRANSFORMER.apply(userDetails);
    }

}
