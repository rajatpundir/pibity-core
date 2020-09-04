/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.RoleConstants
import com.pibity.erp.commons.getExpectedParams
import com.pibity.erp.commons.getJsonParams
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.serializers.serialize
import com.pibity.erp.services.TypeService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.security.RolesAllowed

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/type"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class TypeController(val typeService: TypeService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createType" to getExpectedParams("type", "createType"),
      "getTypeDetails" to getExpectedParams("type", "getTypeDetails")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(RoleConstants.USER)
  fun createType(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(typeService.createType(jsonParams = (getJsonParams(request, expectedParams["createType"]
          ?: JsonObject())))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/details"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(RoleConstants.USER)
  fun getTypeDetails(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(typeService.getTypeDetails(jsonParams = getJsonParams(request, expectedParams["getTypeDetails"]
          ?: JsonObject()))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
