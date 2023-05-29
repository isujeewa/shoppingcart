package os.client;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import os.NameServiceClient;
import os.backend.grpc.generated.*;

import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class StockClient {
    String[] serverNames = { "StockServer01", "StockServer02", "StockServer03" };
    private ManagedChannel channel = null;
    StockServiceGrpc.StockServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;
    String details="";

    String serverName="";
    public static final String NAME_SERVICE_ADDRESS = "http://localhost:2379";
    public StockClient (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static String[] removeElement(String[] array, int index) {
        if (index < 0 || index >= array.length) {
            return array; // Invalid index, return the original array
        }

        String[] newArray = new String[array.length - 1];
        int newArrayIndex = 0;

        for (int i = 0; i < array.length; i++) {
            if (i != index) {
                newArray[newArrayIndex] = array[i];
                newArrayIndex++;
            }
        }

        return newArray;
    }

    public static int getIndexByName(String[] array, String elementName) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(elementName)) {
                return i; // Found the element, return its index
            }
        }

        return -1; // Element not found in the array
    }

    public  String selectRandomServer() {
        if (serverNames.length == 0) {
            throw new IllegalArgumentException("Server names array is empty");
        }

        Random random = new Random();
        int randomIndex = random.nextInt(serverNames.length);
        return serverNames[randomIndex];
    }

    public StockClient ( String serverName) throws IOException, InterruptedException {
        this.serverName =serverName;
        fetchServerDetails(this.selectRandomServer());
    }
    public void setDetails(String details) {

        this.details = details;
    }

    public void setServerName(String serverName) {

        this.serverName = serverName;
    }
    public void initializeConnection () throws IOException, InterruptedException {

        System.out.println("Initializing Connecting to server at " + host + ":" +
                port);
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        clientStub = StockServiceGrpc.newBlockingStub(channel);

    }
    public void closeConnection() {
        channel.shutdown();
    }

    private void fetchServerDetails(String serverName) throws IOException,
            InterruptedException {
        this.serverName =serverName;
        NameServiceClient client = new  NameServiceClient(NAME_SERVICE_ADDRESS);
        NameServiceClient.ServiceDetails serviceDetails =
                client.findService(this.serverName);
        host = serviceDetails.getIPAddress();
        port = serviceDetails.getPort();
    }

    public void processUserRequests(String details) throws InterruptedException, IOException {
        while (true) {
            Scanner userInput = new Scanner(System.in);
            double amount=0;
            String input[]  ;
            String itemId="";

            if(details.isEmpty()) {
                System.out.println("\nEnter ItemID, stock to add to inventory :");
                input = userInput.nextLine().trim().split(",");
                 itemId = input[0];
                amount = Double.parseDouble(input[1]);
                this.details = input[0] + "," + input[1];
            }else{
                input = details.split(",");
                itemId = input[0];
                amount = Double.parseDouble(input[1]);

            }
            System.out.println("Requesting server to set the quantity " + amount + " for " + itemId.toString());
            initializeConnection();
            AddStockRequest request = AddStockRequest
                    .newBuilder()
                    .setItemId(itemId)
                    .setValue(amount)
                    .setIsSentByPrimary(false)
                    .build();
          ConnectivityState state = channel.getState(true);

            AddStockResponse response = null;
            // while (state != ConnectivityState.READY ) { // this code is not working for some reason always returns idel

try {
    response = clientStub.addStock(request);
    details="";
}  catch (StatusRuntimeException e) {
    System.out.println("Connection failed.. retrying with other node");

    removeServer();
    fetchServerDetails(this.selectRandomServer());
    initializeConnection();
    processUserRequests(this.details);

    Thread.sleep(5000);

        }

            System.out.printf("Transaction Status " + (response.getStatus() ? "Sucessful" : "Failed"));
            shoutdownChannel(channel);
            Thread.sleep(1000);
        }
    }

    private void removeServer() {
        int index= getIndexByName( this.serverNames ,this.serverName );
        if(index>0) {
            this.serverNames = removeElement(this.serverNames, index);
        }
    }

    public void processUserRequestsDeduct(String details) throws InterruptedException, IOException {
        while (true) {
            Scanner userInput = new Scanner(System.in);
            System.out.println("\nEnter ItemID, stock, cart reference to process the shopping cart :");
            double amount=0;
            String input[]  ;
            String itemId="";
            String cartRef = "";

            if(details.isEmpty()) {
                input = userInput.nextLine().trim().split(",");
                this.details = input[0] + "," + input[1]+","+input[2];

            }else{
                input = details.split(",");
            }
            itemId = input[0];
            amount = Double.parseDouble(input[1]);
            cartRef = input[2];

            System.out.println("Requesting server to process the cart (item " + amount + " for " + itemId.toString() +")");
            DeductStockRequest request = DeductStockRequest
                    .newBuilder()
                    .setItemId(itemId)
                    .setValue(amount)
                    .setCartId(cartRef)
                    .setIsSentByPrimary(false)
                    .build();
            DeductStockResponse response =null;
                    ConnectivityState state = channel.getState(true);
            try {
                response = clientStub.deductStock(request);
                details="";
            }  catch (StatusRuntimeException e) {

                System.out.println("Connection failed.. retrying with other node");
                removeServer();
                fetchServerDetails(this.selectRandomServer());
                initializeConnection();
                processUserRequestsDeduct(this.details);
                Thread.sleep(5000);

            }
           // System.out.printf("Transaction Status " + (response.getStatus() ? "Sucessful" : "Failed"));
            //shoutdownChannel(channel);
            Thread.sleep(1000);
        }
    }

    public void processUserRequestsDeductTest() throws InterruptedException, IOException {

        while (true) {
            String input[] = details.trim().split(",");
            String itemId = input[0];
            double amount = Double.parseDouble(input[1]);
            String cartRef = input[2];
            System.out.println("Requesting server to set the quantity " + amount + " for " + itemId.toString());
            DeductStockRequest request = DeductStockRequest
                    .newBuilder()
                    .setItemId(itemId)
                    .setValue(amount)
                    .setCartId(cartRef)
                    .setIsSentByPrimary(false)
                    .build();

                  ConnectivityState state = channel.getState(true);
            boolean ok=false;
            while (state != ConnectivityState.READY) {
                System.out.println("Service unavailable,    looking for a service provider..");
                if(!ok){
                    fetchServerDetails(this.selectRandomServer());
                    initializeConnection();
                    processUserRequests(this.details);
                    ok=true;
                }
            Thread.sleep(1000);
            state = channel.getState(true);
        }

            DeductStockResponse response = clientStub.deductStock(request);
          //  System.out.printf("Transaction Status " + (response.getStatus() ? "Sucessful" : "Failed"));
           // shoutdownChannel(channel);
            Thread.sleep(1000);
        }
    }

    public void shoutdownChannel(io.grpc.ManagedChannel channel) throws InterruptedException {
        System.out.println("shout down the channel");
        channel.shutdown();
        while(!channel.awaitTermination(5, TimeUnit.SECONDS)){

            Thread.sleep(2000);
        }
    }
    public void processUserRequestsTest() throws InterruptedException, IOException {

        while (true) {
            String input[] = details.trim().split(",");
            String itemId = input[0];
            double amount = Double.parseDouble(input[1]);

            System.out.println("Requesting server to set the quantity " + amount + " for " + itemId.toString());
            AddStockRequest request = AddStockRequest
                    .newBuilder()
                    .setItemId(itemId)
                    .setValue(amount)
                    .setIsSentByPrimary(false)
                    .build();
            ConnectivityState state = channel.getState(true);
            boolean ok=false;
            while (state != ConnectivityState.READY) {
                System.out.println("Service unavailable,    looking for a service provider..");
                if(!ok){
                    fetchServerDetails(this.selectRandomServer());
                    initializeConnection();
                    ok=true;
                }
                Thread.sleep(1000);

                state = channel.getState(true);
            }
            AddStockResponse response = clientStub.addStock(request);
           // System.out.printf("\nTransaction Status " + (response.getStatus() ? "Sucessful" : "Failed"));
          //  shoutdownChannel(channel);
            Thread.sleep(1000);
        }
    }

}
