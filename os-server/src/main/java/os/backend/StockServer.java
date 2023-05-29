package os.backend;


import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.zookeeper.KeeperException;
import os.NameServiceClient;
import os.synchronization.DistributedLock;
import os.synchronization.DistributedTx;
import os.synchronization.DistributedTxParticipant;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StockServer {


    private DistributedLock leaderLock;
    private AtomicBoolean isLeader = new AtomicBoolean(false);
    private byte[] leaderData;
    private int serverPort;
    private String  serverName;
    private Map<String, Double> items = new HashMap();

    private Object stockLock; // Object used for synchronization

    DistributedTx transaction;
    StockServiceImpl stockServiceImpl;
    CheckStockServiceImpl checkStockServiceImpl;

    public String getServerName(){
        return  serverName;
    }

    public int getServerPort(){
        return  serverPort;
    }
    public static String buildServerData(String IP, int port) {
        StringBuilder builder = new StringBuilder();
        builder.append(IP).append(":").append(port);
        return builder.toString();
    }

    public StockServer(String host, int port) throws InterruptedException, IOException, KeeperException {
        this.serverPort = port;
        this.serverName =host;
        this.stockLock = new Object(); // Initialize synchronization object
        leaderLock = new DistributedLock("StockServerCluster", buildServerData(host, port));

        stockServiceImpl = new StockServiceImpl(this);
        checkStockServiceImpl = new CheckStockServiceImpl(this);
        transaction = new DistributedTxParticipant(stockServiceImpl);
    }

    private void tryToBeLeader() throws KeeperException, InterruptedException {
        Thread leaderCampaignThread = new Thread(new LeaderCampaignThread());
        leaderCampaignThread.start();
    }

    public DistributedTx getTransaction() {
        return transaction;
    }

    public void startServer() throws IOException, InterruptedException, KeeperException {
        Server server = ServerBuilder
                .forPort(serverPort)
                .addService(checkStockServiceImpl)
                .addService(stockServiceImpl)
                .build();
        server.start();


        tryToBeLeader();
        System.out.println("Stock Server Started and ready to accept requests on " + serverName +":"+serverPort  );
        try {
            StockServiceImpl stockServiceImpl = new StockServiceImpl(this);
            stockServiceImpl.updateSecondaryServerInventory();
        }catch (Exception e){

            System.out.println("--");
        }

        System.out.println( this.serverName +":"+ this.serverPort + " Server is ready.");
        server.awaitTermination();
    }

    public boolean isLeader() {
        return isLeader.get();
    }

    private synchronized void setCurrentLeaderData(byte[] leaderData) {
        this.leaderData = leaderData;
    }

    public synchronized String[] getCurrentLeaderData() {
        return new String(leaderData).split(":");
    }
    public  boolean hasLeaderInfo() {
        return  leaderData!=null;
    }
    public  Double addItemQuantity(String itemId, double value) {
        Double newStock = value;
        synchronized (stockLock) {
            Double oldStock = items.get(itemId);

            if (oldStock != null && oldStock > 0) {
                newStock = newStock + oldStock;
            }
            items.put(itemId, newStock);
        }
        return  newStock;
        //items.put(itemId, value);
    }

    public  Double deductItemQuantity(String itemId, double value) {
        Double newStock=value;
        synchronized (stockLock) {
            Double oldStock = items.get(itemId);

            if (oldStock != null && oldStock >= newStock) {
                newStock = oldStock - newStock;
                items.put(itemId, newStock);
            } else {
                newStock = -1.0;
            }
        }
        return  newStock;
        //items.put(itemId, value);
    }

    public double getItemStock(String itemId) {
        Double value = items.get(itemId);
        return (value != null) ? value : 0.0;
    }

    public String getItemStocks() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> entry : items.entrySet()) {
            String item = entry.getKey();
            double quantity = entry.getValue();
            sb.append(item).append("#").append(quantity).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);  // Remove the last comma
        }
        String result = sb.toString();

        return result;
    }

    public List<String[]> getOthersData() throws KeeperException, InterruptedException {
        List<String[]> result = new ArrayList<>();
        List<byte[]> othersData = leaderLock.getOthersData();

        for (byte[] data : othersData) {
            String[] dataStrings = new String(data).split(":");
            result.add(dataStrings);
        }
        return result;
    }



    class LeaderCampaignThread implements Runnable {
        private byte[] currentLeaderData = null;
        @Override
        public void run() {
            System.out.println("Starting the leader Campaign");

            try {
                boolean leader = leaderLock.tryAcquireLock();

                while (!leader) {
                    byte[] leaderData = leaderLock.getLockHolderData();
                    if (currentLeaderData != leaderData) {
                        currentLeaderData = leaderData;
                        setCurrentLeaderData(currentLeaderData);
                    }
                    Thread.sleep(10000);
                    leader = leaderLock.tryAcquireLock();
                }
                currentLeaderData = null;
                beTheLeader();
            } catch (Exception e){
            }
        }
    }

    private void beTheLeader() {
        System.out.println("I got the leader lock. Now acting as primary");
        isLeader.set(true);
        transaction = new os.synchronization.DistributedTxCoordinator(stockServiceImpl);
    }

    public static void main (String[] args) throws Exception{
        DistributedLock.setZooKeeperURL("localhost:2181");
        DistributedTx.setZooKeeperURL("localhost:2181");
        if (args.length ==3) {
           // Scanner userInput = new Scanner(System.in);
           // String input[] = userInput.nextLine().trim().split(",");
            System.out.println("Starting another server");
            System.out.println("Server | Ip | Port");
            lauch s4 = new lauch();
            s4.setName(args[0]);
            s4.setIP(args[1]);
            s4.setPort( Integer.parseInt(args[2]));
            Thread node04 = new Thread(s4);
            node04.start();
        }
        else {
            //   int serverPort = Integer.parseInt(args[0]);

            System.out.println("Node 01 started.");
            lauch s1 = new lauch();
            s1.setName("StockServer01");
            s1.setIP("localhost");
            s1.setPort(11144);
            Thread node01 = new Thread(s1);
            node01.start();

            lauch s2 = new lauch();
            s2.setName("StockServer02");
            s2.setIP("localhost");
            s2.setPort(11145);
            Thread node02 = new Thread(s2);
            node02.start();
            System.out.println("Node 02 started.");
            lauch s3 = new lauch();
            s3.setName("StockServer03");
            s3.setIP("localhost");
            s3.setPort(11146);
            Thread node03 = new Thread(s3);
            node03.start();
            System.out.println("Node 03 started.");
        }

    }
}
