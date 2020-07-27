/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import java.io.Serializable
import java.util.*
import javax.persistence.*
import kotlin.collections.HashSet

@Entity
@Table(name = "list_variable", schema = "inventory")
data class VariableList(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1,

    @OneToOne
    @JoinColumn(name = "list_type_id")
    val listType: TypeList,

    @ManyToMany
    @JoinTable(name = "mapping_list_variables", schema = "inventory",
        joinColumns = [JoinColumn(name = "list_variable_id")],
        inverseJoinColumns = [JoinColumn(name = "organization_id", referencedColumnName = "organization_id"),
          JoinColumn(name = "variable_super_type_name", referencedColumnName = "super_type_name"),
          JoinColumn(name = "variable_type_name", referencedColumnName = "type_name"),
          JoinColumn(name = "super_variable_name", referencedColumnName = "super_variable_name"),
          JoinColumn(name = "variable_name", referencedColumnName = "variable_name")])
    val variables: MutableSet<Variable> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as VariableList
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
