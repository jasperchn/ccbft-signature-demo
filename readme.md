signature-core是签名计算的核心逻辑，仅使用了apache.common包，它尽可能与开发框架解耦，只体现签名计算本身的逻辑
  
signature-springboot是用spring boot webflux实现验签名的一个例子；
  
由于signature-core的核心思想是与框架去耦合，故signature-springboot中对签名的计算效率还有明显地提升空间（有很多不必要的数据结构转换），如果对性能有要求建议根据signature-core针对开发框架自行实现相关逻辑  
signature-springboot的签名实现方式也并不合适，WebClient应该有拦截器/修饰器对request进行处理，应该可以减轻签名逻辑与业务代码的耦合。以后可能会更新，如果有时间的话。  
    
目前测试接口需要登录，只需要用phjr体系测试环境（sit）的任何用户以fi/op渠道登录即可
  

