/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonObject
import com.pibity.erp.commons.getExpectedParams
import com.pibity.erp.commons.getJsonParams
import com.pibity.erp.commons.gson
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.services.RoleService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/role"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class RoleController(val roleService: RoleService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createRole" to getExpectedParams("role", "createRole"),
      "updateRole" to getExpectedParams("role", "updateRole"),
      "getRoleDetails" to getExpectedParams("role", "getRoleDetails")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun createRole(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(roleService.createRole(jsonParams = (getJsonParams(request, expectedParams["createRole"]
          ?: JsonObject())))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun updateRole(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(roleService.updateRole(jsonParams = (getJsonParams(request, expectedParams["updateRole"]
          ?: JsonObject())))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/details"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getRoleDetails(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(roleService.getRoleDetails(jsonParams = (getJsonParams(request, expectedParams["getRoleDetails"]
          ?: JsonObject())))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
