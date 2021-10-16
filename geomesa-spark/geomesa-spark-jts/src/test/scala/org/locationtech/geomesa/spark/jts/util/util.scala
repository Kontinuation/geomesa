/***********************************************************************
 * Copyright (c) 2013-2022 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.spark.jts.util

import org.locationtech.jts.geom.{Geometry, GeometryCollection, LineString, MultiLineString, MultiPoint, MultiPolygon, Point, Polygon}

package object util {
  case class PointContainer(geom: Point)
  case class PolygonContainer(geom: Polygon)
  case class LineStringContainer(geom: LineString)
  case class MultiPointContainer(geom: MultiPoint)
  case class MultiLineStringContainer(geom: MultiLineString)
  case class MultiPolygonContainer(geom: MultiPolygon)
  case class GeometryContainer(geom: Geometry)
  case class GeometryCollectionContainer(geom: GeometryCollection)
}
