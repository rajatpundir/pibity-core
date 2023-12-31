/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.services

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.entities.Organization
import com.pibity.core.entities.Type
import com.pibity.core.entities.circuit.*
import com.pibity.core.entities.function.Function
import com.pibity.core.entities.function.FunctionInput
import com.pibity.core.entities.function.Mapper
import com.pibity.core.repositories.circuit.CircuitRepository
import com.pibity.core.repositories.circuit.jpa.*
import com.pibity.core.repositories.function.FunctionRepository
import com.pibity.core.repositories.function.MapperRepository
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.query.TypeRepository
import com.pibity.core.repositories.query.VariableRepository
import com.pibity.core.commons.Base64DecodedMultipartFile
import com.pibity.core.entities.accumulator.VariableAccumulator
import com.pibity.core.entities.uniqueness.TypeUniqueness
import com.pibity.core.repositories.query.TypeUniquenessRepository
import com.pibity.core.repositories.query.VariableAccumulatorRepository
import com.pibity.core.utils.computeHash
import com.pibity.core.utils.getCircuitOutput
import com.pibity.core.utils.validateCircuit
import com.pibity.core.utils.validateCircuitArgs
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.util.*

@Service
class CircuitService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val typeRepository: TypeRepository,
    val variableRepository: VariableRepository,
    val functionRepository: FunctionRepository,
    val mapperRepository: MapperRepository,
    val circuitRepository: CircuitRepository,
    val circuitJpaRepository: CircuitJpaRepository,
    val circuitInputJpaRepository: CircuitInputJpaRepository,
    val circuitComputationJpaRepository: CircuitComputationJpaRepository,
    val circuitComputationConnectionJpaRepository: CircuitComputationConnectionJpaRepository,
    val circuitOutputJpaRepository: CircuitOutputJpaRepository,
    val functionService: FunctionService,
    val mapperService: MapperService,
    val typeUniquenessRepository: TypeUniquenessRepository,
    val circuitComputationConnectionAccumulatorKeyJpaRepository: CircuitComputationConnectionAccumulatorKeyJpaRepository,
    val variableAccumulatorRepository: VariableAccumulatorRepository
) {

  fun createCircuit(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): Circuit {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
        ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val types: Set<Type> = typeRepository.findTypes(orgId = organization.id)
    val functions: Set<Function> = functionRepository.findFunctions(orgId = organization.id)
    val mappers: Set<Mapper> = mapperRepository.findMappers(orgId = organization.id)
    val circuits: Set<Circuit> = circuitRepository.findCircuits(orgId = organization.id)
    val allTypeUniqueness: Set<TypeUniqueness> = typeUniquenessRepository.findAllTypeUniqueness(orgId = organization.id)
    val (circuitName: String, inputs: JsonObject, computations: JsonObject, outputs: JsonObject) = validateCircuit(jsonParams = jsonParams, types = types, functions = functions, mappers = mappers, circuits = circuits, allTypeUniqueness = allTypeUniqueness, files = files)
    val circuit: Circuit = circuitJpaRepository.save(Circuit(organization = organization, name = circuitName, created = defaultTimestamp))
    circuit.inputs.addAll(inputs.entrySet().map { (inputName, input) ->
      val inputJson: JsonObject = input.asJsonObject
      if (inputJson.get(KeyConstants.KEY_TYPE).isJsonArray) {
        circuitInputJpaRepository.save(CircuitInput(parentCircuit = circuit, name = inputName, type = null, created = defaultTimestamp))
      } else {
        val inputType: Type = types.single { it.name == inputJson.get(KeyConstants.KEY_TYPE).asString }
        circuitInputJpaRepository.save(CircuitInput(parentCircuit = circuit, name = inputName, type = inputType, created = defaultTimestamp).apply {
          when(inputType.name) {
            TypeConstants.TEXT -> defaultStringValue = if (inputJson.has(KeyConstants.DEFAULT)) inputJson.get(KeyConstants.DEFAULT).asString else ""
            TypeConstants.NUMBER -> defaultLongValue = if (inputJson.has(KeyConstants.DEFAULT)) inputJson.get(KeyConstants.DEFAULT).asLong else 0
            TypeConstants.DECIMAL -> defaultDecimalValue = if (inputJson.has(KeyConstants.DEFAULT)) inputJson.get(KeyConstants.DEFAULT).asBigDecimal else (0.0).toBigDecimal()
            TypeConstants.BOOLEAN -> defaultBooleanValue = if (inputJson.has(KeyConstants.DEFAULT)) inputJson.get(KeyConstants.DEFAULT).asBoolean else false
            TypeConstants.DATE -> defaultDateValue = if (inputJson.has(KeyConstants.DEFAULT)) Date(inputJson.get(KeyConstants.DEFAULT).asLong) else null
            TypeConstants.TIMESTAMP -> defaultTimestampValue = if (inputJson.has(KeyConstants.DEFAULT)) Timestamp(inputJson.get(KeyConstants.DEFAULT).asLong) else null
            TypeConstants.TIME -> defaultTimeValue = if (inputJson.has(KeyConstants.DEFAULT)) Time(inputJson.get(KeyConstants.DEFAULT).asLong) else null
            TypeConstants.BLOB -> defaultBlobValue = if (inputJson.has(KeyConstants.DEFAULT)) BlobProxy.generateProxy(files[inputJson.get(KeyConstants.DEFAULT).asInt].bytes) else BlobProxy.generateProxy(Base64.getEncoder().encode("".toByteArray()))
            TypeConstants.FORMULA -> throw CustomJsonException("{}")
            else -> if (inputJson.has(KeyConstants.DEFAULT)) {
              referencedVariable = variableRepository.findVariable(type = inputType, name = inputJson.get(KeyConstants.DEFAULT).asString)
                ?: throw CustomJsonException("{${CircuitConstants.INPUTS}: {$inputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
            }
          }
        })
      }
    })
    computations.entrySet().sortedBy { it.value.asJsonObject.get(CircuitConstants.ORDER).asInt }.forEach { (computationName, computation) ->
      val computationJson: JsonObject = computation.asJsonObject
      circuit.computations.add(when(computationJson.get(KeyConstants.KEY_TYPE).asString) {
        CircuitConstants.FUNCTION -> {
          val computationFunction: Function = functions.single { it.name == computationJson.get(CircuitConstants.EXECUTE).asString }
          circuitComputationJpaRepository.save(CircuitComputation(parentCircuit = circuit, name = computationName, order = computationJson.get(CircuitConstants.ORDER).asInt, level = computationJson.get(CircuitConstants.LEVEL).asInt, function = computationFunction, created = defaultTimestamp)).apply {
            connections.addAll(computationFunction.inputs.map { functionInput ->
              val connection: JsonArray = computationJson.get(CircuitConstants.CONNECT).asJsonObject.get(functionInput.name).asJsonArray
              when(connection.first().asString) {
                CircuitConstants.INPUT -> CircuitComputationConnection(parentComputation = this, functionInput = functionInput, connectedCircuitInput = circuit.inputs.single { it.name == connection[1].asString }, created = defaultTimestamp)
                CircuitConstants.COMPUTATION -> {
                  val connectedCircuitComputation = circuit.computations.single { it.name == connection[1].asString }
                  if (connectedCircuitComputation.function != null)
                    circuitComputationConnectionJpaRepository.save(CircuitComputationConnection(parentComputation = this, functionInput = functionInput, connectedCircuitComputation = connectedCircuitComputation, connectedCircuitComputationFunctionOutput = connectedCircuitComputation.function.outputs.single { it.name == connection[2].asString && it.type == functionInput.type }, created = defaultTimestamp))
                  else if (connectedCircuitComputation.circuit != null)
                    circuitComputationConnectionJpaRepository.save(CircuitComputationConnection(parentComputation = this, functionInput = functionInput, connectedCircuitComputation = connectedCircuitComputation, connectedCircuitComputationCircuitOutput = connectedCircuitComputation.circuit.outputs.single { it.name == connection[2].asString && getCircuitOutput(it).type == functionInput.type }, created = defaultTimestamp))
                  else circuitComputationConnectionJpaRepository.save(CircuitComputationConnection(parentComputation = this, functionInput = functionInput, connectedCircuitComputation = connectedCircuitComputation,
                    connectedCircuitComputationTypeAccumulator = allTypeUniqueness.single { it.type == connectedCircuitComputation.mapper!!.functionInput.function.outputs.single { output -> output.name == connection[2].asString }.type && it.name == connection[3].asString }
                      .accumulators.single { it.name == connection[4].asString && it.type == functionInput.type } , created = defaultTimestamp)).apply {
                    connectedCircuitComputationAccumulatorKeys.addAll(circuitComputationConnectionAccumulatorKeyJpaRepository.saveAll(this.connectedCircuitComputationTypeAccumulator!!.keys.map { key ->
                      CircuitComputationConnectionAccumulatorKey(parentComputationConnection = this, key = key,
                        circuitInput = circuit.inputs.single { it.name == connection[5].asJsonObject.get(key.name).asString && it.type == key.type}, created = defaultTimestamp)
                    }))
                  }
                }
                else -> throw CustomJsonException("{}")
              }
            })
          }
        }
        CircuitConstants.MAPPER -> {
          val computationMapper: Mapper = mappers.single { it.name == computationJson.get(CircuitConstants.EXECUTE).asString }
          circuitComputationJpaRepository.save(CircuitComputation(parentCircuit = circuit, name = computationName, order = computationJson.get(CircuitConstants.ORDER).asInt, level = computationJson.get(CircuitConstants.LEVEL).asInt,
            mapper = computationMapper, created = defaultTimestamp).apply {
            val argsConnection: JsonArray = computationJson.get(CircuitConstants.CONNECT).asJsonObject.get(CircuitConstants.ARGS).asJsonArray
            when(argsConnection.first().asString) {
              CircuitConstants.INPUT -> connectedMapperCircuitInput = circuit.inputs.single { it.name == argsConnection[1].asString && it.type == null }
              CircuitConstants.COMPUTATION -> connectedMapperCircuitComputation = circuit.computations.single { it.name == argsConnection[1].asString && it.mapper != null }
              else -> throw CustomJsonException("{}")
            }
          }).apply {
            connections.addAll(computationMapper.queryParams.map { functionInput ->
              val connection: JsonArray = computationJson.get(CircuitConstants.CONNECT).asJsonObject.get(MapperConstants.QUERY_PARAMS).asJsonObject.get(functionInput.name).asJsonArray
              when(connection.first().asString) {
                CircuitConstants.INPUT -> CircuitComputationConnection(parentComputation = this, functionInput = functionInput, connectedCircuitInput = circuit.inputs.single { it.name == connection[1].asString && it.type == null }, created = defaultTimestamp)
                CircuitConstants.COMPUTATION -> {
                  val connectedCircuitComputation = circuit.computations.single { it.name == connection[1].asString && it.mapper != null }
                  if (connectedCircuitComputation.function != null)
                    CircuitComputationConnection(parentComputation = this, functionInput = functionInput, connectedCircuitComputation = connectedCircuitComputation, connectedCircuitComputationFunctionOutput = connectedCircuitComputation.function.outputs.single { it.name == connection[2].asString }, created = defaultTimestamp)
                  else CircuitComputationConnection(parentComputation = this, functionInput = functionInput, connectedCircuitComputation = connectedCircuitComputation,connectedCircuitComputationCircuitOutput = connectedCircuitComputation.circuit!!.outputs.single { it.name == connection[2].asString }, created = defaultTimestamp)
                }
                else -> throw CustomJsonException("{}")
              }
            })
            val argsConnection: JsonArray = computationJson.get(CircuitConstants.CONNECT).asJsonObject.get(CircuitConstants.ARGS).asJsonArray
            if (argsConnection.first().asString == CircuitConstants.COMPUTATION) {
              val referencedComputationMapperFunction = circuit.computations.single { it.name == argsConnection[1].asString }.mapper!!.functionInput.function
              connectedMapperCircuitComputationConnections.addAll(if (computationMapper.query) {
                computationMapper.functionInput.function.inputs.filter { it != computationMapper.functionInput }.map { functionInput ->
                  CircuitComputationMapperConnection(parentComputation = this, functionInput = functionInput,
                    referencedMapperFunctionOutput = referencedComputationMapperFunction.outputs.single { it.name == argsConnection[2].asJsonObject.get(functionInput.name).asString
                        && it.type == functionInput.type && it.operation != FunctionConstants.DELETE }, created = defaultTimestamp)
                }
              } else {
                computationMapper.functionInput.function.inputs.map { functionInput ->
                  CircuitComputationMapperConnection(parentComputation = this, functionInput = functionInput,
                    referencedMapperFunctionOutput = referencedComputationMapperFunction.outputs.single { it.name == argsConnection[2].asJsonObject.get(functionInput.name).asString
                        && it.type == functionInput.type && it.operation != FunctionConstants.DELETE }, created = defaultTimestamp)
                }
              })
            }
          }
        }
        CircuitConstants.CIRCUIT -> {
          val computationCircuit: Circuit = circuits.single { it.name == computationJson.get(CircuitConstants.EXECUTE).asString }
          circuitComputationJpaRepository.save(CircuitComputation(parentCircuit = circuit, name = computationName, order = computationJson.get(CircuitConstants.ORDER).asInt, level = computationJson.get(CircuitConstants.LEVEL).asInt, circuit = computationCircuit, created = defaultTimestamp)).apply {
            connections.addAll(circuitComputationConnectionJpaRepository.saveAll(computationCircuit.inputs.map { circuitInput ->
              val connection: JsonArray = computationJson.get(CircuitConstants.CONNECT).asJsonObject.get(circuitInput.name).asJsonArray
              when(connection.first().asString) {
                CircuitConstants.INPUT -> CircuitComputationConnection(parentComputation = this, circuitInput = circuitInput, connectedCircuitInput = circuit.inputs.single { it.name == connection[1].asString }, created = defaultTimestamp)
                CircuitConstants.COMPUTATION -> {
                  val connectedCircuitComputation: CircuitComputation = computationCircuit.computations.single { it.name == connection[1].asString }
                  if (connectedCircuitComputation.function != null)
                    CircuitComputationConnection(parentComputation = this, circuitInput = circuitInput, connectedCircuitComputation = connectedCircuitComputation, connectedCircuitComputationFunctionOutput = connectedCircuitComputation.function.outputs.single { it.name == connection[2].asString && it.type == circuitInput.type }, created = defaultTimestamp)
                  else if (connectedCircuitComputation.circuit != null)
                    CircuitComputationConnection(parentComputation = this, circuitInput = circuitInput, connectedCircuitComputation = connectedCircuitComputation, connectedCircuitComputationCircuitOutput = connectedCircuitComputation.circuit.outputs.single { it.name == connection[2].asString && getCircuitOutput(it).type == circuitInput.type }, created = defaultTimestamp)
                  else circuitComputationConnectionJpaRepository.save(CircuitComputationConnection(parentComputation = this, circuitInput = circuitInput, connectedCircuitComputation = connectedCircuitComputation,
                    connectedCircuitComputationTypeAccumulator = allTypeUniqueness.single { it.type == connectedCircuitComputation.mapper!!.functionInput.function.outputs.single { output -> output.name == connection[2].asString }.type && it.name == connection[3].asString }
                      .accumulators.single { it.name == connection[4].asString && it.type == circuitInput.type } , created = defaultTimestamp)).apply {
                    connectedCircuitComputationAccumulatorKeys.addAll(circuitComputationConnectionAccumulatorKeyJpaRepository.saveAll(this.connectedCircuitComputationTypeAccumulator!!.keys.map { key ->
                      CircuitComputationConnectionAccumulatorKey(parentComputationConnection = this, key = key,
                        circuitInput = circuit.inputs.single { it.name == connection[5].asJsonObject.get(key.name).asString && it.type == key.type}, created = defaultTimestamp)
                    }))
                  }
                }
                else -> throw CustomJsonException("{}")
              }
            }))
          }
        }
        else -> throw CustomJsonException("{}")
      })
    }
    circuit.outputs.addAll(outputs.entrySet().map { (outputName, output) ->
      val outputJson: JsonArray = output.asJsonArray
      val outputComputation: CircuitComputation = circuit.computations.single { it.name == outputJson.first().asString }
      circuitOutputJpaRepository.save(if (outputComputation.function != null)
        CircuitOutput(parentCircuit = circuit, name = outputName, connectedCircuitComputation = outputComputation, connectedCircuitComputationFunctionOutput = outputComputation.function.outputs.single { it.name == outputJson[1].asString }, created = defaultTimestamp)
      else if (outputComputation.circuit != null)
        CircuitOutput(parentCircuit = circuit, name = outputName, connectedCircuitComputation = outputComputation, connectedCircuitComputationCircuitOutput = outputComputation.circuit.outputs.single { it.name == outputJson[1].asString }, created = defaultTimestamp)
      else
        CircuitOutput(parentCircuit = circuit, name = outputName, connectedCircuitComputation = outputComputation, created = defaultTimestamp))
    })
    return circuit
  }

  fun getMatchingVariableAccumulator(args: JsonObject, connection: CircuitComputationConnection, variableAccumulator: VariableAccumulator, files: List<MultipartFile>): VariableAccumulator? {
    return if (variableAccumulator.values.all { valueAccumulator ->
        val accumulatorValueCircuitInput: CircuitInput = connection.connectedCircuitComputationAccumulatorKeys.single { it.key == valueAccumulator.key }.circuitInput
        when(valueAccumulator.key.type.name) {
          TypeConstants.TEXT -> valueAccumulator.stringValue!! == args.get(accumulatorValueCircuitInput.name).asString
          TypeConstants.NUMBER -> valueAccumulator.longValue!! == args.get(accumulatorValueCircuitInput.name).asLong
          TypeConstants.DECIMAL -> valueAccumulator.decimalValue!! == args.get(accumulatorValueCircuitInput.name).asBigDecimal
          TypeConstants.BOOLEAN -> valueAccumulator.booleanValue!! == args.get(accumulatorValueCircuitInput.name).asBoolean
          TypeConstants.DATE -> valueAccumulator.dateValue!! == Date(args.get(accumulatorValueCircuitInput.name).asLong)
          TypeConstants.TIMESTAMP -> valueAccumulator.timestampValue!! == Timestamp(args.get(accumulatorValueCircuitInput.name).asLong)
          TypeConstants.TIME -> valueAccumulator.timeValue!! == Time(args.get(accumulatorValueCircuitInput.name).asLong)
          TypeConstants.BLOB -> valueAccumulator.blobValue!!.getBytes(1, valueAccumulator.blobValue!!.length().toInt()).contentEquals(files[args.get(accumulatorValueCircuitInput.name).asInt].bytes)
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> valueAccumulator.referencedVariable!! == variableRepository.findVariable(type = valueAccumulator.key.type, name = args.get(accumulatorValueCircuitInput.name).asString)!!
        }
      }) variableAccumulator
    else if (variableAccumulator.nextVariableAccumulator != null) {
      getMatchingVariableAccumulator(args = args, connection = connection, variableAccumulator = variableAccumulator.nextVariableAccumulator!!, files = files)
    } else null
  }

  fun executeCircuit(jsonParams: JsonObject, files: MutableList<MultipartFile>, defaultTimestamp: Timestamp): JsonObject {
    val circuit: Circuit = circuitRepository.findCircuit(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(CircuitConstants.CIRCUIT_NAME).asString)
        ?: throw CustomJsonException("{${CircuitConstants.CIRCUIT_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val args: JsonObject = validateCircuitArgs(args = jsonParams.get(CircuitConstants.ARGS).asJsonObject, inputs = circuit.inputs, files = files, defaultTimestamp = defaultTimestamp)
    val computationResults: MutableMap<String, JsonElement> = mutableMapOf()
    return circuit.computations.fold(sortedMapOf<Int, MutableSet<CircuitComputation>>()) { acc, computation ->
      acc.apply {
        if (this.containsKey(computation.level))
          this[computation.level]!!.add(computation)
        else
          this[computation.level] = mutableSetOf(computation)
      }
    }.entries.fold(JsonObject()) { acc, (_, computations) ->
      acc.apply {
        computations.forEach { computation ->
          if(computation.function != null) {
            val computationFunction: Function = computation.function
            computationResults[computation.name] = functionService.executeFunction(jsonParams = JsonObject().apply {
              addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
              addProperty(FunctionConstants.FUNCTION_NAME, computationFunction.name)
              add(FunctionConstants.ARGS, computation.connections.fold(JsonObject()) { acc1, connection ->
                acc1.apply {
                  val input: FunctionInput = connection.functionInput!!
                  if (connection.connectedCircuitInput != null)
                    when(connection.functionInput.type.name) {
                      TypeConstants.TEXT -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asString)
                      TypeConstants.NUMBER -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.DECIMAL -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asBigDecimal)
                      TypeConstants.BOOLEAN -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asBoolean)
                      TypeConstants.DATE -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.TIMESTAMP -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.TIME -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.BLOB -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asInt)
                      TypeConstants.FORMULA -> throw CustomJsonException("{}")
                      else -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asString)
                    }
                  else if (connection.connectedCircuitComputation!!.function != null)
                    when(connection.connectedCircuitComputationFunctionOutput!!.type.name) {
                      TypeConstants.TEXT -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asString)
                      TypeConstants.NUMBER -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.DECIMAL -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asBigDecimal)
                      TypeConstants.BOOLEAN -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asBoolean)
                      TypeConstants.DATE -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.TIMESTAMP -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.TIME -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.BLOB -> addProperty(input.name, files.apply { add(Base64DecodedMultipartFile(Base64.getDecoder().decode(computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asString))) }.size - 1)
                      TypeConstants.FORMULA -> throw CustomJsonException("{}")
                      else -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asJsonObject.get(VariableConstants.VARIABLE_NAME).asString)
                    }
                  else if (connection.connectedCircuitComputation.circuit != null)
                    when(getCircuitOutput(connection.connectedCircuitComputationCircuitOutput!!).type.name) {
                      TypeConstants.TEXT -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asString)
                      TypeConstants.NUMBER -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                      TypeConstants.DECIMAL -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asBigDecimal)
                      TypeConstants.BOOLEAN -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asBoolean)
                      TypeConstants.DATE -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                      TypeConstants.TIMESTAMP -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                      TypeConstants.TIME -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                      TypeConstants.BLOB -> addProperty(input.name, files.apply { add(Base64DecodedMultipartFile(Base64.getDecoder().decode(computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asString))) }.size - 1)
                      TypeConstants.FORMULA -> throw CustomJsonException("{}")
                      else -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asJsonObject.get(VariableConstants.VARIABLE_NAME).asString)
                    }
                  else {
                    val computedHash: String = computeHash(connection.connectedCircuitComputationTypeAccumulator!!.keys.sortedBy { it.id }.fold("") { acc2, key ->
                      val accumulatorValueCircuitInput: CircuitInput = connection.connectedCircuitComputationAccumulatorKeys.single { it.key == key }.circuitInput
                      acc2 + when (key.type.name) {
                        TypeConstants.TEXT -> args.get(accumulatorValueCircuitInput.name).asString
                        TypeConstants.NUMBER -> args.get(accumulatorValueCircuitInput.name).asLong
                        TypeConstants.DECIMAL -> args.get(accumulatorValueCircuitInput.name).asBigDecimal.toString()
                        TypeConstants.BOOLEAN -> args.get(accumulatorValueCircuitInput.name).asBoolean.toString()
                        TypeConstants.DATE -> Date(args.get(accumulatorValueCircuitInput.name).asLong).toString()
                        TypeConstants.TIMESTAMP -> Timestamp(args.get(accumulatorValueCircuitInput.name).asLong).toString()
                        TypeConstants.TIME -> Time(args.get(accumulatorValueCircuitInput.name).asLong).toString()
                        TypeConstants.BLOB -> Base64.getEncoder().encodeToString(files[args.get(accumulatorValueCircuitInput.name).asInt].bytes)
                        TypeConstants.FORMULA -> throw CustomJsonException("{}")
                        else -> (variableRepository.findVariable(type = key.type, name = args.get(accumulatorValueCircuitInput.name).asString)
                          ?: throw CustomJsonException("{${CircuitConstants.ARGS}: {${accumulatorValueCircuitInput.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")).id
                      }
                    })
                    val rootVariableAccumulator: VariableAccumulator? = variableAccumulatorRepository.findVariableAccumulator(typeAccumulator = connection.connectedCircuitComputationTypeAccumulator, level = 0, hash = computedHash)
                    if (rootVariableAccumulator == null)
                      throw CustomJsonException("{}")
                    else {
                      val matchingVariableAccumulator: VariableAccumulator? = getMatchingVariableAccumulator(args = args, connection = connection, variableAccumulator = rootVariableAccumulator, files = files)
                      if(matchingVariableAccumulator == null)
                        throw CustomJsonException("{}")
                      else {
                        when(connection.connectedCircuitComputationTypeAccumulator.type.name) {
                          TypeConstants.TEXT -> addProperty(input.name, matchingVariableAccumulator.stringValue!!)
                          TypeConstants.NUMBER -> addProperty(input.name, matchingVariableAccumulator.longValue!!)
                          TypeConstants.DECIMAL -> addProperty(input.name, matchingVariableAccumulator.decimalValue!!)
                          TypeConstants.BOOLEAN -> addProperty(input.name, matchingVariableAccumulator.booleanValue!!)
                          TypeConstants.DATE -> addProperty(input.name, matchingVariableAccumulator.dateValue!!.time)
                          TypeConstants.TIMESTAMP -> addProperty(input.name, matchingVariableAccumulator.timestampValue!!.time)
                          TypeConstants.TIME -> addProperty(input.name, matchingVariableAccumulator.timeValue!!.time)
                          TypeConstants.BLOB -> addProperty(input.name, files.apply { add(Base64DecodedMultipartFile(matchingVariableAccumulator.blobValue!!.getBytes(1, matchingVariableAccumulator.blobValue!!.length().toInt()))) }.size - 1)
                          TypeConstants.FORMULA -> throw CustomJsonException("{}")
                          else -> addProperty(input.name, matchingVariableAccumulator.referencedVariable!!.name)
                        }
                      }
                    }
                  }
                }
              })
            }, files = files, defaultTimestamp = defaultTimestamp)
          } else if (computation.circuit != null) {
            val computationCircuit: Circuit = computation.circuit
            computationResults[computation.name] = executeCircuit(jsonParams = JsonObject().apply {
              addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
              addProperty(FunctionConstants.FUNCTION_NAME, computationCircuit.name)
              add(FunctionConstants.ARGS, computation.connections.fold(JsonObject()) { acc1, connection ->
                acc1.apply {
                  val input: CircuitInput = connection.circuitInput!!
                  if (connection.connectedCircuitInput != null)
                    when(connection.circuitInput.type!!.name) {
                      TypeConstants.TEXT -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asString)
                      TypeConstants.NUMBER -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.DECIMAL -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asBigDecimal)
                      TypeConstants.BOOLEAN -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asBoolean)
                      TypeConstants.DATE -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.TIMESTAMP -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.TIME -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.BLOB -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asInt)
                      TypeConstants.FORMULA -> throw CustomJsonException("{}")
                      else -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asString)
                    }
                  else if (connection.connectedCircuitComputation!!.function != null)
                    when(connection.connectedCircuitComputationFunctionOutput!!.type.name) {
                      TypeConstants.TEXT -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asString)
                      TypeConstants.NUMBER -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.DECIMAL -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asBigDecimal)
                      TypeConstants.BOOLEAN -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asBoolean)
                      TypeConstants.DATE -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.TIMESTAMP -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.TIME -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.BLOB -> addProperty(input.name, files.apply { add(Base64DecodedMultipartFile(Base64.getDecoder().decode(computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asString))) }.size - 1)
                      TypeConstants.FORMULA -> throw CustomJsonException("{}")
                      else -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asJsonObject.get(VariableConstants.VARIABLE_NAME).asString)
                    }
                  else if(connection.connectedCircuitComputation.circuit != null)
                    when(getCircuitOutput(connection.connectedCircuitComputationCircuitOutput!!).type.name) {
                      TypeConstants.TEXT -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asString)
                      TypeConstants.NUMBER -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                      TypeConstants.DECIMAL -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asBigDecimal)
                      TypeConstants.BOOLEAN -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asBoolean)
                      TypeConstants.DATE -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                      TypeConstants.TIMESTAMP -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                      TypeConstants.TIME -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                      TypeConstants.BLOB -> addProperty(input.name, files.apply { add(Base64DecodedMultipartFile(Base64.getDecoder().decode(computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asString))) }.size - 1)
                      TypeConstants.FORMULA -> throw CustomJsonException("{}")
                      else -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asJsonObject.get(VariableConstants.VARIABLE_NAME).asString)
                    }
                  else {
                    val computedHash: String = computeHash(connection.connectedCircuitComputationTypeAccumulator!!.keys.sortedBy { it.id }.fold("") { acc2, key ->
                      val accumulatorValueCircuitInput: CircuitInput = connection.connectedCircuitComputationAccumulatorKeys.single { it.key == key }.circuitInput
                      acc2 + when (key.type.name) {
                        TypeConstants.TEXT -> args.get(accumulatorValueCircuitInput.name).asString
                        TypeConstants.NUMBER -> args.get(accumulatorValueCircuitInput.name).asLong
                        TypeConstants.DECIMAL -> args.get(accumulatorValueCircuitInput.name).asBigDecimal.toString()
                        TypeConstants.BOOLEAN -> args.get(accumulatorValueCircuitInput.name).asBoolean.toString()
                        TypeConstants.DATE -> Date(args.get(accumulatorValueCircuitInput.name).asLong).toString()
                        TypeConstants.TIMESTAMP -> Timestamp(args.get(accumulatorValueCircuitInput.name).asLong).toString()
                        TypeConstants.TIME -> Time(args.get(accumulatorValueCircuitInput.name).asLong).toString()
                        TypeConstants.BLOB -> Base64.getEncoder().encodeToString(files[args.get(accumulatorValueCircuitInput.name).asInt].bytes)
                        TypeConstants.FORMULA -> throw CustomJsonException("{}")
                        else -> (variableRepository.findVariable(type = key.type, name = args.get(accumulatorValueCircuitInput.name).asString)
                          ?: throw CustomJsonException("{${CircuitConstants.ARGS}: {${accumulatorValueCircuitInput.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")).id
                      }
                    })
                    val rootVariableAccumulator: VariableAccumulator? = variableAccumulatorRepository.findVariableAccumulator(typeAccumulator = connection.connectedCircuitComputationTypeAccumulator, level = 0, hash = computedHash)
                    if (rootVariableAccumulator == null)
                      throw CustomJsonException("{}")
                    else {
                      val matchingVariableAccumulator: VariableAccumulator? = getMatchingVariableAccumulator(args = args, connection = connection, variableAccumulator = rootVariableAccumulator, files = files)
                      if(matchingVariableAccumulator == null)
                        throw CustomJsonException("{}")
                      else {
                        when(connection.connectedCircuitComputationTypeAccumulator.type.name) {
                          TypeConstants.TEXT -> addProperty(input.name, matchingVariableAccumulator.stringValue!!)
                          TypeConstants.NUMBER -> addProperty(input.name, matchingVariableAccumulator.longValue!!)
                          TypeConstants.DECIMAL -> addProperty(input.name, matchingVariableAccumulator.decimalValue!!)
                          TypeConstants.BOOLEAN -> addProperty(input.name, matchingVariableAccumulator.booleanValue!!)
                          TypeConstants.DATE -> addProperty(input.name, matchingVariableAccumulator.dateValue!!.time)
                          TypeConstants.TIMESTAMP -> addProperty(input.name, matchingVariableAccumulator.timestampValue!!.time)
                          TypeConstants.TIME -> addProperty(input.name, matchingVariableAccumulator.timeValue!!.time)
                          TypeConstants.BLOB -> addProperty(input.name, files.apply { add(Base64DecodedMultipartFile(matchingVariableAccumulator.blobValue!!.getBytes(1, matchingVariableAccumulator.blobValue!!.length().toInt()))) }.size - 1)
                          TypeConstants.FORMULA -> throw CustomJsonException("{}")
                          else -> addProperty(input.name, matchingVariableAccumulator.referencedVariable!!.name)
                        }
                      }
                    }
                  }
                }
              })
            }, files = files, defaultTimestamp = defaultTimestamp)
          } else {
            val computationMapper: Mapper = computation.mapper!!
            computationResults[computation.name] = mapperService.executeMapper(jsonParams = JsonObject().apply {
              addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
              addProperty(MapperConstants.MAPPER_NAME, computationMapper.name)
              add(MapperConstants.QUERY_PARAMS, computation.connections.fold(JsonObject()) { acc1, connection ->
                acc1.apply {
                  val input: FunctionInput = connection.functionInput!!
                  if (connection.connectedCircuitInput != null)
                    when(connection.functionInput.type.name) {
                      TypeConstants.TEXT -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asString)
                      TypeConstants.NUMBER -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.DECIMAL -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asBigDecimal)
                      TypeConstants.BOOLEAN -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asBoolean)
                      TypeConstants.DATE -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.TIMESTAMP -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.TIME -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asLong)
                      TypeConstants.BLOB -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asInt)
                      TypeConstants.FORMULA -> throw CustomJsonException("{}")
                      else -> addProperty(input.name, args.get(connection.connectedCircuitInput.name).asString)
                    }
                  else if (connection.connectedCircuitComputation!!.function != null)
                    when(connection.connectedCircuitComputationFunctionOutput!!.type.name) {
                      TypeConstants.TEXT -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asString)
                      TypeConstants.NUMBER -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.DECIMAL -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asBigDecimal)
                      TypeConstants.BOOLEAN -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asBoolean)
                      TypeConstants.DATE -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.TIMESTAMP -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.TIME -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asLong)
                      TypeConstants.BLOB -> addProperty(input.name, files.apply { add(Base64DecodedMultipartFile(Base64.getDecoder().decode(computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asString))) }.size - 1)
                      TypeConstants.FORMULA -> throw CustomJsonException("{}")
                      else -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationFunctionOutput.name).asJsonObject.get(VariableConstants.VARIABLE_NAME).asString)
                    }
                  else when(getCircuitOutput(connection.connectedCircuitComputationCircuitOutput!!).type.name) {
                    TypeConstants.TEXT -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asString)
                    TypeConstants.NUMBER -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                    TypeConstants.DECIMAL -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asBigDecimal)
                    TypeConstants.BOOLEAN -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asBoolean)
                    TypeConstants.DATE -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                    TypeConstants.TIMESTAMP -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                    TypeConstants.TIME -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asLong)
                    TypeConstants.BLOB -> addProperty(input.name, files.apply { add(Base64DecodedMultipartFile(Base64.getDecoder().decode(computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asString))) }.size - 1)
                    TypeConstants.FORMULA -> throw CustomJsonException("{}")
                    else -> addProperty(input.name, computationResults[connection.connectedCircuitComputation.name]!!.asJsonObject.get(connection.connectedCircuitComputationCircuitOutput.name).asJsonObject.get(VariableConstants.VARIABLE_NAME).asString)
                  }
                }
              })
              add(MapperConstants.ARGS, if (computation.connectedMapperCircuitInput != null)
                args.get(computation.connectedMapperCircuitInput!!.name).asJsonArray
              else
                computationResults[computation.connectedMapperCircuitComputation!!.name]!!.asJsonArray.fold(JsonArray()) { acc1, json ->
                  acc1.apply {
                    computation.connectedMapperCircuitComputationConnections.fold(JsonObject()) { acc2, mapperCircuitComputationConnection ->
                      acc2.apply {
                        when(mapperCircuitComputationConnection.functionInput.type.name) {
                          TypeConstants.TEXT -> addProperty(mapperCircuitComputationConnection.functionInput.name, json.asJsonObject.get(mapperCircuitComputationConnection.referencedMapperFunctionOutput.name).asString)
                          TypeConstants.NUMBER -> addProperty(mapperCircuitComputationConnection.functionInput.name, json.asJsonObject.get(mapperCircuitComputationConnection.referencedMapperFunctionOutput.name).asLong)
                          TypeConstants.DECIMAL -> addProperty(mapperCircuitComputationConnection.functionInput.name, json.asJsonObject.get(mapperCircuitComputationConnection.referencedMapperFunctionOutput.name).asBigDecimal)
                          TypeConstants.BOOLEAN -> addProperty(mapperCircuitComputationConnection.functionInput.name, json.asJsonObject.get(mapperCircuitComputationConnection.referencedMapperFunctionOutput.name).asBoolean)
                          TypeConstants.DATE -> addProperty(mapperCircuitComputationConnection.functionInput.name, json.asJsonObject.get(mapperCircuitComputationConnection.referencedMapperFunctionOutput.name).asLong)
                          TypeConstants.TIMESTAMP -> addProperty(mapperCircuitComputationConnection.functionInput.name, json.asJsonObject.get(mapperCircuitComputationConnection.referencedMapperFunctionOutput.name).asLong)
                          TypeConstants.TIME -> addProperty(mapperCircuitComputationConnection.functionInput.name, json.asJsonObject.get(mapperCircuitComputationConnection.referencedMapperFunctionOutput.name).asLong)
                          TypeConstants.FORMULA -> throw CustomJsonException("{}")
                          else -> addProperty(mapperCircuitComputationConnection.functionInput.name, json.asJsonObject.get(mapperCircuitComputationConnection.referencedMapperFunctionOutput.name).asJsonObject.get(VariableConstants.VARIABLE_NAME).asString)
                        }
                      }
                    }
                  }
                })
            }, files = files, defaultTimestamp = defaultTimestamp)
          }
          circuit.outputs.filter { it.connectedCircuitComputation == computation }.forEach { output ->
            if (computation.function != null)
              when(output.connectedCircuitComputationFunctionOutput!!.type.name) {
                TypeConstants.TEXT -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationFunctionOutput.name).asString)
                TypeConstants.NUMBER -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationFunctionOutput.name).asLong)
                TypeConstants.DECIMAL -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationFunctionOutput.name).asBigDecimal)
                TypeConstants.BOOLEAN -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationFunctionOutput.name).asBoolean)
                TypeConstants.DATE -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationFunctionOutput.name).asLong)
                TypeConstants.TIMESTAMP -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationFunctionOutput.name).asLong)
                TypeConstants.TIME -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationFunctionOutput.name).asLong)
                TypeConstants.BLOB -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationFunctionOutput.name).asString)
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationFunctionOutput.name).asJsonObject.get(VariableConstants.VARIABLE_NAME).asString)
              }
            else if(computation.circuit != null)
              when(getCircuitOutput(output.connectedCircuitComputationCircuitOutput!!).type.name) {
                TypeConstants.TEXT -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationCircuitOutput.name).asString)
                TypeConstants.NUMBER -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationCircuitOutput.name).asLong)
                TypeConstants.DECIMAL -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationCircuitOutput.name).asBigDecimal)
                TypeConstants.BOOLEAN -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationCircuitOutput.name).asBoolean)
                TypeConstants.DATE -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationCircuitOutput.name).asLong)
                TypeConstants.TIMESTAMP -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationCircuitOutput.name).asLong)
                TypeConstants.TIME -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationCircuitOutput.name).asLong)
                TypeConstants.BLOB -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationCircuitOutput.name).asString)
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> addProperty(output.name, computationResults[computation.name]!!.asJsonObject.get(output.connectedCircuitComputationCircuitOutput.name).asJsonObject.get(VariableConstants.VARIABLE_NAME).asString)
              }
            else
              add(output.name, computationResults[computation.name]!!.asJsonArray)
          }
        }
      }
    }
  }
}
