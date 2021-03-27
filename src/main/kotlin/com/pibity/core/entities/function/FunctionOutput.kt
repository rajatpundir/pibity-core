/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.function

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.commons.constants.FunctionConstants
import com.pibity.core.entities.Key
import com.pibity.core.entities.Type
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_output", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["function_id", "name"])])
data class FunctionOutput(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "function_output_generator")
    @SequenceGenerator(name = "function_output_generator", sequenceName = "function_output_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumns(JoinColumn(name = "function_id", referencedColumnName = "id"))
    val function: Function,

    @Column(name = "name", nullable = false)
    val name: String,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @OneToOne
    @JoinColumns(JoinColumn(name = "type_id", referencedColumnName = "id"))
    val type: Type,

    @Column(name = "operation", nullable = false)
    val operation: Int = FunctionConstants.CREATE,

    @Lob
    @Column(name = "variable_name", nullable = false)
    val variableName: String,

    @ManyToMany
    @JoinTable(name = "mapping_function_output_variable_name_dependencies", schema = ApplicationConstants.SCHEMA,
        joinColumns = [JoinColumn(name = "function_output_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "dependency_key_id", referencedColumnName = "id")])
    val variableNameKeyDependencies: MutableSet<Key> = HashSet(),

    @OneToMany(mappedBy = "functionOutput", cascade = [CascadeType.ALL], orphanRemoval = true)
    val values: MutableSet<FunctionOutputKey> = HashSet(),

    @Column(name = "created", nullable = false)
    val created: Timestamp,

    @Column(name = "updated")
    var updated: Timestamp? = null

) : Serializable {

    @PreUpdate
    fun onUpdate() {
        updated = Timestamp(System.currentTimeMillis())
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        if (this === other) return true
        other as FunctionOutput
        return this.function == other.function && this.name == other.name
    }

    override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
