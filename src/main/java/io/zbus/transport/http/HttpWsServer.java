package io.zbus.transport.http;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.ssl.SslHandler;
import io.zbus.kit.HttpKit;
import io.zbus.kit.JsonKit;
import io.zbus.transport.Message;
import io.zbus.transport.Server;
import io.zbus.transport.http.Http.FormData; 

/**
 * 
 * Support both HTTP and Websocket in one port -- unified port.
 * 
 * For HTTP protocol, it also support file uploading forms.
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public class HttpWsServer extends Server { 
	private DecodeFilter decodeFilter;
	public HttpWsServer() {
		CorsConfig corsConfig = CorsConfigBuilder
				.forAnyOrigin()
				.allowNullOrigin() 
				.allowedRequestMethods(HttpMethod.PUT, HttpMethod.POST, HttpMethod.GET, HttpMethod.OPTIONS)
				.allowedRequestHeaders("*") 
				.exposeHeaders("*")
				.allowCredentials()
				.build();  
		initCodec(corsConfig);
	} 
	
	public HttpWsServer(CorsConfig corsConfig) {
		initCodec(corsConfig);
	}
	
	protected void initCodec(CorsConfig corsConfig) {
		codec(p -> {
			p.add(new HttpServerCodec());
			p.add(new HttpObjectAggregator(packageSizeLimit)); 
			if(corsConfig != null) {
				//p.add(new ChunkedWriteHandler());
				p.add(new CorsHandler(corsConfig));
			} 
			HttpWsServerCodec handler = new HttpWsServerCodec();
			handler.setDecodeFilter(decodeFilter);
			p.add(handler);  
		}); 
	}
	
	public void setDecodeFilter(DecodeFilter decodeFilter) {
		this.decodeFilter = decodeFilter;
	}
	
	public static class HttpWsServerCodec extends MessageToMessageCodec<Object, Object> {
		private static final Logger logger = LoggerFactory.getLogger(HttpWsServerCodec.class); 
		private DecodeFilter decodeFilter;
		
		private WebSocketServerHandshaker handshaker;
	
		//File upload
//		private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk if size exceed
//	    private HttpPostRequestDecoder decoder;
	
	    static {
	        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file // on exit (in normal // exit)
	        DiskFileUpload.baseDirectory = null; // system temp directory
	        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on  exit (in normal exit)
	        DiskAttribute.baseDirectory = null; // system temp directory
	    } 
	    
	    
	    @Override
	    public boolean acceptOutboundMessage(Object msg) throws Exception {
	    	return msg instanceof Message || msg instanceof byte[];
	    }
	     
	    
		@Override
		protected void encode(ChannelHandlerContext ctx, Object obj,  List<Object> out) throws Exception {
			if(!(obj instanceof Message)){
				logger.error("Message type required");
				return;
			} 
			
			Message msg = (Message)obj;
			//1) WebSocket mode  
			if(handshaker != null){   
				byte[] data = JsonKit.toJSONBytes(msg);
				ByteBuf buf = Unpooled.wrappedBuffer(data);
				WebSocketFrame frame = new TextWebSocketFrame(buf);
				out.add(frame);  
				return;
			} 
						
			//2) HTTP mode  
			FullHttpMessage httpMessage = null;
			
			if (msg.getStatus() == null) {// as request
				httpMessage = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(msg.getMethod()),
						msg.getUrl()); 
			} else {// as response
				httpMessage = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
						HttpResponseStatus.valueOf(Integer.valueOf(msg.getStatus())));
			}
			//content-type and encoding
			String contentType = msg.getHeader(Http.CONTENT_TYPE); 
			if(contentType == null) {
				contentType = "application/json; charset=utf8";
			} 
			
			for (Entry<String, Object> e : msg.getHeaders().entrySet()) { 
				String key = e.getKey();
				Object value = e.getValue(); 
				if(value instanceof List) {
					@SuppressWarnings("unchecked")
					List<Object> listValue = (List<Object>)value;
					for(Object listItem : listValue) {
						httpMessage.headers().add(key, listItem);
					}
				} else if (value instanceof Object[]) {
					for(Object arrayItem : (Object[])value) {
						httpMessage.headers().add(key, arrayItem);
					}
				} else {
					httpMessage.headers().add(key, value);
				} 
			}
			
			httpMessage.headers().set("Connection", "Keep-Alive");
			httpMessage.headers().set(Http.CONTENT_TYPE, contentType); 
			
			byte[] body = Http.body(msg); 
			httpMessage.headers().set(Http.CONTENT_LENGTH, body.length);
			httpMessage.content().writeBytes(body); 
			out.add(httpMessage);
		}
		
		@Override
		protected void decode(ChannelHandlerContext ctx, Object obj, List<Object> out) throws Exception {
			//1) WebSocket mode
			if(obj instanceof WebSocketFrame){
				byte[] bytes = decodeWebSocketFrame(ctx, (WebSocketFrame)obj);
				if(bytes != null) {//ws close may be null
					Message msg = JsonKit.parseObject(bytes, Message.class);
					if(!HttpKit.isText(msg.getHeader(Http.CONTENT_TYPE))) {
						if(msg.getBody() instanceof String) { //NOT TEXT data, but body String typed
							try {
								byte[] body = Base64.getDecoder().decode((String)msg.getBody());
								msg.setBody(body);
							}catch (Exception e) { 
								//ignore
							} 
						}
					}
					if(msg != null){
						out.add(msg);
					}
				}
				return;
			}
			
			//2) HTTP mode
			if(!(obj instanceof io.netty.handler.codec.http.HttpMessage)){
				throw new IllegalArgumentException("HttpMessage object required: " + obj);
			}
			
			if(decodeFilter != null) {
				if(decodeFilter.decode(ctx, obj, out)) return; //filter out some http message
			}
			 
			io.netty.handler.codec.http.HttpMessage httpMsg = (io.netty.handler.codec.http.HttpMessage) obj;   
			Message msg = decodeHeaders(httpMsg); 
			String contentType = msg.getHeader(Http.CONTENT_TYPE);
			if(contentType != null) contentType = contentType.toLowerCase();

			//Special case for file uploads
			if(httpMsg instanceof HttpRequest 
				&& contentType != null
				&& (
				   contentType.startsWith("application/x-www-form-urlencoded")
				   || contentType.startsWith("multipart/form-data")
				)
			){
				handleFormMessage((HttpRequest) httpMsg, msg);
			}  else {
				//Body
				ByteBuf body = null;
				if (httpMsg instanceof FullHttpMessage) {
					FullHttpMessage fullReq = (FullHttpMessage) httpMsg;
					body = fullReq.content();
				}
				if (body != null) { 
					int size = body.readableBytes();
					if (size > 0) {
						byte[] data = new byte[size];
						body.readBytes(data); 
						if(contentType != null
							&& (
								contentType.startsWith("text")
								|| contentType.startsWith("application/json")
							)
						) {
							try{
								String charset = Http.charset(contentType);
								msg.setBody(new String(data, charset));
							} catch (Exception e) {
								logger.error(e.getMessage(), e);
								msg.setBody(new String(data));
							}
						} else {
							msg.setBody(data);
						}
					}
				}
			}
	
			out.add(msg);
		} 
	
		@SuppressWarnings("unchecked")
		private Message decodeHeaders(io.netty.handler.codec.http.HttpMessage httpMsg){
			Message msg = new Message();
			Iterator<Entry<String, String>> iter = httpMsg.headers().iteratorAsString();
			while (iter.hasNext()) {
				Entry<String, String> e = iter.next();
				Object existValue = msg.getHeaderObject(e.getKey());
				if(existValue == null) {
					msg.setHeader(e.getKey(), e.getValue()); 
					continue;
				} 
				 
				if(existValue instanceof List) {
					List<Object> valueList = (List<Object>)existValue;
					valueList.add(e.getValue());
					continue;
				}   
				
				List<Object> valueList = new ArrayList<>();
				valueList.add(existValue);
				valueList.add(e.getValue()); 
				msg.setHeader(e.getKey(), valueList); 
			}  
	
			if (httpMsg instanceof HttpRequest) {
				HttpRequest req = (HttpRequest) httpMsg;
				msg.setMethod(req.method().name()); 
				String url = req.uri();
				try {
					url = URLDecoder.decode(req.uri(), "UTF-8");
				} catch (Throwable e) {
					//ignore
				}
				msg.setUrl(url);
			} else if (httpMsg instanceof HttpResponse) {
				HttpResponse resp = (HttpResponse) httpMsg;
				int status = resp.status().code();
				msg.setStatus(status);
			}
			return msg;
		}
		
		private void handleFormMessage(HttpRequest httpRequest, Message uploadMessage) throws IOException{
	    	HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(httpRequest);
			try {
				while (decoder.hasNext()) {
					InterfaceHttpData data = decoder.next();
					if (data != null) {
						handleFormData(data, uploadMessage);
					}
				}
			} catch (EndOfDataDecoderException e1) {
				//ignore
			} finally {
				decoder.destroy();
			}
		}
		
		private void handleFormData(InterfaceHttpData data, Message uploadMessage) throws IOException{
			FormData formData = (FormData)uploadMessage.getBody();
	        if(formData == null){
	        	formData = new FormData();
	        	uploadMessage.setBody(formData);
	        }
	        
			if (data.getHttpDataType() == HttpDataType.Attribute) {
	            Attribute attribute = (Attribute) data;
	            formData.addAttribute(attribute.getName(), attribute.getValue()); 
	            return;
			}
			
			if (data.getHttpDataType() == HttpDataType.FileUpload) {
	            FileUpload fileUpload = (FileUpload) data;
	            Http.FileUpload file = new Http.FileUpload();
	            file.fileName = fileUpload.getFilename();
	            file.contentType = fileUpload.getContentType();
	            file.data = fileUpload.get(); 
	            
	            List<Http.FileUpload> uploads = formData.files.get(data.getName());
	            if(uploads == null){
	            	uploads = new ArrayList<Http.FileUpload>();
	            	formData.files.put(data.getName(), uploads);
	            }
	            uploads.add(file);
			}
		}
		
//		private void resetUpload() {
//	        //decoder.destroy(); //TODO
//	        decoder = null;
//	    }
		
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if(msg instanceof FullHttpRequest){
				FullHttpRequest req = (FullHttpRequest) msg; 
				
				//check if websocket upgrade encountered
				if(req.headers().contains("Upgrade") || req.headers().contains("upgrade")) { 
					WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
							getWebSocketLocation(req, ctx), null, true, 1024 * 1024 * 1024);
					handshaker = wsFactory.newHandshaker(req);
					if (handshaker == null) {
						WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
					} else {
						handshaker.handshake(ctx.channel(), req);
					}
					return;
				}
			}
			super.channelRead(ctx, msg);
		}
	 
		private byte[] decodeWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
			// Check for closing frame
			if (frame instanceof CloseWebSocketFrame) {
				handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
				return null;
			}
			
			if (frame instanceof PingWebSocketFrame) {
				ctx.write(new PongWebSocketFrame(frame.content().retain()));
				return null;
			}
			
			if (frame instanceof TextWebSocketFrame) {
				TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
				return parseMessage(textFrame.content());
			}
			
			if (frame instanceof BinaryWebSocketFrame) {
				BinaryWebSocketFrame binFrame = (BinaryWebSocketFrame) frame;
				return parseMessage(binFrame.content());
			}
			
			logger.warn("Message format error: " + frame); 
			return null;
		}
		
		private byte[] parseMessage(ByteBuf buf){
			int size = buf.readableBytes();
			byte[] data = new byte[size];
			buf.readBytes(data); 
			return data;
		}
	
		private static String getWebSocketLocation(io.netty.handler.codec.http.HttpMessage req, ChannelHandlerContext ctx) {
			String location = req.headers().get(HttpHeaderNames.HOST) + "/";
			if (ctx.pipeline().get(SslHandler.class) != null) {
				return "wss://" + location;
			} else {
				return "ws://" + location;
			}
		} 
		public void setDecodeFilter(DecodeFilter decodeFilter) {
			this.decodeFilter = decodeFilter;
		}
	}
}
