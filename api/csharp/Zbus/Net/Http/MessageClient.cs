﻿using System;
using Zbus.Net.Tcp;

namespace Zbus.Net.Http
{
   public interface IMessageInvoker : IInvoker<Message, Message>, IDisposable
   { 
   }
   public class MessageClient : Client<Message>, IMessageInvoker
   {
      public MessageClient(): base(new MessageCodec())
      { 
      }

      public override void Heartbeat()
      {
         Message msg = new Message();
         msg.Cmd = Message.HEARTBEAT;
         SendAsync(msg);
      }
   } 
}