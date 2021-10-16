/***********************************************************************
 * Copyright (c) 2013-2022 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.spark.jts.udf

import org.apache.spark.sql.{Column, Encoder, TypedColumn}
import org.locationtech.jts.geom._
import org.apache.spark.sql.functions.lit
import org.junit.runner.RunWith
import org.locationtech.geomesa.spark.jts._
import org.locationtech.geomesa.spark.jts.util.util._
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.reflect.ClassTag

@RunWith(classOf[JUnitRunner])
class GeometricCastFunctionsTest extends Specification with TestEnvironment {

  import spark.implicits._

  private def testCastFunctionOnNull[G : ClassTag, C <: AnyRef : ClassTag : Encoder](udfName: String, udf: Column => TypedColumn[Any, _]) = {
    sc.sql(s"select $udfName(null)").collect.head(0) must beNull
    dfBlank.select(udf(lit(null))).first must beNull
    dfBlank.select(udf(lit(null)) as 'geom).as[C].head must haveClass[C]
  }

  private def testCastFunctionOnWKT[G : ClassTag, C <: AnyRef : ClassTag : Encoder](wkt: String, udfName: String, udf: Column => TypedColumn[Any, _ <: AnyRef])= {
    val geom = s"st_geomFromWKT('$wkt')"
    val df = sc.sql(s"select $udfName($geom) geom")
    df.collect.head(0).asInstanceOf[AnyRef] must haveClass[G]
    dfBlank.select(udf(st_geomFromWKT(wkt))).first must haveClass[G]
    df.as[C].head must haveClass[C]
  }

  "sql geometry accessors" should {
    sequential

    // before
    step {
      // Trigger initialization of spark session
      val _ = spark
    }

    "st_castToPoint" >> {
      "null" >> {
        testCastFunctionOnNull[Point, PointContainer]("st_castToPoint", st_castToPoint)
      }
      "point" >> {
        testCastFunctionOnWKT[Point, PointContainer]("POINT(1 1)", "st_castToPoint", st_castToPoint)
      }
    }

    "st_castToPolygon" >> {
      "null" >> {
        testCastFunctionOnNull[Polygon, PolygonContainer]("st_castToPolygon", st_castToPolygon)
      }
      "polygon" >> {
        testCastFunctionOnWKT[Polygon, PolygonContainer]("POLYGON((1 1, 1 2, 2 2, 2 1, 1 1))", "st_castToPolygon", st_castToPolygon)
      }
    }

    "st_castToLineString" >> {
      "null" >> {
        testCastFunctionOnNull[LineString, LineStringContainer]("st_castToLineString", st_castToLineString)
      }
      "linestring" >> {
        testCastFunctionOnWKT[LineString, LineStringContainer]("LINESTRING(1 1, 2 2)", "st_castToLineString", st_castToLineString)
      }
    }

    "st_castToMultiPoint" >> {
      "null" >> {
        testCastFunctionOnNull[MultiPoint, MultiPointContainer]("st_castToMultiPoint", st_castToMultiPoint)
      }
      "multipoint" >> {
        testCastFunctionOnWKT[MultiPoint, MultiPointContainer]("MULTIPOINT (1 1,2 2)", "st_castToMultiPoint", st_castToMultiPoint)
      }
    }

    "st_castToMultiPolygon" >> {
      "null" >> {
        testCastFunctionOnNull[MultiPolygon, MultiPolygonContainer]("st_castToMultiPolygon", st_castToMultiPolygon)
      }
      "multipolygon" >> {
        testCastFunctionOnWKT[MultiPolygon, MultiPolygonContainer](
          "MULTIPOLYGON(((1 1,1 2,2 2,2 1,1 1)),((1 1,2 1,2 2,1 1)))", "st_castToMultiPolygon", st_castToMultiPolygon)
      }
    }

    "st_castToMultiLineString" >> {
      "null" >> {
        testCastFunctionOnNull[MultiLineString, MultiLineStringContainer]("st_castToMultiLineString", st_castToMultiLineString)
      }
      "multilinestring" >> {
        testCastFunctionOnWKT[MultiLineString, MultiLineStringContainer](
          "MULTILINESTRING((1 1,2 2),(3 3,4 4))", "st_castToMultiLineString", st_castToMultiLineString)
      }
    }

    "st_castToGeometryCollection" >> {
      "null" >> {
        testCastFunctionOnNull[GeometryCollection, GeometryCollectionContainer]("st_castToGeometryCollection", st_castToGeometryCollection)
      }
      "geometrycollection" >> {
        testCastFunctionOnWKT[GeometryCollection, GeometryCollectionContainer](
          "GEOMETRYCOLLECTION(POINT(45.0 49.0),POINT(45.1 49.1))", "st_castToGeometryCollection", st_castToGeometryCollection)
      }
    }

    "st_castToGeometry" >> {
      "null" >> {
        testCastFunctionOnNull[Geometry, GeometryContainer]("st_castToGeometry", st_castToGeometry)
      }
      "point" >> {
        testCastFunctionOnWKT[Point, GeometryContainer]("POINT(1 1)", "st_castToGeometry", st_castToGeometry)
      }
      "polygon" >> {
        testCastFunctionOnWKT[Polygon, GeometryContainer]("POLYGON((1 1, 1 2, 2 2, 2 1, 1 1))", "st_castToGeometry", st_castToGeometry)
      }
      "linestring" >> {
        testCastFunctionOnWKT[LineString, GeometryContainer]("LINESTRING(1 1, 2 2)", "st_castToGeometry", st_castToGeometry)
      }
      "multipoint" >> {
        testCastFunctionOnWKT[MultiPoint, GeometryContainer]("MULTIPOINT(1 1,2 2)", "st_castToGeometry", st_castToGeometry)
      }
      "multipolygon" >> {
        testCastFunctionOnWKT[MultiPolygon, GeometryContainer](
          "MULTIPOLYGON(((1 1,1 2,2 2,2 1,1 1)),((1 1,2 1,2 2,1 1)))", "st_castToGeometry", st_castToGeometry)
      }
      "multilinestring" >> {
        testCastFunctionOnWKT[MultiLineString, GeometryContainer](
          "MULTILINESTRING((1 1,2 2),(3 3,4 4))", "st_castToGeometry", st_castToGeometry)
      }
      "geometrycollection" >> {
        testCastFunctionOnWKT[GeometryCollection, GeometryContainer](
          "GEOMETRYCOLLECTION(POINT(45.0 49.0),POINT(45.1 49.1))", "st_castToGeometry", st_castToGeometry)
      }
    }

    "st_bytearray" >> {
      "null" >> {
        sc.sql("select st_byteArray(null)").collect.head(0) must beNull
        dfBlank.select(st_byteArray(lit(null))).first must beNull
      }

      "bytearray" >> {
        val df = sc.sql(s"select st_byteArray('foo')")
        val expected = "foo".toArray.map(_.toByte)
        df.collect.head(0) mustEqual expected
        dfBlank.select(st_byteArray(lit("foo"))).first mustEqual expected
      }
    }

    // after
    step {
      spark.stop()
    }
  }
}
