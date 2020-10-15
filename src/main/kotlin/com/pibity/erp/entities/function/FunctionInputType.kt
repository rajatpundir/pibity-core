/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function

import com.pibity.erp.entities.function.embeddables.FunctionInputTypeId
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_input_type", schema = "inventory")
data class FunctionInputType(

    @EmbeddedId
    val id: FunctionInputTypeId,

    @OneToMany(mappedBy = "id.functionInputType", cascade = [CascadeType.ALL])
    val functionInputKeys: MutableSet<FunctionInputKey> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionInputType
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
