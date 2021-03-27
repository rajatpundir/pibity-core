/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.function

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.Key
import com.pibity.core.entities.Type
import com.pibity.core.entities.Variable
import java.io.Serializable
import java.math.BigDecimal
import java.sql.Blob
import java.sql.Time
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_input", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["function_id", "name"])])
data class FunctionInput(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "function_input_generator")
    @SequenceGenerator(name = "function_input_generator", sequenceName = "function_input_sequence")
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

    @Lob
    @Column(name = "variable_name")
    var variableName: String? = null,

    @ManyToMany
    @JoinTable(name = "mapping_function_input_variable_name_dependencies", schema = ApplicationConstants.SCHEMA,
        joinColumns = [JoinColumn(name = "function_input_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "dependency_key_id", referencedColumnName = "id")])
    var variableNameKeyDependencies: MutableSet<Key> = HashSet(),

    @OneToMany(mappedBy = "functionInput", cascade = [CascadeType.ALL], orphanRemoval = true)
    val values: MutableSet<FunctionInputKey> = HashSet(),

    @Column(name = "value_string")
    var defaultStringValue: String? = null,

    @Column(name = "value_long")
    var defaultLongValue: Long? = null,

    @Column(name = "value_decimal")
    var defaultDecimalValue: BigDecimal? = null,

    @Column(name = "value_boolean")
    var defaultBooleanValue: Boolean? = null,

    @Column(name = "value_date")
    var defaultDateValue: Date? = null,

    @Column(name = "value_timestamp")
    var defaultTimestampValue: Timestamp? = null,

    @Column(name = "value_time")
    var defaultTimeValue: Time? = null,

    @Lob
    @Column(name = "value_blob")
    var defaultBlobValue: Blob? = null,

    @ManyToOne
    @JoinColumns(JoinColumn(name = "value_referenced_variable_id", referencedColumnName = "id"))
    var referencedVariable: Variable? = null,

    @Column(name = "created", nullable = false)
    val created: Timestamp = Timestamp(System.currentTimeMillis()),

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
        other as FunctionInput
        return this.function == other.function && this.name == other.name
    }

    override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
