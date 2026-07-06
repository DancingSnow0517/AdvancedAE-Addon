# AdvancedAE Addon

[English](README.md)

AdvancedAE Addon 是一个面向 Applied Energistics 2 和 Advanced AE 的 NeoForge 附属模组。

它为 Advanced AE 的量子合成器添加 AE2 合成卡支持。安装合成卡后，量子合成器在样板材料不足时可以向 ME 自动合成网络请求缺失输入，而不是直接停在缺少材料的状态。

## 功能

- 允许量子合成器安装 AE2 合成卡。
- 通过 AE2 的自动合成系统请求缺失材料。
- 支持量子合成器加速卡，安装 4 张加速卡时每 tick 最多按 64 次合成请求材料。
- 计算请求数量时遵守最小输入保留数量。
- 计算请求数量时遵守输出数量限制。

## 需求

- Minecraft 1.21.1
- NeoForge 21.1.209 或更高版本
- Applied Energistics 2 19.2.17 或更高版本
- Advanced AE 1.6.11-1.21.1 或更高版本

## 构建

```sh
./gradlew build
```
