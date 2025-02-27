/***********************************************************************
 * Copyright (c) 2013-2022 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/


package org.locationtech.geomesa.process

import org.geotools.process.factory.AnnotatedBeanProcessFactory
import org.geotools.text.Text
import org.locationtech.geomesa.process.GeoMesaProcessFactory.{Name, NameSpace, processes}

import java.util.ServiceLoader

class GeoMesaProcessFactory extends AnnotatedBeanProcessFactory(Name, NameSpace, processes: _*)

object GeoMesaProcessFactory {

  val NameSpace = "geomesa"
  val Name = Text.text("GeoMesa Process Factory")

  def processes: Array[Class[_]] = {
    import scala.collection.JavaConverters._
    ServiceLoader.load(classOf[GeoMesaProcess]).iterator().asScala.map(_.getClass).toArray
  }
}
