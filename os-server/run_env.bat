@echo off

REM Start ZooKeeper
start cmd /k zkServer.cmd

cd /d D:\etcd-v3.5.9-windows-amd64\etcd-v3.5.9-windows-amd64
etcd