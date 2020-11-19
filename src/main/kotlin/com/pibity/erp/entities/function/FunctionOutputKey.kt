/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function

import com.pibity.erp.entities.Key
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_output_key", schema = "inventory", uniqueConstraints = [UniqueConstraint(columnNames = ["function_output_type_id", "key_id"])])
data class FunctionOutputKey(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "function_output_key_generator")
    @SequenceGenerator(name = "function_output_key_generator", sequenceName = "function_output_key_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "function_output_type_id", referencedColumnName = "id")])
    val functionOutputType: FunctionOutputType,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "key_id", referencedColumnName = "id")])
    val key: Key,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "expression")
    var expression: String? = null,

    @OneToOne
    val referencedFunctionOutputType: FunctionOutputType? = null,

    @ManyToMany
    @JoinTable(name = "mapping_function_output_key_dependencies", schema = "inventory", joinColumns = [JoinColumn(name = "function_output_key_id", referencedColumnName = "id")], inverseJoinColumns = [JoinColumn(name = "dependency_key_id", referencedColumnName = "id")])
    val keyDependencies: MutableSet<Key> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionOutputKey
    return this.functionOutputType == other.functionOutputType && this.key == other.key
  }

  override fun hashCode(): Int = Objects.hash(id)
}
