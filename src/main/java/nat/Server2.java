package nat;

import vo.ClientInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by robu on 2016/11/30.
 */
public class Server2 {
    public static final int PORT = 12346;//监听的端口号

    private List<ClientInfo> cinfs = new ArrayList<ClientInfo>();

    public static void main(String[] args) {
        System.out.println("服务器启动...\n");
        Server2 server = new Server2();
        server.init();
    }

    public void init() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            while (true) {
                // 一旦有堵塞, 则表示服务器与客户端获得了连接
                Socket client = serverSocket.accept();
                // 处理这次连接
                new HandlerThread(client);
            }
        } catch (Exception e) {
            System.out.println("服务器异常: " + e.getMessage());
        }
    }
    private class HandlerThread implements Runnable {
        private Socket socket;
        public HandlerThread(Socket client) {
            socket = client;
            new Thread(this).start();
        }

        public void run() {
            Socket socketC = null;
            try {
                // 读取客户端A数据
                DataInputStream input = new DataInputStream(socket.getInputStream());
                String clientInputStr = input.readUTF();//这里要注意和客户端输出流的写方法对应,否则会抛 EOFException
                System.out.print("socket:"+socket.getInetAddress().getHostAddress());
                // 处理客户端数据
                System.out.println("客户端发过来的内容:" + clientInputStr+",then:");
                // 向客户端回复信息
                socketC = new Socket(Client.IP_ADDR, Server.PORT);
                DataOutputStream out = new DataOutputStream(socketC.getOutputStream());
                out.writeUTF(clientInputStr);
                out.close();
                input.close();
            } catch (Exception e) {
                System.out.println("服务器 run 异常: " + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        //socketPrev = socket;
                        //if (socketPrev != null && !socketPrev.isClosed()){
                            //System.out.println("++++++++++++++++socketPrev close");
                        socketC.close();
                        socket.close();
                        //}
                        //socketPrev = socket;
                        //socket.close();
                    } catch (Exception e) {
                        socket = null;
                        System.out.println("服务端 finally 异常:" + e.getMessage());
                    }
                }
            }
        }
    }
}
