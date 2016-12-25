package nat;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**main
 * Created by robu on 2016/11/30.
 */
public class Server2 {
    public static final int PORT = 12346;//监听的端口号

    private Set<ClientInfo> cinfs = new HashSet<ClientInfo>();



    public static void main(String[] args) {
        System.out.println("服务器启动...\n");
        Server2 server = new Server2();
        server.init();
    }

    public void init() {
        try {
            ServerSocket serverSocket = new ServerSocket(Server2.PORT);
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
            //Socket socketC = null;
            try {
                // 读取客户端A数据
                System.out.println("连接："+socket.getInetAddress().getHostAddress()+","+socket.getPort());
                killClosedSocket();
                socket.setKeepAlive(true);
                ClientInfo clientInf = new ClientInfo(socket.getInetAddress().getHostAddress(),socket.getPort(),socket);
                cinfs.add(clientInf);
                for (int itm = 0;itm < 10; itm ++){
                	System.out.println("itm:"+itm);
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    
                    byte[] bt = new byte[2048];
                    System.out.println("------------------------1------------");
                    while (input.read(bt) == -1){
                    	break;
                    }
                    String clientInputStr = new String(bt, "UTF-8");
//                    String clientInputStr = input.readUTF();//这里要注意和客户端输出流的写方法对应,否则会抛 EOFException
                    System.out.print("socket:"+socket.getInetAddress().getHostAddress()+":"+socket.getPort());
                    
                    
                    
                    // 处理客户端数据
                    System.out.println("客户端发过来的内容:" + clientInputStr+";");
//                    String[] infs = clientInputStr.split(":");
//                    if (infs == null || infs.length != 2){
//                        throw new Exception("ERROR INFORMATION."+ Arrays.toString(infs));
//                    }
                    //===向消息来源回复消息
                    DataOutputStream out2 = new DataOutputStream(socket.getOutputStream());
                    out2.writeUTF("ok");

                    // 向客户端B转发信息
                    for (ClientInfo ci: cinfs){
                        sendMsg(ci,clientInputStr,clientInf);
                    }
                }
//                socketC = new Socket(infs[0], Integer.valueOf(infs[1]));
//                DataOutputStream out = new DataOutputStream(socketC.getOutputStream());
//                out.writeUTF(clientInputStr);
//                out.close();

//                out2.close();
//                input.close();
            } catch (Exception e) {
                System.out.println("服务器 run 异常: " + e.getMessage());
                e.printStackTrace();

            } finally {
                if (socket != null) {
                    try {
                            //socketPrev = socket;
                            //if (socketPrev != null && !socketPrev.isClosed()){
                            //System.out.println("++++++++++++++++socketPrev close");
                            //socketC.close();
                            //socket.close();
                            //}
                            //socketPrev = socket;
                        //socket.close();
                    } catch (Exception e) {
//                        try {
                            //socket.close();
//                        } catch (IOException e1) {
//                            e1.printStackTrace();
//                        }
                        //socket = null;
                        System.out.println("服务端 finally 异常:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private synchronized void killClosedSocket(){
        List<ClientInfo> arrs = new LinkedList<ClientInfo>();
        for (ClientInfo ci : cinfs){
            if (ci.getSocket().isClosed()){
                arrs.add(ci);
                System.out.println("kill "+ci.getIp()+":"+ci.getPort());
            }
        }
        cinfs.removeAll(arrs);

    }

    private synchronized void sendMsg(ClientInfo ci,String inf,ClientInfo exceptCi) throws IOException {
        if (exceptCi != null && exceptCi.equals(ci)){
            System.out.println("return;");
            return;
        }
        System.out.println("server准备向"+ci.getIp()+":"+ci.getPort()+" 发送"+inf);
        Socket socketC = ci.getSocket();
        if (socketC.isClosed()){
            System.err.println("socketC closed");
        }else{
            DataOutputStream out = new DataOutputStream(socketC.getOutputStream());
            out.writeUTF(inf);
            //out.close();
        }

        //socketC.close();
    }

    /**
     * vo inf
     */
    private class ClientInfo {

        public ClientInfo(String ip, int port,Socket socket) {
            this.ip = ip;
            this.port = port;
            this.socket = socket;
        }

        private Socket socket;

        private String ip;

        private int port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public Socket getSocket() {
            return socket;
        }

        public void setSocket(Socket socket) {
            this.socket = socket;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null){
                return false;
            }
            ClientInfo ci = (ClientInfo) obj;
            if (ci == null){
                return false;
            }
            if (this.getIp().equals(ci.getIp()) && this.getPort() == ci.getPort()){
                return true;
            }else{
                return false;
            }
//        return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return (this.getIp()+":"+this.getPort()).hashCode();
        }
    }
}
