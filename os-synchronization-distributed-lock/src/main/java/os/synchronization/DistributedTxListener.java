package os.synchronization;

public interface DistributedTxListener {
    void onGlobalCommit();
    void onGlobalAbort();
}
