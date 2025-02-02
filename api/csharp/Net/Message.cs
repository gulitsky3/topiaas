﻿using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

namespace Zbus.Net
{

    public class Message
    {
        public static readonly string REMOTE_ADDR = "remote-addr";


        public static readonly string CMD = "cmd";
        public static readonly string BROKER = "broker";
        public static readonly string TOPIC = "topic";
        public static readonly string SENDER = "sender";
        public static readonly string RECVER = "recver";
        public static readonly string MQ = "mq";
        public static readonly string ID = "id";
        public static readonly string ACK = "ack";
        public static readonly string ENCODING = "encoding";
        public static readonly string ORIGIN_URL = "origin_url";
        public static readonly string ORIGIN_STATUS = "reply_code";
        public static readonly string ORIGIN_ID = "rawid";

        private Meta meta = new Meta("");
        private IDictionary<string, string> head = new Dictionary<string, string>();
        private byte[] body;

        public string GetHead(string key)
        {
            return GetHead(key, null);
        }

        public string GetHead(string key, string defaultValue)
        {
            string val = null;
            this.head.TryGetValue(key, out val);
            if (val == null)
            {
                val = defaultValue;
            }
            return val;
        }

        public void SetHead(string key, string val)
        {
            this.head[key] = val;
        }

        public void RemoveHead(string key)
        {
            this.head.Remove(key);
        }

        public void SetBody(byte[] body)
        {
            this.body = body;
            int bodyLen = 0;
            if (this.body != null)
            {
                bodyLen = this.body.Length;
            }
            this.SetHead("content-length", string.Format("{0}", bodyLen));
        }

        public void SetJsonBody(string body, System.Text.Encoding encoding)
        {
            this.SetBody(body, encoding);
            this.SetHead("content-type", "application/json");
        }

        public void SetJsonBody(string body)
        {
            SetJsonBody(body, System.Text.Encoding.Default);
        }

        public void SetBody(string body, System.Text.Encoding encoding)
        {
            SetBody(encoding.GetBytes(body));
        }

        public void SetBody(string body)
        {
            SetBody(body, System.Text.Encoding.Default);
        }

        public void SetBody(string format, params object[] args)
        {
            SetBody(string.Format(format, args));
        }


        #region AUX_GET_SET

        public String Mq
        {
            get
            {
                return GetHead(MQ);
            }
            set
            {
                SetHead(MQ, value);

            }
        }

        public String Cmd
        {
            get
            {
                return GetHead(CMD);
            }
            set
            {
                SetHead(CMD, value);

            }
        }


        public String Id
        {
            get
            {
                return GetHead(ID);
            }
            set
            {
                SetHead(ID, value);

            }
        }

        public String Sender
        {
            get
            {
                return GetHead(SENDER);
            }
            set
            {
                SetHead(SENDER, value);

            }
        }
        public String Recver
        {
            get
            {
                return GetHead(RECVER);
            }
            set
            {
                SetHead(RECVER, value);

            }
        }
        public String Topic
        {
            get
            {
                return GetHead(TOPIC);
            }
            set
            {
                SetHead(TOPIC, value);

            }
        }

        public String Encoding
        {
            get
            {
                return GetHead(ENCODING);
            }
            set
            {
                SetHead(ENCODING, value);

            }
        }

        public String OriginUrl
        {
            get
            {
                return GetHead(ORIGIN_URL);
            }
            set
            {
                SetHead(ORIGIN_URL, value);

            }
        }

        public String OriginStatus
        {
            get
            {
                return GetHead(ORIGIN_STATUS);
            }
            set
            {
                SetHead(ORIGIN_STATUS, value);

            }
        }

        public String OriginId
        {
            get
            {
                return GetHead(ORIGIN_ID);
            }
            set
            {
                SetHead(ORIGIN_ID, value);

            }
        }

        public bool Ack
        {
            get
            {
                string ack = GetHead(ACK);
                if (ack == null) return true; //default to true
                return "1".Equals(ack);
            }
            set
            {
                string val = value ? "1" : "0";
                SetHead(ACK, val);
            }
        }

        public string Url
        {
            get
            {
                return this.meta.Url;
            }
            set
            {
                this.meta.Url = value;
            }
        }

        public string RequestPath
        {
            get
            {
                return this.meta.RequestPath;
            }
            set
            {
                this.meta.RequestPath = value;
            }
        }

        public IDictionary<string, string> RequestParams
        {
            get
            {
                return this.meta.RequestParams;
            }
            set
            {
                this.meta.RequestParams = value;
            }
        }

        public string Status
        {
            get
            {
                return this.meta.Status;
            }
            set
            { 
                this.meta.Status = value;
            }
        }

        public bool IsStatus200()
        {
            return "200".Equals(this.meta.Status);
        }
        public bool IsStatus404()
        {
            return "404".Equals(this.meta.Status);
        }
        public bool IsStatus500()
        {
            return "500".Equals(this.meta.Status);
        }

#endregion

        public void Encode(IoBuffer buf)
        {
            buf.Put("{0}\r\n", this.meta);
            foreach (KeyValuePair<string, string> e in this.head)
            {
                buf.Put("{0}: {1}\r\n", e.Key, e.Value);
            }
            string lenKey = "content-length";
            if (!this.head.ContainsKey(lenKey))
            {
                int bodyLen = this.body == null ? 0 : this.body.Length;
                buf.Put("{0}: {1}\r\n", lenKey, bodyLen);
            }

            buf.Put("\r\n");

            if (this.body != null)
            {
                buf.Put(this.body);
            }
        }

        private static int FindHeaderEnd(IoBuffer buf)
        {
            int i = buf.Position;
            byte[] data = buf.Data;
            while (i + 3 < buf.Limit)
            {
                if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n')
                {
                    return i + 3;
                }
                i++;
            }
            return -1;

        }

        public static Message DecodeHeader(string header)
        {
            Message msg = new Message();
            string[] lines = Regex.Split(header, "\r\n");
            msg.meta = new Meta(lines[0]);
            for (int i = 1; i < lines.Length; i++)
            {
                string line = lines[i];
                int idx = line.IndexOf(':');
                if (idx < 0) continue; //ignore
                string key = line.Substring(0, idx).Trim().ToLower(); //key to lower case
                string val = line.Substring(idx + 1).Trim();
                msg.SetHead(key, val);
            }

            return msg;
        }

        public static Message Decode(IoBuffer buf)
        {
            int idx = FindHeaderEnd(buf);
            int headLen = idx - buf.Position+1;
            if (idx < 0) return null;

            string header = System.Text.Encoding.Default.GetString(buf.Data, buf.Position, headLen);

            Message msg = DecodeHeader(header);
            string bodyLenString = msg.GetHead("content-length");
            if (bodyLenString == null)
            {
                buf.Drain(headLen);
                return msg;
            }
            int bodyLen = int.Parse(bodyLenString);
            if (buf.Remaining() < headLen + bodyLen)
            {
                return null;
            }
            buf.Drain(headLen);
            byte[] body = buf.Get(bodyLen);
            msg.SetBody(body);
            return msg;
        }

        public override string ToString()
        {
            IoBuffer buf = new IoBuffer();
            Encode(buf);
            return buf.ReadAllToString();
        }

        public string GetBody()
        {
            return System.Text.Encoding.Default.GetString(body);
        }

        public string GetBody(System.Text.Encoding encoding)
        {
            return encoding.GetString(body);
        }
    }

    class Meta
    {
        static readonly HashSet<string> httpMethod = new HashSet<string>();
        static IDictionary<string, string> httpStatus = new Dictionary<string, string>();
        static Meta()
        {
            httpMethod.Add("GET");
            httpMethod.Add("POST");
            httpMethod.Add("HEAD");
            httpMethod.Add("PUT");
            httpMethod.Add("DELETE");
            httpMethod.Add("OPTIONS");

            httpStatus.Add("200", "OK");
            httpStatus.Add("201", "Created");
            httpStatus.Add("202", "Accepted");
            httpStatus.Add("204", "No Content");
            httpStatus.Add("206", "Partial Content");
            httpStatus.Add("301", "Moved Permanently");
            httpStatus.Add("304", "Not Modified");
            httpStatus.Add("400", "Bad Request");
            httpStatus.Add("401", "Unauthorized");
            httpStatus.Add("403", "Forbidden");
            httpStatus.Add("404", "Not Found");
            httpStatus.Add("405", "Method Not Allowed");
            httpStatus.Add("416", "Requested Range Not Satisfiable");
            httpStatus.Add("500", "Internal Server Error");
        }

        public string Method { get; set; }
        public string Status { get; set; }
        public string Url
        {
            get
            {
                return _url;
            }
            set
            {
                _url = value;
                Status = null;
            }
        }
        public string RequestPath
        {
            get
            {
                return _path;
            }
            set
            {
                this._path = value;
                CalcUrl();
            }
        }

        public IDictionary<string, string> RequestParams
        {
            get
            {
                return _requestParams;
            }
            set
            {
                this._requestParams = value;
                CalcUrl();
            }
        }

        private string _url;
        private string _path; 
        private IDictionary<string, string> _requestParams; 
        public string GetParam(string key)
        {
            return GetParam(key, null);
        }

        public string GetParam(string key, string defaultValue)
        {
            if (this._requestParams == null)
                return defaultValue;
            string value;
            this._requestParams.TryGetValue(key, out value);
            if (value == null)
                value = defaultValue;
            return value;
        }

        public void SetParam(string key, string val)
        {
            if (this._requestParams == null)
            {
                this._requestParams = new Dictionary<string, string>();
            }
            this._requestParams[key] = val;
            CalcUrl();
        }



        public Meta(string meta)
        {
            this._url = "/";
            this.Method = "GET";
            if (meta == null || meta.Trim().Equals("")) return;
            string[] blocks = meta.Split(' ');
            string method = blocks[0];
            if (!httpMethod.Contains(method))
            {
                this.Status = blocks[1];
                return;
            }
            this.Method = method; 
            this.DecodeUrl(blocks[1]);
        }

        public override string ToString()
        {
            if (this.Status != null)
            {
                string desc = null;
                httpStatus.TryGetValue(this.Status, out desc);
                if (desc == null)
                {
                    desc = "Unknown Status";
                }
                return string.Format("HTTP/1.1 {0} {1}", this.Status, desc);
            }
            else 
            {
                return string.Format("{0} {1} HTTP/1.1", this.Method, this.Url);
            } 
        }

        private void CalcUrl()
        {
            StringBuilder sb = new StringBuilder();
            if (_path != null)
            {
                sb.Append(_path);
            }
            if(_requestParams != null)
            {
                sb.Append("?"); 
                var iter = _requestParams.GetEnumerator();
                bool first = true;
                while (iter.MoveNext())
                {  
                    if (!first)
                    {
                        sb.Append("&&");
                    }
                    else
                    {
                        first = false;
                    }

                    sb.Append(iter.Current.Key + "=" + iter.Current.Value); 
                }
            }
            this.Url = sb.ToString();

        }

        public void DecodeUrl(string url)
        {
            this._url = url;

            int idx = url.IndexOf('?');
            if (idx < 0)
            {
                this._path = url;
            }
            else
            {
                this._path = url.Substring(0, idx);
            }
            if (this._path[0] == '/')
            {
                this._path = this._path.Substring(1);
            } 

            if (idx < 0) return;
            string args = url.Substring(idx + 1);
            this._requestParams = new Dictionary<string, string>();
            string[] blocks = args.Split('&');
            foreach (string kv in blocks)
            {
                string[] kvb = kv.Split("=".ToCharArray(), 2);
                this._requestParams[kvb[0].Trim()] = kvb[1].Trim();
            }
        }

    }
}
