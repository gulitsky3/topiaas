﻿using System;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Mq.Net;

namespace Zbus.Mq
{
   
    public class MqClient : MessageClient
    {
        public string Token { get; set; } 
        public MqClient(string serverAddress)
            : base(serverAddress)
        {
        }
        public MqClient(ServerAddress serverAddress, string certFile = null)
            : base(serverAddress, certFile)
        {
        }

        private async Task<T> InvokeObjectAsync<T>(Message msg, CancellationToken? token = null) where T: ErrorInfo, new()
        {
            msg.Token = this.Token;
            if (token == null)
            {
                token = CancellationToken.None;
            }
            Message res = await this.InvokeAsync(msg, token.Value);
            if (res.Status != "200")
            {
                T info = new T();
                info.Error = res.BodyString;
                return info;
            }
            return ConvertKit.DeserializeObject<T>(res.BodyString);
        }

        private async Task CheckedInvokeAsync(Message msg, CancellationToken? token = null)
        {
            msg.Token = this.Token;
            if (token == null)
            {
                token = CancellationToken.None;
            }
            Message res = await this.InvokeAsync(msg, token.Value);
            if (res.Status != "200")
            {
                throw new MqException(res.BodyString);
            }  
        }

        public async Task<ServerInfo> QueryServerAsync(CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.QUERY,
            }; 
            return await InvokeObjectAsync<ServerInfo>(msg, token);
        }

        public async Task<TopicInfo> QueryTopicAsync(string topic, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.QUERY,
                Topic = topic,
            };
            return await InvokeObjectAsync<TopicInfo>(msg, token);
        }

        public async Task<ConsumeGroupInfo> QueryGroupAsync(string topic, string group, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.QUERY,
                Topic = topic,
                ConsumeGroup = group,
           
            };
            return await InvokeObjectAsync<ConsumeGroupInfo>(msg, token);
        }

        public async Task<TopicInfo> DeclareTopicAsync(string topic, int? topicMask =null, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.DECLARE,
                Topic = topic, 
                TopicMask = topicMask,
            };
            return await InvokeObjectAsync<TopicInfo>(msg, token);
        }

        public async Task<ConsumeGroupInfo> DeclareGroupAsync(string topic, string group, CancellationToken? token = null)
        {
            return await DeclareGroupAsync(topic, new ConsumeGroup(group), token);
        }

        public async Task<ConsumeGroupInfo> DeclareGroupAsync(string topic, ConsumeGroup group, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.DECLARE,
                Topic = topic,
                ConsumeGroup = group.GroupName,
                GroupFilter = group.Filter,
                GroupMask = group.Mask,
                GroupStartCopy = group.StartCopy,
                GroupStartMsgid = group.StartMsgId,
                GroupStartOffset = group.StartOffset,
                GroupStartTime = group.StartTime,
            };
            return await InvokeObjectAsync<ConsumeGroupInfo>(msg, token);
        }

        public async Task RemoveTopicAsync(string topic, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.REMOVE,
                Topic = topic,
            };
            await CheckedInvokeAsync(msg, token);
        }

        public async Task RemoveGroupAsync(string topic, string group, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.REMOVE,
                Topic = topic,
                ConsumeGroup = group,
            };
            await CheckedInvokeAsync(msg, token);
        }

        public async Task EmptyTopicAsync(string topic, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.EMPTY,
                Topic = topic,
            };
            await CheckedInvokeAsync(msg, token);
        }

        public async Task EmptyGroupAsync(string topic, string group, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.EMPTY,
                Topic = topic,
                ConsumeGroup = group,
            };
            await CheckedInvokeAsync(msg, token);
        }
    }

    public class MqClientPool : Pool<MqClient>
    {
        private readonly ServerAddress serverAddress;
        private readonly string certFile;
        public MqClientPool(string serverAddress)
        {
            this.serverAddress = new ServerAddress(serverAddress);
        }
        public MqClientPool(ServerAddress serverAddress, string certFile = null)
        {
            this.serverAddress = serverAddress;
            this.certFile = certFile;

            ObjectActive = IsClientActive;
            ObjectFactory = ClientFacotry;
        }

        private bool IsClientActive(MqClient client)
        {
            return client.Connected;
        }

        private MqClient ClientFacotry()
        {
            return new MqClient(this.serverAddress, this.certFile);
        }
    }
}
