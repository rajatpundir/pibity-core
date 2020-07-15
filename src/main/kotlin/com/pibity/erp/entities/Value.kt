/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.entities.embeddables.ValueId
import java.io.Serializable
import java.util.*
import javax.persistence.*
import kotlin.collections.HashSet

@Entity
@Table(name = "value", schema = "inventory")
data class Value(

    @EmbeddedId
    var id: ValueId,

    @Column(name = "string_value")
    var stringValue: String? = null,

    @Column(name = "long_value")
    var longValue: Long? = null,

    @Column(name = "double_value")
    var doubleValue: Double? = null,

    @Column(name = "boolean_value")
    var booleanValue: Boolean? = null,

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "referenced_variable_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "referenced_variable_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "referenced_variable_type_name", referencedColumnName = "type_name"),
      JoinColumn(name = "referenced_super_variable_name", referencedColumnName = "super_variable_name"),
      JoinColumn(name = "referenced_variable_name", referencedColumnName = "variable_name")])
    var referencedVariable: Variable? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "list_variable_id")
    var list: VariableList? = null,

    @ManyToMany
    @JoinTable(name = "mapping_value_formula", schema = "inventory"
        , joinColumns = [JoinColumn(name = "value_variable_organization_id", referencedColumnName = "variable_organization_id"),
      JoinColumn(name = "value_variable_super_type_name", referencedColumnName = "variable_super_type_name"),
      JoinColumn(name = "value_variable_type_name", referencedColumnName = "variable_type_name"),
      JoinColumn(name = "value_super_variable_name", referencedColumnName = "super_variable_name"),
      JoinColumn(name = "value_variable_name", referencedColumnName = "variable_name"),
      JoinColumn(name = "value_key_organization_id", referencedColumnName = "key_organization_id"),
      JoinColumn(name = "value_key_super_type_name", referencedColumnName = "key_super_type_name"),
      JoinColumn(name = "value_key_type_name", referencedColumnName = "key_type_name"),
      JoinColumn(name = "value_key_name", referencedColumnName = "key_name")]
        , inverseJoinColumns = [JoinColumn(name = "dependent_value_variable_organization_id", referencedColumnName = "variable_organization_id"),
      JoinColumn(name = "dependent_value_variable_super_type_name", referencedColumnName = "variable_super_type_name"),
      JoinColumn(name = "dependent_value_variable_type_name", referencedColumnName = "variable_type_name"),
      JoinColumn(name = "dependent_value_super_variable_name", referencedColumnName = "super_variable_name"),
      JoinColumn(name = "dependent_value_variable_name", referencedColumnName = "variable_name"),
      JoinColumn(name = "dependent_value_key_organization_id", referencedColumnName = "key_organization_id"),
      JoinColumn(name = "dependent_value_key_super_type_name", referencedColumnName = "key_super_type_name"),
      JoinColumn(name = "dependent_value_key_type_name", referencedColumnName = "key_type_name"),
      JoinColumn(name = "dependent_value_key_name", referencedColumnName = "key_name")])
    var dependentFormulaValues: MutableSet<Value> = HashSet(),

    @ManyToMany(mappedBy = "dependentFormulaValues")
    var formulaDependencies: MutableSet<Value> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Value
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
