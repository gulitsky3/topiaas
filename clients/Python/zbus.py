#encoding=utf8  
import uuid, time, json
import logging.config, os 
import threading 
import socket, ssl 
import queue as Queue
    
class Protocol:  
    VERSION_VALUE = "0.8.0"       #start from 0.8.0 
    #############Command Values############
    #MQ Produce/Consume
    PRODUCE = "produce"
    CONSUME = "consume"
    RPC = "rpc"
    ROUTE = "route"     #route back message to sender, designed for RPC
    
    #Topic/ConsumeGroup control
    DECLARE = "declare"
    QUERY = "query"
    REMOVE = "remove"
    EMPTY = "empty"
    
    #Tracker
    TRACK_PUB = "track_pub"
    TRACK_SUB = "track_sub"
    
    COMMAND = "cmd"
    TOPIC = "topic";
    TOPIC_FLAG = "topic_flag"
    TAG = "tag"
    OFFSET = "offset"
    
    CONSUME_GROUP = "consume_group"
    CONSUME_GROUP_COPY_FROM = "consume_group_copy_from"
    CONSUME_START_OFFSET = "consume_start_offset"
    CONSUME_START_MSGID = "consume_start_msgid"
    CONSUME_START_TIME = "consume_start_time"
    CONSUME_WINDOW = "consume_window"
    CONSUME_FILTER_TAG = "consume_filter_tag"
    CONSUME_GROUP_FLAG = "consume_group_flag"
    
    SENDER = "sender"
    RECVER = "recver"
    ID = "id"
    
    SERVER = "server"
    ACK = "ack"
    ENCODING = "encoding"
    
    ORIGIN_ID = "origin_id"         #original id, TODO compatible issue: rawid
    ORIGIN_URL = "origin_url"       #original URL  
    ORIGIN_STATUS = "origin_status" #original Status  TODO compatible issue: reply_code
    
    #Security 
    TOKEN = "token"
    
    
    ############Flag values############    
    FLAG_PAUSE = 1 << 0
    FLAG_RPC = 1 << 1
    FLAG_EXCLUSIVE = 1 << 2
    FLAG_DELETE_ON_EXIT = 1 << 3


###################################################################################   
try:
    log_file = 'log.conf'   
    if os.path.exists(log_file):
        logging.config.fileConfig(log_file)
    else:
        import os.path
        log_dir = os.path.dirname(os.path.realpath(__file__))
        log_file = os.path.join(log_dir, 'log.conf')
        logging.config.fileConfig(log_file)
except: 
    logging.basicConfig(format='%(asctime)s - %(filename)s-%(lineno)s - %(levelname)s - %(message)s')


    
class Message(dict):
    http_status = { 
        "200": "OK",
        "201": "Created",
        "202": "Accepted",
        "204": "No Content",
        "206": "Partial Content",
        "301": "Moved Permanently",
        "304": "Not Modified",
        "400": "Bad Request",
        "401": "Unauthorized", 
        "403": "Forbidden",
        "404": "Not Found", 
        "405": "Method Not Allowed", 
        "416": "Requested Range Not Satisfiable",
        "500": "Internal Server Error",
    }
    reserved_keys = set(['status', 'method', 'url', 'body'])
    
    def __init__(self, opt = None):
        self.body = None
        if opt and isinstance(opt, dict):
            for k in opt:
                self[k] = opt[k]
        
    def __getattr__(self, name):
        if name in self:
            return self[name]
        else:
            return None

    def __setattr__(self, name, value):
        self[name] = value

    def __delattr__(self, name):
        if name in self:
            del self[name] 
            
    def __getitem__(self, key):
        if key not in self:
            return None
        return dict.__getitem__(self, key)


def msg_encode(msg):
    if not isinstance(msg, dict):
        raise ValueError('%s must be dict type'%msg)
    if not isinstance(msg, Message):
        msg = Message(msg)
     
    res = bytearray() 
    if msg.status is not None:
        desc = Message.http_status.get('%s'%msg.status)
        if desc is None: desc = b"Unknown Status"
        res += bytes("HTTP/1.1 %s %s\r\n"%(msg.status, desc), 'utf8')  
    else:
        m = msg.method
        if not m: 
            m = 'GET'
        url = msg.url
        if not url:
            url = '/'
        res += bytes("%s %s HTTP/1.1\r\n"%(m, url), 'utf8') 
        
    body_len = 0
    if msg.body is not None:
        body_len = len(msg.body) 
        if not isinstance(msg.body, (bytes, bytearray)):
            if not msg['content-type']:
                msg['content-type'] = 'text/plain'
        
    for k in msg:
        if k.lower() in Message.reserved_keys: continue
        if msg[k] is None: continue
        res += bytes('%s: %s\r\n'%(k,msg[k]), 'utf8')
    len_key = 'content-length'
    if len_key not in msg:
        res += bytes('%s: %s\r\n'%(len_key, body_len), 'utf8')
    
    res += bytes('\r\n', 'utf8')
    
    body_encoding = 'utf8'
    if msg.encoding:
        body_encoding = msg.encoding
    if msg.body is not None:
        if isinstance(msg.body, (bytes, bytearray)):
            res += msg.body
        else:
            res += bytes(str(msg.body), body_encoding)
    return res


 
def find_header_end(buf, start=0):
    i = start
    end = len(buf)
    while i+3<end:
        if buf[i]==13 and buf[i+1]==10 and buf[i+2]==13 and buf[i+3]==10:
            return i+3
        i += 1
    return -1 
     
def decode_headers(buf):
    msg = Message()
    buf = buf.decode('utf8')
    lines = buf.splitlines() 
    meta = lines[0]
    blocks = meta.split()
    if meta.startswith('HTTP'):
        msg.status = blocks[1] 
    else: 
        msg.method = blocks[0]
        if len(blocks) > 1:
            msg.url = blocks[1] 
    
    for i in range(1,len(lines)):
        line = lines[i] 
        if len(line) == 0: continue
        try:
            p = line.index(':') 
            key = str(line[0:p]).strip()
            val = str(line[p+1:]).strip()
            msg[key] = val
        except Exception as e:
            logging.error(e) 
            
    return msg
  
def msg_decode(buf, start=0): 
    p = find_header_end(buf, start)
    if p < 0:
        return (None, start) 
    head = buf[start: p]
    msg = decode_headers(head) 
    if msg is None:
        return (None, start)
    p += 1 #new start

    body_len = msg['content-length']
    if body_len is None: 
        return (msg, p)
    body_len = int(body_len)
    if len(buf)-p < body_len:
        return (None, start)
    
    msg.body = buf[p: p+body_len]
    content_type = msg['content-type']
    encoding = 'utf8'
    if msg.encoding: encoding = msg.encoding
    if content_type:
        if str(content_type).startswith('text') or str(content_type) == 'application/json': 
            msg.body = msg.body.decode(encoding) 
            
    return (msg,p+body_len) 


def normalize_address(address):       
    sslEnabled = False
    if isinstance(address, dict): 
        if 'address' not in address:
            raise TypeError('missing address in dictionary')
        if 'sslEnabled' not in address:
            raise TypeError('missing sslEnabled in dictionary')
        addr_string = address['address']
        sslEnabled = address['sslEnabled']
    else:
        addr_string = address
    
    return {'address': addr_string,
            'sslEnabled': sslEnabled
    }

def address_key(address): 
    if address['sslEnabled']:
        return '[SSL]%s'%address['address']
    return address['address']
            
class MessageClient(object):
    log = logging.getLogger(__name__)
    
    def __init__(self, address='localhost:15555', ssl_cert_file=None):  
        self.server_address = normalize_address(address)
            
        self.ssl_cert_file = ssl_cert_file    
        
        bb = self.server_address['address'].split(':')
        self.host = bb[0]
        self.port = 80
        if(len(bb)>1): 
            self.port = int(bb[1]);  
        
        self.read_buf = bytearray() 
        self.sock = None  
        self.pid = os.getpid()
        self.auto_reconnect = True
        self.reconnect_interval = 3 #3 seconds
        
        self.result_table = {}  
        
        self.lock = threading.Lock()
        self.on_connected = None
        self.on_disconnected = None
        self.on_message = None
        self.manually_closed = False
     
    
    def close(self): 
        self.manually_closed = True
        self.auto_reconnect = False
        self.on_disconnected = None
        self.sock.close() 
        self.read_buf = bytearray() 

    
    def invoke(self, msg, timeout=10): 
        with self.lock:  
            msgid = self._send(msg, timeout)
            return self._recv(msgid, timeout)
    
    def send(self, msg, timeout=3):
        with self.lock:
            return self._send(msg, timeout) 
        
    def heartbeat(self):
        msg = Message()
        msg.cmd = 'heartbeat'
        self.send(msg)
     
    def recv(self, msgid=None, timeout=3):
        with self.lock:
            return self._recv(msgid, timeout)   
     

    def connect(self):   
        with self.lock: 
            self.manually_closed = False
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            if self.server_address['sslEnabled']:
                self.sock = ssl.wrap_socket(self.sock,  ca_certs=self.ssl_cert_file, cert_reqs=ssl.CERT_REQUIRED)
                
            address = address_key(self.server_address)
            self.log.info('Trying connect to (%s)'%address)
            self.sock.connect( (self.host, self.port) )
            self.log.info('Connected to (%s)'%address) 
            
        if self.on_connected:
            self.on_connected()
        
        self.read_buf = bytearray() 
    
    def _send(self, msg, timeout=10):
        msgid = msg.id
        if not msgid:
            msgid = msg.id = str(uuid.uuid4())
        
        self.log.debug('Request: %s'%msg)   
        self.sock.sendall(msg_encode(msg))  
        return msgid 
    
    def _recv(self, msgid=None, timeout=3):
        if not msgid and len(self.result_table)>0:
            try:
                self.result_table.popitem()[1]
            except:
                pass
            
        if msgid in self.result_table:
            return self.result_table[msgid]  
        
        self.sock.settimeout(timeout)    
        while True: 
            buf = self.sock.recv(1024)   
            #!!! when remote socket idle closed, could return empty, fixed by raising exception!!!
            if buf == None or len(buf) == 0: 
                raise socket.error('remote server socket status error, possible idle closed')
            self.read_buf += buf
            idx = 0
            while True:
                msg, idx = msg_decode(self.read_buf, idx)
                if msg is None: 
                    if idx != 0:
                        self.read_buf = self.read_buf[idx:]
                    break
                
                self.read_buf = self.read_buf[idx:]   
                
                if msgid:
                    if msg.id != msgid:
                        self.result_table[msg.id] = msg
                        continue 
                
                self.log.debug('Result: %s'%msg) 
                return msg   
    
    
    def start(self, recv_timeout=3):  
        def serve(): 
            while True: 
                try: 
                    self.connect()
                    break
                except socket.error as e:
                    self.log.warn(e) 
                    time.sleep(self.reconnect_interval)
                    
            while True:  
                try: 
                    msg = self.recv(None, recv_timeout) 
                    if msg and self.on_message:
                        self.on_message(msg)
                except socket.timeout as e:
                    try:
                        self.heartbeat() #after timeout send heartbeat
                    except Exception as e: 
                        pass
                    continue
                except socket.error as e: 
                    if self.manually_closed: break
                    
                    self.log.warn(e)
                    if self.on_disconnected:
                        self.on_disconnected()
                    if not self.auto_reconnect:
                        break
                    while self.auto_reconnect:
                        try:
                            self.sock.close()
                            self.connect()
                            break
                        except Exception as e:
                            self.log.warn(e) 
                            time.sleep(self.reconnect_interval)
                
        self._thread = threading.Thread(target=serve)  
        self._thread.start()


class MqClient(MessageClient):
    def __init__(self, address='localhost:15555', ssl_cert_file=None):
        MessageClient.__init__(self, address, ssl_cert_file)
        self.token = None
    
    def _invoke_void(self, cmd, msg, timeout=3):    
        msg.cmd = cmd
        msg.token = self.token
        
        res = self.invoke(msg, timeout)
        if res.status != "200":
            encoding = res.encoding
            if not encoding: encoding = 'utf8'
            raise Exception(res.body.decode(encoding))
    
    def _invoke_json(self, cmd, msg, timeout=3):    
        msg.cmd = cmd
        msg.token = self.token
        
        res = self.invoke(msg, timeout)
        if res.status != "200":
            encoding = res.encoding
            if not encoding: encoding = 'utf8'
            raise Exception(res.body.decode(encoding))
        return json.loads(res.body, encoding=res.encoding) 
    
    
    def produce(self, msg, timeout=3):
        self._invoke_void(Protocol.PRODUCE, msg, timeout)
        
    def consume(self, topic, ctrl=None, timeout=3):
        msg = Message()
        msg.topic = topic
        msg.token = self.token 
        if isinstance(ctrl, str):
            msg.consume_group = ctrl
        if isinstance(ctrl, (dict, Message)):
            for k in ctrl:
                msg[k] = ctrl[k]
        msg.cmd = Protocol.CONSUME 
        return self.invoke(msg, timeout) 
    
    def query(self, topic=None, group=None, timeout=3):
        msg = Message();
        msg.topic = topic
        msg.consume_group = group
        return self._invoke_json(Protocol.QUERY, msg, timeout)     
         
    def declare(self, topic, group=None, options=None, timeout=3):
        msg = Message();
        msg.topic = topic 
        if isinstance(options, dict):
            for k in options:
                msg[k] = options[k]
                
        return self._invoke_json(Protocol.DECLARE, msg, timeout)    
        
    def remove(self, topic, group=None, timeout=3):
        msg = Message();
        msg.topic = topic
        msg.consume_group = group 
        self._invoke_void(Protocol.REMOVE, msg, timeout)    
        
    def empty(self, topic, group=None, timeout=3):
        msg = Message();
        msg.topic = topic
        msg.consume_group = group 
        self._invoke_void(Protocol.EMPTY, msg, timeout)            



class MqClientPool:
    log = logging.getLogger(__name__)
    def __init__(self, server_address='localhost:15555', ssl_cert_file=None, maxsize=50, timeout=3):   
        self.server_address = normalize_address(server_address)
        
        self.maxsize = maxsize
        self.timeout = timeout
        self.ssl_cert_file = ssl_cert_file 
        self.reset()
        self.on_connected = None
        self.on_disconnected = None 
        
    def start(self): 
        self.detect_client = MqClient(self.server_address, self.ssl_cert_file)  
        def pool_connected():
            if self.on_connected:
                info = self.detect_client.query()
                self.server_address = info['serverAddress']
                self.on_connected(info)
        def pool_disconnected():
            if self.on_disconnected:
                self.on_disconnected(self.server_address)
                
        self.detect_client.on_connected = pool_connected
        self.detect_client.on_disconnected = pool_disconnected
        self.detect_client.start()
        
    
    def make_client(self):
        client = MqClient(self.server_address, self.ssl_cert_file)  
        client.connect()
        self.clients.append(client)
        self.log.debug('New client created %s', client)
        return client
    
    def _check_pid(self):
        if self.pid != os.getpid():
            with self._check_lock:
                if self.pid == os.getpid(): 
                    return
                self.log.debug('new process, pid changed')
                self.destroy()
                self.reset()
                    
    def reset(self):
        self.pid = os.getpid() 
        self._check_lock = threading.Lock()
        
        self.client_pool = Queue.LifoQueue(self.maxsize)
        while True:
            try:
                self.client_pool.put_nowait(None)
            except Queue.Full:
                break 
        self.clients = []
        
    
    def borrow_client(self):  
        self._check_pid()
        client = None
        try:
            client = self.client_pool.get(block=True, timeout=self.timeout)
        except Queue.Empty: 
            raise Exception('No client available')
        if client is None:
            client = self.make_client()
        return client 
    
    def return_client(self, client):
        self._check_pid()
        if client.pid != self.pid:
            return 
        if not isinstance(client, (tuple, list)):
            client = [client]
        for c in client:
            try:
                self.client_pool.put_nowait(c)
            except Queue.Full: 
                pass
            
    def close(self):
        if hasattr(self, 'detect_client'):
            self.detect_client.close()
        for client in self.clients:
            client.close()
            

class BrokerRouteTable:
    def update_votes(self, tracker_info):
        print(tracker_info)
        
    def update_server(self, server_info):  
        print(server_info)    
        
    def remove_server(self, server_address):  
        print(server_address)     


class Broker:
    log = logging.getLogger(__name__)
    def __init__(self):
        self.pool_table = {}
        self.route_table = BrokerRouteTable()
        self.ssl_cert_file_table = {}
        self.tracker_subscribers = {}
        
    
    def add_tracker(self, tracker_address, cert_file=None):
        pass
    
    
    def add_server(self, server_address, cert_file=None):
        server_address = normalize_address(server_address)
        if cert_file:
            key = server_address['address'] 
            self.ssl_cert_file_table[key] = cert_file
        
        pool = MqClientPool(server_address, cert_file)
        
        def server_connected(server_info): 
            self.log.info('server connected: %s'%server_info)
            key = address_key(pool.server_address)
            self.pool_table[key] = pool 
            self.route_table.update_server(server_info)
            
        def server_disconnected(server_address):
            self.log.info('server disconnected: %s'%server_address)
            self.route_table.remove_server(server_address)
            key = address_key(server_address)
            if key in self.pool_table:
                pool = self.pool_table[key]
                #del self.pool_table[key]
                #pool.close()
                
        pool.on_connected = server_connected
        pool.on_disconnected = server_disconnected
        pool.start()
         
 
class Producer:
    pass

class Consumer:
    pass

class RpcInvoker:
    pass

class RpcProcessor:
    pass 
          
__all__ = [
    Message, MessageClient, MqClient, MqClientPool, 
    Broker, Producer, Consumer, RpcInvoker, RpcProcessor
]    