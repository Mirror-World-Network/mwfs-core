# CDWH DEVELOP DOCUNMENT

## Airdrop 空投相关API
> Ref code:<br> org.conch.http.Airdrop <br> & <br> org.conch.http.AirdropDetection

```shell
curl -X POST "https://IP/sharder?requestType=airdrop"
curl -X POST "https://IP/sharder?requestType=airdropDetection"
```
> request data:

```json
{
    "key": "***",
    "jsonString": "{"feeNQT":"0","list": [], "deadline":"1440","secretPhrase":""}", // 选填
    "pathName": "/conf/airdrop.json", // 默认不填
}
```
### 空投处理流程图
![空投处理流程图](https://s1.ax1x.com/2020/11/02/BDHMWT.png)
> 空投检测入口与返回逻辑与此类似

### 空投相关配置项

```properties
### Airdrop ###
# airdrop pathName
sharder.airdrop.pathName=conf/airdrop.json
# valid keys for airdrop
sharder.airdrop.validKeys=finish-rant-princess;shift-things-problem
# airdrop append mode switch
sharder.airdrop.isAppendMode=true
# airdrop switch
sharder.airdrop.enable=true
# airdrop open account 
sharder.airdrop.account=
```

## 区块链详细信息打印至日志API
> Ref code: org/conch/http/GetCommandLineClientInfo.java

```shell
curl "https://IP/sharder?requestType=getCommandLineClientInfo"
```
> request data:

```json
{
"firstIndex": 0, 
"lastIndex": 9
// 一些非必填参数
}
```

> response data:

```markdown
- 最近的一个区块的详情

- 最近3个区块详情，最近10个区块的详情

- 当前节点矿工挖矿的信息

- 最近收到的被挖出区块的信息列表

- 引导节点间boot、na、nb的tcp和udp端口连通检测
```

## 创建交易deadline属性详解

**deadline:** 创建的交易的有效时间（min）,超过该时间后该交易将不再被其他节点处理,不再进行广播处理

> eg: deadline = 18, 新创建的交易有效时间为18分钟


> org.conch.tx.TransactionImpl.getExpiration
```java
public int getExpiration() {
    return timestamp + deadline * 60;
}
```
getExpiration 定义了transaction的有效时间截止至 timestamp + deadline * 60, timestamp = transaction被build的时间戳


> org.conch.chain.BlockchainProcessorImpl.validateTransactions
```java
// org/conch/chain/BlockchainProcessorImpl.java:1836
if (transaction.getTimestamp() > block.getTimestamp() + Constants.MAX_TIMEDRIFT
        || (transaction.getExpiration() < block.getTimestamp())) {
    throw new TransactionNotAcceptedException(
            "Invalid transaction timestamp "
                    + transaction.getTimestamp()
                    + ", current time is "
                    + curTime
                    + ", block timestamp is "
                    + block.getTimestamp(),
            transaction);
}
```
当交易的有效截至时间戳 小于 区块生成的时间戳，交易校验为无效

> org.conch.chain.BlockchainProcessorImpl.selectUnconfirmedTransactions
```java
if (blockTimestamp > 0
        && (unconfirmedTransaction.getTimestamp() > blockTimestamp + Constants.MAX_TIMEDRIFT
        || unconfirmedTransaction.getExpiration() < blockTimestamp)) {
    continue;
}
```
当未确认的交易有效截至时间戳 小于 区块生成的时间戳，将被丢且

> org.conch.tx.TransactionProcessorImpl.rebroadcastTransactionsThread
```java
for (TransactionImpl transaction : broadcastedTransactions) {
    if (transaction.getExpiration() < curTime || TransactionDb.hasTransaction(transaction.getId())) {
        broadcastedTransactions.remove(transaction);
    } else if (transaction.getTimestamp() < curTime - 30) {
        transactionList.add(transaction);
    }
}
```
已广播的交易的有效截至时间 小于 当前时间时，将取消该交易的再次广播

> org.conch.tx.TransactionProcessorImpl.processTransaction
> org.conch.tx.TransactionProcessorImpl.processWaitingTransactions

在处理未确认的交易&等待的交易时，交易的有效截至时间 小于 当前时间，该交易将被视为无效

## 转账API相关说明

### 转账接口中离线签名模式流程图

![](https://mwfs.oss-cn-shenzhen.aliyuncs.com/docs/2_20201012155304.png)

### 转账接口内部逻辑流程图

![](https://mwfs.oss-cn-shenzhen.aliyuncs.com/docs/1_20201012155258.png)
