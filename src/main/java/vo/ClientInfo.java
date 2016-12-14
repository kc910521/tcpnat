package vo;

/**
 * Created by robu on 2016/12/6.
 */
public class ClientInfo {

    public ClientInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

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
