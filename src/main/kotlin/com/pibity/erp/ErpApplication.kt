/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.pibity.erp.commons.utils.gson
import org.keycloak.KeycloakSecurityContext
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File
import java.io.FileReader
import javax.servlet.http.HttpServletRequest

@SpringBootApplication
class ErpApplication

fun main(args: Array<String>) {
  File("src/main/resources/postmaster.json").writeText(
      GsonBuilder().setPrettyPrinting().create().toJson(
          gson.fromJson(FileReader("src/main/resources/postmaster.json"), JsonObject::class.java).apply {
            addProperty("runCount", get("runCount").asInt + 1)
          }))
//  File("src/main/kotlin/com/pibity/erp/").walk()
//      .filter {
//        it.absolutePath.matches(Regex("^.*\\.kt$"))
//      }.forEach { file ->
//        if (file.isFile) {
//          println("PROCESSING AST")
//          println("PATH: " + file.parent)
//          println("FILE: " + file.path)
//          val kotlinFile = KotlinGrammarAntlrKotlinParser.parseKotlinFile(AstSource.File(file.path))
//          kotlinFile.summary(attachRawAst = false)
//              .onSuccess { astList ->
//                var ast = ""
//                astList.forEach { ast += it.printString() }
//                File(file.parent + "/ast/" + file.name + ".ast").also { it.parentFile.mkdirs() }.writeText(ast)
//              }.onFailure { errors ->
//                errors.forEach(::println)
//              }
//        }
//      }
  runApplication<ErpApplication>(*args)
}

fun HttpServletRequest.getKeycloakSecurityContext(): KeycloakSecurityContext {
  return this.getAttribute(KeycloakSecurityContext::class.java.name) as KeycloakSecurityContext
}
