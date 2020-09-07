/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.KeycloakConstants
import com.pibity.erp.commons.getExpectedParams
import com.pibity.erp.commons.getJsonParams
import com.pibity.erp.commons.gson
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.getKeycloakSecurityContext
import com.pibity.erp.services.OrganizationService
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.UserResource
import org.keycloak.admin.client.resource.UsersResource
import org.keycloak.representations.idm.CredentialRepresentation
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
  @RolesAllowed(KeycloakConstants.ROLE_SUPERUSER)
  fun createOrganization(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["createOrganization"] ?: JsonObject())
      ResponseEntity(gson.toJson(organizationService.createOrganization(jsonParams = jsonParams)), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/test"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun test(request: HttpServletRequest): ResponseEntity<String> {
    return try {
      println("----------------START HERE------------------------")
      getJWTToken(request)
      val serverUrl = "http://localhost:8081/auth"
      val realm = "inventory"
      val keycloak = Keycloak.getInstance(
          serverUrl,
          "inventory",
          "superuser@pibity.com",//realm admin
          "1234",
          "pibity-erp-admin",
          "2c9e3906-5a03-44e5-96e3-e1e7d514e6b4")

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

      val usersResource: UsersResource = realmResource.users()!!

      val passwordCred = CredentialRepresentation()
      passwordCred.isTemporary = false
      passwordCred.type = CredentialRepresentation.PASSWORD
      passwordCred.value = "test"

      val userRepresentation = usersResource.search("user4").single()

      val userResource: UserResource = usersResource.get(userRepresentation.id)
//      userResource.resetPassword(passwordCred)


//      val updatedUser = userResource.toRepresentation()
      userRepresentation.email = "Accccccc96496196@gamil.com"
      userResource.update(userRepresentation)


      ResponseEntity("s", HttpStatus.OK)
    } catch (exception: Exception) {
      ResponseEntity("unable to access", HttpStatus.BAD_REQUEST)
    }
  }

  fun getJWTToken(request: HttpServletRequest): String? {
    val securityContext = request.getKeycloakSecurityContext()
    println(securityContext.realm)
    println(securityContext.token.allowedOrigins.toString())
    println("---")
    for (r in securityContext.token.resourceAccess.entries) {
      println(r.key)
      println(r.value.roles.toString())
    }
    println("---")
    println(securityContext.token.resourceAccess.entries.toString())
    println(securityContext.token.toString())
    return securityContext.token?.email ?: "Whatever"
  }
}
