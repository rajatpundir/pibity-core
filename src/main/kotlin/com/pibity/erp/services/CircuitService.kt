/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.*
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.*
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable
import com.pibity.erp.entities.circuit.*
import com.pibity.erp.entities.function.Function
import com.pibity.erp.repositories.circuit.CircuitRepository
import com.pibity.erp.repositories.circuit.jpa.*
import com.pibity.erp.repositories.function.FunctionRepository
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.query.TypeRepository
import com.pibity.erp.repositories.query.VariableRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CircuitService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val typeRepository: TypeRepository,
    val variableRepository: VariableRepository,
    val functionRepository: FunctionRepository,
    val circuitRepository: CircuitRepository,
    val circuitJpaRepository: CircuitJpaRepository,
    val circuitInputJpaRepository: CircuitInputJpaRepository,
    val circuitComputationJpaRepository: CircuitComputationJpaRepository,
    val circuitComputationConnectionJpaRepository: CircuitComputationConnectionJpaRepository,
    val circuitOutputJpaRepository: CircuitOutputJpaRepository,
    val functionService: FunctionService
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createCircuit(jsonParams: JsonObject): Circuit {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get("orgId").asLong)
        ?: throw CustomJsonException("{orgId: 'Organization could not be found'}")
    val globalTypes: MutableSet<Type> = typeRepository.findGlobalTypes(organizationId = organization.id) as MutableSet<Type>
    val circuitName: String = validateCircuitName(circuitName = jsonParams.get("circuitName").asString)
    val functions: Set<Function> = functionRepository.findFunctions(jsonParams.get("orgId").asLong)
    val circuits: Set<Circuit> = circuitRepository.findCircuits(jsonParams.get("orgId").asLong)
    val (inputs: JsonObject, computations: JsonObject, outputs: JsonObject) = validateCircuitOutputs(outputs = jsonParams.get("outputs").asJsonObject, globalTypes = globalTypes, functions = functions,
        inputsAndComputations = validateCircuitComputations(computations = jsonParams.get("computations").asJsonObject, functions = functions, circuits = circuits,
            inputs = validateCircuitInputs(inputs = jsonParams.get("inputs").asJsonObject, globalTypes = globalTypes)))
    val circuit: Circuit = circuitJpaRepository.save(Circuit(organization = organization, name = circuitName))
    inputs.entrySet().forEach { (inputName, inputElement) ->
      val input: JsonObject = inputElement.asJsonObject
      val inputType: Type = globalTypes.single { it.name == input.get(KeyConstants.KEY_TYPE).asString }
      when (inputType.name) {
        TypeConstants.TEXT -> circuit.inputs.add(circuitInputJpaRepository.save(CircuitInput(parentCircuit = circuit, name = inputName, type = inputType,
            defaultStringValue = if (input.has(KeyConstants.DEFAULT)) input.get(KeyConstants.DEFAULT).asString else "")))
        TypeConstants.NUMBER -> circuit.inputs.add(circuitInputJpaRepository.save(CircuitInput(parentCircuit = circuit, name = inputName, type = inputType,
            defaultLongValue = if (input.has(KeyConstants.DEFAULT)) input.get(KeyConstants.DEFAULT).asLong else 0)))
        TypeConstants.DECIMAL -> circuit.inputs.add(circuitInputJpaRepository.save(CircuitInput(parentCircuit = circuit, name = inputName, type = inputType,
            defaultDoubleValue = if (input.has(KeyConstants.DEFAULT)) input.get(KeyConstants.DEFAULT).asDouble else 0.0)))
        TypeConstants.BOOLEAN -> circuit.inputs.add(circuitInputJpaRepository.save(CircuitInput(parentCircuit = circuit, name = inputName, type = inputType,
            defaultBooleanValue = if (input.has(KeyConstants.DEFAULT)) input.get(KeyConstants.DEFAULT).asBoolean else false)))
        TypeConstants.FORMULA -> {
        }
        else -> {
          if (input.has(KeyConstants.DEFAULT)) {
            val referencedVariable: Variable = variableRepository.findByTypeAndName(type = inputType, name = input.get(KeyConstants.DEFAULT).asString)
                ?: throw CustomJsonException("{inputs: {$inputName: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}}")
            circuit.inputs.add(circuitInputJpaRepository.save(CircuitInput(parentCircuit = circuit, name = inputName, type = inputType, referencedVariable = referencedVariable)))
          } else circuit.inputs.add(circuitInputJpaRepository.save(CircuitInput(parentCircuit = circuit, name = inputName, type = inputType)))
        }
      }
    }
    computations.entrySet().forEach { (computationName, computationObject) ->
      val computation: JsonObject = computationObject.asJsonObject
      val connections: JsonObject = computation.get(CONNECT).asJsonObject
      when (computation.get(KeyConstants.KEY_TYPE).asString) {
        "Function" -> {
          val computationFunction: Function = functions.single { it.name == computation.get(EXECUTE).asString }
          val circuitComputation: CircuitComputation = circuitComputationJpaRepository.save(CircuitComputation(parentCircuit = circuit, name = computationName,
              order = computation.get(ORDER).asInt, level = computation.get(LEVEL).asInt, connectedToFunction = true, function = computationFunction))
          for (functionInput in computationFunction.inputs) {
            val connectionsArray: JsonArray = connections.get(functionInput.name).asJsonArray
            when (connectionsArray.first().asString) {
              INPUT -> {
                circuitComputation.connections.add(circuitComputationConnectionJpaRepository.save(CircuitComputationConnection(parentComputation = circuitComputation, functionInput = functionInput,
                    connectedToComputation = false, connectedCircuitInput = circuit.inputs.single { it.name == connectionsArray[1].asString })))
              }
              COMPUTATION -> {
                val connectedCircuitComputation: CircuitComputation = circuit.computations.single { it.name == connectionsArray[1].asString }
                if (connectedCircuitComputation.connectedToFunction) {
                  circuitComputation.connections.add(circuitComputationConnectionJpaRepository.save(CircuitComputationConnection(parentComputation = circuitComputation, functionInput = functionInput,
                      connectedToComputation = true, connectedCircuitComputation = connectedCircuitComputation,
                      connectedCircuitComputationFunctionOutput = connectedCircuitComputation.function!!.outputs.single { it.name == connectionsArray[2].asString }
                  )))
                } else {
                  circuitComputation.connections.add(circuitComputationConnectionJpaRepository.save(CircuitComputationConnection(parentComputation = circuitComputation, functionInput = functionInput,
                      connectedToComputation = true, connectedCircuitComputation = connectedCircuitComputation,
                      connectedCircuitComputationCircuitOutput = connectedCircuitComputation.circuit!!.outputs.single { it.name == connectionsArray[2].asString }
                  )))
                }
              }
            }
          }
          circuit.computations.add(circuitComputation)
        }
        "Circuit" -> {
          val computationCircuit: Circuit = circuits.single { it.name == computation.get(EXECUTE).asString }
          val circuitComputation: CircuitComputation = circuitComputationJpaRepository.save(CircuitComputation(parentCircuit = circuit, name = computationName,
              order = computation.get(ORDER).asInt, level = computation.get(LEVEL).asInt, connectedToFunction = false, circuit = computationCircuit))
          for (circuitInput in computationCircuit.inputs) {
            val connectionsArray: JsonArray = connections.get(circuitInput.name).asJsonArray
            when (connectionsArray.first().asString) {
              INPUT -> {
                circuitComputation.connections.add(circuitComputationConnectionJpaRepository.save(CircuitComputationConnection(parentComputation = circuitComputation, circuitInput = circuitInput,
                    connectedToComputation = false, connectedCircuitInput = circuit.inputs.single { it.name == connectionsArray[1].asString })))
              }
              COMPUTATION -> {
                val connectedCircuitComputation: CircuitComputation = circuit.computations.single { it.name == connectionsArray[1].asString }
                if (connectedCircuitComputation.connectedToFunction) {
                  circuitComputation.connections.add(circuitComputationConnectionJpaRepository.save(CircuitComputationConnection(parentComputation = circuitComputation, circuitInput = circuitInput,
                      connectedToComputation = true, connectedCircuitComputation = connectedCircuitComputation,
                      connectedCircuitComputationFunctionOutput = connectedCircuitComputation.function!!.outputs.single { it.name == connectionsArray[2].asString }
                  )))
                } else {
                  circuitComputation.connections.add(circuitComputationConnectionJpaRepository.save(CircuitComputationConnection(parentComputation = circuitComputation, circuitInput = circuitInput,
                      connectedToComputation = true, connectedCircuitComputation = connectedCircuitComputation,
                      connectedCircuitComputationCircuitOutput = connectedCircuitComputation.circuit!!.outputs.single { it.name == connectionsArray[2].asString }
                  )))
                }
              }
            }
          }
          circuit.computations.add(circuitComputation)
        }
        else -> throw CustomJsonException("{computations: {$computationName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}}")
      }
    }
    outputs.entrySet().forEach { (outputName, outputObject) ->
      val outputConnections: JsonArray = outputObject.asJsonObject.get(CONNECT).asJsonArray
      val connectedCircuitComputation = circuit.computations.single { it.name == outputConnections.first().asString }
      val circuitOutput: CircuitOutput = when (connectedCircuitComputation.connectedToFunction) {
        true -> CircuitOutput(parentCircuit = circuit, name = outputName,
            connectedCircuitComputation = connectedCircuitComputation,
            connectedCircuitComputationFunctionOutput = connectedCircuitComputation.function!!.outputs.single { it.name == outputConnections[1].asString })
        false -> CircuitOutput(parentCircuit = circuit, name = outputName,
            connectedCircuitComputation = connectedCircuitComputation,
            connectedCircuitComputationCircuitOutput = connectedCircuitComputation.circuit!!.outputs.single { it.name == outputConnections[1].asString }.connectedCircuitComputationCircuitOutput
        )
      }
      circuit.outputs.add(circuitOutputJpaRepository.save(circuitOutput))
    }
    return circuit
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun executeCircuit(jsonParams: JsonObject): JsonObject {
    val circuit: Circuit = circuitRepository.findCircuit(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("circuitName").asString)
        ?: throw CustomJsonException("{circuitName: 'Circuit could not be determined'}")
    val inputs: JsonObject = validateCircuitArgs(args = jsonParams.get("args").asJsonObject, inputs = circuit.inputs)
    val outputs = JsonObject()
    val computationResults = JsonObject()
    circuit.computations.map { it.level }.sorted().forEach { level ->
      val computationsInLevel: Set<CircuitComputation> = circuit.computations.filter { it.level == level }.toSet()
      for (computation in computationsInLevel) {
        if (computation.connectedToFunction) {
          val results: JsonObject = functionService.executeFunction(jsonParams = JsonObject().apply {
            addProperty("orgId", circuit.organization.id)
            addProperty("username", jsonParams.get("username").asString)
            addProperty("functionName", computation.function!!.name)
            add("args", JsonObject().apply {
              for (connection in computation.connections) {
                when (connection.functionInput!!.type.name) {
                  TypeConstants.TEXT -> addProperty(connection.functionInput.name, if (connection.connectedToComputation)
                    computationResults.get(connection.connectedCircuitComputation!!.name).asJsonObject.get(
                        if (connection.connectedCircuitComputation.connectedToFunction) connection.connectedCircuitComputationFunctionOutput!!.name
                        else connection.connectedCircuitComputationCircuitOutput!!.name).asString
                  else inputs.get(connection.connectedCircuitInput!!.name).asString)
                  TypeConstants.NUMBER -> addProperty(connection.functionInput.name, if (connection.connectedToComputation)
                    computationResults.get(connection.connectedCircuitComputation!!.name).asJsonObject.get(
                        if (connection.connectedCircuitComputation.connectedToFunction) connection.connectedCircuitComputationFunctionOutput!!.name
                        else connection.connectedCircuitComputationCircuitOutput!!.name).asLong
                  else inputs.get(connection.connectedCircuitInput!!.name).asLong)
                  TypeConstants.DECIMAL -> addProperty(connection.functionInput.name, if (connection.connectedToComputation)
                    computationResults.get(connection.connectedCircuitComputation!!.name).asJsonObject.get(
                        if (connection.connectedCircuitComputation.connectedToFunction) connection.connectedCircuitComputationFunctionOutput!!.name
                        else connection.connectedCircuitComputationCircuitOutput!!.name).asDouble
                  else inputs.get(connection.connectedCircuitInput!!.name).asDouble)
                  TypeConstants.BOOLEAN -> addProperty(connection.functionInput.name, if (connection.connectedToComputation)
                    computationResults.get(connection.connectedCircuitComputation!!.name).asJsonObject.get(
                        if (connection.connectedCircuitComputation.connectedToFunction) connection.connectedCircuitComputationFunctionOutput!!.name
                        else connection.connectedCircuitComputationCircuitOutput!!.name).asBoolean
                  else inputs.get(connection.connectedCircuitInput!!.name).asBoolean)
                  TypeConstants.FORMULA -> {
                  }
                  else -> addProperty(connection.functionInput.name, if (connection.connectedToComputation)
                    computationResults.get(connection.connectedCircuitComputation!!.name).asJsonObject.get(
                        if (connection.connectedCircuitComputation.connectedToFunction) connection.connectedCircuitComputationFunctionOutput!!.name
                        else connection.connectedCircuitComputationCircuitOutput!!.name).asJsonObject.get("variableName").asString
                  else inputs.get(connection.connectedCircuitInput!!.name).asString)
                }
              }
            })
          })
          computationResults.add(computation.name, results)
          circuit.outputs.filter { it.connectedCircuitComputation == computation }.forEach { circuitOutput ->
            outputs.add(circuitOutput.name, results.get(circuitOutput.connectedCircuitComputationFunctionOutput!!.name))
          }
        } else {
          val results: JsonObject = executeCircuit(jsonParams = JsonObject().apply {
            addProperty("orgId", circuit.organization.id)
            addProperty("username", jsonParams.get("username").asString)
            addProperty("circuitName", computation.circuit!!.name)
            add("args", JsonObject().apply {
              for (connection in computation.connections) {
                when (connection.circuitInput!!.type.name) {
                  TypeConstants.TEXT -> addProperty(connection.circuitInput.name, if (connection.connectedToComputation)
                    computationResults.get(connection.connectedCircuitComputation!!.name).asJsonObject.get(
                        if (connection.connectedCircuitComputation.connectedToFunction) connection.connectedCircuitComputationFunctionOutput!!.name
                        else connection.connectedCircuitComputationCircuitOutput!!.name).asString
                  else inputs.get(connection.connectedCircuitInput!!.name).asString)
                  TypeConstants.NUMBER -> addProperty(connection.circuitInput.name, if (connection.connectedToComputation)
                    computationResults.get(connection.connectedCircuitComputation!!.name).asJsonObject.get(
                        if (connection.connectedCircuitComputation.connectedToFunction) connection.connectedCircuitComputationFunctionOutput!!.name
                        else connection.connectedCircuitComputationCircuitOutput!!.name).asLong
                  else inputs.get(connection.connectedCircuitInput!!.name).asLong)
                  TypeConstants.DECIMAL -> addProperty(connection.circuitInput.name, if (connection.connectedToComputation)
                    computationResults.get(connection.connectedCircuitComputation!!.name).asJsonObject.get(
                        if (connection.connectedCircuitComputation.connectedToFunction) connection.connectedCircuitComputationFunctionOutput!!.name
                        else connection.connectedCircuitComputationCircuitOutput!!.name).asDouble
                  else inputs.get(connection.connectedCircuitInput!!.name).asDouble)
                  TypeConstants.BOOLEAN -> addProperty(connection.circuitInput.name, if (connection.connectedToComputation)
                    computationResults.get(connection.connectedCircuitComputation!!.name).asJsonObject.get(
                        if (connection.connectedCircuitComputation.connectedToFunction) connection.connectedCircuitComputationFunctionOutput!!.name
                        else connection.connectedCircuitComputationCircuitOutput!!.name).asBoolean
                  else inputs.get(connection.connectedCircuitInput!!.name).asBoolean)
                  TypeConstants.FORMULA -> {
                  }
                  else -> addProperty(connection.circuitInput.name, if (connection.connectedToComputation)
                    computationResults.get(connection.connectedCircuitComputation!!.name).asJsonObject.get(
                        if (connection.connectedCircuitComputation.connectedToFunction) connection.connectedCircuitComputationFunctionOutput!!.name
                        else connection.connectedCircuitComputationCircuitOutput!!.name).asJsonObject.get("variableName").asString
                  else inputs.get(connection.connectedCircuitInput!!.name).asString)
                }
              }
            })
          })
          computationResults.add(computation.name, results)
          circuit.outputs.filter { it.connectedCircuitComputation == computation }.forEach { circuitOutput ->
            outputs.add(circuitOutput.name, results.get(circuitOutput.connectedCircuitComputationCircuitOutput!!.name))
          }
        }
      }
    }
    return outputs
  }
}
