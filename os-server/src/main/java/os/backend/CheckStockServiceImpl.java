package os.backend;

import java.util.Random;

import os.backend.grpc.generated.CheckStockRequest;
import os.backend.grpc.generated.CheckStockResponse;
import os.backend.grpc.generated.StockServiceGrpc;
import os.backend.grpc.generated.CheckStockServiceGrpc;

public class CheckStockServiceImpl extends  CheckStockServiceGrpc.CheckStockServiceImplBase {

    private StockServer server;

    public CheckStockServiceImpl(StockServer server){
        this.server = server;
    }

    @Override
    public void checkStock(os.backend.grpc.generated.CheckStockRequest request,
                             io.grpc.stub.StreamObserver<os.backend.grpc.generated.CheckStockResponse> responseObserver) {

        String itemId = request.getItemId();
        System.out.println("Request received..");
        double balance = getItemStock(itemId);
        CheckStockResponse response = CheckStockResponse
                .newBuilder()
                .setBalance(balance)
                .build();
        System.out.println("Responding, Stock for item " + itemId + " is " + balance);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private double getItemStock(String itemId) {
        return server.getItemStock(itemId);
    }
}
