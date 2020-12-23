/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.commons.exceptions.CustomJsonException
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "variable_list", schema = "inventory")
data class VariableList(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "variable_list_generator")
    @SequenceGenerator(name="variable_list_generator", sequenceName = "variable_list_sequence")
    val id: Long = -1,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "size", nullable = false)
    var size: Int = 0,

    @ManyToOne
    @JoinColumn(name = "list_type_id")
    val listType: TypeList,

    @ManyToMany
    @JoinTable(name = "mapping_list_variables", schema = "inventory", joinColumns = [JoinColumn(name = "variable_list_id")], inverseJoinColumns = [JoinColumn(name = "variable_id", referencedColumnName = "id")])
    val variables: MutableSet<Variable> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as VariableList
    return this.id == other.id
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  init {
    if (size < 0)
      throw CustomJsonException("{size: 'List size cannot be less than zero'}")
  }
}
