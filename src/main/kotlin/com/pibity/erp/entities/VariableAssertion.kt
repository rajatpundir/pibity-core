/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "variable_assertion", schema = "inventory", uniqueConstraints = [UniqueConstraint(columnNames = ["type_assertion_id", "variable_id"])])
data class VariableAssertion(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "variable_assertion_generator")
    @SequenceGenerator(name = "variable_assertion_generator", sequenceName = "variable_assertion_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "type_assertion_id", referencedColumnName = "id")])
    val typeAssertion: TypeAssertion,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "variable_id", referencedColumnName = "id")])
    val variable: Variable,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "result", nullable = false)
    var result: Boolean,

    @ManyToMany
    @JoinTable(name = "mapping_variable_assertion_value_dependencies", schema = "inventory", joinColumns = [JoinColumn(name = "variable_assertion_id", referencedColumnName = "id")], inverseJoinColumns = [JoinColumn(name = "dependency_value_id", referencedColumnName = "id")])
    var valueDependencies: MutableSet<Value> = HashSet(),

    @ManyToMany
    @JoinTable(name = "mapping_variable_assertion_variable_dependencies", schema = "inventory", joinColumns = [JoinColumn(name = "variable_assertion_id", referencedColumnName = "id")], inverseJoinColumns = [JoinColumn(name = "dependency_variable_id", referencedColumnName = "id")])
    var variableDependencies: MutableSet<Variable> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as VariableAssertion
    return this.typeAssertion == other.typeAssertion && this.variable == other.variable
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
