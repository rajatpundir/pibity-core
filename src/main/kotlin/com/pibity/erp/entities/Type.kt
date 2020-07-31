/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.commons.gson
import com.pibity.erp.entities.embeddables.TypeId
import java.io.Serializable
import java.util.*
import javax.persistence.*
import kotlin.collections.HashSet

@Entity
@Table(name = "type", schema = "inventory")
data class Type(

    @EmbeddedId
    val id: TypeId,

    @Column(name = "auto_increment_id", nullable = false)
    var autoIncrementId: Int = 0,

    @Column(name = "auto_assign_id", nullable = false)
    var autoAssignId: Boolean = false,

    @Column(name = "display_name", nullable = false)
    var displayName: String = "",

    @Column(name = "multiplicity", nullable = false)
    val multiplicity: Long,

    @Column(name = "variable_count", nullable = false)
    var variableCount: Long = 0,

    @Column(name = "depth", nullable = false)
    var depth: Int = 0,

    @Column(name = "primitive_type", nullable = false)
    var primitiveType: Boolean = false,

    @OneToMany(mappedBy = "id.parentType", cascade = [CascadeType.ALL])
    val keys: MutableSet<Key> = HashSet(),

    @OneToMany(mappedBy = "id.type", cascade = [CascadeType.ALL])
    var variables: MutableSet<Variable> = HashSet(),

    @ManyToMany
    @JoinTable(name = "mapping_type_category", schema = "inventory",
        joinColumns = [JoinColumn(name = "organization_id", referencedColumnName = "organization_id"),
          JoinColumn(name = "super_type_name", referencedColumnName = "super_type_name"),
          JoinColumn(name = "type_name", referencedColumnName = "type_name")],
        inverseJoinColumns = [JoinColumn(name = "category_id")])
    val categories: MutableSet<Category> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Type
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  override fun toString(): String = gson.toJson(this)
}
