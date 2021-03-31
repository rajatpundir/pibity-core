/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.exceptions.CustomJsonException
import com.pibity.core.entities.Type
import com.pibity.core.entities.circuit.Circuit
import com.pibity.core.entities.circuit.CircuitInput
import com.pibity.core.entities.circuit.CircuitOutput
import com.pibity.core.entities.function.Function
import com.pibity.core.entities.function.FunctionInput
import com.pibity.core.entities.function.FunctionOutput
import com.pibity.core.entities.function.Mapper
import org.springframework.web.multipart.MultipartFile
import java.lang.Integer.max
import java.sql.Timestamp

fun validateCircuitName(circuitName: String): String {
  if (!keyIdentifierPattern.matcher(circuitName).matches())
    throw CustomJsonException("{${CircuitConstants.CIRCUIT_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
  return circuitName
}

fun validateCircuitInputs(inputs: JsonObject, types: Set<Type>, files: List<MultipartFile>): JsonObject = inputs.entrySet().fold(JsonObject()) { acc, (inputName, input) ->
  acc.apply {
    try {
      if (!input.isJsonObject) {
        if (input.isJsonArray)
          add(inputName, JsonObject().apply { add(KeyConstants.KEY_TYPE, JsonArray()) })
        else
          add(inputName, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, types.single { it.name == input.asString }.name) })
      }
      else {
        add(inputName, JsonObject().apply {
          val inputJson: JsonObject = input.asJsonObject
          if (inputJson.get(KeyConstants.KEY_TYPE).isJsonArray)
            add(KeyConstants.KEY_TYPE, JsonArray())
          else {
            val inputType: Type = types.single { it.name == inputJson.get(KeyConstants.KEY_TYPE).asString && it.name != TypeConstants.FORMULA }
            addProperty(KeyConstants.KEY_TYPE, inputType.name)
            if (inputJson.has(KeyConstants.DEFAULT)) {
              when(inputType.name) {
                TypeConstants.TEXT -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asString)
                TypeConstants.NUMBER -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asLong)
                TypeConstants.DECIMAL -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asDouble)
                TypeConstants.BOOLEAN -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asBoolean)
                TypeConstants.DATE -> addProperty(KeyConstants.DEFAULT, java.sql.Date(inputJson.get(KeyConstants.DEFAULT).asLong).time)
                TypeConstants.TIMESTAMP -> addProperty(KeyConstants.DEFAULT, Timestamp(inputJson.get(KeyConstants.DEFAULT).asLong).time)
                TypeConstants.TIME -> addProperty(KeyConstants.DEFAULT, java.sql.Time(inputJson.get(KeyConstants.DEFAULT).asLong).time)
                TypeConstants.BLOB -> {
                  val fileIndex: Int = inputJson.get(KeyConstants.DEFAULT).asInt
                  if (fileIndex < 0 && fileIndex > (files.size - 1))
                    throw CustomJsonException("{}")
                  else
                    addProperty(KeyConstants.DEFAULT, fileIndex)
                }
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asString)
              }
            }
          }
        })
      }
    } catch (exception: Exception) {
      throw CustomJsonException("{${CircuitConstants.INPUTS}: {$inputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
    }
  }
}

fun validateCircuitComputations(computations: JsonObject, inputs: JsonObject, types: Set<Type>, functions: Set<Function>, mappers: Set<Mapper>, circuits: Set<Circuit>): JsonObject {
  val computationLevels: MutableMap<String, Int> = mutableMapOf()
  return computations.entrySet()
    .sortedBy {
      try {
        it.value.asJsonObject.get(CircuitConstants.ORDER).asInt
      } catch (exception: Exception) {
        throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${it.key}: ${MessageConstants.UNEXPECTED_VALUE}}}")
      }
    }.foldIndexed(JsonObject()) { index, acc, (computationName, computation) ->
      acc.apply {
        add(computationName, JsonObject().apply {
          val computationJson: JsonObject = computation.asJsonObject
          addProperty(CircuitConstants.ORDER, index)
          val computationType: String = when(computationJson.get(KeyConstants.KEY_TYPE).asString) {
            CircuitConstants.FUNCTION -> CircuitConstants.FUNCTION
            CircuitConstants.MAPPER -> CircuitConstants.MAPPER
            CircuitConstants.CIRCUIT -> CircuitConstants.CIRCUIT
            else -> throw CustomJsonException("{}")
          }
          addProperty(KeyConstants.KEY_TYPE, computationType)
          computationLevels[computationName] = 0
          when(computationType) {
            CircuitConstants.FUNCTION -> {
              val function: Function = functions.single { it.name == computationJson.get(CircuitConstants.EXECUTE).asString }
              addProperty(CircuitConstants.EXECUTE, function.name)
              add(CircuitConstants.CONNECT, function.inputs.fold(JsonObject()) { acc1, functionInput ->
                acc1.apply {
                  val connection: JsonArray = computationJson.get(CircuitConstants.CONNECT).asJsonObject.get(functionInput.name).asJsonArray
                  // Check if input is being mutated by any referencing computation that is already processed
                  computations.entrySet().filter { it.value.asJsonObject.get(CircuitConstants.ORDER).asInt < computationJson.get(CircuitConstants.ORDER).asInt }.forEach { (referencingComputationName, referencingComputation) ->
                    val referencingComputationJson: JsonObject = referencingComputation.asJsonObject
                    when(referencingComputationJson.get(KeyConstants.KEY_TYPE).asString) {
                      CircuitConstants.FUNCTION -> {
                        val referencingComputationFunction: Function = functions.single { it.name == referencingComputationJson.get(CircuitConstants.EXECUTE).asString }
                        referencingComputationFunction.inputs.filter { it.variableName != null || it.values.isNotEmpty() }.forEach { referencingFunctionInput ->
                          val referencingConnection:JsonArray = referencingComputationJson.get(CircuitConstants.CONNECT).asJsonObject.get(referencingFunctionInput.name).asJsonArray
                          when(connection.first().asString) {
                            CircuitConstants.INPUT -> if (referencingConnection.first().asString == CircuitConstants.INPUT && referencingConnection[1].asString == functionInput.name)
                              throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${computationName}: {${CircuitConstants.CONNECT}: {${functionInput.name}: 'Value is mutated by computation ${referencingComputationName}:${referencingFunctionInput.name}'}}}}")
                            CircuitConstants.COMPUTATION -> if (referencingConnection.first().asString == CircuitConstants.COMPUTATION && referencingConnection[1].asString == computationName && referencingConnection[2].asString == functionInput.name)
                              throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${computationName}: {${CircuitConstants.CONNECT}: {${functionInput.name}: 'Value is mutated by computation ${referencingComputationName}:${referencingFunctionInput.name}'}}}}")
                            else -> throw CustomJsonException("{}")
                          }
                        }
                      }
                      CircuitConstants.MAPPER -> {}
                      CircuitConstants.CIRCUIT -> {
                        val referencingComputationCircuit: Circuit = circuits.single { it.name == referencingComputationJson.get(CircuitConstants.EXECUTE).asString }
                        referencingComputationCircuit.inputs.filter { isCircuitInputMutated(circuitInput = it, mutationCount = 0) != 0 }.forEach { referencingCircuitInput ->
                          val referencingConnection:JsonArray = referencingComputationJson.get(CircuitConstants.CONNECT).asJsonObject.get(referencingCircuitInput.name).asJsonArray
                          when(connection.first().asString) {
                            CircuitConstants.INPUT -> if (referencingConnection.first().asString == CircuitConstants.INPUT && referencingConnection[1].asString == functionInput.name)
                              throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${computationName}: {${CircuitConstants.CONNECT}: {${functionInput.name}: 'Value is mutated by computation ${referencingComputationName}:${referencingCircuitInput.name}'}}}}")
                            CircuitConstants.COMPUTATION -> if (referencingConnection.first().asString == CircuitConstants.COMPUTATION && referencingConnection[1].asString == computationName && referencingConnection[2].asString == functionInput.name)
                              throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${computationName}: {${CircuitConstants.CONNECT}: {${functionInput.name}: 'Value is mutated by computation ${referencingComputationName}:${referencingCircuitInput.name}'}}}}")
                            else -> throw CustomJsonException("{}")
                          }
                        }
                      }
                      else -> throw CustomJsonException("{}")
                    }
                  }
                  add(functionInput.name, when(connection.first().asString) {
                    CircuitConstants.INPUT -> JsonArray().apply {
                      add(CircuitConstants.INPUT)
                      add(inputs.entrySet().single { (inputName, json) -> inputName == connection[1].asString && types.single { !json.asJsonObject.get(KeyConstants.KEY_TYPE).isJsonArray && json.asJsonObject.get(KeyConstants.KEY_TYPE).asString == it.name } == functionInput.type }.key)
                    }
                    CircuitConstants.COMPUTATION -> JsonArray().apply {
                      add(CircuitConstants.COMPUTATION)
                      val (referencedComputationName: String, referencedComputation: JsonElement) = computations.entrySet().single { (name, json) ->
                        name == connection[1].asString && json.asJsonObject.get(CircuitConstants.ORDER).asInt < computationJson.get(CircuitConstants.ORDER).asInt }
                      computationLevels[computationName] = max(computationLevels[computationName]!!, 1 + computationLevels[referencedComputationName]!!)
                      add(referencedComputationName)
                      when(referencedComputation.asJsonObject.get(KeyConstants.KEY_TYPE).asString) {
                        CircuitConstants.FUNCTION -> add(functions.single { it.name == computationJson.get(CircuitConstants.EXECUTE).asString }.outputs
                          .single { it.name == connection[2].asString && it.type == functionInput.type && it.operation != FunctionConstants.DELETE }.name)
                        CircuitConstants.MAPPER -> throw CustomJsonException("{}")
                        CircuitConstants.CIRCUIT -> add(circuits.single { it.name == computationJson.get(CircuitConstants.EXECUTE).asString }.outputs
                          .single { it.name == connection[2].asString && getCircuitOutput(it).type == functionInput.type && getCircuitOutput(it).operation != FunctionConstants.DELETE }.name)
                        else -> throw CustomJsonException("{}")
                      }
                    }
                    else -> throw CustomJsonException("{}")
                  })
                }
              })
            }
            CircuitConstants.MAPPER -> {
              val mapper: Mapper = mappers.single { it.name == computationJson.get(CircuitConstants.EXECUTE).asString }
              addProperty(CircuitConstants.EXECUTE, mapper.name)
              add(CircuitConstants.CONNECT, JsonObject().apply {
                add(MapperConstants.QUERY_PARAMS, mapper.queryParams.fold(JsonObject()) { acc1, functionInput ->
                  acc1.apply {
                    val connection: JsonArray = computationJson.get(CircuitConstants.CONNECT).asJsonObject.get(MapperConstants.QUERY_PARAMS).asJsonObject.get(functionInput.name).asJsonArray
                    add(functionInput.name, when(connection.first().asString) {
                      CircuitConstants.INPUT -> JsonArray().apply {
                        add(CircuitConstants.INPUT)
                        add(inputs.entrySet().single { (inputName, json) -> inputName == connection[1].asString && !json.asJsonObject.get(KeyConstants.KEY_TYPE).isJsonArray && types.single { it.name == json.asJsonObject.get(KeyConstants.KEY_TYPE).asString } == functionInput.type }.key)
                      }
                      CircuitConstants.COMPUTATION -> JsonArray().apply {
                        add(CircuitConstants.COMPUTATION)
                        val (referencedComputationName: String, referencedComputation: JsonElement) = computations.entrySet().single { (name, json) ->
                          name == connection[1].asString && json.asJsonObject.get(CircuitConstants.ORDER).asInt < computationJson.get(CircuitConstants.ORDER).asInt }
                        computationLevels[computationName] = max(computationLevels[computationName]!!, 1 + computationLevels[referencedComputationName]!!)
                        add(referencedComputationName)
                        when(referencedComputation.asJsonObject.get(KeyConstants.KEY_TYPE).asString) {
                          CircuitConstants.FUNCTION -> add(functions.single { it.name == referencedComputation.asJsonObject.get(CircuitConstants.EXECUTE).asString }.outputs
                            .single { it.name == connection[2].asString && it.type == functionInput.type && it.operation != FunctionConstants.DELETE }.name)
                          CircuitConstants.MAPPER -> throw CustomJsonException("{}")
                          CircuitConstants.CIRCUIT -> add(circuits.single { it.name == referencedComputation.asJsonObject.get(CircuitConstants.EXECUTE).asString }.outputs
                            .single { it.name == connection[2].asString && getCircuitOutput(it).type == functionInput.type && getCircuitOutput(it).operation != FunctionConstants.DELETE }.name)
                          else -> throw CustomJsonException("{}")
                        }
                      }
                      else -> throw CustomJsonException("{}")
                    })
                  }
                })
                add(CircuitConstants.ARGS, JsonObject().apply {
                  val connection: JsonArray = computationJson.get(CircuitConstants.CONNECT).asJsonObject.get(CircuitConstants.ARGS).asJsonArray
                  computations.entrySet()
                    .filter { it.value.asJsonObject.get(CircuitConstants.ORDER).asInt < computationJson.get(CircuitConstants.ORDER).asInt && it.value.asJsonObject.get(KeyConstants.KEY_TYPE).asString == CircuitConstants.MAPPER }
                    .forEach { (referencingComputationName, referencingComputation) ->
                      val referencingComputationJson: JsonObject = referencingComputation.asJsonObject
                      val referencingComputationConnection: JsonArray = referencingComputationJson.get(CircuitConstants.ARGS).asJsonArray
                      if (connection.first().asString == referencingComputationConnection.first().asString && connection[1].asString == referencingComputationConnection[1].asString) {
                        val referencingComputationMapper: Mapper = mappers.single { it.name == referencingComputationJson.get(CircuitConstants.EXECUTE).asString }
                        referencingComputationMapper.functionInput.function.inputs.filter { it.variableName != null || it.values.isNotEmpty() }.forEach { referencingFunctionInput ->
                          if (mapper.query) {
                            if (referencingFunctionInput != mapper.functionInput)
                              throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${computationName}: {${CircuitConstants.ARGS}: 'Value is mutated by computation ${referencingComputationName}:${referencingFunctionInput.name}'}}}")
                          } else
                            throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${computationName}: {${CircuitConstants.ARGS}: 'Value is mutated by computation ${referencingComputationName}:${referencingFunctionInput.name}'}}}")
                        }
                      }
                  }
                  when(connection.first().asString) {
                    CircuitConstants.INPUT -> {
                      add(CircuitConstants.CONNECT, JsonArray().apply {
                        add(CircuitConstants.INPUT)
                        add(inputs.entrySet().single { (inputName, json) -> inputName == connection[1].asString && json.asJsonObject.get(KeyConstants.KEY_TYPE).isJsonArray }.key)
                      })
                    }
                    CircuitConstants.COMPUTATION -> {
                      add(CircuitConstants.CONNECT, JsonArray().apply {
                        add(CircuitConstants.COMPUTATION)
                        val (referencedComputationName: String, referencedComputation: JsonElement) = computations.entrySet().single { (name, json) ->
                          name == connection[1].asString && json.asJsonObject.get(CircuitConstants.ORDER).asInt < computationJson.get(CircuitConstants.ORDER).asInt
                              && json.asJsonObject.get(KeyConstants.KEY_TYPE).asString == CircuitConstants.MAPPER }
                        computationLevels[computationName] = max(computationLevels[computationName]!!, 1 + computationLevels[referencedComputationName]!!)
                        add(referencedComputationName)
                        val referencedComputationMapperFunction: Function = mappers.single { it.name == referencedComputation.asJsonObject.get(CircuitConstants.EXECUTE).asString }.functionInput.function
                        add(if (mapper.query) {
                          mapper.functionInput.function.inputs.filter { it != mapper.functionInput }.fold(JsonObject()) { acc1, functionInput ->
                            acc1.apply {
                              addProperty(functionInput.name, referencedComputationMapperFunction.outputs.single {
                                it.name == connection[2].asJsonObject.get(FunctionConstants.INPUTS).asJsonObject.get(functionInput.name).asString
                                    && it.type == functionInput.type && it.operation != FunctionConstants.DELETE }.name)
                            }
                          }
                        } else {
                          mapper.functionInput.function.inputs.fold(JsonObject()) { acc1, functionInput ->
                            acc1.apply {
                              addProperty(functionInput.name, referencedComputationMapperFunction.outputs.single {
                                it.name == connection[2].asJsonObject.get(FunctionConstants.INPUTS).asJsonObject.get(functionInput.name).asString
                                    && it.type == functionInput.type && it.operation != FunctionConstants.DELETE }.name)
                            }
                          }
                        })
                      })
                    }
                    else -> throw CustomJsonException("{}")
                  }
                })
              })
            }
            CircuitConstants.CIRCUIT -> {
              val circuit: Circuit = circuits.single { it.name == computationJson.get(CircuitConstants.EXECUTE).asString }
              addProperty(CircuitConstants.EXECUTE, circuit.name)
              add(CircuitConstants.CONNECT, circuit.inputs.fold(JsonObject()) { acc1, circuitInput ->
                acc1.apply {
                  val connection: JsonArray = computationJson.get(CircuitConstants.CONNECT).asJsonObject.get(circuitInput.name).asJsonArray
                  // Check if input is being mutated by any referencing computation that is already processed
                  computations.entrySet().filter { it.value.asJsonObject.get(CircuitConstants.ORDER).asInt < computationJson.get(CircuitConstants.ORDER).asInt }.forEach { (referencingComputationName, referencingComputation) ->
                    val referencingComputationJson: JsonObject = referencingComputation.asJsonObject
                    when(referencingComputationJson.get(KeyConstants.KEY_TYPE).asString) {
                      CircuitConstants.FUNCTION -> {
                        val referencingComputationFunction: Function = functions.single { it.name == referencingComputationJson.get(CircuitConstants.EXECUTE).asString }
                        referencingComputationFunction.inputs.filter { it.variableName != null || it.values.isNotEmpty() }.forEach { referencingFunctionInput ->
                          val referencingConnection:JsonArray = referencingComputationJson.get(CircuitConstants.CONNECT).asJsonObject.get(referencingFunctionInput.name).asJsonArray
                          when(connection.first().asString) {
                            CircuitConstants.INPUT -> if (referencingConnection.first().asString == CircuitConstants.INPUT && referencingConnection[1].asString == circuitInput.name)
                              throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${computationName}: {${CircuitConstants.CONNECT}: {${circuitInput.name}: 'Value is mutated by computation ${referencingComputationName}:${referencingFunctionInput.name}'}}}}")
                            CircuitConstants.COMPUTATION -> if (referencingConnection.first().asString == CircuitConstants.COMPUTATION && referencingConnection[1].asString == computationName && referencingConnection[2].asString == circuitInput.name)
                              throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${computationName}: {${CircuitConstants.CONNECT}: {${circuitInput.name}: 'Value is mutated by computation ${referencingComputationName}:${referencingFunctionInput.name}'}}}}")
                            else -> throw CustomJsonException("{}")
                          }
                        }
                      }
                      CircuitConstants.MAPPER -> {}
                      CircuitConstants.CIRCUIT -> {
                        val referencingComputationCircuit: Circuit = circuits.single { it.name == referencingComputationJson.get(CircuitConstants.EXECUTE).asString }
                        referencingComputationCircuit.inputs.filter { isCircuitInputMutated(circuitInput = it, mutationCount = 0) != 0 }.forEach { referencingCircuitInput ->
                          val referencingConnection:JsonArray = referencingComputationJson.get(CircuitConstants.CONNECT).asJsonObject.get(referencingCircuitInput.name).asJsonArray
                          when(connection.first().asString) {
                            CircuitConstants.INPUT -> if (referencingConnection.first().asString == CircuitConstants.INPUT && referencingConnection[1].asString == circuitInput.name)
                              throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${computationName}: {${CircuitConstants.CONNECT}: {${circuitInput.name}: 'Value is mutated by computation ${referencingComputationName}:${referencingCircuitInput.name}'}}}}")
                            CircuitConstants.COMPUTATION -> if (referencingConnection.first().asString == CircuitConstants.COMPUTATION && referencingConnection[1].asString == computationName && referencingConnection[2].asString == circuitInput.name)
                              throw CustomJsonException("{${CircuitConstants.COMPUTATIONS}: {${computationName}: {${CircuitConstants.CONNECT}: {${circuitInput.name}: 'Value is mutated by computation ${referencingComputationName}:${referencingCircuitInput.name}'}}}}")
                            else -> throw CustomJsonException("{}")
                          }
                        }
                      }
                      else -> throw CustomJsonException("{}")
                    }
                  }
                  add(circuitInput.name, when(connection.first().asString) {
                    CircuitConstants.INPUT -> JsonArray().apply {
                      add(CircuitConstants.INPUT)
                      add(inputs.entrySet().single { (inputName, json) -> inputName == connection[1].asString && types.single { !json.asJsonObject.get(KeyConstants.KEY_TYPE).isJsonArray && json.asJsonObject.get(KeyConstants.KEY_TYPE).asString == it.name } == circuitInput.type }.key)
                    }
                    CircuitConstants.COMPUTATION -> JsonArray().apply {
                      add(CircuitConstants.COMPUTATION)
                      val (referencedComputationName: String, referencedComputation: JsonElement) = computations.entrySet().single { (name, json) ->
                        name == connection[1].asString && json.asJsonObject.get(CircuitConstants.ORDER).asInt < computationJson.get(CircuitConstants.ORDER).asInt }
                      computationLevels[computationName] = max(computationLevels[computationName]!!, 1 + computationLevels[referencedComputationName]!!)
                      add(referencedComputationName)
                      when(referencedComputation.asJsonObject.get(KeyConstants.KEY_TYPE).asString) {
                        CircuitConstants.FUNCTION -> add(functions.single { it.name == computationJson.get(CircuitConstants.EXECUTE).asString }.outputs
                          .single { it.name == connection[2].asString && it.type == circuitInput.type && it.operation != FunctionConstants.DELETE }.name)
                        CircuitConstants.MAPPER -> throw CustomJsonException("{}")
                        CircuitConstants.CIRCUIT -> add(circuits.single { it.name == computationJson.get(CircuitConstants.EXECUTE).asString }.outputs
                          .single { it.name == connection[2].asString && getCircuitOutput(it).type == circuitInput.type && getCircuitOutput(it).operation != FunctionConstants.DELETE }.name)
                        else -> throw CustomJsonException("{}")
                      }
                    }
                    else -> throw CustomJsonException("{}")
                  })
                }
              })
            }
            else -> throw CustomJsonException("{}")
          }
          addProperty(CircuitConstants.LEVEL, computationLevels[computationName]!!)
        })
      }
  }
}

fun getCircuitOutput(circuitOutput: CircuitOutput): FunctionOutput {
  return if (circuitOutput.connectedCircuitComputation.function != null)
    circuitOutput.connectedCircuitComputationFunctionOutput!!
  else
    getCircuitOutput(circuitOutput.connectedCircuitComputationCircuitOutput!!)
}

fun isCircuitInputMutated(circuitInput: CircuitInput, mutationCount: Int): Int {
  return circuitInput.referencingCircuitComputationConnections.fold(mutationCount) { acc, referencingCircuitComputationConnection ->
    when (referencingCircuitComputationConnection.parentComputation.function != null) {
      true -> {
        val functionInput: FunctionInput = referencingCircuitComputationConnection.functionInput!!
        if (functionInput.variableName != null || functionInput.values.isNotEmpty())
          1 + acc
        else acc
      }
      false -> isCircuitInputMutated(circuitInput = referencingCircuitComputationConnection.circuitInput!!, mutationCount = acc)
    }
  }
}

fun validateCircuitOutputs(outputs: JsonObject, computations: JsonObject): JsonObject = outputs.entrySet().fold(JsonObject()) { acc, (outputName, output) ->
  acc.apply {
    if (!keyIdentifierPattern.matcher(outputName).matches())
      throw CustomJsonException("{${CircuitConstants.OUTPUTS}: {$outputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
    else {
      add(outputName, JsonArray().apply {
        val (computationName, computation) = computations.entrySet().single {it.key == output.asJsonArray.first().asString }
        add(computationName)
        val computationJson: JsonObject = computation.asJsonObject
        when(computationJson.get(KeyConstants.KEY_TYPE).asString) {
          CircuitConstants.MAPPER -> {}
          else -> add(computationJson.get(CircuitConstants.CONNECT).asJsonObject.keySet().single { it == output.asJsonArray[1].asString })
        }
      })
    }
  }
}

fun validateCircuit(jsonParams: JsonObject, types: Set<Type>, functions: Set<Function>, mappers: Set<Mapper>, circuits: Set<Circuit>, files: List<MultipartFile>): Quadruple<String, JsonObject, JsonObject, JsonObject> {
  val inputs: JsonObject = validateCircuitInputs(inputs = jsonParams.get(CircuitConstants.INPUTS).asJsonObject, types = types, files = files)
  val computations: JsonObject = validateCircuitComputations(computations = jsonParams.get(CircuitConstants.COMPUTATIONS).asJsonObject,
    inputs = inputs, types = types, functions = functions, mappers = mappers, circuits = circuits)
  val outputs: JsonObject = validateCircuitOutputs(outputs = jsonParams.get(CircuitConstants.OUTPUTS).asJsonObject, computations = computations)
  return Quadruple(validateCircuitName(circuitName = jsonParams.get(CircuitConstants.CIRCUIT_NAME).asString), inputs, computations, outputs)
}

fun validateCircuitArgs(args: JsonObject, inputs: Set<CircuitInput>, defaultTimestamp: Timestamp, files: List<MultipartFile>): JsonObject = inputs.fold(JsonObject()) { acc, input ->
  acc.apply {
    try {
      if (input.type != null)
        when (input.type.name) {
          TypeConstants.TEXT -> addProperty(input.name, if (args.has(input.name)) args.get(input.name).asString else input.defaultStringValue!!)
          TypeConstants.NUMBER -> addProperty(input.name, if (args.has(input.name)) args.get(input.name).asLong else input.defaultLongValue!!)
          TypeConstants.DECIMAL -> addProperty(input.name, if (args.has(input.name)) args.get(input.name).asBigDecimal else input.defaultDecimalValue!!)
          TypeConstants.BOOLEAN -> addProperty(input.name, if (args.has(input.name)) args.get(input.name).asBoolean else input.defaultBooleanValue!!)
          TypeConstants.DATE -> addProperty(input.name, if (args.has(input.name)) java.sql.Date(args.get(input.name).asLong).time
          else if (input.defaultDateValue != null) input.defaultDateValue!!.time else java.sql.Date(defaultTimestamp.time).time)
          TypeConstants.TIMESTAMP -> addProperty(input.name, if (args.has(input.name)) Timestamp(args.get(input.name).asLong).time
          else if (input.defaultTimestampValue != null) input.defaultTimestampValue!!.time else defaultTimestamp.time)
          TypeConstants.TIME -> addProperty(input.name, if (args.has(input.name)) java.sql.Time(args.get(input.name).asLong).time
          else if (input.defaultTimeValue != null) input.defaultTimeValue!!.time else java.sql.Time(defaultTimestamp.time).time)
          TypeConstants.BLOB -> {
            val fileIndex: Int = args.get(input.name).asInt
            if (fileIndex < 0 && fileIndex > (files.size - 1))
              throw CustomJsonException("{}")
            else
              addProperty(input.name, fileIndex)
          }
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> addProperty(input.name, if (args.has(input.name)) args.get(input.name).asString else input.referencedVariable!!.name)
        }
      else
        add(input.name, args.get(input.name).asJsonArray)

    } catch (exception: Exception) {
      throw CustomJsonException("{${CircuitConstants.ARGS}: {${input.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
    }
  }
}
