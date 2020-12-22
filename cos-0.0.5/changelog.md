The latest version is cos-client-0.0.5 2020-12-16 14:17:25, You can get the newest version at official site.

![COS](https://mwfs.oss-cn-shenzhen.aliyuncs.com/cos/mwfs_icon.png)
                                    
MW is an easy-to-use and available distributed storage net.

## Supported platforms
- Windows 32-bit / 64-bit   
- Linux 32-bit / 64-bit    
- MacOS 64-bit  
## v0.0.5 Changelog
- Testnet Reset
- 110+ new features and bug fix
- Table division and performance optimization
- Data file growth optimization
- Improved node connectivity & reduced bifurcation probability
- Poc Weight table adjustment
- Snapshots and airdrops
- Mobile UI upgrade and optimization 
----
- 测试网重置
- 110+的功能优化和缺陷修复
- '合格矿工'收益定时集中清算
- 分表和性能优化
- 数据文件增长优化
- 提升节点连通性并降低分叉几率
- 权重表调整
- 快照和空投
- 移动端UI升级和优化
## v0.0.5-Hotfix1 Changelog
- Fix blockchain stuck bug
- Fix PoC score display bug
- Some UI display optimization and fixed other known issues
----
- 修复卡块缺陷
- 修复PoC分数显示缺陷
- 一些UI显示优化和已经缺陷修复
## v0.0.5-Hotfix2 Changelog
- Fix account balance wrong bug
- Fix double spend bug
- Some UI display optimization and fixed other known issues
----
- 修复账户余额缺陷
- 修复双花缺陷
- 一些UI显示优化和已经缺陷修复
## v0.0.5-Hotfix3 Changelog
- Fix transfer bug of UI
- Some UI display optimization and fixed other known issues
----
- 修复UI上的转账缺陷
- 一些UI显示优化和已经缺陷修复
## v0.0.5-Hotfix4 Changelog
- Emergency Fix: 42001-194 SQL bug
- Some UI display optimization and fixed other known issues
----
- 紧急修复：42001-194 SQL缺陷
- 一些UI显示优化和已经缺陷修复
## v0.0.5-Hotfix5 Changelog
- Fix reward distribution stuck bug 
- Some UI display optimization and fixed other known issues
----
- 修复奖励发放导致卡块的缺陷
- 一些UI显示优化和已知缺陷修复
## v0.0.5-Hotfix6 Changelog
- Fix bug of reward interval compatibility
- Optimize the daemon shell of ubuntu OS
- Optimize the network stable and connectivity between peers
- Some UI display optimization and fixed other known issues
----
- 修复奖励间隔的兼容性缺陷
- 优化ubuntu系统的守护进程脚本
- 优化网络的稳定性和节点间的连通性
- 一些UI显示优化和已知缺陷修复
----------------------------- Other Versions ------------------------------
## v0.0.4 Changelog
- New function: Crowd miners rewards distribution. Effective at the block height 13686
- New function: Crowd miners rewards distribution & block mining rewards distribution detail page 
- Qualified miner conditions updated: a) holding the 4256 MW (staking MW amounts of 32T). b) create a declare node tx and linked the mining address
- Fixed an issue of linked address wrong
- Fixed an issue of next mining time display wrong
- Fixed the issues: mobile style wrong and risk tips of the login page
- Fixed an PC client issue of longer address of miner wrong display problem  
- Default value of the numeric type be set to '--'
- Add the 'testna' and 'testnb' boot nodes into default bootstrap node list 
- Fixed and upgraded the formula of the PoC calculation and display wrong problem
- Fixed the issues: lower precision caused wrong frozen amount display of account
- Improve the connectivity between nodes to increase the efficiency of block synchronize
- Improve the algorithm of consensus to decrease the fork chance
- Improve the node found logic to decrease the fork chance
- Some UI display optimization and fixed other known issues
----
- 新增有效矿工共享出块收益功能，从13686区块高度开始生效。
- 增加系统奖励的分配详情显示：有效矿工奖励和出块奖励
- 有效矿工条件更新：a）持有4256 MW（32T的MW抵押数量）。b）完成过节点声明，绑定了矿工地址。
- 修复错误的绑定地址显示问题。
- 修复移动端出块预计出块时间显示错误。
- 优化移动端的显示样式和风险提示信息。
- 修复PC端矿工地址超长显示问题。
- 数据读取过程中默认显示'--'。
- 增加'testna'和'testnb'节点到默认的测试网引导节点列表。
- 修复和升级矿工的PoC分数计算公式和显示错误。
- 修复冻结金额显示精度导致无法正确显示问题。
- 提高节点连通度检测，优化节点同步效率。
- 共识算法调优以降低分叉几率。
- 优化节点发现逻辑以降低分叉几率。
- 一些UI显示优化和已经缺陷修复。
## v0.0.4-Hotfix1 Changelog
- Optimize the data storage, fix the problem: rapid growth of data file
- Optimize the PoC score calculation: reduce the magnification to 500(effective from height 15414)
- Improve the node found logic to decrease the fork chance
- Improve the txs processing performance per block
- Improve the connectivity between nodes to increase the efficiency of block synchronize
- Some UI display optimization and fixed other known issues
----
- 优化数据存储，解决数据文件增长过快问题
- PoC计算优化：MW持有分数的扩大倍数下调到500, 从15414高度开始生效
- 优化节点同步，降低分叉几率
- 提升单个区块交易处理性能
- 提升区块间的节点同步性能
- 一些UI显示优化和已经缺陷修复
## v0.0.4-Hotfix2 Changelog
- Optimize the data storage
- Improve the node found logic to decrease the fork chance
- Improve the txs processing performance per block
- New feature of db archive synchronisation
- New feature of performance listener and log analysis
- Fix the bug of amount parse wrong
- Some UI display optimization and fixed other known issues
----
- 优化数据存储
- 修复PoC计算缺陷，增加向下兼容逻辑
- 优化节点同步，降低分叉几率
- 提升单个区块交易处理性能
- 新增数据文件快照同步功能
- 新增性能监控和性能日志
- 修复金额解析缺陷
- 一些UI显示优化和已经缺陷修复
## v0.0.3 Changelog
- Change the magnification of PoC score to 10.
- According to the community vote, set the mining reward height to 2600.
- Fixed an issue that the mining machine's public IP is wrong be set to internal IP, to solve the sync the newest block height failed or open the auto mining failed problems. 
- Open the newest client version check and upgrade to the latest version automatically.
- Improve the validation of the bad block generator, to avoid random txs caused the PoC score calculation wrong in the forking state.
- Fixed an issue of PoC calculation wrong when the node didn't create a statement(PocNodeTypeTx)
- Account page support to filter the PoC txs.
- Network page support to display the history declared node size.
----
- 测试网络的PoC放大系数调整为10。
- 根据社区投票结果，将算力争夺开启高度调整到了2600。
- 修复矿机的公网IP被错误设置为内网IP，解决无法自动同步和开启自动挖矿。
- 开通最新的客户端检测和自动升级功能。
- 不合格出块着验证加强，避免分叉状态下的随机达到交易导致的PoC分数计算错误。
- 修复未声明节点的PoC分数计算错误。
- 账户页面支持筛选PoC交易。
- 网络页面增加历史节点声明总数
## Hotfix1 Changelog
- Optimize the PoC calculation: adjust the MW holding and hardware score
- Optimize the block synchronization, lower down the fork odds
- Optimize the hardware score calculation: lower limit value is 1T, upper limit value is 96T
- Optimize the peer map drawing: access the local client service to fetch the coordinates of node，unify the color of  node  in the map
- Optimize the PoC txs query: support the V1 and V2 version PoC tx data structure and query
- New function: default ignore blocks map, signature validation of block in the CheckSumValidator
- New function: add tips and warnings on the login page
- Fixed an issue: 0 value of the MW holding score of the miner
- Fixed an issue: remove the duplicate PoC tx in the tx query result
- Fixed an issue: null pointer exception occurred in the miner's auto mining process
- Some UI display optimization and fixed other known issues
----
- PoC计算优化：MW持有分数调整，硬件分数计算
- 优化节点同步，降低分叉几率
- 硬盘得分优化：下限为1T，上限为96T
- 节点地图渲染优化：访问本地节点服务获取经纬度，颜色统一
- PoC交易查询优化：支持V1和V2版本的PoC交易查询
- 增加默认的忽略区块集合和区块签名校验
- 登录页面增加私钥和体验帐号的风险提示
- 修复部分节点上矿工MW抵押分数为0的缺陷
- 修复重复的PoC交易显示缺陷
- 修复矿工开启自动挖矿时的空指针异常
- 一些UI显示优化和已经缺陷修复
## Hotfix2 Changelog
- Optimize the PoC calculation: adjust the MW holding and hardware score
- Force to trigger the mw-holding and hardware score re-calculation at the height 7288
- Reduce the mw-holding score of bootstrap nodes: set the upper limit value of mw-holding score
- Fixed an issue: 0 value of the MW holding score of the miner
- Some UI display optimization and fixed other known issues
----
- PoC计算优化：MW持有分数调整，硬件分数计算
- 在7329高度强制触发MW持有分数和硬盘得分重算
- 降低引导节点的MW抵押分数：设置上限并对引导节点的MW抵押得分进行限制
- 修复矿工MW抵押分数为0的缺陷
- 一些UI显示优化和已经缺陷修复
----
## COS-Client-0.0.2
- Open the public test of the MW Testnet
- Mining machine register
----
- 开启测试网络公测。
- 矿机认证和接入网络。
### COS-Client-0.0.2-Hotfix1
- Emergency fix of the wrong available balance display and unusable error 
----
- 紧急修复『有效金额』显示错误和不可用的缺陷
### COS-Client-0.0.2-Hotfix2
- Emergency fix of the myAddress parse error when value is 'undefined'
- Auto fetch db archive
----
- 紧急修复『myAddress』解析错误问题
- 新增自动获取最新的区块备份功能
---
## COS-Client-0.0.1
- Initial
- MW Client
- 初始化
- MW客户端