package io.zbus.mq.commands;

import java.io.IOException;

import io.zbus.transport.Message;
import io.zbus.transport.Session;

public interface CommandHandler{
	void handle(Message msg, Session sess) throws IOException;
}