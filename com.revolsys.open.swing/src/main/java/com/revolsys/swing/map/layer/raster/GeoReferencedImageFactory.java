package com.revolsys.swing.map.layer.raster;

import org.springframework.core.io.Resource;

import com.revolsys.io.IoFactory;

public interface GeoReferencedImageFactory extends IoFactory {

 GeoReferencedImage loadImage(Resource resource);
}
