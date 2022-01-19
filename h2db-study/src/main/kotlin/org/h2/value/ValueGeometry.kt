package org.h2.value

import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.h2.util.Bits
import org.h2.util.StringUtils
import org.h2.util.geometry.EWKBUtils

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
}