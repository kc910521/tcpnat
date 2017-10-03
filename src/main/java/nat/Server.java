package nat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by robu on 2016/11/30.
 */
public class Server {
	
	
    public static final int PORT = 12345;//监听的端口号
    
    private static ServerSocketChannel ssc = null;

    private static Selector selector = null;
    
    private static Charset charset = Charset.forName("GBK");//设置编码集,用于编码,解码
    
    private static final ConcurrentHashMap<String,SocketChannel> clientSockets = new ConcurrentHashMap<>();
    
    static{
        try {
        	ssc = ServerSocketChannel.open();
        	ssc.socket().setReuseAddress(true);
        	ssc.socket().bind(new InetSocketAddress(PORT));
        	ssc.configureBlocking(false);//设置为非阻塞
            selector = Selector.open();//实例化一个选择器
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("服务器启动...\n" + PORT);
        service();
//        Server server = new Server();
//        server.init();
    }
    
    private static void service(){
        SocketChannel clientChannel = null;
        SelectionKey selectionKey = null;
        SocketChannel targetChannel = null;
        try {
            ssc.register(selector,SelectionKey.OP_ACCEPT);//服务端监听连接
            while(true){
                selector.select();//阻塞至有新的连接就开始处理
                Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();
                while(selectionKeys.hasNext()){
                    selectionKey = selectionKeys.next();
                    if(selectionKey.isAcceptable()){//如果事件是连接事件
                    	
                        ServerSocketChannel serverChannel = (ServerSocketChannel)selectionKey.channel();//获取事件绑定的channel
                        clientChannel = serverChannel.accept();//连接获取带客户端信息的socketChannel
                        clientChannel.configureBlocking(false);//客户设置为非阻塞,因为非阻塞才支持选择器.避免盲等浪费资源
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);//作为每一个客户端的附件缓冲器
                        System.out.println("connected from " + clientChannel.getRemoteAddress());
                        /**
                         * 只监听读事件,这里千万别监听写事件,因为只要连接有效,那么写事件会一直为true,导致死循环,很耗资源
                         * 可以跟serverSocket用同一个选择器,因为绑定的channel不同
                         */
                        clientChannel.register(selector,SelectionKey.OP_READ,byteBuffer);
                    }else if(selectionKey.isReadable()){//只要有客户端写入,那么就可以处理
                        //获取客户端附件,也就是写入的数据
                        ByteBuffer byteBuffer = (ByteBuffer)selectionKey.attachment();
                        //从selectionKey获取客户端的channel
                        SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
                        //把附件读出,解码为字符串
                        String msg = read(socketChannel,byteBuffer);
                        System.out.println("msg:" + msg);
                        //这里用了->分割收件人,->后面跟着的字符串是收件人
                        if(msg.indexOf("->")!=-1){
                            //内容
                            String content = msg.substring(0,msg.lastIndexOf("->"));
                            //从map里获取收件人的socket
                            String mKey = msg.substring(msg.lastIndexOf("->") + 2);
                            System.out.println("searching " + mKey);
                            targetChannel = clientSockets.get(mKey);
                            if (targetChannel == null) {
                            	System.out.println("not found");
                            }else{
                                //实例化一个缓冲区,用来写出到收件人的socketChannel
                                ByteBuffer temp = ByteBuffer.allocate(1024);
                                temp.put(charset.encode(content));
                                //写出
                                handleWrite(targetChannel,temp);
                            }
                        }else{
                            //如果内容没有收件人,那么视为第一次连接,客户端发过来的userName,作为KEY存入MAP
                            clientSockets.put(msg,socketChannel);
                        }
                    }
                    selectionKeys.remove();
                }
            }
        } catch (IOException e) {
            try {
                if(selectionKey!=null)selectionKey.cancel();
                if(clientChannel!=null){
                    clientChannel.shutdownInput();
                    clientChannel.shutdownOutput();
                    clientChannel.close();
                }
                if(targetChannel!=null){
                    targetChannel.shutdownInput();
                    targetChannel.shutdownOutput();
                    targetChannel.close();
                }
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            e.printStackTrace();
        }

    }
    
    private static String read(SocketChannel socketChannel,ByteBuffer byteBuffer){
        //重置position limit为写入做准备
        byteBuffer.clear();
        try {
            int flag =socketChannel.read(byteBuffer);
            //判断客户端是否断开连接
            if(flag==-1){
                //如果客户端无故断开,一定要关闭,否则读取事件一直为true造成死循环,非常耗资源
                socketChannel.close();
            }
        } catch (IOException e) {
            try {
                socketChannel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        //position =0 limit等于有效下标,为写出做准备
        byteBuffer.flip();
        return charset.decode(byteBuffer).toString();
    }
    
    //写出
    private static void handleWrite(SocketChannel socketChannel,ByteBuffer byteBuffer){
        synchronized (byteBuffer) {
            byteBuffer.flip();
            try {
                socketChannel.write(byteBuffer);
            } catch (IOException e) {
                try {
                    socketChannel.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }
    }


    public void init() {
    	
/*        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            while (true) {
                // 一旦有堵塞, 则表示服务器与客户端获得了连接
                Socket client = serverSocket.accept();
                // 处理这次连接
                new HandlerThread(client);
            }
        } catch (Exception e) {
            System.out.println("服务器异常: " + e.getMessage());
        }*/
    }
    private class HandlerThread implements Runnable {
        private Socket socket;
        public HandlerThread(Socket client) {
            socket = client;
            new Thread(this).start();
        }

        public void run() {
        	
/*            try {
                System.out.println("conn?");
                // 读取客户端A数据
                DataInputStream input = new DataInputStream(socket.getInputStream());
                String clientInputStr = input.readUTF();//这里要注意和客户端输出流的写方法对应,否则会抛 EOFException
                System.out.print("socket:"+socket.getInetAddress().getHostAddress());
                // 处理客户端数据
                System.out.println("客户端发过来的内容:" + clientInputStr);
                // 向客户端回复信息
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                //System.out.print("请输入:\t");
                // 发送键盘输入的一行
                //String s = new BufferedReader(new InputStreamReader(System.in)).readLine();
                out.writeUTF(clientInputStr);
                out.close();
                input.close();
            } catch (Exception e) {
                System.out.println("服务器 run 异常: " + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        //socketPrev = socket;
                        if (socket != null ){
                            System.out.println("++++++++++++++++socketPrev close");
                            socket.close();
                        }
                        //socket.close();
                    } catch (Exception e) {
                        socket = null;
                        System.out.println("服务端 finally 异常:" + e.getMessage());
                    }
                }
            }*/
        }
    }
}
