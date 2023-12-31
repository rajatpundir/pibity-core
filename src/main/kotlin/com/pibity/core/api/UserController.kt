/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.api

import com.google.gson.JsonObject
import com.pibity.core.commons.constants.KeycloakConstants
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.commons.Logger
import com.pibity.core.commons.constants.OrganizationConstants
import com.pibity.core.utils.getExpectedParams
import com.pibity.core.utils.getJsonParams
import com.pibity.core.utils.validateOrganizationClaim
import com.pibity.core.entities.User
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.serializers.serialize
import com.pibity.core.services.UserService
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.keycloak.representations.AccessToken
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.annotation.security.RolesAllowed

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/user"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class UserController(val userService: UserService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createUser" to getExpectedParams("user", "createUser"),
      "updateUserGroups" to getExpectedParams("user", "updateUserGroups"),
      "updateUserSubspaces" to getExpectedParams("user", "updateUserSubspaces"),
      "getUserDetails" to getExpectedParams("user", "getUserDetails"),
      "getUserTypePermissions" to getExpectedParams("user", "getUserTypePermissions"),
      "getUserFunctionPermissions" to getExpectedParams("user", "getUserFunctionPermissions")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun createUser(@RequestParam("files") files: List<MultipartFile>, @RequestParam("request") request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["createUser"]
          ?: JsonObject()).apply { addProperty(OrganizationConstants.USERNAME, token.subject) }
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = KeycloakConstants.SUBGROUP_ADMIN)
      val (user: User, typePermission: TypePermission) = userService.createUser(jsonParams = jsonParams, files = files, defaultTimestamp = Timestamp.valueOf(
        ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()))
      ResponseEntity(userService.serialize(user, typePermission).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update/groups"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun updateUserGroups(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["updateUserGroups"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = KeycloakConstants.SUBGROUP_ADMIN)
      val (user: User, typePermission: TypePermission) = userService.updateUserGroups(jsonParams = jsonParams, defaultTimestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()))
      ResponseEntity(userService.serialize(user, typePermission).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update/subspaces"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun updateUserSubspaces(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["updateUserSubspaces"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = KeycloakConstants.SUBGROUP_ADMIN)
      val (user: User, typePermission: TypePermission) = userService.updateUserSubspaces(jsonParams = jsonParams, defaultTimestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()))
      ResponseEntity(userService.serialize(user, typePermission).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/details"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun getUserDetails(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["getUserDetails"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = KeycloakConstants.SUBGROUP_USER)
      val (user: User, typePermission: TypePermission) = userService.getUserDetails(jsonParams = jsonParams, defaultTimestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()))
      ResponseEntity(userService.serialize(user, typePermission).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/typePermissions"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun getUserTypePermissions(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["getUserTypePermissions"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = KeycloakConstants.SUBGROUP_ADMIN)
      ResponseEntity(serialize(userService.getUserTypePermissions(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/functionPermissions"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun getUserFunctionPermissions(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["getUserFunctionPermissions"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = KeycloakConstants.SUBGROUP_ADMIN)
      ResponseEntity(serialize(userService.getUserFunctionPermissions(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
