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
@Table(name = "formula", schema = "inventory")
data class Formula(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1,

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "return_type_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "return_type_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "return_type_name", referencedColumnName = "type_name")])
    var returnType: Type,

    @Column(name = "symbol_paths", nullable = false)
    var symbolPaths: String,

    @Column(name = "expression", nullable = false)
    var expression: String,

    @ManyToMany
    @JoinTable(name = "mapping_formula_dependencies", schema = "inventory",
        joinColumns = [JoinColumn(name = "formula_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "key_parent_type_organization_id", referencedColumnName = "parent_type_organization_id"),
          JoinColumn(name = "key_parent_super_type_name", referencedColumnName = "parent_super_type_name"),
          JoinColumn(name = "key_parent_type_name", referencedColumnName = "parent_type_name"),
          JoinColumn(name = "key_name", referencedColumnName = "key_name")])
    val keyDependencies: MutableSet<Key> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Formula
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
