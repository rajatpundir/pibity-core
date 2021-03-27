/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.pibity.core.commons.utils.gson
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File
import java.io.FileReader

@SpringBootApplication
class CoreApplication

fun main(args: Array<String>) {
  File("src/main/resources/postmaster.json").writeText(
    GsonBuilder().setPrettyPrinting().create().toJson(
      gson.fromJson(FileReader("src/main/resources/postmaster.json"), JsonObject::class.java).apply {
        addProperty("runCount", get("runCount").asInt + 1)
      }))
  runApplication<CoreApplication>(*args)
}
