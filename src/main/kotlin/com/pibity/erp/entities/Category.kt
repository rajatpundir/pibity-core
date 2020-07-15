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
@Table(name = "category", schema = "inventory")
data class Category(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = -1,

    @OneToOne
    @JoinColumn(name = "organization_id")
    val organization: Organization,

    @ManyToMany
    @JoinTable(name = "mapping_category_ancestors", schema = "inventory",
        joinColumns = [JoinColumn(name = "category_id")],
        inverseJoinColumns = [JoinColumn(name = "ancestor_id")])
    val ancestors: MutableSet<Category> = HashSet(),

    @ManyToMany(mappedBy = "ancestors", cascade = [CascadeType.ALL])
    val children: MutableSet<Category> = HashSet(),

    @OneToOne
    @JoinColumn(name = "parent_id")
    var parent: Category? = null,

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "next_label", nullable = false)
    var nextLabel: String = "",

    @Column(name = "code", nullable = false)
    var code: String = "",

    @ManyToMany(mappedBy = "categories")
    val types: MutableSet<Type> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Category
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
