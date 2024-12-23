// File generated by hadoop record compiler. Do not edit.
/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.zbus.zookeeper.proto;

import org.zbus.zookeeper.jute.*;
public class MultiHeader implements Record {
  private int type;
  private boolean done;
  private int err;
  public MultiHeader() {
  }
  public MultiHeader(
        int type,
        boolean done,
        int err) {
    this.type=type;
    this.done=done;
    this.err=err;
  }
  public int getType() {
    return type;
  }
  public void setType(int m_) {
    type=m_;
  }
  public boolean getDone() {
    return done;
  }
  public void setDone(boolean m_) {
    done=m_;
  }
  public int getErr() {
    return err;
  }
  public void setErr(int m_) {
    err=m_;
  }
  public void serialize(OutputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(this,tag);
    a_.writeInt(type,"type");
    a_.writeBool(done,"done");
    a_.writeInt(err,"err");
    a_.endRecord(this,tag);
  }
  public void deserialize(InputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(tag);
    type=a_.readInt("type");
    done=a_.readBool("done");
    err=a_.readInt("err");
    a_.endRecord(tag);
}
  public String toString() {
    try {
      java.io.ByteArrayOutputStream s =
        new java.io.ByteArrayOutputStream();
      CsvOutputArchive a_ = 
        new CsvOutputArchive(s);
      a_.startRecord(this,"");
    a_.writeInt(type,"type");
    a_.writeBool(done,"done");
    a_.writeInt(err,"err");
      a_.endRecord(this,"");
      return new String(s.toByteArray(), "UTF-8");
    } catch (Throwable ex) {
      ex.printStackTrace();
    }
    return "ERROR";
  }
  public void write(java.io.DataOutput out) throws java.io.IOException {
    BinaryOutputArchive archive = new BinaryOutputArchive(out);
    serialize(archive, "");
  }
  public void readFields(java.io.DataInput in) throws java.io.IOException {
    BinaryInputArchive archive = new BinaryInputArchive(in);
    deserialize(archive, "");
  }
  public int compareTo (Object peer_) throws ClassCastException {
    if (!(peer_ instanceof MultiHeader)) {
      throw new ClassCastException("Comparing different types of records.");
    }
    MultiHeader peer = (MultiHeader) peer_;
    int ret = 0;
    ret = (type == peer.type)? 0 :((type<peer.type)?-1:1);
    if (ret != 0) return ret;
    ret = (done == peer.done)? 0 : (done?1:-1);
    if (ret != 0) return ret;
    ret = (err == peer.err)? 0 :((err<peer.err)?-1:1);
    if (ret != 0) return ret;
     return ret;
  }
  public boolean equals(Object peer_) {
    if (!(peer_ instanceof MultiHeader)) {
      return false;
    }
    if (peer_ == this) {
      return true;
    }
    MultiHeader peer = (MultiHeader) peer_;
    boolean ret = false;
    ret = (type==peer.type);
    if (!ret) return ret;
    ret = (done==peer.done);
    if (!ret) return ret;
    ret = (err==peer.err);
    if (!ret) return ret;
     return ret;
  }
  public int hashCode() {
    int result = 17;
    int ret;
    ret = (int)type;
    result = 37*result + ret;
     ret = (done)?0:1;
    result = 37*result + ret;
    ret = (int)err;
    result = 37*result + ret;
    return result;
  }
  public static String signature() {
    return "LMultiHeader(izi)";
  }
}
