signature-core是签名计算的核心逻辑，仅使用了apache.commons包，它尽可能与开发框架解耦，只体现签名计算本身的逻辑
```java
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
```
commons实际上只用一个工具函数和随机字符串函数，自行实行也是可以的，这样就可以少引入一个包  

signature-springboot是用spring boot webflux实现验签名的一个例子。它引用了signature-core包：
```xml
        <dependency>
            <groupId>com.ccbft</groupId>
            <artifactId>signature-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```
因此运行spring boot工程之前需要编译core并在本地仓库安装;  
**注意：我这样写只是为了把signature-core和signature-springboot彻底隔离开，避免在core中无意引入spring boot的方法**     
**最佳实践是将core的源码迁移到自己工程中直接使用！signature-core是代码示例，不是发布包，core中api的稳定性不作保证**  
  
由于signature-core旨在与框架去耦合，故signature-springboot中对签名的计算效率并不是很高（存在不必要的数据结构转换），如果对性能有要求可以根据signature-core的逻辑针对开发框架自行实现相关逻辑  
signature-springboot的签名实现方式也并不合适，签名计算和业务逻辑混在一起
```java
        /**
         * 计算签名
         * */
        String timestamp = SignatureUtils.generateTimestamp(Config.TIMESTAMP_BIAS);
        String nonce = SignatureUtils.generateNonce(Config.NONCE_LENGTH);
        String signature = SignatureUtils.buildSignature(
                HttpMethod.GET.name(),
                uriMaps.getRoute(),
                "",
                "",
                timestamp,
                nonce,
                Config.appKey,
                Config.appSecret,
                Collections.emptyMap(),
                queries
        );
        log.info("signature = {}", signature);

        /**
         * 放置Header
         * */
        Consumer<HttpHeaders> headersConsumer = httpHeaders -> {
            httpHeaders.add(auths[0], auths[1]);
            httpHeaders.add(HttpHeader.HTTP_HEADER_NONCE, nonce);
            httpHeaders.add(HttpHeader.HTTP_HEADER_APPKEY, Config.appKey);
            httpHeaders.add(HttpHeader.HTTP_HEADER_TIMESTAMP, timestamp);
            httpHeaders.add(HttpHeader.HTTP_HEADER_SIGNATURE, signature);
        };

        UriBuilderFactory factory = new DefaultUriBuilderFactory();
        URI uri = factory.builder().host(host).path(uriMaps.getRoute()).queryParams(convertMultiple(queries)).build().normalize();
        return WebClient
                .create(uri.getAuthority())
      
```
WebClient应该有拦截器/修饰器对request进行处理，将签名实现移动到修饰器里应该可以减轻签名逻辑与业务代码的耦合。  
以后可能会更新，如果有时间的话。  
    
目前测试接口需要登录，只需要用phjr体系测试环境（sit）的任何用户以fi/op渠道登录即可  
可以用我以前做的postman请求（内建js脚本）登录：  
```
POST {{host}}/sts/publicrs/oauth/login/{{loginRole}}?
```
获得返回值：  
```json
{
    "ret": 0,
    "id": "b10465e7de105f69",
    "msg": "操作成功",
    "data": "b8e181ce-291c-4a49-b1a8-4d202696c55d"
}
```
其中data即为短token

