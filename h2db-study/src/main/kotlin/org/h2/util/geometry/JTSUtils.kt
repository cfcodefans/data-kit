package org.h2.util.geometry

import org.h2.message.DbException
import org.h2.util.geometry.EWKBUtils.EWKBTarget
import org.h2.util.geometry.GeometryUtils.DimensionSystemTarget
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory
import java.io.ByteArrayOutputStream

/**
 * Utilities for Geometry data type from JTS library.
 */
object JTSUtils {

    /**
     * Converter output target that creates a JTS Geometry.
     */
    class GeometryTarget(private val dimensionSystem: Int, private var factory: GeometryFactory? = nul) : GeometryUtils.Target() {

        private var type = 0
        private var coordinates: CoordinateSequence? = null
        private var innerCoordinates: Array<CoordinateSequence?> = emptyArray()
        private var innerOffset = 0
        private var subgeometries: Array<out Geometry?> = emptyArray()

        /**
         * Creates a new instance of JTS Geometry target.
         *
         * @param dimensionSystem dimension system to use
         */

        override fun init(srid: Int) {
            factory = GeometryFactory(PrecisionModel(), srid,
                    if (dimensionSystem and GeometryUtils.DIMENSION_SYSTEM_XYM != 0) PackedCoordinateSequenceFactory.DOUBLE_FACTORY else CoordinateArraySequenceFactory.instance())
        }

        override fun startPoint() {
            type = GeometryUtils.POINT
            initCoordinates(1)
            innerOffset = -1
        }

        override fun startLineString(numPoints: Int) {
            type = GeometryUtils.LINE_STRING
            initCoordinates(numPoints)
            innerOffset = -1
        }

        override fun startPolygon(numInner: Int, numPoints: Int) {
            type = GeometryUtils.POLYGON
            initCoordinates(numPoints)
            innerCoordinates = arrayOfNulls(numInner)
            innerOffset = -1
        }

        override fun startPolygonInner(numInner: Int) {
            innerCoordinates[++innerOffset] = createCoordinates(numInner)
        }

        override fun startCollection(type: Int, numItems: Int) {
            this.type = type
            when (type) {
                GeometryUtils.MULTI_POINT -> subgeometries = arrayOfNulls<Point>(numItems)
                GeometryUtils.MULTI_LINE_STRING -> subgeometries = arrayOfNulls<LineString>(numItems)
                GeometryUtils.MULTI_POLYGON -> subgeometries = arrayOfNulls<Polygon>(numItems)
                GeometryUtils.GEOMETRY_COLLECTION -> subgeometries = arrayOfNulls(numItems)
                else -> throw IllegalArgumentException()
            }
        }

        override fun startCollectionItem(index: Int, total: Int): GeometryUtils.Target {
            return GeometryTarget(dimensionSystem, factory)
        }

        override fun endCollectionItem(target: GeometryUtils.Target, type: Int, index: Int, total: Int) {
            subgeometries[index] = (target as GeometryTarget).geometry
        }

        private fun initCoordinates(numPoints: Int) {
            coordinates = createCoordinates(numPoints)
        }

        private fun createCoordinates(numPoints: Int): CoordinateSequence {
            val d: Int
            val m: Int
            when (dimensionSystem) {
                GeometryUtils.DIMENSION_SYSTEM_XY -> {
                    d = 2
                    m = 0
                }
                GeometryUtils.DIMENSION_SYSTEM_XYZ -> {
                    d = 3
                    m = 0
                }
                GeometryUtils.DIMENSION_SYSTEM_XYM -> {
                    d = 3
                    m = 1
                }
                GeometryUtils.DIMENSION_SYSTEM_XYZM -> {
                    d = 4
                    m = 1
                }
                else -> throw DbException.getInternalError()
            }
            return factory!!.coordinateSequenceFactory.create(numPoints, d, m)
        }

        override fun addCoordinate(x: Double, y: Double, z: Double, m: Double, index: Int, total: Int) {
            if (type == GeometryUtils.POINT
                    && java.lang.Double.isNaN(x)
                    && java.lang.Double.isNaN(y)
                    && java.lang.Double.isNaN(z)
                    && java.lang.Double.isNaN(m)) {
                coordinates = createCoordinates(0)
                return
            }
            val coordinates = if (innerOffset < 0) coordinates else innerCoordinates[innerOffset]
            coordinates!!.setOrdinate(index, GeometryUtils.X, GeometryUtils.checkFinite(x))
            coordinates.setOrdinate(index, GeometryUtils.Y, GeometryUtils.checkFinite(y))
            when (dimensionSystem) {
                GeometryUtils.DIMENSION_SYSTEM_XYZM -> {
                    coordinates.setOrdinate(index, GeometryUtils.M, GeometryUtils.checkFinite(m))
                    coordinates.setOrdinate(index, GeometryUtils.Z, GeometryUtils.checkFinite(z))
                }
                GeometryUtils.DIMENSION_SYSTEM_XYZ -> coordinates.setOrdinate(index, GeometryUtils.Z, GeometryUtils.checkFinite(z))
                GeometryUtils.DIMENSION_SYSTEM_XYM -> coordinates.setOrdinate(index, 2, GeometryUtils.checkFinite(m))
            }
        }

        val geometry: Geometry
            get() = when (type) {
                GeometryUtils.POINT -> Point(coordinates, factory)
                GeometryUtils.LINE_STRING -> LineString(coordinates, factory)
                GeometryUtils.POLYGON -> {
                    val shell = LinearRing(coordinates, factory)
                    Polygon(shell, innerCoordinates.map { LinearRing(it, factory) }.toTypedArray(), factory)
                }
                GeometryUtils.MULTI_POINT -> MultiPoint(subgeometries as Array<Point?>, factory)
                GeometryUtils.MULTI_LINE_STRING -> MultiLineString(subgeometries as Array<LineString?>, factory)
                GeometryUtils.MULTI_POLYGON -> MultiPolygon(subgeometries as Array<Polygon?>, factory)
                GeometryUtils.GEOMETRY_COLLECTION -> GeometryCollection(subgeometries, factory)
                else -> throw IllegalStateException()
            }
    }

    /**
     * Converts EWKB to a JTS geometry object.
     *
     * @param ewkb source EWKB
     * @return JTS geometry object
     */
    fun ewkb2geometry(ewkb: ByteArray?): Geometry? {
        // Determine dimension system first
        val dimensionTarget = DimensionSystemTarget()
        EWKBUtils.parseEWKB(ewkb, dimensionTarget)
        // Generate a Geometry
        return ewkb2geometry(ewkb, dimensionTarget.dimensionSystem)
    }

    /**
     * Converts EWKB to a JTS geometry object.
     *
     * @param ewkb source EWKB
     * @param dimensionSystem dimension system
     * @return JTS geometry object
     */
    fun ewkb2geometry(ewkb: ByteArray?, dimensionSystem: Int): Geometry {
        val target = GeometryTarget(dimensionSystem)
        EWKBUtils.parseEWKB(ewkb, target)
        return target.geometry
    }

    /**
     * Converts Geometry to EWKB.
     *
     * @param geometry source geometry
     * @return EWKB representation
     */
    fun geometry2ewkb(geometry: Geometry): ByteArray? {
        // Determine dimension system first
        val dimensionTarget = DimensionSystemTarget()
        parseGeometry(geometry, dimensionTarget)
        // Write an EWKB
        return geometry2ewkb(geometry, dimensionTarget.dimensionSystem)
    }

    /**
     * Converts Geometry to EWKB.
     *
     * @param geometry source geometry
     * @param dimensionSystem dimension system
     * @return EWKB representation
     */
    fun geometry2ewkb(geometry: Geometry?, dimensionSystem: Int): ByteArray? {
        // Write an EWKB
        val output = ByteArrayOutputStream()
        val target = EWKBTarget(output, dimensionSystem)
        parseGeometry(geometry!!, target)
        return output.toByteArray()
    }

    /**
     * Parses a JTS Geometry object.
     *
     * @param geometry
     * geometry to parse
     * @param target
     * output target
     */
    fun parseGeometry(geometry: Geometry, target: GeometryUtils.Target) = parseGeometry(geometry, target, 0)

    /**
     * Parses a JTS Geometry object.
     *
     * @param geometry geometry to parse
     * @param target output target
     * @param parentType type of parent geometry collection, or 0 for the root geometry
     */
    private fun parseGeometry(geometry: Geometry, target: GeometryUtils.Target, parentType: Int) {
        if (parentType == 0) target.init(geometry.srid)

        when (geometry) {
            is Point -> {
                require(!(parentType != 0 && parentType != GeometryUtils.MULTI_POINT && parentType != GeometryUtils.GEOMETRY_COLLECTION))
                target.startPoint()
                val p = geometry
                if (p.isEmpty) {
                    target.addCoordinate(Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0, 1)
                } else {
                    val sequence = p.coordinateSequence
                    addCoordinate(sequence, target, 0, 1)
                }
                target.endObject(GeometryUtils.POINT)
            }
            is LineString -> {
                require(!(parentType != 0 && parentType != GeometryUtils.MULTI_LINE_STRING && parentType != GeometryUtils.GEOMETRY_COLLECTION))
                val cs = geometry.coordinateSequence
                val numPoints = cs.size()
                require(!(numPoints < 0 || numPoints == 1))
                target.startLineString(numPoints)
                for (i in 0 until numPoints) {
                    addCoordinate(cs, target, i, numPoints)
                }
                target.endObject(GeometryUtils.LINE_STRING)
            }
            is Polygon -> {
                require(!(parentType != 0 && parentType != GeometryUtils.MULTI_POLYGON && parentType != GeometryUtils.GEOMETRY_COLLECTION))
                val p = geometry
                val numInner = p.numInteriorRing
                require(numInner >= 0)
                var cs = p.exteriorRing.coordinateSequence
                var size = cs.size()
                // Size may be 0 (EMPTY) or 4+
                require(!(size < 0 || size >= 1 && size <= 3))
                require(!(size == 0 && numInner > 0))
                target.startPolygon(numInner, size)
                if (size > 0) {
                    JTSUtils.addRing(cs, target, size)
                    for (i in 0 until numInner) {
                        cs = p.getInteriorRingN(i).coordinateSequence
                        size = cs.size()
                        // Size may be 0 (EMPTY) or 4+
                        require(!(size < 0 || size >= 1 && size <= 3))
                        target.startPolygonInner(size)
                        JTSUtils.addRing(cs, target, size)
                    }
                    target.endNonEmptyPolygon()
                }
                target.endObject(GeometryUtils.POLYGON)
            }
            is GeometryCollection -> {
                require(!(parentType != 0 && parentType != GeometryUtils.GEOMETRY_COLLECTION))
                val gc = geometry
                val type: Int = when (gc) {
                    is MultiPoint -> GeometryUtils.MULTI_POINT
                    is MultiLineString -> GeometryUtils.MULTI_LINE_STRING
                    is MultiPolygon -> GeometryUtils.MULTI_POLYGON
                    else -> GeometryUtils.GEOMETRY_COLLECTION
                }
                val numItems = gc.numGeometries
                require(numItems >= 0)
                target.startCollection(type, numItems)
                for (i in 0 until numItems) {
                    val innerTarget = target.startCollectionItem(i, numItems)
                    parseGeometry(gc.getGeometryN(i), innerTarget, type)
                    target.endCollectionItem(innerTarget, type, i, numItems)
                }
                target.endObject(type)
            }
            else -> throw IllegalArgumentException()
        }
    }

    private fun addRing(sequence: CoordinateSequence, target: GeometryUtils.Target, size: Int) {
        // 0 or 4+ are valid
        if (size < 4) return

        val startX = GeometryUtils.toCanonicalDouble(sequence.getX(0))
        val startY = GeometryUtils.toCanonicalDouble(sequence.getY(0))
        addCoordinate(sequence, target, 0, size, startX, startY)
        for (i in 1 until size - 1) {
            addCoordinate(sequence, target, i, size)
        }
        val endX = GeometryUtils.toCanonicalDouble(sequence.getX(size - 1))
        //
        val endY = GeometryUtils.toCanonicalDouble(sequence.getY(size - 1))
        /*
     * TODO OGC 06-103r4 determines points as equal if they have the
     * same X and Y coordinates. Should we check Z and M here too?
     */require(!(startX != endX || startY != endY))
        addCoordinate(sequence, target, size - 1, size, endX, endY)
    }


    private fun addCoordinate(sequence: CoordinateSequence,
                              target: GeometryUtils.Target,
                              index: Int,
                              total: Int) {
        addCoordinate(sequence,
                target,
                index,
                total,
                GeometryUtils.toCanonicalDouble(sequence.getX(index)),
                GeometryUtils.toCanonicalDouble(sequence.getY(index)))
    }

    private fun addCoordinate(sequence: CoordinateSequence,
                              target: GeometryUtils.Target,
                              index: Int,
                              total: Int,
                              x: Double,
                              y: Double) {
        val z = GeometryUtils.toCanonicalDouble(sequence.getZ(index))
        val m = GeometryUtils.toCanonicalDouble(sequence.getM(index))
        target.addCoordinate(x, y, z, m, index, total)
    }
}