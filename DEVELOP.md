# CDWH API DOCUNMENT

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