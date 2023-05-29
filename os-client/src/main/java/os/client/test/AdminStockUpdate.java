package os.client.test;

import os.client.StockClient;

import java.io.IOException;
import java.util.UUID;

public class AdminStockUpdate implements Runnable{
    private int port;
    String details="";
    String serverName="";


    public void setSetPort(int port) {
        this.port = port;
    }
    public void serverName(String serverName) {
        this.serverName = serverName;
    }

    public void setDetails(String details) {
        System.out.println("\nEnter Item ID, quantity to set the stock :");
        System.out.println(details);
        this.details = details;
    }


    @Override
    public void run() {
        // If the delay flag is set, sleep for 2 seconds before performing the stock update
/*
            try {
                Thread.sleep(2000); // Sleep for 2 seconds
            } catch (InterruptedException e) {
                // Handle the exception
            }*/
        StockClient client = null;
        try {
            client = new StockClient("StockServer01");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        client.setServerName("StockServer01");
        client.setDetails(details);
        try {
            client.initializeConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            client.processUserRequestsTest();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        client.closeConnection();

    }
}
