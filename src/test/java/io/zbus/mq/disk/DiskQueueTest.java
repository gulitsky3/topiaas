package io.zbus.mq.disk;

import java.io.File;

import io.zbus.mq.Protocol.ChannelInfo;
import io.zbus.mq.model.Channel;

public class DiskQueueTest {

	public static void main(String[] args) throws Exception { 
		File baseDir = new File("/tmp");
		DiskQueue q = new DiskQueue("/", baseDir);
		System.out.println(q.name());
		q = new DiskQueue("/abc/def/", baseDir);
		Channel channel = new Channel(q.name());
		q.saveChannel(channel);
		System.out.println(q.name());
		ChannelInfo info = q.channel(q.name());
		System.out.println(info.name);
	}

}
