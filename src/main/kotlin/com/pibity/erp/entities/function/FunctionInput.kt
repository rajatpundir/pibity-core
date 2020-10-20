/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function

import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable
import com.pibity.erp.entities.function.embeddables.FunctionInputId
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_input", schema = "inventory")
data class FunctionInput(

    @EmbeddedId
    val id: FunctionInputId,

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "input_type_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "input_type_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "input_type_name", referencedColumnName = "type_name")])
    val type: Type,

    @Column(name = "variable_name")
    val variableName: String? = null,

    @ManyToMany
    @JoinTable(name = "mapping_function_input_variable_name_dependencies", schema = "inventory",
        joinColumns = [JoinColumn(name = "organization_id", referencedColumnName = "organization_id"),
          JoinColumn(name = "function_name", referencedColumnName = "function_name"),
          JoinColumn(name = "input_name", referencedColumnName = "input_name")],
        inverseJoinColumns = [JoinColumn(name = "dependency_key_parent_type_organization_id", referencedColumnName = "parent_type_organization_id"),
          JoinColumn(name = "dependency_key_parent_super_type_name", referencedColumnName = "parent_super_type_name"),
          JoinColumn(name = "dependency_key_parent_type_name", referencedColumnName = "parent_type_name"),
          JoinColumn(name = "dependency_key_name", referencedColumnName = "key_name")])
    val variableNameKeyDependencies: MutableSet<Key> = HashSet(),

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "values_function_organization_id", referencedColumnName = "function_organization_id"),
      JoinColumn(name = "values_function_name", referencedColumnName = "function_name"),
      JoinColumn(name = "values_function_input_name", referencedColumnName = "function_input_name"),
      JoinColumn(name = "values_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "values_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "values_type_name", referencedColumnName = "type_name")])
    var values: FunctionInputType? = null,

    @Column(name = "default_string_value")
    var defaultStringValue: String? = null,

    @Column(name = "default_long_value")
    var defaultLongValue: Long? = null,

    @Column(name = "default_double_value")
    var defaultDoubleValue: Double? = null,

    @Column(name = "default_boolean_value")
    var defaultBooleanValue: Boolean? = null,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "referenced_variable_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "referenced_variable_super_list_id", referencedColumnName = "super_list_id"),
      JoinColumn(name = "referenced_variable_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "referenced_variable_type_name", referencedColumnName = "type_name"),
      JoinColumn(name = "referenced_variable_name", referencedColumnName = "variable_name")])
    var referencedVariable: Variable? = null

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionInput
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
