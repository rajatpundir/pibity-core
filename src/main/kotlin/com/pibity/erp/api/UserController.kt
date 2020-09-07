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
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.commons.validateOrganizationClaim
import com.pibity.erp.serializers.serialize
import com.pibity.erp.services.UserService
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.security.RolesAllowed

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/user"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class UserController(val userService: UserService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createUser" to getExpectedParams("user", "createUser"),
      "updateUserGroups" to getExpectedParams("user", "updateUserGroups"),
      "updateUserRoles" to getExpectedParams("user", "updateUserRoles"),
      "getUserDetails" to getExpectedParams("user", "getUserDetails"),
      "getUserPermissions" to getExpectedParams("user", "getUserPermissions"),
      "superimposeUserPermissions" to getExpectedParams("user", "superimposeUserPermissions")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_OWNER)
  fun createUser(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["createUser"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams)
      ResponseEntity(serialize(userService.createUser(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update/groups"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_OWNER)
  fun updateUserGroups(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["updateUserGroups"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams)
      ResponseEntity(serialize(userService.updateUserGroups(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update/roles"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_OWNER)
  fun updateUserRoles(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["updateUserRoles"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams)
      ResponseEntity(serialize(userService.updateUserRoles(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/details"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_OWNER)
  fun getUserDetails(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["getUserDetails"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams)
      ResponseEntity(serialize(userService.getUserDetails(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/permissions"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_OWNER)
  fun getUserPermissions(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["getUserPermissions"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams)
      ResponseEntity(serialize(userService.getUserPermissions(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/superimpose"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_SUPERUSER)
  fun superimposeUserPermissions(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["superimposeUserPermissions"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams)
      ResponseEntity(serialize(userService.superimposeUserPermissions(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
