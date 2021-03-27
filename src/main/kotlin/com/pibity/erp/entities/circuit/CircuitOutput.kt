/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.circuit

import com.pibity.erp.commons.constants.ApplicationConstants
import com.pibity.erp.entities.function.FunctionOutput
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "circuit_output", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["parent_circuit_id", "name"])])
data class CircuitOutput(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "circuit_output_generator")
  @SequenceGenerator(name = " circuit_output_generator", sequenceName = "circuit_output_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "parent_circuit_id", nullable = false)
  val parentCircuit: Circuit,

  @Column(name = "name", nullable = false)
  val name: String,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp(System.currentTimeMillis()),

  @ManyToOne
  @JoinColumn(name = "circuit_computation_id")
  val connectedCircuitComputation: CircuitComputation,

  @ManyToOne
  @JoinColumn(name = "circuit_computation_function_output_id")
  val connectedCircuitComputationFunctionOutput: FunctionOutput? = null,

  @ManyToOne
  @JoinColumn(name = "circuit_computation_circuit_output_id")
  val connectedCircuitComputationCircuitOutput: CircuitOutput? = null,

  @Column(name = "created", nullable = false)
  val created: Timestamp,

  @Column(name = "updated")
  var updated: Timestamp? = null

) : Serializable {

  @PreUpdate
  fun onUpdate() {
    updated = Timestamp(System.currentTimeMillis())
  }

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as CircuitOutput
    return this.parentCircuit == other.parentCircuit && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
