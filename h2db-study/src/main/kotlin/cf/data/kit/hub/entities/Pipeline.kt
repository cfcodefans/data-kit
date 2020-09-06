package cf.data.kit.hub.entities

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "pipelines")
open class Pipeline : BaseEn() {
    @ManyToOne
    var src: UrlEn? = null

    @ManyToOne
    var target: UrlEn? = null

    @Column(name = "info")
    var info: String? = null

    @Column(name = "opts")
    var optStr: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pipeline) return false
        if (!super.equals(other)) return false

        if (src != other.src) return false
        if (target != other.target) return false
        return true
    }

    override fun hashCode(): Int = Objects.hash(super.hashCode(), src, target)
}