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
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.serializers.serialize
import com.pibity.erp.services.GroupService
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
  @RolesAllowed("SUPERUSER", "OWNER" )
  fun createGroup(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(groupService.createGroup(jsonParams = (getJsonParams(request, expectedParams["createGroup"]
          ?: JsonObject())))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed("SUPERUSER", "OWNER" )
  fun updateGroup(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(groupService.updateGroup(jsonParams = (getJsonParams(request, expectedParams["updateGroup"]
          ?: JsonObject())))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/details"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed("SUPERUSER", "OWNER" )
  fun getGroupDetails(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(groupService.getGroupDetails(jsonParams = (getJsonParams(request, expectedParams["getGroupDetails"]
          ?: JsonObject())))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
