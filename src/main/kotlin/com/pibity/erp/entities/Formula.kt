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
@Table(name = "formula", schema = "inventory")
data class Formula(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "formula_generator")
    @SequenceGenerator(name="formula_generator", sequenceName = "formula_sequence")
    val id: Long = -1,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "return_type_id", referencedColumnName = "id")])
    var returnType: Type,

    @Column(name = "symbol_paths", nullable = false)
    var symbolPaths: String,

    @Column(name = "expression", nullable = false)
    var expression: String,

    @ManyToMany
    @JoinTable(name = "mapping_formula_key_dependencies", schema = "inventory", joinColumns = [JoinColumn(name = "formula_id", referencedColumnName = "id")], inverseJoinColumns = [JoinColumn(name = "key_id", referencedColumnName = "id")])
    val keyDependencies: MutableSet<Key> = HashSet(),

    @ManyToMany
    @JoinTable(name = "mapping_formula_type_dependencies", schema = "inventory", joinColumns = [JoinColumn(name = "formula_id", referencedColumnName = "id")], inverseJoinColumns = [JoinColumn(name = "type_id", referencedColumnName = "id")])
    val typeDependencies: MutableSet<Type> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Formula
    return this.id == other.id
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
