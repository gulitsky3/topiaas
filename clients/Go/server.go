package main

import (
	"bytes"
	"errors"
	"flag"
	"fmt"
	"log"
	"net"
	"sync/atomic"

	"time"

	"./protocol"
	"./websocket"
)

// Session abstract socket connection
type Session struct {
	ID          string
	netConn     net.Conn
	wsConn      websocket.Conn
	isWebsocket bool
}

//NewSession create session
func NewSession(netConn *net.Conn, wsConn *websocket.Conn) *Session {
	sess := &Session{}
	sess.ID = uuid()
	if netConn != nil {
		sess.isWebsocket = false
		sess.netConn = *netConn
	}
	if wsConn != nil {
		sess.isWebsocket = true
		sess.wsConn = *wsConn
	}
	return sess
}

//Upgrade session to be based on websocket
func (s *Session) Upgrade(wsConn *websocket.Conn) {
	s.wsConn = *wsConn
	s.isWebsocket = true
}

//String get string value of session
func (s *Session) String() string {
	return fmt.Sprintf("%s-%s", s.ID, s.netConn.RemoteAddr())
}

//WriteMessage write message to underlying connection
func (s *Session) WriteMessage(msg *Message, msgType *int) error {
	buf := new(bytes.Buffer)
	msg.EncodeMessage(buf)
	if s.isWebsocket {
		return s.wsConn.WriteMessage(*msgType, buf.Bytes())
	}
	_, err := s.netConn.Write(buf.Bytes()) //TODO write may return 0 without err
	return err
}

//SessionHandler handles session lifecyle
type SessionHandler interface {
	Created(sess *Session)
	ToDestroy(sess *Session)
	OnMessage(msg *Message, sess *Session, msgType *int) //msgType only used for websocket
	OnError(err error, sess *Session)
}

var upgrader = Upgrader{}

func handleConnection(conn net.Conn, handler SessionHandler) {
	defer conn.Close()
	bufRead := new(bytes.Buffer)
	var wsConn *websocket.Conn
	session := NewSession(&conn, nil)
	handler.Created(session)
outter:
	for {
		data := make([]byte, 1024)
		n, err := conn.Read(data)
		if err != nil {
			handler.OnError(err, session)
			break
		}
		bufRead.Write(data[0:n])

		for {
			req := DecodeMessage(bufRead)
			if req == nil {
				bufRead2 := new(bytes.Buffer)
				bufRead2.Write(bufRead.Bytes())
				bufRead = bufRead2
				break
			}

			//upgrade to Websocket if requested
			if IsWebSocketUpgrade(req.Header) {
				wsConn, err = upgrader.Upgrade(conn, req)
				if err == nil {
					log.Printf("Upgraded to websocket: %s\n", req)
					session.Upgrade(wsConn)
					break outter
				}
			}

			handler.OnMessage(req, session, nil)
		}
	}

	if wsConn != nil { //upgraded to Websocket
		bufRead = new(bytes.Buffer)
		for {
			msgtype, data, err := wsConn.ReadMessage()
			if err != nil {
				handler.OnError(err, session)
				break
			}
			bufRead.Write(data)
			req := DecodeMessage(bufRead)
			if req == nil {
				err = errors.New("Websocket invalid message: " + string(data))
				handler.OnError(err, session)
				break
			}
			if IsWebSocketUpgrade(req.Header) {
				continue
			}
			handler.OnMessage(req, session, &msgtype)
		}
	}
	handler.ToDestroy(session)
	conn.Close()
}

//Server = MqServer + Tracker
type Server struct {
	ServerAddress *protocol.ServerAddress
	MqTable       map[string]*MessageQueue

	MqDir       string
	TrackerList []string

	infoVersion int64

	trackerOnly bool
}

func newServer() *Server {
	s := &Server{}
	s.infoVersion = time.Now().UnixNano() / int64(time.Millisecond)
	s.trackerOnly = false
	return s
}

func (s *Server) serverInfo() *protocol.ServerInfo {
	info := &protocol.ServerInfo{}
	info.ServerAddress = s.ServerAddress
	info.ServerVersion = protocol.VersionValue
	atomic.AddInt64(&s.infoVersion, 1)
	info.InfoVersion = s.infoVersion
	info.TrackerList = []protocol.ServerAddress{}
	info.TopicTable = make(map[string]*protocol.TopicInfo)
	for key, mq := range s.MqTable {
		info.TopicTable[key] = mq.TopicInfo()
	}

	return info
}

func (s *Server) trackerInfo() *protocol.TrackerInfo {
	info := &protocol.TrackerInfo{}
	info.ServerAddress = s.ServerAddress
	info.ServerVersion = protocol.VersionValue
	atomic.AddInt64(&s.infoVersion, 1)
	info.InfoVersion = s.infoVersion
	info.ServerTable = make(map[string]*protocol.ServerInfo)
	if !s.trackerOnly {
		info.ServerTable[s.ServerAddress.String()] = s.serverInfo()
	}

	return info
}

func main() {
	log.SetFlags(log.Lshortfile | log.Ldate | log.Ltime)
	var host = *flag.String("h", "0.0.0.0", "zbus server host")
	var port = *flag.Int("p", 15555, "zbus server port")
	var addr = fmt.Sprintf("%s:%d", host, port)
	var mqDir = *flag.String("dir", "/tmp/zbus", "zbus MQ directory")
	var trackerOnly = *flag.Bool("trackonly", false, "server work as tracker only, or both tracker and mqserver")

	tcpAddr, err := net.ResolveTCPAddr("tcp", addr)
	if err != nil {
		log.Println("Error addres:", err.Error())
		return
	}
	fd, err := net.ListenTCP("tcp", tcpAddr)
	if err != nil {
		log.Println("Error listening:", err.Error())
		return
	}
	defer fd.Close()

	log.Println("Listening on " + addr)
	realAddr := ServerAddress(host, port)
	server := newServer()
	server.MqDir = mqDir
	server.ServerAddress = &protocol.ServerAddress{realAddr, false}
	server.trackerOnly = trackerOnly

	mqTable, err := LoadMqTable(mqDir)
	if err != nil {
		log.Println("Error loading MQ table: ", err.Error())
		return
	}
	server.MqTable = mqTable

	handler := NewServerHandler(server)
	for {
		conn, err := fd.AcceptTCP()
		if err != nil {
			log.Println("Error accepting: ", err.Error())
			return
		}
		go handleConnection(conn, handler)
	}
}
