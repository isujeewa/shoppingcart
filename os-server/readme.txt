
java -jar os-server-1.0.1-jar-with-dependencies.jar StockServer01 localhost 11121
java -jar os-server-1.0.1-jar-with-dependencies.jar StockServer02 localhost 11122
java -jar os-server-1.0.1-jar-with-dependencies.jar StockServer03 localhost 11123
java -jar os-server-1.0.1-jar-with-dependencies.jar StockServer04 localhost 11124

java -jar  os-client-1.0.0-jar-with-dependencies.jar StockServer03 s

zkserver
etcd