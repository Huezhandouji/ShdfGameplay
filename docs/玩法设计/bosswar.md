# BOSSWAR
## 配置文件格式
```yaml
match_mode: "bosswar"
map_path: "maps/example_world"
display_name: "首领模式-示例地图"
description: "首领战-示例地图。击杀敌人的首领到指定次数来取得胜利"
icon: "STONE" #图标
metadata:

  spawns: #双方出生点区域配置
    red_spawn_region: #红队出生点
      type: "cube"
      form: "corners"
      corner1: [10, 10, 10]
      corner2: [15, 10, 15]
    blue_spawn_region: #蓝队出生点
      type: "cube"
      form: "origin_and_size"
      origin: [20, 10, 20]
      size: [5, 5, 5]

      
  boss_life_number: 5 #被选中当首领的那个玩家被杀几次所在队伍输掉...
  time_limit: 12000 #游戏倒计时，时间限制（tick）

  respawn_delay: 100 #重生需要的倒计时（tick）
  immune_time: 100 #重生后的无敌时间
  
  allows_duplicated_roles: true #是否允许重复角色

  available_roles:
    team_red:
      - "sinThorn"
      - "mikomako"
      #....
    team_blue:
      - "sinThorn"
      - "mikomako"
      #....
      
  play_region: #限定的游玩区域
    region:
      type: "sphere"
      centre: [0, 0, 0]
      radius: 100
```
## 游戏玩法
bosswar获胜的目标是击杀足够次数的敌方首领
### 流程
#### 选队mode
在大厅决定了本场对局是bosswar并创建了Session后，所有玩家先被传送至一个准备大厅
首先，所有玩家使用菜单选择红队或者蓝队。有一个倒计时。在倒计时结束后，未选队的玩家被随机分配至双方队伍。
然后，流转至下一个mode
#### 选角mode
该mode沿用选队mode的准备大厅地图（REUSE_DIRTY）。所有玩家不被传送。
玩家使用菜单选定自己本局使用的角色。各队可以选择的角色依据available_roles配置字段。
根据allow_duplicated_roles配置决定是否多个玩家可以选择同一个角色。如果这个配置为false，那么某个角色被一名玩家选择后，后续玩家不可以通过菜单再选择这个角色。如果在不允许重复角色的配置下，如果在本阶段开始时检测到某一队玩家数量大于可用角色数量，那么本局将允许重复角色。
有一个倒计时，结束后流转到下个mode。在倒计时结束后，还没有选角的玩家被随机分配一个可用角色。
#### 倒计时mode
该mode沿用选队mode的准备大厅地图（REUSE_DIRTY）。所有玩家不被传送。
在5秒的倒计时后，为双方随机选择一名玩家作为首领，在聊天框公布。
然后，在10秒的倒计时后，流转到下一个mode
#### 游戏mode
这个mode开启时，让hydrokarbs根据本局游戏配置的地图配置项来创建一个游戏地图。将所有玩家依据队伍出生点传送至对应区域。
除首领外的其他玩家有无限的重生次数。重生有一段倒计时间隔。玩家重生后将有一小段无敌时间。
首领将有一些生命值，sante，能量和生命回复的增益。
游戏结束条件：
  一队的首领重生次数耗尽。这队将输掉游戏，另一队获胜。
  游戏倒计时（时间限制）结束，这时首领重生次数剩余更多的一队获胜。
  游戏倒计时结束，双方首领剩余重生次数相等。平局。
游戏结束后，流转至下一个mode
#### 结算mode
将所有玩家传送回选队mode的准备大厅地图（REUSE_DIRTY）。
展示结算信息。
有一个倒计时。倒计时结束后，将所有玩家传回大厅。然后解散这个Session

#### 断线重连规则
这个玩法一旦玩家进入了session，在任何时候允许其断线重连。
如果是在选角mode后重连，没有角色，那么为其随机分配一个可用的角色。