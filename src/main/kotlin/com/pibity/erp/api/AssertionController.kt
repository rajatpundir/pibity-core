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
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.commons.utils.getExpectedParams
import com.pibity.erp.commons.utils.getJsonParams
import com.pibity.erp.commons.utils.validateOrganizationClaim
import com.pibity.erp.services.AssertionService
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.security.RolesAllowed

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/assertion"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class AssertionController(val assertionService: AssertionService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createAssertion" to getExpectedParams("assertion", "createAssertion")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_SUPERUSER)
  fun createAssertion(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["createAssertion"] ?: JsonObject())
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = RoleConstants.ADMIN)
      ResponseEntity(assertionService.createAssertion(jsonParams = jsonParams).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
