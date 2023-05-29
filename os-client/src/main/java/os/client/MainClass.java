package os.client;

import os.client.test.AdminStockUpdate;
import os.client.test.CustomerPurchase;

import java.io.IOException;
import java.util.Random;

public class MainClass {
    public static String[]  serverNames = { "StockServer01", "StockServer02", "StockServer03" };
    public static String selectRandomServer() {
        if (serverNames.length == 0) {
            throw new IllegalArgumentException("Server names array is empty");
        }

        Random random = new Random();
        int randomIndex = random.nextInt(serverNames.length);
        return serverNames[randomIndex];
    }
    public static void main(String[] args) throws InterruptedException, IOException {

        String serverName = selectRandomServer();
        String operation = "s";


        if (args.length == 1) {
            System.out.println("Usage StockClient server name, <s(et)|c(heck)>");


         //   serverName = args[0];
           operation = args[0];

            if ("s".equals(operation)) {
                StockClient client = new StockClient(serverName);
                client.initializeConnection();
                client.processUserRequests("");
                client.closeConnection();
            }
            else if ("d".equals(operation)) {
                StockClient client = new StockClient(serverName);
                client.initializeConnection();
                client.processUserRequestsDeduct("");
                client.closeConnection();
            }
            else {
                CheckStockClient client = new CheckStockClient(serverName);
                client.initializeConnection();
                client.processUserRequests("");
                client.closeConnection();
            }
        } else if (args.length==2) {

                System.out.println("Usage StockClient server name, <s(et)|c(heck)>");


                   serverName = args[0];
                operation = args[1];

                if ("s".equals(operation)) {
                    StockClient client = new StockClient(serverName);
                    client.initializeConnection();
                    client.processUserRequests("");
                    client.closeConnection();
                }
                else if ("d".equals(operation)) {
                    StockClient client = new StockClient(serverName);
                    client.initializeConnection();
                    client.processUserRequestsDeduct("");
                    client.closeConnection();
                }
                else {
                    CheckStockClient client = new CheckStockClient(serverName);
                    client.initializeConnection();
                    client.processUserRequests("");
                    client.closeConnection();
                }


        } else {

            System.out.println("Please use use correct options... argument count " + args.length);
/*
        AdminStockUpdate adminStockUpdate=new AdminStockUpdate();
        adminStockUpdate.setSetPort(11144);
        adminStockUpdate.serverName("StockServer01");
        adminStockUpdate.setDetails("Apple,100");
        Thread adminTread01 = new Thread(adminStockUpdate);
        adminTread01.setPriority(Thread.MAX_PRIORITY);
        adminTread01.start();

        CustomerPurchase customerPurchase02 =new CustomerPurchase();
        customerPurchase02.setSetPort(11146);
        customerPurchase02.setCheckStock(false);
        customerPurchase02.setDetails("Apple,120,customer01,sujeewa01");

        Thread thread2 = new Thread(customerPurchase02);
        thread2.setPriority(Thread.MIN_PRIORITY);


        CustomerPurchase customerPurchase =new CustomerPurchase();
        customerPurchase.setSetPort(11145);
        customerPurchase.serverName("StockServer01");

        customerPurchase.setCheckStock(true);
        customerPurchase.setDetails("Apple");






        Thread thread1 = new Thread(customerPurchase);
        thread1.setPriority(Thread.MIN_PRIORITY);
  //thread1.start();



   thread2.start();

   */


        }












    }
}
