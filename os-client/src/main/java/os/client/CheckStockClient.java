package os.client;

import io.grpc.ConnectivityState;
import io.grpc.StatusRuntimeException;
import os.NameServiceClient;
import os.backend.grpc.generated.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class CheckStockClient {

    private ManagedChannel channel = null;
    CheckStockServiceGrpc.CheckStockServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;
    String details="";
 String serverName="";
    String[] serverNames = { "StockServer01", "StockServer02", "StockServer03" };
    public  String selectRandomServer() {
        if (serverNames.length == 0) {
            throw new IllegalArgumentException("Server names array is empty");
        }

        Random random = new Random();
        int randomIndex = random.nextInt(serverNames.length);
        return serverNames[randomIndex];
    }
        private void removeServer() {
        int index= getIndexByName( this.serverNames ,this.serverName );
        if(index>=0) {
            this.serverNames = removeElement(this.serverNames, index);
        }
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

    public void setDetails(String details) {

        this.details = details;
    }

    public void setServerName(String serverName) {

        this.serverName = serverName;
    }
    public CheckStockClient (String host, int port) {
        this.host = host;
        this.port = port;
    }
    public CheckStockClient (String serverName) throws IOException, InterruptedException {
            this.serverName=serverName;
        initializeConnection();
    }

    public static final String NAME_SERVICE_ADDRESS = "http://localhost:2379";
    private void fetchServerDetails() throws IOException,
            InterruptedException {
        NameServiceClient client = new  NameServiceClient(NAME_SERVICE_ADDRESS);
        NameServiceClient.ServiceDetails serviceDetails =
                client.findService(this.serverName);
        host = serviceDetails.getIPAddress();
        port = serviceDetails.getPort();
    }

    private void fetchServerDetails(String serverName) throws IOException,
            InterruptedException {
        NameServiceClient client = new  NameServiceClient(NAME_SERVICE_ADDRESS);
        NameServiceClient.ServiceDetails serviceDetails =
                client.findService(serverName);
        host = serviceDetails.getIPAddress();
        port = serviceDetails.getPort();
    }
    public void initializeConnection () throws IOException, InterruptedException {


this.fetchServerDetails();
        System.out.println("Initializing Connecting to server at " + host + ":" +
                port);
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        clientStub = CheckStockServiceGrpc.newBlockingStub(channel);

    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests(String details) throws InterruptedException, IOException {

        while (true) {
            String itemId ="";
            Scanner userInput = new Scanner(System.in);
            System.out.println("\nEnter Account ID to check the balance :");
            if(details.isEmpty()) {
                 itemId = userInput.nextLine().trim();
                details=itemId;
            }
            System.out.println("Requesting server to check the account balance for " + itemId.toString());

            CheckStockRequest request = CheckStockRequest
                    .newBuilder()
                    .setItemId(itemId)
                    .build();

            ConnectivityState state = channel.getState(true);

            CheckStockResponse response=null;

            try {
                 response = clientStub.checkStock(request);
                details="";
            }  catch (StatusRuntimeException e) {
                System.out.println("Connection failed.. retrying with other node");
                removeServer();
                fetchServerDetails(this.selectRandomServer());
                initializeConnection();
                processUserRequests(this.details);
                Thread.sleep(5000);

            }

            System.out.printf(itemId+ " Stock is " + response.getBalance() +itemId );


            Thread.sleep(1000);
        }
    }
    public void shoutdownChannel(io.grpc.ManagedChannel channel) throws InterruptedException {

        channel.shutdown();
        while(!channel.awaitTermination(5, TimeUnit.SECONDS)){

            Thread.sleep(3000);
        }
    }
    public void processUserRequestsTest() throws InterruptedException, IOException {
        while (true) {


            String itemId = details;
            System.out.println("Requesting server to check the account balance for " + itemId.toString());
            CheckStockRequest request = CheckStockRequest
                    .newBuilder()
                    .setItemId(itemId)
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
            CheckStockResponse response = clientStub.checkStock(request);
            System.out.printf(itemId+ " Stock is " + response.getBalance() +itemId );
            //shoutdownChannel(channel);
            Thread.sleep(1000);
        }
    }
}
