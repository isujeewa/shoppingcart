package os.client.test;


import os.client.CheckStockClient;
import os.client.StockClient;

import java.io.IOException;

public class CustomerPurchase implements Runnable {

    private int port;

    private boolean checkStock;
    String item = null;

    String details="";

    String serverName="";


    public void serverName(String serverName) {
        this.serverName = serverName;
    }
    public void setDetails(String details) {
        System.out.println("\nEnter Item ID, quantity to set the stock :");
        this.details = details;
    }
    public void setSetPort(int port) {
        this.port = port;
    }
    public void setItem(String item) {
        System.out.println("\nEnter Item ID, quantity to set the stock :");
        this.item = item;
    }
    public void setCheckStock(boolean check) {
        this.checkStock = check;
    }



    String remove="";

    public void setPurchase(String purchase) {
        System.out.println("\nEnter Item ID, quantity to set the stock :");
        this.remove = purchase;
    }
    @Override
    public void run() {
        if(checkStock){

            CheckStockClient client = null;
            try {
                client = new CheckStockClient("StockServer02");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
        else {
            StockClient client = null;
            try {
                client = new StockClient("StockServer02");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            client.setDetails(details);
            try {
                client.initializeConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                try {
                    client.processUserRequestsDeductTest();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            client.closeConnection();

        }

    }

}