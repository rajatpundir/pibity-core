/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.circuit

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.function.FunctionInput
import com.pibity.core.entities.function.FunctionOutput
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "circuit_computation_mapper_connection", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["parent_circuit_computation_id", "mapper_function_input_id"])])
data class CircuitComputationMapperConnection(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "circuit_computation_mapper_connection_generator")
    @SequenceGenerator(name = " circuit_computation_mapper_connection_generator", sequenceName = "circuit_computation_mapper_connection_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumn(name = "parent_circuit_computation_id", nullable = false)
    val parentComputation: CircuitComputation,

    @ManyToOne
    @JoinColumn(name = "mapper_function_input_id")
    val functionInput: FunctionInput,

    @ManyToOne
    @JoinColumn(name = "referenced_mapper_function_output_id")
    val referencedMapperFunctionOutput: FunctionOutput,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

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
        other as CircuitComputationMapperConnection
        return this.parentComputation == other.parentComputation && this.functionInput == other.functionInput
    }

    override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
