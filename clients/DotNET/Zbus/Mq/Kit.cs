﻿using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using System;
using System.Linq.Expressions;
using System.Reflection;
using System.Runtime.Remoting.Messaging;
using System.Runtime.Remoting.Proxies;
using System.Threading.Tasks;

namespace Zbus.Mq
{ 

    public static class ConvertKit
    {
        public static JsonSerializerSettings JsonSettings = new JsonSerializerSettings
        {
            ContractResolver = new CamelCasePropertyNamesContractResolver(),
            TypeNameHandling = TypeNameHandling.Objects,
        };

        public static string SerializeObject(object value)
        {
            return JsonConvert.SerializeObject(value, JsonSettings);
        }

        public static T DeserializeObject<T>(string value)
        {
            return JsonConvert.DeserializeObject<T>(value, JsonSettings);
        }

        public static object Convert(object raw, Type type)
        {
            if (raw == null)
            {
                return null;
            }

            if (type == typeof(void)) return null;

            if (raw.GetType().IsAssignableFrom(type)) return raw;

            string jsonRaw = JsonConvert.SerializeObject(raw);
            return JsonConvert.DeserializeObject(jsonRaw, type, JsonSettings);
        }
    }
}