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
import com.pibity.core.serializers.serialize
import com.pibity.core.services.MapperService
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
@RequestMapping(path = ["/api/mapper"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class MapperController(val mapperService: MapperService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
    "createMapper" to getExpectedParams("mapper", "createMapper"),
    "getMapperDetails" to getExpectedParams("mapper", "getMapperDetails"),
    "executeMapper" to getExpectedParams("mapper", "executeMapper")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_SUPERUSER)
  fun createMapper(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["createMapper"] ?: JsonObject())
      ResponseEntity(mapperService.createMapper(jsonParams = jsonParams, defaultTimestamp = Timestamp.valueOf(
        ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime())).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/details"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun getMapperDetails(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["getMapperDetails"]
        ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = KeycloakConstants.SUBGROUP_USER)
      ResponseEntity(serialize(mapperService.getMapperDetails(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/execute"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun executeMapper(@RequestParam("files") files: MutableList<MultipartFile>, @RequestParam("request") request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["executeMapper"]
        ?: JsonObject()).apply { addProperty(OrganizationConstants.USERNAME, token.subject) }
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = KeycloakConstants.SUBGROUP_USER)
      ResponseEntity(mapperService.executeMapper(jsonParams = jsonParams, files = files, defaultTimestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime())).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
