/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */

package com.linkedin.tony.common;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class OutputInfo implements Writable {
  private String localLocation;

  private String dfsLocation;

  public OutputInfo() {
  }

  public String getLocalLocation() {
    return localLocation;
  }

  public void setLocalLocation(String localLocation) {
    this.localLocation = localLocation;
  }

  public String getDfsLocation() {
    return dfsLocation;
  }

  public void setDfsLocation(String dfsLocation) {
    this.dfsLocation = dfsLocation;
  }

  public String toString() {
    return dfsLocation;
  }

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    Text.writeString(dataOutput, localLocation);
    Text.writeString(dataOutput, dfsLocation);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.localLocation = Text.readString(dataInput);
    this.dfsLocation = Text.readString(dataInput);
  }
}
