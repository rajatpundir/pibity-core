/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.circuit

import com.pibity.erp.entities.function.Function
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "circuit_computation", schema = "inventory",
    uniqueConstraints = [
      UniqueConstraint(columnNames = ["parent_circuit_id", "name"]),
      UniqueConstraint(columnNames = ["parent_circuit_id", "computation_order"])
    ])
data class CircuitComputation(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "circuit_computation_generator")
    @SequenceGenerator(name = " circuit_computation_generator", sequenceName = "circuit_computation_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumn(name = "parent_circuit_id", nullable = false)
    val parentCircuit: Circuit,

    @Column(name = "name", nullable = false)
    val name: String,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "computation_order", nullable = false)
    val order: Int,

    @Column(name = "level", nullable = false)
    val level: Int,

    @Column(name = "connected_to_function", nullable = false)
    val connectedToFunction: Boolean,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "function_id", referencedColumnName = "id")])
    val function: Function? = null,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "circuit_id", referencedColumnName = "id")])
    val circuit: Circuit? = null,

    @OneToMany(mappedBy = "parentComputation", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val connections: MutableSet<CircuitComputationConnection> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as CircuitComputation
    return this.parentCircuit == other.parentCircuit && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
