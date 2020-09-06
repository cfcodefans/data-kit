package cf.data.kit.hub.entities

import java.net.URL
import java.util.*
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "urls")
open class UrlEn : BaseEn() {
    @Column(name = "url_str", unique = true, nullable = false)
    var urlStr: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UrlEn) return false
        if (!super.equals(other)) return false
        if (urlStr != other.urlStr) return false
        return true
    }

    override fun hashCode(): Int = Objects.hash(super.hashCode(), urlStr)

    fun getUrl(): URL = URL(this.urlStr)

    @Embedded
    var account: UrlAccount? = null
    var info: String? = null
    var optStr: String? = null
}