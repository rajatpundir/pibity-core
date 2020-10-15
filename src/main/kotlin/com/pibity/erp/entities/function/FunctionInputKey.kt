/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function

import com.pibity.erp.entities.Key
import com.pibity.erp.entities.function.embeddables.FunctionInputKeyId
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_input_key", schema = "inventory")
data class FunctionInputKey(

    @EmbeddedId
    val id: FunctionInputKeyId,

    @Column(name = "expression")
    var expression: String? = null,

    @OneToOne
    val referencedFunctionInputType: FunctionInputType? = null,

    @ManyToMany
    @JoinTable(name = "mapping_function_input_key_dependencies", schema = "inventory",
        joinColumns = [JoinColumn(name = "function_input_type_function_organization_id", referencedColumnName = "function_organization_id"),
          JoinColumn(name = "function_input_type_function_name", referencedColumnName = "function_name"),
          JoinColumn(name = "function_input_type_function_input_name", referencedColumnName = "function_input_name"),
          JoinColumn(name = "function_input_type_organization_id", referencedColumnName = "organization_id"),
          JoinColumn(name = "function_input_type_super_type_name", referencedColumnName = "super_type_name"),
          JoinColumn(name = "function_input_type_type_name", referencedColumnName = "type_name"),
          JoinColumn(name = "key_organization_id", referencedColumnName = "key_organization_id"),
          JoinColumn(name = "key_super_type_name", referencedColumnName = "key_super_type_name"),
          JoinColumn(name = "key_type_name", referencedColumnName = "key_type_name"),
          JoinColumn(name = "key_name", referencedColumnName = "key_name")],
        inverseJoinColumns = [JoinColumn(name = "dependency_key_parent_type_organization_id", referencedColumnName = "parent_type_organization_id"),
          JoinColumn(name = "dependency_key_parent_super_type_name", referencedColumnName = "parent_super_type_name"),
          JoinColumn(name = "dependency_key_parent_type_name", referencedColumnName = "parent_type_name"),
          JoinColumn(name = "dependency_key_name", referencedColumnName = "key_name")])
    val keyDependencies: MutableSet<Key> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionInputKey
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
