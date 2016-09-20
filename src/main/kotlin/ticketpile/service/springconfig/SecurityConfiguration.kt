package ticketpile.service.springconfig

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
import org.springframework.web.filter.OncePerRequestFilter
import ticketpile.service.security.AuthKey
import ticketpile.service.security.AuthKeys
import ticketpile.service.security.User
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
    override fun configure(web : WebSecurity) {
        // Allow Swagger UI stuff through. No one without
        // the admin credentials can hit real data.
        web.ignoring().antMatchers(
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
        .addFilterBefore(BearerTokenFilter(), AnonymousAuthenticationFilter::class.java)
    }
    
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(provider)
    }
}

class BearerTokenFilter() : OncePerRequestFilter() {
    override fun doFilterInternal(
            request: HttpServletRequest?,
            response: HttpServletResponse?,
            filterChain: FilterChain?
    ) {
        val bearer : String = request?.getHeader(apiTokenHeader) ?: ""
        val user = transaction {
            AuthKey.find {
                AuthKeys.authKey eq bearer
            }.firstOrNull()?.user
                    //?: throw AuthenticationException("Please provide a valid Bearer token in your header.")
        }

        val auth = BearerToken(user)
        SecurityContextHolder.getContext().authentication = auth
        filterChain?.doFilter(request, response)
    }
}

internal object provider : AuthenticationProvider {
    override fun supports(authentication: Class<*>?): Boolean {
        return BearerToken::class.java.isAssignableFrom(authentication)
    }

    override fun authenticate(authentication: Authentication?): Authentication? {
        val user = (authentication as BearerToken).user
        if(user != null)
            return authentication
        return null
    }
}

internal class BearerToken(val user : User?) : AbstractAuthenticationToken(emptyList()) {
    override fun getCredentials(): User? {
        return user
    }

    override fun getPrincipal(): Any? {
        return null
    }
}