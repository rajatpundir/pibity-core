/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.getExpectedParams
import com.pibity.erp.commons.getJsonParams
import com.pibity.erp.commons.gson
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.services.OrganizationService
import org.keycloak.KeycloakSecurityContext
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.UserResource
import org.keycloak.admin.client.resource.UsersResource
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.security.RolesAllowed
import javax.servlet.http.HttpServletRequest


@CrossOrigin
@RestController
@RequestMapping(path = ["/api/organization"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class OrganizationController(val organizationService: OrganizationService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createOrganization" to getExpectedParams("organization", "createOrganization")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed("SUPERUSER")
  fun createOrganization(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(organizationService.createOrganization(jsonParams = getJsonParams(request, expectedParams["createOrganization"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/test"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed("USER")
  fun test(request: HttpServletRequest): ResponseEntity<String> {
    return try {
      println("----------------START HERE------------------------")
      getJWTToken(request)
      val serverUrl = "http://localhost:8081/auth"
      val realm = "inventory"
      val keycloak = Keycloak.getInstance(
          serverUrl,
          "inventory",
          "testuser",//realm admin
          "12345678",
          "test-client",
          "13823722-e981-41fa-9088-59fe309b83d3")

//      val x=JsonArray()
//      x.add("zs")
//      x.add("asd")
//      x.add("sdfg")
//
//      val user  = UserRepresentation()
//      user.username = "user4"
//      user.firstName = "test"
//      user.email = "test4@gmail.com"
//      user.lastName = "user"
//      user.isEnabled=true
//      user.attributes= mapOf("mobile" to listOf("964161616"),"organization" to listOf(x.toString()))

      val realmResource = keycloak.realm(realm)
//      realmResource.users().create(user)

      val usersResource : UsersResource= realmResource.users()!!

      val passwordCred = CredentialRepresentation()
      passwordCred.isTemporary = false
      passwordCred.type = CredentialRepresentation.PASSWORD
      passwordCred.value = "test"


      val userResource: UserResource = usersResource?.get("a9dd7287-428d-44ee-99be-ab2364150dba")
//      userResource.resetPassword(passwordCred)


      val updatedUser = userResource.toRepresentation()
      updatedUser.email = "A56161616bc@gamil.com"
      userResource.update(updatedUser)


      ResponseEntity("s", HttpStatus.OK)
    } catch (exception: Exception) {
      ResponseEntity("unable to access", HttpStatus.BAD_REQUEST)
    }
  }

  fun getJWTToken(request: HttpServletRequest): String? {
//    val keycloakPrincipal = (SecurityContextHolder.getContext().authentication as KeycloakAuthenticationToken).principal as KeycloakPrincipal<KeycloakSecurityContext>
    val securityContext = getKeycloakSecurityContext(request)
    if (securityContext != null) {
      println(securityContext.realm)
      println(securityContext.token.allowedOrigins.toString())
//      println(securityContext.token.realmAccess.roles.toString())
//      println(securityContext.token.realmAccess.isUserInRole("ADMIN"))
//      println(securityContext.token.realmAccess.addRole("ADMIN"))
      println("---")
      for (r in securityContext.token.resourceAccess.entries) {
        println(r.key)
        println(r.value.roles.toString())
      }
      println("---")
      println(securityContext.token.resourceAccess.entries.toString())
      println(securityContext.token.toString())
    }
    return securityContext?.token?.email ?: "Whatever"
  }

  private fun getKeycloakSecurityContext(request: HttpServletRequest): KeycloakSecurityContext? {
    return request.getAttribute(KeycloakSecurityContext::class.java.name) as KeycloakSecurityContext
  }

}
