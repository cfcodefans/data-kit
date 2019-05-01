package cf.data.kit.hub.dtos

import java.io.Serializable
import java.util.*

open class ServiceDTO : Serializable {

    var id: String? = null
    var time = Date()

    constructor(id: String) {
        this.id = id
    }

    constructor() : this(System.currentTimeMillis().toString())

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is ServiceDTO) return false
        val that = o as ServiceDTO?
        return id == that!!.id
    }

    override fun hashCode(): Int = Objects.hash(id)
}
