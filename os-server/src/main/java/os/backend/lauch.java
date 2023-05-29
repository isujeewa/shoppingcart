package os.backend;

import org.apache.zookeeper.KeeperException;
import os.NameServiceClient;
import os.synchronization.DistributedLock;
import os.synchronization.DistributedTx;

import java.io.IOException;

public class lauch implements Runnable {
    public static final String NAME_SERVICE_ADDRESS =
            "http://localhost:2379";
    String name="";
    String ip="";
    int port =0;
    public void setName(String name1) {

        this.name = name1;
    }
    public void setIP(String ip1) {

        this.ip = ip1;
    }
    public void setPort(int port) {

        this.port = port;
    }
    @Override
    public void run() {

        System.out.println("running " + name);
     //   String  name ="";//""StockServer01";
       // String  ip  ="";//""127.0.0.1";

        try {
            NameServiceClient client = new NameServiceClient(NAME_SERVICE_ADDRESS);
            client.registerService(name, this.ip, this.port, "tcp");
        }catch (Exception e){

            System.out.println("etcd is not available");
        }

        StockServer server = null;
        try {
            server = new StockServer(ip, port);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }

        try {
            server.startServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }
    }
}
