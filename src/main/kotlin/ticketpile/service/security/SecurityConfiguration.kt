package ticketpile.service.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ticketpile.service.AdvanceSSOConfig
import ticketpile.service.advance.AdvanceManager
import ticketpile.service.util.transaction
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

val apiTokenHeader = "Bearer"

/**
 * Spring Authentication provider that lets user see API docs, but not get through unless
 * they provide their bearer token.
 * Created by jonlatane on 9/12/16.
 */
@Configuration
@EnableWebSecurity
open class SecurityConfiguration : WebSecurityConfigurerAdapter() {
    @Autowired
    lateinit var bearerFilter : BearerTokenFilter
    override fun configure(web : WebSecurity) {
        // Allow Swagger UI and GraphiQL stuff through. No one without
        // the admin credentials can hit real data.
        web.ignoring().antMatchers(
                "/graphiql.html",
                "/swagger-ui.html",
                "/webjars/springfox-swagger-ui/**/*",
                "/swagger-resources",
                "/swagger-resources/**/*",
                "/v2/api-docs"
        )
    }

    override fun configure(http : HttpSecurity)  {
        http.authorizeRequests()
        .anyRequest().authenticated()
        .and()
                .anonymous()
        .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
                .securityContext()
        .and()
                .headers().disable()
                .rememberMe().disable()
                .requestCache().disable()
                .x509().disable()
                .csrf().disable()
                .httpBasic().disable()
                .formLogin().disable()
                .logout().disable()
        .addFilterBefore(bearerFilter, AnonymousAuthenticationFilter::class.java)
    }
    
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(provider)
    }
}

@Component
open class BearerTokenFilter() : OncePerRequestFilter() {
    @Autowired
    lateinit var ssoConfig : AdvanceSSOConfig
    
    override fun doFilterInternal(
            request: HttpServletRequest?,
            response: HttpServletResponse?,
            filterChain: FilterChain?
    ) {
        val bearer : String = request?.getHeader(apiTokenHeader) ?: ""
        var user : ApplicationUser? = transaction {
            UserAuthKey.find {
                UserAuthKeys.authKey eq bearer
            }.firstOrNull()?.user
        }
        if(user == null // Allow user to login with Advance credentials
                && bearer.isNotBlank() // for the GraphQL endpoint.
                && request?.requestURI?.startsWith("/graphql") ?: false
        ) {
            try {
                user = AdvanceSSOUser(
                        AdvanceManager(ssoConfig.host, bearer, 0).currentUser!!
                )
            } catch(t: Throwable) {
                println("Advance SSO failed - user not authenticated: $t")
            }
        }

        val auth = TicketPileToken(user)
        SecurityContextHolder.getContext().authentication = auth
        filterChain?.doFilter(request, response)
    }
}

private object provider : AuthenticationProvider {
    override fun supports(authentication: Class<*>?): Boolean {
        return TicketPileToken::class.java.isAssignableFrom(authentication)
    }

    override fun authenticate(authentication: Authentication?): Authentication? {
        val user = (authentication as TicketPileToken).user
        if(user != null)
            return authentication
        return null
    }
}

internal class TicketPileToken(val user : ApplicationUser?) : AbstractAuthenticationToken(emptyList()) {
    override fun getCredentials(): ApplicationUser? {
        return user
    }

    override fun getPrincipal(): Any? {
        return null
    }
}