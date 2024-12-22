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

package org.zbus.zookeeper.server.quorum;

import org.zbus.jute.*;
public class LearnerInfo implements Record {
  private long serverid;
  private int protocolVersion;
  public LearnerInfo() {
  }
  public LearnerInfo(
        long serverid,
        int protocolVersion) {
    this.serverid=serverid;
    this.protocolVersion=protocolVersion;
  }
  public long getServerid() {
    return serverid;
  }
  public void setServerid(long m_) {
    serverid=m_;
  }
  public int getProtocolVersion() {
    return protocolVersion;
  }
  public void setProtocolVersion(int m_) {
    protocolVersion=m_;
  }
  public void serialize(OutputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(this,tag);
    a_.writeLong(serverid,"serverid");
    a_.writeInt(protocolVersion,"protocolVersion");
    a_.endRecord(this,tag);
  }
  public void deserialize(InputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(tag);
    serverid=a_.readLong("serverid");
    protocolVersion=a_.readInt("protocolVersion");
    a_.endRecord(tag);
}
  public String toString() {
    try {
      java.io.ByteArrayOutputStream s =
        new java.io.ByteArrayOutputStream();
      CsvOutputArchive a_ = 
        new CsvOutputArchive(s);
      a_.startRecord(this,"");
    a_.writeLong(serverid,"serverid");
    a_.writeInt(protocolVersion,"protocolVersion");
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
    if (!(peer_ instanceof LearnerInfo)) {
      throw new ClassCastException("Comparing different types of records.");
    }
    LearnerInfo peer = (LearnerInfo) peer_;
    int ret = 0;
    ret = (serverid == peer.serverid)? 0 :((serverid<peer.serverid)?-1:1);
    if (ret != 0) return ret;
    ret = (protocolVersion == peer.protocolVersion)? 0 :((protocolVersion<peer.protocolVersion)?-1:1);
    if (ret != 0) return ret;
     return ret;
  }
  public boolean equals(Object peer_) {
    if (!(peer_ instanceof LearnerInfo)) {
      return false;
    }
    if (peer_ == this) {
      return true;
    }
    LearnerInfo peer = (LearnerInfo) peer_;
    boolean ret = false;
    ret = (serverid==peer.serverid);
    if (!ret) return ret;
    ret = (protocolVersion==peer.protocolVersion);
    if (!ret) return ret;
     return ret;
  }
  public int hashCode() {
    int result = 17;
    int ret;
    ret = (int) (serverid^(serverid>>>32));
    result = 37*result + ret;
    ret = (int)protocolVersion;
    result = 37*result + ret;
    return result;
  }
  public static String signature() {
    return "LLearnerInfo(li)";
  }
}
