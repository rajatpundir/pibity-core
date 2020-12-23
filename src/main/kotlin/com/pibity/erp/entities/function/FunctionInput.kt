/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function

import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_input", schema = "inventory", uniqueConstraints = [UniqueConstraint(columnNames = ["function_id", "name"])])
data class FunctionInput(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "function_input_generator")
    @SequenceGenerator(name = "function_input_generator", sequenceName = "function_input_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "function_id", referencedColumnName = "id")])
    val function: Function,

    @Column(name = "name", nullable = false)
    val name: String,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "type_id", referencedColumnName = "id")])
    val type: Type,

    @Column(name = "variable_name")
    val variableName: String? = null,

    @ManyToMany
    @JoinTable(name = "mapping_function_input_variable_name_dependencies", schema = "inventory",
        joinColumns = [JoinColumn(name = "function_input_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "dependency_key_id", referencedColumnName = "id")])
    val variableNameKeyDependencies: MutableSet<Key> = HashSet(),

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "values_function_input_type_id", referencedColumnName = "id")])
    var values: FunctionInputType? = null,

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
    var referencedVariable: Variable? = null

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionInput
    return this.function == other.function && this.name == other.name
  }

  override fun hashCode(): Int = Objects.hash(id)
}
