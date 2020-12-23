/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function

import com.pibity.erp.entities.Type
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_output_type", schema = "inventory", uniqueConstraints = [UniqueConstraint(columnNames = ["function_output_id", "type_id"])])
data class FunctionOutputType(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "function_output_type_generator")
    @SequenceGenerator(name = "function_output_type_generator", sequenceName = "function_output_type_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "function_output_id", referencedColumnName = "id")])
    val functionOutput: FunctionOutput,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "type_id", referencedColumnName = "id")])
    val type: Type,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @OneToMany(mappedBy = "functionOutputType", cascade = [CascadeType.ALL])
    val functionOutputKeys: MutableSet<FunctionOutputKey> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionOutputType
    return this.functionOutput == other.functionOutput && this.type == other.type
  }

  override fun hashCode(): Int = Objects.hash(id)
}
