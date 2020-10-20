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
import com.pibity.erp.entities.function.embeddables.FunctionOutputId
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_output", schema = "inventory")
data class FunctionOutput(

    @EmbeddedId
    val id: FunctionOutputId,

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "output_type_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "output_type_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "output_type_name", referencedColumnName = "type_name")])
    val type: Type,

    @Column(name = "variable_name", nullable = false)
    val variableName: String,

    @ManyToMany
    @JoinTable(name = "mapping_function_output_variable_name_dependencies", schema = "inventory",
        joinColumns = [JoinColumn(name = "organization_id", referencedColumnName = "organization_id"),
          JoinColumn(name = "function_name", referencedColumnName = "function_name"),
          JoinColumn(name = "output_name", referencedColumnName = "output_name")],
        inverseJoinColumns = [JoinColumn(name = "dependency_key_parent_type_organization_id", referencedColumnName = "parent_type_organization_id"),
          JoinColumn(name = "dependency_key_parent_super_type_name", referencedColumnName = "parent_super_type_name"),
          JoinColumn(name = "dependency_key_parent_type_name", referencedColumnName = "parent_type_name"),
          JoinColumn(name = "dependency_key_name", referencedColumnName = "key_name")])
    val variableNameKeyDependencies: MutableSet<Key> = HashSet(),

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "values_function_organization_id", referencedColumnName = "function_organization_id"),
      JoinColumn(name = "values_function_name", referencedColumnName = "function_name"),
      JoinColumn(name = "values_function_output_name", referencedColumnName = "function_output_name"),
      JoinColumn(name = "values_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "values_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "values_type_name", referencedColumnName = "type_name")])
    var values: FunctionOutputType? = null

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionOutput
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
