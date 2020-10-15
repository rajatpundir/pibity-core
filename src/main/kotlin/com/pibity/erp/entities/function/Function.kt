/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function

import com.pibity.erp.entities.function.embeddables.FunctionId
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function", schema = "inventory")
data class Function(

    @EmbeddedId
    val id: FunctionId,

    @Column(name = "symbol_paths", nullable = false)
    val symbolPaths: String,

    @OneToMany(mappedBy = "id.function", cascade = [CascadeType.ALL])
    val inputs: MutableSet<FunctionInput> = HashSet(),

    @OneToMany(mappedBy = "id.function", cascade = [CascadeType.ALL])
    val outputs: MutableSet<FunctionOutput> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Function
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  override fun toString(): String = serialize(this).toString()
}
