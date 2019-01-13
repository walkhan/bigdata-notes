  对于Kafka的迭代更新，也使得很多配置属性交替更新，有时候可能会出现低版本的属性在高版本中根本不适用。然而在做项目的时候，也出现了很多问题，不得去查资料一点一点摸索去解决。另外大数据平台采用的是CDH5.14.4，Kafka版本是1.0.0。今天记一次双网卡在Kafka中是如何配置的。

  如果想要在brokers上配置双网卡，则必须修改listeners属性，这涉及到kafka.properties使用高级代码端，配置多网卡有两个选项：
  ###1.listener地址配置为通配符
   将listener地址配置为通配符（0.0.0.0），并使用advertised.listeners参数指定要连接到的客户端地址：

  1.从Cloudera Manager导航到kafka.properties kafka > 配置 > broker高级代码段；
  2.添加
      listeners= SASL_PLAINTEXT://0.0.0.0:9092
      advertised.listeners = SASL_PLAINTEXT://example.com:9092
  3.保存配置
  4.重新启动broker以实现更新；

注意事项：
     1.每个Kafka主机都需要单独修改高级代码段；
     2.修改listeners参数以匹配所需的协议。项目是SASL_PLAINTEXT协议；
     3.在大数据领域，尽量少用IP去做配置，尽可能的用主机名比较合适；举例：如果是在有内网和外网的情况下，如果适配了IP地址，可能造成外网无法找到beoker的情况。这种解决的办法是内网和外网都通过主机名映射，方可解决问题。


###2.多个特定IP地址配置listeners地址
  使用多个特定IP地址配置listeners地址。每个网卡需要使用不同的协议，但不能在不同的网卡上使用相同的协议。即跨2个网卡要使用相同的协议。
  1.从Cloudera Manager导航到kafka.properties kafka > 配置 > broker高级代码段；
  2.添加
    listeners= SASL_PLAINTEXT://cdh1:9092,SASL_PLAINTEXT://data1:9092
  3.保存配置
  4.重新启动代理以实现更新。
      





