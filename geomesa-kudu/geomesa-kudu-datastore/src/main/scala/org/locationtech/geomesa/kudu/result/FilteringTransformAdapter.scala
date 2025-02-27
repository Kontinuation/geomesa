/***********************************************************************
 * Copyright (c) 2013-2022 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.kudu.result

import org.apache.kudu.client.RowResult
import org.geotools.filter.text.ecql.ECQL
import org.locationtech.geomesa.features.TransformSimpleFeature
import org.locationtech.geomesa.filter.factory.FastFilterFactory
import org.locationtech.geomesa.filter.{FilterHelper, filterToString}
import org.locationtech.geomesa.kudu.result.KuduResultAdapter.KuduResultAdapterSerialization
import org.locationtech.geomesa.kudu.schema.KuduIndexColumnAdapter.{FeatureIdAdapter, VisibilityAdapter}
import org.locationtech.geomesa.kudu.schema.{KuduSimpleFeatureSchema, RowResultSimpleFeature}
import org.locationtech.geomesa.security.{SecurityUtils, VisibilityEvaluator}
import org.locationtech.geomesa.utils.collection.CloseableIterator
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes
import org.locationtech.geomesa.utils.geotools.Transform.Transforms
import org.locationtech.geomesa.utils.io.ByteBuffers.ExpandingByteBuffer
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.opengis.filter.Filter

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
  * Converts rows into simple features, first filtering and then transforming
  *
  * @param sft simple feature type
  * @param auths authorizations
  * @param ecql filter
  * @param tsft transform simple feature type
  * @param tdefs transform definitions
  */
case class FilteringTransformAdapter(sft: SimpleFeatureType,
                                     auths: Seq[Array[Byte]],
                                     ecql: Filter,
                                     tsft: SimpleFeatureType,
                                     tdefs: String) extends KuduResultAdapter {

  import org.locationtech.geomesa.filter.RichTransform.RichTransform

  // determine all the attributes that we need to be able to evaluate the transform and filter
  private val attributes = {
    val fromTransform = Transforms(sft, tdefs).flatMap(_.properties)
    val fromFilter = FilterHelper.propertyNames(ecql, sft)
    (fromTransform ++ fromFilter).distinct
  }

  private val schema = KuduSimpleFeatureSchema(sft)
  private val feature = new RowResultSimpleFeature(sft, FeatureIdAdapter, schema.adapters)
  private val transformFeature = TransformSimpleFeature(sft, tsft, tdefs)
  transformFeature.setFeature(feature)

  override val columns: Seq[String] =
    Seq(FeatureIdAdapter.name, VisibilityAdapter.name) ++ schema.schema(attributes).map(_.getName)

  override def result: SimpleFeatureType = tsft

  override def adapt(results: CloseableIterator[RowResult]): CloseableIterator[SimpleFeature] = {
    results.flatMap { row =>
      val vis = VisibilityAdapter.readFromRow(row)
      if ((vis == null || VisibilityEvaluator.parse(vis).evaluate(auths)) &&
          { feature.setRowResult(row); ecql.evaluate(feature)}) {
        SecurityUtils.setFeatureVisibility(feature, vis)
        Iterator.single(transformFeature)
      } else {
        CloseableIterator.empty
      }
    }
  }

  override def toString: String =
    s"FilteringTransformAdapter(sft=${sft.getTypeName}{${SimpleFeatureTypes.encodeType(sft)}}, " +
        s"filter=${filterToString(ecql)}, transform=$tdefs, " +
        s"auths=${auths.map(new String(_, StandardCharsets.UTF_8)).mkString(",")})"
}

object FilteringTransformAdapter extends KuduResultAdapterSerialization[FilteringTransformAdapter] {
  override def serialize(adapter: FilteringTransformAdapter, bb: ExpandingByteBuffer): Unit = {
    bb.putString(adapter.sft.getTypeName)
    bb.putString(SimpleFeatureTypes.encodeType(adapter.sft, includeUserData = true))
    bb.putInt(adapter.auths.length)
    adapter.auths.foreach(bb.putBytes)
    bb.putString(ECQL.toCQL(adapter.ecql))
    bb.putString(SimpleFeatureTypes.encodeType(adapter.tsft, includeUserData = true))
    bb.putString(adapter.tdefs)
  }

  override def deserialize(bb: ByteBuffer): FilteringTransformAdapter = {
    import org.locationtech.geomesa.utils.io.ByteBuffers.RichByteBuffer

    val sft = SimpleFeatureTypes.createType(bb.getString, bb.getString)
    val auths = Seq.fill(bb.getInt)(bb.getBytes)
    val ecql = FastFilterFactory.toFilter(sft, bb.getString)
    val tsft = SimpleFeatureTypes.createType(sft.getTypeName, bb.getString)
    val tdefs = bb.getString
    FilteringTransformAdapter(sft, auths, ecql, tsft, tdefs)
  }
}