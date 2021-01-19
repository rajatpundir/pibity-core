/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.circuit

import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "circuit_input", schema = "inventory", uniqueConstraints = [UniqueConstraint(columnNames = ["parent_circuit_id", "name"])])
data class CircuitInput(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "circuit_input_generator")
    @SequenceGenerator(name = " circuit_input_generator", sequenceName = "circuit_input_sequence")
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
    @JoinColumns(*[JoinColumn(name = "type_id", referencedColumnName = "id")])
    val type: Type,

    @Column(name = "value_string")
    var defaultStringValue: String? = null,

    @Column(name = "value_long")
    var defaultLongValue: Long? = null,

    @Column(name = "value_double")
    var defaultDoubleValue: Double? = null,

    @Column(name = "value_boolean")
    var defaultBooleanValue: Boolean? = null,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "value_referenced_variable_id", referencedColumnName = "id")])
    var referencedVariable: Variable? = null,

    @OneToMany(mappedBy = "connectedCircuitInput", cascade = [CascadeType.ALL])
    val referencingCircuitComputationConnections: Set<CircuitComputationConnection> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as CircuitInput
    return this.parentCircuit == other.parentCircuit && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
