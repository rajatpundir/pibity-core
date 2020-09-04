/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.config

import org.keycloak.adapters.KeycloakConfigResolver
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.KeycloakDeploymentBuilder
import org.keycloak.adapters.spi.HttpFacade
import org.keycloak.adapters.springsecurity.KeycloakConfiguration
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy

@KeycloakConfiguration
class SecurityConfig : KeycloakWebSecurityConfigurerAdapter() {

  @Autowired
  fun configureGlobal(auth: AuthenticationManagerBuilder) {
    val keycloakAuthenticationProvider = keycloakAuthenticationProvider()
    keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(SimpleAuthorityMapper())
    auth.authenticationProvider(keycloakAuthenticationProvider)
  }

  @Bean
  fun keycloakConfigResolver(): KeycloakConfigResolver {
    return object : KeycloakConfigResolver {
      private var keycloakDeployment: KeycloakDeployment? = null
      override fun resolve(facade: HttpFacade.Request): KeycloakDeployment {
        if (keycloakDeployment != null)
          return keycloakDeployment as KeycloakDeployment
        val path = "/keycloak.json"
        val configInputStream = javaClass.getResourceAsStream(path)
        if (configInputStream != null)
          return KeycloakDeploymentBuilder.build(configInputStream)
        else
          throw RuntimeException("Could not load Keycloak deployment info: $path")
      }
    }
  }

  @Bean
  override fun sessionAuthenticationStrategy(): SessionAuthenticationStrategy {
    return RegisterSessionAuthenticationStrategy(SessionRegistryImpl())
  }

  override fun configure(http: HttpSecurity) {
    super.configure(http)
    http.csrf().disable()
        .authorizeRequests()
        .anyRequest().permitAll()
  }
}
