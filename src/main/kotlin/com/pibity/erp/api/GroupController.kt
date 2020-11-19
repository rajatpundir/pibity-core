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
import com.pibity.erp.commons.constants.RoleConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.serializers.serialize
import com.pibity.erp.services.GroupService
import com.pibity.erp.commons.utils.getExpectedParams
import com.pibity.erp.commons.utils.getJsonParams
import com.pibity.erp.commons.utils.validateOrganizationClaim
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.security.RolesAllowed

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/group"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class GroupController(val groupService: GroupService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createGroup" to getExpectedParams("group", "createGroup"),
      "updateGroup" to getExpectedParams("group", "updateGroup"),
      "getGroupDetails" to getExpectedParams("group", "getGroupDetails")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun createGroup(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["createGroup"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = RoleConstants.ADMIN)
      ResponseEntity(serialize(groupService.createGroup(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun updateGroup(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["updateGroup"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = RoleConstants.ADMIN)
      ResponseEntity(serialize(groupService.updateGroup(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/details"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun getGroupDetails(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["getGroupDetails"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = RoleConstants.ADMIN)
      ResponseEntity(serialize(groupService.getGroupDetails(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
