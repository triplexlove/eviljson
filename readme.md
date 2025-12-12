# fastjson 模糊测试

## 测试
- fastjson 1.2.83
- jackson-databind 2.17.2
```
mvn -q -DskipTests exec:java -Dexec.mainClass=com.example.JacksonFuzz 
```

## 使用方法

1. 编译项目：`mvn clean install`
2. 运行模糊测试：`mvn -q -DskipTests exec:java`
3. 运行模糊测试并指定输入文件：`mvn -q -DskipTests exec:java -Dexec.args=@/tmp/sample.json`
