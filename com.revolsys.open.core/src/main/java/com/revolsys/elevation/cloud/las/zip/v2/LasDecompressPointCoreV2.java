/*
 * Copyright 2007-2012, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.revolsys.elevation.cloud.las.zip.v2;

import static com.revolsys.elevation.cloud.las.zip.MyDefs.U32_ZERO_BIT_0;
import static com.revolsys.elevation.cloud.las.zip.MyDefs.U8_FOLD;
import static com.revolsys.elevation.cloud.las.zip.StreamingMedian5.newStreamingMedian5;
import static com.revolsys.elevation.cloud.las.zip.v2.Common_v2.number_return_level;
import static com.revolsys.elevation.cloud.las.zip.v2.Common_v2.number_return_map;

import com.revolsys.elevation.cloud.las.LasPoint;
import com.revolsys.elevation.cloud.las.LasPointCloud;
import com.revolsys.elevation.cloud.las.zip.ArithmeticDecoder;
import com.revolsys.elevation.cloud.las.zip.ArithmeticModel;
import com.revolsys.elevation.cloud.las.zip.IntegerCompressor;
import com.revolsys.elevation.cloud.las.zip.LasDecompressPointCore;
import com.revolsys.elevation.cloud.las.zip.StreamingMedian5;

public class LasDecompressPointCoreV2 extends LasDecompressPointCore {

  private final ArithmeticModel decompressScanAngleRankFalse = ArithmeticDecoder
    .createSymbolModel(256);

  private final ArithmeticModel decompressScanAngleRankTrue = ArithmeticDecoder
    .createSymbolModel(256);

  private final int[] last_height = new int[8]; // signed

  private final int[] lastIntensity = new int[16];

  private final StreamingMedian5[] lastXDiffMedian5 = newStreamingMedian5(16);

  private final StreamingMedian5[] lastYDiffMedian5 = newStreamingMedian5(16);

  private byte returnCount;

  private byte returnNumber;

  public LasDecompressPointCoreV2(final LasPointCloud pointCloud, final ArithmeticDecoder decoder) {
    super(pointCloud, decoder);

    this.decompressDeltaX = new IntegerCompressor(decoder, 32, 2);
    this.decompressDeltaY = new IntegerCompressor(decoder, 32, 22);
    this.decompressZ = new IntegerCompressor(decoder, 32, 20);
    this.decompressIntensity = new IntegerCompressor(decoder, 16, 4);
    this.decompressPointSourceId = new IntegerCompressor(decoder, 16);
  }

  @Override
  public boolean init(final LasPoint point) {
    super.init(point);
    for (int i = 0; i < 16; i++) {
      this.lastXDiffMedian5[i].init();
      this.lastYDiffMedian5[i].init();
      this.lastIntensity[i] = 0;
      this.last_height[i / 2] = 0;
    }

    ArithmeticModel.initSymbolModel(this.decompressScanAngleRankFalse);
    ArithmeticModel.initSymbolModel(this.decompressScanAngleRankTrue);

    this.returnNumber = point.getReturnNumber();
    this.returnCount = point.getNumberOfReturns();

    this.intensity = 0;

    return true;
  }

  @Override
  public void read(final LasPoint point) {

    final int changedValues = this.decoder.decodeSymbol(this.changedValues);

    final int m = number_return_map[this.returnCount][this.returnNumber];
    final int l = number_return_level[this.returnCount][this.returnNumber];

    if (changedValues != 0) {
      // decompress the edge_of_flight_line, scan_direction_flag, ... if it has changed
      if ((changedValues & 32) != 0) {
        this.returnByte = read(this.decompressBitByte, this.returnByte);
      }

      // decompress the intensity if it has changed
      if ((changedValues & 16) != 0) {
        this.intensity = this.decompressIntensity.decompress(this.lastIntensity[m], m < 3 ? m : 3);
        this.lastIntensity[m] = this.intensity;
      } else {
        this.intensity = this.lastIntensity[m];
      }

      if ((changedValues & 8) != 0) {
        this.classificationByte = read(this.decompressClassification, this.classificationByte);
      }

      // decompress the scan_angle_rank ... if it has changed
      if ((changedValues & 4) != 0) {
        ArithmeticModel model;
        if (this.scanDirectionFlag) {
          model = this.decompressScanAngleRankTrue;
        } else {
          model = this.decompressScanAngleRankFalse;
        }
        final int val = this.decoder.decodeSymbol(model);
        this.scanAngleRank = (short)Byte.toUnsignedInt(U8_FOLD(val + (byte)this.scanAngleRank));
      }

      if ((changedValues & 2) != 0) {
        this.userData = read(this.decompressUserData, this.userData);
      }

      // decompress the point_source_ID ... if it has changed
      if ((changedValues & 1) != 0) {
        this.pointSourceId = this.decompressPointSourceId.decompress(this.pointSourceId);
      }
    }

    // decompress x coordinate
    final int medianX = this.lastXDiffMedian5[m].get();
    final int diffX = this.decompressDeltaX.decompress(medianX, this.returnCount == 1 ? 1 : 0);
    this.x += diffX;
    this.lastXDiffMedian5[m].add(diffX);

    // decompress y coordinate
    final int medianY = this.lastYDiffMedian5[m].get();
    final int kBitsY = this.decompressDeltaX.getK();
    final int diffY = this.decompressDeltaY.decompress(medianY,
      (this.returnCount == 1 ? 1 : 0) + (kBitsY < 20 ? U32_ZERO_BIT_0(kBitsY) : 20));
    this.y += diffY;
    this.lastYDiffMedian5[m].add(diffY);

    // decompress z coordinate
    final int kBitsZ = (this.decompressDeltaX.getK() + this.decompressDeltaY.getK()) / 2;
    this.z = this.decompressZ.decompress(this.last_height[l],
      (this.returnCount == 1 ? 1 : 0) + (kBitsZ < 18 ? U32_ZERO_BIT_0(kBitsZ) : 18));
    this.last_height[l] = this.z;

    postRead(point);
  }

}
