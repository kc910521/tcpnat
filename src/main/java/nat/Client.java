package nat;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * Created by robu on 2016/11/30.
 */
public class Client {
	
    public static final String IP_ADDR = "localhost";//服务器地址120.27.46.193
    
    private static Charset charset = Charset.forName("GBK");
    private static ByteBuffer receiveBuffer = ByteBuffer.allocate(10240);
    private static ByteBuffer sendBuffer = ByteBuffer.allocate(10240);
    private static SocketChannel socketChannel = null;
    private static Selector selector = null;
    private static String userName = "client1";//客户端名
    private static String targetName = "client2";//收件人名

    /*public static void main(String[] args) {
        System.out.println("客户端启动...");
        System.out.println("当接收到服务器端字符为 \"OK\" 的时候, 客户端将终止\n");
        
        while (true) {
            Socket socket = null;
            try {
                //创建一个流套接字并将其连接到指定主机上的指定端口号
                socket = new Socket(IP_ADDR, Server2.PORT);
                socket.setKeepAlive(true);
                //读取服务器端数据
                DataInputStream input = new DataInputStream(socket.getInputStream());
                //向服务器端发送数据
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                System.out.print("请输入: \t");
                String str = new BufferedReader(new InputStreamReader(System.in)).readLine();
                out.writeUTF(str);

                String ret = input.readUTF();
                System.out.println("服务器端返回过来的是: " + ret);
                // 如接收到 "OK" 则断开连接
                if ("OK".equals(ret)) {
                    System.out.println("客户端将关闭连接");
                    Thread.sleep(500);
                    break;
                }

//                out.close();
//                input.close();
            } catch (Exception e) {
                System.out.println("客户端异常:" + e.getMessage());
            } finally {
                if (socket != null) {
//                    try {
//                        socket.close();
//                    } catch (IOException e) {
//                        socket = null;
//                        System.out.println("客户端 finally 异常:" + e.getMessage());
//                    }
                }
            }
        }
    }*/
    
    public static void main(String[] args) {
        try {
            socketChannel = SocketChannel.open();
            //连接到服务端
            SocketAddress socketAddress = new InetSocketAddress(IP_ADDR,Server.PORT);
            selector = Selector.open();//实例化一个选择器
            socketChannel.configureBlocking(false);//设置为非阻塞
            //先监听一个连接事件
            socketChannel.register(selector,SelectionKey.OP_CONNECT);
            //连接
            socketChannel.connect(socketAddress);
            //jdk 1.8的lambda表达式，用一个线程监控控制台输入
            new Thread(()->{
                    try {
                        receiveFromUser();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }).start();

            talk();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void talk(){
        try {
            while(true){
                selector.select();//阻塞直到连接事件
                Iterator<SelectionKey> readyKeys = selector.selectedKeys().iterator();
               while(readyKeys.hasNext()){
                    SelectionKey key =readyKeys.next();
                    if(key.isConnectable()){
                        //非阻塞的情况下可能没有连接完成，这里调用finishConnect阻塞至连接完成
                        socketChannel.finishConnect();
                        //连接完成以后，先发送自己的userName以便保存在服务端的客户端map里面
                        synchronized (sendBuffer){
                            SocketChannel socketChannel1 = (SocketChannel)key.channel();
                            sendBuffer.clear();
                            sendBuffer.put(charset.encode(userName));
                            send(socketChannel1);
                            socketChannel.register(selector,SelectionKey.OP_READ);//仅监听一个读取事件
                        }

                    }else if(key.isReadable()){
                        //处理读事件
                        receive(key);
                    }
                    readyKeys.remove();
                }
            }
        } catch (ClosedChannelException e) {
            try {
                socketChannel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
    /**
     * 从控制台获取用户输入
     * @throws IOException
     */
    private static void receiveFromUser() throws IOException{
        //阻塞直到控制台有输入
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for(String msg = br.readLine();msg!=null&&!msg.equals("bye");msg = br.readLine()){
            //同步锁避免线程竞争
            synchronized (sendBuffer) {
                sendBuffer.clear();
                //编码
                sendBuffer.put(charset.encode(msg));
                //分割副
                sendBuffer.put(charset.encode("->"));
                //目标名
                sendBuffer.put(charset.encode(targetName));
                send(socketChannel);
            }
        }
    }
    /**
     * 接收服务端的数据
     * @param key
     */
    private static void receive(SelectionKey key) throws IOException {
        //获取服务端的channel
        SocketChannel channel = (SocketChannel) key.channel();
        //为写入缓冲器做准备position=0,limit=capacity
            receiveBuffer.clear();
            //从服务端的channel把数据读入缓冲器
            channel.read(receiveBuffer);
            //position=0,limit=有效下标最后一位
            receiveBuffer.flip();
            //解码
            String msg = charset.decode(receiveBuffer).toString();
            //输出到控制台
            System.out.println(msg);
    }

    /**
     * 发送到服务端
     */
    private static void send(SocketChannel sendChannel) throws IOException {
            if(sendBuffer.remaining()!=0){
                synchronized (sendBuffer){
                    sendBuffer.flip();
                    sendChannel.write(sendBuffer);
                }
            }
    }
}
