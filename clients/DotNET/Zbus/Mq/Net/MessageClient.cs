﻿using System;
using System.Threading.Tasks;
using Zbus.Net;

namespace Zbus.Mq.Net
{
    public class MessageCodec : ICodec
    {
        public object Decode(ByteBuffer buf)
        {
            return Message.Decode(buf);
        }

        public ByteBuffer Encode(object obj)
        {
            if (!(obj is Message))
            {
                throw new ArgumentException("Message type required for: " + obj);
            }
            Message msg = obj as Message;
            ByteBuffer buf = new ByteBuffer();
            msg.Encode(buf);
            buf.Flip();

            return buf;
        }
    }

    public class MessageClient : Client<Message>
   {
      public MessageClient(string serverAddress): base(serverAddress, new MessageCodec())
      { 
      } 
   } 
}
