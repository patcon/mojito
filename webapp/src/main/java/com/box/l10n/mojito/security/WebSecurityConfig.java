package com.box.l10n.mojito.security;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.Filter;

/**
 * @author wyau
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, mode = AdviceMode.ASPECTJ)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Autowired
    SecurityConfig securityConfig;

    @Autowired
    LdapConfig ldapConfig;

    @Autowired
    ActiveDirectoryConfig activeDirectoryConfig;

    @Autowired
    UserDetailsContextMapperImpl userDetailsContextMapperImpl;

    @Autowired(required = false)
    @Qualifier("oauth2Filter")
    Filter oauth2Filter;

    @Autowired(required = false)
    RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter;

    @Autowired(required = false)
    PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        for (SecurityConfig.AuthenticationType authenticationType : securityConfig.getAuthenticationType()) {
            switch (authenticationType) {
                case DATABASE:
                    configureDatabase(auth);
                    break;
                case LDAP:
                    configureLdap(auth);
                    break;
                case AD:
                    configureActiveDirectory(auth);
                    break;
                case HEADER:
                    configureHeaderAuth(auth);
                    break;
            }
        }
    }

    void configureActiveDirectory(AuthenticationManagerBuilder auth) throws Exception {
        logger.debug("Configuring in active directory authentication");
        ActiveDirectoryAuthenticationProviderConfigurer<AuthenticationManagerBuilder> activeDirectoryManagerConfigurer = new ActiveDirectoryAuthenticationProviderConfigurer<>();

        activeDirectoryManagerConfigurer.domain(activeDirectoryConfig.getDomain());
        activeDirectoryManagerConfigurer.url(activeDirectoryConfig.getUrl());
        activeDirectoryManagerConfigurer.rootDn(activeDirectoryConfig.getRootDn());
        activeDirectoryManagerConfigurer.userServiceDetailMapper(userDetailsContextMapperImpl);

        auth.apply(activeDirectoryManagerConfigurer);
    }

    void configureDatabase(AuthenticationManagerBuilder auth) throws Exception {
        logger.debug("Configuring in database authentication");
        auth.userDetailsService(getUserDetailsService()).passwordEncoder(new BCryptPasswordEncoder());
    }

    void configureLdap(AuthenticationManagerBuilder auth) throws Exception {
        logger.debug("Configuring ldap server");
        LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder>.ContextSourceBuilder contextSourceBuilder = auth.ldapAuthentication()
                .userSearchBase(ldapConfig.getUserSearchBase())
                .userSearchFilter(ldapConfig.getUserSearchFilter())
                .groupSearchBase(ldapConfig.getGroupSearchBase())
                .groupSearchFilter(ldapConfig.getGroupSearchFilter())
                .groupRoleAttribute(ldapConfig.getGroupRoleAttribute())
                .userDetailsContextMapper(userDetailsContextMapperImpl)
                .contextSource();

        if (ldapConfig.getPort() != null) {
            contextSourceBuilder.port(ldapConfig.getPort());
        }

        contextSourceBuilder
                .root(ldapConfig.getRoot())
                .url(ldapConfig.getUrl())
                .managerDn(ldapConfig.getManagerDn())
                .managerPassword(ldapConfig.getManagerPassword())
                .ldif(ldapConfig.getLdif());
    }

    void configureHeaderAuth(AuthenticationManagerBuilder auth) {
        Preconditions.checkNotNull(preAuthenticatedAuthenticationProvider, "The preAuthenticatedAuthenticationProvider must be configured");
        logger.debug("Configuring in pre authentication");
        auth.authenticationProvider(preAuthenticatedAuthenticationProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        logger.debug("Configuring web security");

        http.headers().cacheControl().disable();

        http.csrf().ignoringAntMatchers("/shutdown", "/api/rotation");


        http.authorizeRequests()
                .antMatchers("/intl/*", "/img/*", "/fonts/*", "/login/**", "/webjars/**", "/cli/**", "/health", "/js/**", "/css/**").permitAll()
                .antMatchers("/shutdown", "/api/rotation").hasIpAddress("127.0.0.1").anyRequest().permitAll()
                .anyRequest().fullyAuthenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .successHandler(new ShowPageAuthenticationSuccessHandler())
                .and()
                .logout().logoutSuccessUrl("/login?logout").permitAll();


        http.exceptionHandling().defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), new AntPathRequestMatcher("/*"));
        http.exceptionHandling().defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AntPathRequestMatcher("/api/*"));

        for (SecurityConfig.AuthenticationType authenticationType : securityConfig.getAuthenticationType()) {
            switch (authenticationType) {
                case HEADER:
                    Preconditions.checkNotNull(requestHeaderAuthenticationFilter, "The requestHeaderAuthenticationFilter must be configured");
                    logger.debug("Add request header Auth filter");
                    requestHeaderAuthenticationFilter.setAuthenticationManager(authenticationManager());
                    http.addFilterBefore(requestHeaderAuthenticationFilter, BasicAuthenticationFilter.class);
                    break;
                case OAUTH2:
                    Preconditions.checkNotNull(oauth2Filter, "The OAuth2 filter must be configured");
                    logger.debug("Add OAuth2 filter");
                    http.addFilterBefore(oauth2Filter, BasicAuthenticationFilter.class);
                    http.exceptionHandling().defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login/oauth"), new AntPathRequestMatcher("/*"));
                    break;
            }
        }
    }

    @Primary
    @Bean
    protected UserDetailsServiceImpl getUserDetailsService() {
        return new UserDetailsServiceImpl();
    }

}
