```java
/**
 * author: shuike
 * email: shuike007@126.com
 * version: 1.0.0
 * describe: 安卓多国语言追加工具
 */
```

# 使用方法
<h2 style="color:red">该工具使用时务必开新分支进行操作!!!!!</h2>

> Java
```shell
java -jar strings_tool.jar 项目路径 语言追加数据文件路径[json文件]
```

> Kotlin
```shell
kotlin strings_tool.jar 项目路径 语言追加数据文件路径[json文件]
```

## 效果
运行前工具自行检测当前分支，在当前分支为`main`、`master`时自动停止运行工具。

追加翻译内容目标语言的`strings.xml`文件或目录不存在时新建该语言的`values`文件夹与`strings.xml`文件后插入翻译结果。

追加翻译内容目标语言文件存在时如果存在该`key`的条目则更新`value`，否则在文件末尾追加翻译结果内容。
