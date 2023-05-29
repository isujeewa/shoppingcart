package os.backend;



import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javafx.util.Pair;
import org.apache.zookeeper.KeeperException;
import os.backend.grpc.generated.*;
import os.synchronization.DistributedTxCoordinator;
import os.synchronization.DistributedTxListener;
import os.synchronization.DistributedTxParticipant;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockServiceImpl extends StockServiceGrpc.StockServiceImplBase implements DistributedTxListener {
    private ManagedChannel channel = null;
    StockServiceGrpc.StockServiceBlockingStub clientStub = null;
    private StockServer server;

    private Pair<String, Double> tempDataHolder;
    private boolean transactionStatus = false;

    private boolean addOpperation=false;

    public StockServiceImpl(StockServer server){
        this.server = server;
    }
    public void setOpperation(boolean isAdd){
        this.addOpperation = isAdd;
    }
    private void startDistributedTx(String itemId, double value) {
        try {
            server.getTransaction().start(itemId, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new Pair<>(itemId, value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addStock(os.backend.grpc.generated.AddStockRequest request,
                           io.grpc.stub.StreamObserver<os.backend.grpc.generated.AddStockResponse> responseObserver) {

        this.setOpperation(true);
        String itemId  = request.getItemId();
        double value = request.getValue();
        if (server.isLeader()){
            // Act as primary
            try {
                System.out.println("Updating item stock as Primary ");
                startDistributedTx(itemId, value);
                updateSecondaryServers(itemId, value);
                System.out.println("going to perform");
                if (value > 0){
                    ((DistributedTxCoordinator)server.getTransaction()).perform();
                } else {
                    ((DistributedTxCoordinator)server.getTransaction()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println(server.getServerName() +":"+server.getServerPort() + " Error while updating the item stock" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As Secondary
            if (request.getIsSentByPrimary()) {
                System.out.println("Updating item stock on secondary, on Primary's command");
                startDistributedTx(itemId, value);
                if (value != 0.0d) {
                    ((DistributedTxParticipant)server.getTransaction()).voteCommit();
                } else {
                    ((DistributedTxParticipant)server.getTransaction()).voteAbort();
                }
            } else {
                AddStockResponse response = callPrimary(itemId, value);
                if (response.getStatus()) {
                    transactionStatus = true;
                }
            }
        }
        AddStockResponse response = AddStockResponse
                .newBuilder()
                .setStatus(transactionStatus)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void updateQuantity() {
        if (tempDataHolder != null) {
            String itemId = tempDataHolder.getKey();
            double value = tempDataHolder.getValue();
            double newStock= server.addItemQuantity(itemId, value);
            System.out.println(server.getServerName() +":"+server.getServerPort() + " Item " + itemId + " updated to stock " + value + " committed total new stock :" + newStock);
            tempDataHolder = null;
        }
    }

    private AddStockResponse callServer(String itemID, double value, boolean isSentByPrimary, String IPAddress, int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = StockServiceGrpc.newBlockingStub(channel);

        AddStockRequest request = AddStockRequest
                .newBuilder()
                .setItemId(itemID)
                .setValue(value)
                .setIsSentByPrimary(isSentByPrimary)
                .build();
        AddStockResponse response = clientStub.addStock(request);
        return response;
    }

    private AddStockResponse callPrimary(String itemId, double value) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServer(itemId, value, false, IPAddress, port);
    }

    private void updateSecondaryServers(String itemId, double value) throws KeeperException, InterruptedException {
        System.out.println("Updating other servers");
        List<String[]> othersData = server.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServer(itemId, value, true, IPAddress, port);
        }
    }

    //deduct qty

    @Override
    public void deductStock(os.backend.grpc.generated.DeductStockRequest request,
                         io.grpc.stub.StreamObserver<os.backend.grpc.generated.DeductStockResponse> responseObserver) {
        this.setOpperation(false);
        String itemId  = request.getItemId();
        double value = request.getValue();
        String cartReference = request.getCartId();
        if(!cartReference.isEmpty())
        {
            itemId=  itemId.concat("_").concat(cartReference);
        }
        if (server.isLeader()){
            // Act as primary
            try {
                System.out.println("Updating item stock as Primary");
                startDistributedTx(itemId, value);
                updateSecondaryServersDeduct(itemId, value);
                System.out.println("going to perform");
                if (value > 0){
                    ((DistributedTxCoordinator)server.getTransaction()).perform();
                } else {
                    ((DistributedTxCoordinator)server.getTransaction()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println(server.getServerName() +":"+server.getServerPort() + " Error while updating the item stock" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As Secondary
            if (request.getIsSentByPrimary()) {
                System.out.println("Updating item stock on secondary, on Primary's command");
                startDistributedTx(itemId, value);
                if (value != 0.0d) {
                    ((DistributedTxParticipant)server.getTransaction()).voteCommit();
                } else {
                    ((DistributedTxParticipant)server.getTransaction()).voteAbort();
                }
            } else {
                DeductStockResponse response = callPrimaryDeduct(itemId, value);
                if (response.getStatus()) {
                    transactionStatus = true;
                }
            }
        }
        DeductStockResponse response = DeductStockResponse
                .newBuilder()
                .setStatus(transactionStatus)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void deductQuantity() {
        if (tempDataHolder != null) {
            String itemId = tempDataHolder.getKey();
            double value = tempDataHolder.getValue();
            String cartRef="";
            if(itemId.indexOf("_")>=0)
            {
                String[] splitStrings = itemId.split("_");
                itemId=splitStrings[0];
                cartRef =splitStrings[1];
            }

            double newStock= server.deductItemQuantity(itemId, value);
            if(newStock>=0 ) {
                System.out.println(server.getServerName() +":"+server.getServerPort() + " Item " + itemId + "  stock deducted by" + value + " committed total new stock :" + newStock +"  Cart Reference: "+ cartRef);
            }else{
                System.out.println(server.getServerName() +":"+server.getServerPort() + " Insufficient stock.Operation cancelled ");

            }
            tempDataHolder = null;
        }
    }

    private DeductStockResponse callServerDeduct(String itemID, double value, boolean isSentByPrimary, String IPAddress, int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = StockServiceGrpc.newBlockingStub(channel);

        DeductStockRequest request = DeductStockRequest
                .newBuilder()
                .setItemId(itemID)
                .setValue(value)
                .setIsSentByPrimary(isSentByPrimary)
                .build();
        DeductStockResponse response = clientStub.deductStock(request);
        return response;
    }

    private DeductStockResponse callPrimaryDeduct(String itemId, double value) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServerDeduct(itemId, value, false, IPAddress, port);
    }

    private void updateSecondaryServersDeduct(String itemId, double value) throws KeeperException, InterruptedException {
        System.out.println("Updating other servers");
        List<String[]> othersData = server.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServerDeduct(itemId, value, true, IPAddress, port);
        }
    }


    //deduct end
//Get all stock

    @Override
    public void getAllStock(os.backend.grpc.generated.GetAllStockRequest request,
                           io.grpc.stub.StreamObserver<os.backend.grpc.generated.GetAllStockResponse> responseObserver) {

       // String itemId = request.getItemId();
        System.out.println(server.getServerName() +":"+server.getServerPort() + " Request received..");
       String items = getItemStock();
        GetAllStockResponse response = GetAllStockResponse
                .newBuilder()
                .setItems(items)
                .build();
        System.out.println(server.getServerName() +":"+server.getServerPort() + " Number of items : " +items.split("#").length);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    private   void parseItems(String input) {
        String[] keyValuePairs = input.split(",");

        for (String pair : keyValuePairs) {
            String[] parts = pair.split("#");
            String item = parts[0];
            double quantity = Double.parseDouble(parts[1]);
            System.out.println(  server.getServerName() +":"+server.getServerPort() + " Updating stocks ....item id "+ item +" qty " +quantity);
            server.addItemQuantity(item, quantity );
        }


    }
    private String getItemStock( ) {

        return server.getItemStocks();
    }
    //get all stock end

    public void updateSecondaryServerInventory() throws KeeperException, InterruptedException {
        System.out.println("Updating stocks ....");
                boolean ok=false;
                while(!ok ) {
                    if(server.hasLeaderInfo()) {
                        ok=true;
                        String[] currentLeaderData = server.getCurrentLeaderData();

                        String IPAddress = currentLeaderData[0];
                        int port = Integer.parseInt(currentLeaderData[1]);
                        System.out.println("leader stocks ...." + IPAddress +" " +port);
                        callStockGetAll(IPAddress,port);
                    }else {
                        if(this.server.isLeader()){
                            ok=true;
                        }

                        Thread.sleep(1000);
                    }
                }

        // callServer(itemId, value, true, IPAddress, port);
    }

    private void callStockGetAll(String ip,int port ){
         StockServiceGrpc.StockServiceBlockingStub clientStub = null;
        channel = ManagedChannelBuilder.forAddress(ip, port)
                .usePlaintext()
                .build();
        clientStub = StockServiceGrpc.newBlockingStub(channel);

        GetAllStockRequest request = GetAllStockRequest
                .newBuilder()
                .build();
        GetAllStockResponse response = clientStub.getAllStock(request);

        String value = response.getItems();
        parseItems(value);
        System.out.println(value);

    }
    @Override
    public void onGlobalCommit() {
        if(this.addOpperation){
           updateQuantity();
        }
        else{
            deductQuantity();
        }
    }

    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        System.out.println("Transaction Aborted by the Coordinator");
    }
}
