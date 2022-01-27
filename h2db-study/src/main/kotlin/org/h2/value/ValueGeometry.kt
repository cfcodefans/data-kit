package org.h2.value

import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.h2.util.Bits
import org.h2.util.StringUtils
import org.h2.util.geometry.EWKBUtils
import org.h2.util.geometry.EWKTUtils
import org.h2.util.geometry.GeometryUtils.EnvelopeAndDimensionSystemTarget
import org.h2.util.geometry.JTSUtils
import org.locationtech.jts.geom.Geometry

/**
 * Implementation of the GEOMETRY data type.
 * @param bytes the EWKB bytes
 * @param envelope the envelope
 */
class ValueGeometry(bytes: ByteArray, envelope: DoubleArray) : ValueBytesBase(bytes) {
    /**
     * Geometry type and dimension system in OGC geometry code format (type + dimensionSystem * 1000).
     */
    private var typeAndDimensionSystem = 0

    /**
     * The envelope of the value. Calculated only on request.
     */
    private lateinit var envelope: DoubleArray

    /**
     * Spatial reference system identifier.
     */
    private var srid = 0

    /**
     * The value. Converted from WKB only on request as conversion from/to WKB
     * cost a significant amount of CPU cycles.
     */
    private var geometry: Any? = null

    init {
        if (bytes.size < 9 || bytes[0].toInt() != 0) {
            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, StringUtils.convertBytesToHex(bytes))
        }
        this.envelope = envelope
        val t = Bits.readInt(bytes, 1)
        srid = if (t and EWKBUtils.EWKB_SRID != 0) Bits.readInt(bytes, 5) else 0
        typeAndDimensionSystem = (t and 0xffff) % 1000 + EWKBUtils.type2dimensionSystem(t) * 1000
    }

    companion object {
        /**
         * Get or create a geometry value for the given geometry.
         *
         * @param o the geometry object (of type org.locationtech.jts.geom.Geometry)
         * @return the value
         */
        fun getFromGeometry(o: Any): ValueGeometry? = try {
            val target = EnvelopeAndDimensionSystemTarget()
            val g = o as Geometry
            JTSUtils.parseGeometry(g, target)
            cache(ValueGeometry(JTSUtils.geometry2ewkb(g, target.dimensionSystem)!!, target.envelope)) as ValueGeometry
        } catch (ex: RuntimeException) {
            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, o.toString())
        }

        /**
         * Get or create a geometry value for the given geometry.
         *
         * @param s the WKT or EWKT representation of the geometry
         * @return the value
         */
        operator fun get(s: String?): ValueGeometry? = try {
            val target = EnvelopeAndDimensionSystemTarget()
            EWKTUtils.parseEWKT(s, target)
            cache(ValueGeometry(EWKTUtils.ewkt2ewkb(s, target.dimensionSystem), target.envelope)) as ValueGeometry
        } catch (ex: RuntimeException) {
            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, s!!)
        }

        /**
         * Get or create a geometry value for the given internal EWKB representation.
         *
         * @param bytes the WKB representation of the geometry. May not be modified.
         * @return the value
         */
        operator fun get(bytes: ByteArray?): ValueGeometry? {
            return cache(ValueGeometry(bytes!!, ValueGeometry.UNKNOWN_ENVELOPE)) as ValueGeometry
        }

    }
}