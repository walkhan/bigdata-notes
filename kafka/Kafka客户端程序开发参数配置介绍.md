在kafka程序代码中，为了生产和消费数据，需要在程序中配置相应的参数。在配置参数中，又分为生产者配置参数和消费者配置参数，下面介绍的参数主要是以Kafka1.x版本为主。首先先介绍生产者的相关参数配置。
## 一.生产者
运行完整的一个Kafka producer实例，首先需要定义Properties对象，没有这个对象，Kafka是无法完成整个运行流程的。下面介绍Kafka producer程序中一些重要的参数。
### 1.bootstrap.servers
该参数指定了一对host:port 用于创建向Kafka Broker服务器的连接。这个参数是必须要指定的，如果Kafka集群中机器较多，那至少需要指定一个。
说明：该参数指定多台只是故障转移使用，如果其中一台挂了，Producer重启后依然可以通过其他broker连接kafka集群。
### 2.key.serializer
被发送到 broker端的任何消息格式都是都必须是字节数组，因此消息必须先做序列化，然后才发送到 broker 该参数就是消息的key做序列化用的。kafka中默认的序列化器是通过org.apache.kafka.common.serialization.Serializer 
接口类实现的，Kafka也提供了十几种现成的序列化器，提供给开发者使用。这个参数也是必须的。
### 3.value.serializer
与 key.serializer类似，只是它用来对消息体的value做部分序列化，将value转换为字节数组

说明：以上的2个序列化器参数必须是全限额类名，使用单独的类名是不可行的做法。
### 4.acks
acks 参数用来控制Producer生产消息的持久性，对于Producer而言，Kafka有3种方案在给producer发送响应前，leader broker必须要确保已经成功写入该消息的副本数，当前有3个取值：0，1和all。
* acks = 0:设置成producer完全不用管leader broker端的处理结果。此时，producer发送消息后立即开启下一条消息的发送，不会等待leader broker端返回结果。这种情况下，producer.send的回调就失去了作用，用户是无法通过回调机制感知任何发送过程种的失败。所以acks=0时producer并不保证消息会被成功发送。但这种设置下producr的吞吐量是最高的。
* acks=all or -1:表示当前发送消息时，leader broker不仅会将消息写入本地日志，同时还会等待ISR种所有其他副本都成功写入各自的本地日志，才会响应结果给producer.当设置acks=all时，只要ISR中，有一个副本处于“alive"状态。那这条消息肯定就不会丢失。这种情况下，producr的吞吐量也是最低的。
* acks=1:这种设置是一种折中方案，也是默认的参数值。producer发送消息后，leader broker仅将该消息写入本地日志，然后又便发送响应结果给producer，无须等待ISR中其他副本写入该消息，只要该broker一直存活，kafka就能保证数据不丢失。

     acks参数取值说明
| acks   | 吞吐量   | 消息持久性   | 使用场景   | 
|:----|:----|:----|:----|
| 0   | 最高   | 最差   | 不关心消息是否发送成功，允许消息丢失   | 
| 1   | 适中   | 适中   | 一般场景   | 
| all   | 最差   | 最高   | 不能容忍消息丢失   | 


### 5.buffer.memery
该参数指定了producer端用于缓存消息的缓冲区大小，单位是字节，默认是33554432，即32M。由于Kafka采用了异步发送的架构设计，在java版本的producer中启动会先创建一块内存缓冲区用于保存待发送的消息，然后会有一个专有线程负责从缓冲区读取消息。这部分内存空间是有bufffer.memery参数指定。
### 6.compreseion.type
该参数指定producer是否要压缩，默认是none。压缩的原因是在producer端可以显著的降低I/O传输开销从而提升吞吐量。目前在kafka1.0.1支持的压缩有3种，分别是GZIP，Snappy和LZ4。在后续的版本支持Zstandard。比上面3种压缩率有所提升。
### 7.retries
broker在处理写入请求时，可能因为瞬间的故障（leader选举或者网络抖动）导致消息发送失败。这种故障通常是可以自动恢复的。当前kafka prducer内部自动实现了重试。即可以在producer种设置retries参数。retries参数默认是0.推荐的一个是值是要大于0。但在重试的情况下，要考虑到几点：
  * 重试可能会造成消息的重复发送：根据版本的选择，在Kafka0.11后，实现了producer的幂等性，保证了producer的精准一次语义；
  * 重试可能造成消息的乱序：当前producer会将消息默认发送请求是5个。如果在重试的情况下，可能就会造成消息的乱序。为避免乱序，Kafka producer java版本提供了max.in.flight.requests.per.connection参数。如果此参数设置为1，producer确保某一时刻只发送一个请求；
### 8.batch.size
该参数是衡量producer吞吐量和延时性能的一个重要指标。理论上是当batch满了，producer才会发送batch中的所有消息。但实际情况下，batch还有很大空闲空间时，producer就发送该batch了。如果batch的消息量很小，那producer的吞吐量就很低；倘若batch巨大，给内存带来压力，producer会batch分配固定大小的内存。此参数默认是16384，即16kb。合理的增加该值，producer的吞吐量也会响应的增加。
### 9.linger.ms
该参数是为控制消息延迟行为的，默认是0，表示立马发送。但这样虽然合理，但会极大的降低producer的吞吐量。如果在batch中的消息越多，producer的吞吐量也会提升，故增加linger.ms的时间会相应的增加producer的吞吐量。
### 10.max.request.size
该参数控制的是producer端能够发送的最大消息大小。如果要发送消息很大的消息，此参数是需要被设置的。默认是1048576字节；
### 11.request.timeout.ms
当producer端发送请求给broker后，broker需要在规定的时间范围内将处理结果返回给producer,默认是30s。如果在30s内，没有给producer发送响应，就说明请求超时，并在回调函数中抛出TimeoutException异常。如果producer负荷较大，可适当的调整此参数。
### 12.enable.idempotence
幂等producer：保证发送单个分区的消息只会发送一次，不会出现重复消息；启用幂等producer：在producer程序中设置属性enable.idempotence=true，但不要设置transactional.id。注意是不要设置，而不是设置成空字符串或"null"。所谓幂等producer指producer.send的逻辑是幂等的，即发送相同的Kafka消息，broker端不会重复写入消息。同一条消息Kafka保证底层日志中只会持久化一次，既不会丢失也不会重复。幂等性可以极大地减轻下游consumer系统实现消息去重的工作负担，因此是非常实用的功能。值得注意的是，幂等producer提供的语义保证是有条件的：
* 单分区幂等性：幂等producer无法实现多分区上的幂等性。如前所述，若要实现多分区上的原子性，需要引入事务
* 单会话幂等性：幂等producer无法跨会话实现幂等性。即使同一个producer宕机并重启也无法保证消息的EOS语义

对于事务操作，官网说是可以。kafka1.0.1版本，能不能真正处理，就不好说了。没有做尝试。

由于Kafka0.11后引入了幂等性操作，如果设置了enable.idempotence=true就无需在设置acks=all  + max.inflight.requests.per.connection =1 两者选一即可。
## 二.消费者
当然运行完整的一个Kafka consumer实例，首先需要定义Properties对象，没有这个对象，Kafka是无法完成整个运行流程的。下面介绍Kafka consumer程序中一些重要的参数。
### 1.bootstrap.servers
   该参数指定了一对host:port 用于创建向Kafka Broker服务器的连接。这个参数是必须要指定的，如果Kafka集群中机器较多，那至少需要指定一个。此参数和producer端类似。
### 2.key.deserializer
   consumer从broker获取的消息都是字节数组的格式，因此消息需要通过反序列化还原为原来的对象格式。该参数必须设置org.apache.kafka.common.serialization.Deserializer 接口类实现的。consumer也支持用户自定义的Deserializer 。consumer是否指定了key，都必须指定此参数，不然程序会抛出ConfigException异常。
### 3.value.deserializer
与key.deserializer类似，此参数用来对消息体进行解序列化，从而把消息还原为原来的对象类型。当然key.deserializer与value.deserializer设置成不同的值。

说明：以上的2个解序列化器参数必须是全限额类名，使用单独的类名是不可行的做法。
### 4.group.id
该参数指定consumer group的名字，能够标识一个consumer group 。在程序开发中，要显示的设置一个group.id。不然会抛出InvalidGroupIdException异常。
### 5.client.id
该参数指定消费者组的客户端名称，与group.id名称一致即可
### 6.session.timeout.ms
此参数是用来检测consumer group内成员发送的奔溃时间。如果设置了5分钟，当组成员突然奔溃了，组协调器可能5分钟后才会感知到。还有另外一个含义是消息处理逻辑的最大时间，如果两次poll之间的间隔超过了该参数的阈值，那么coordinator就认为consumer已经追不上组内其他成员的消费进度，因此consumer实例就会被剔除组。但在社区0.10.1版本后，此参数明确为coordinator检测失败的时间。默认是10s。
### 7.max.poll.interval.ms
在0.10.1消息处理逻辑的最大时间已经被剥离出来，此参数就是控制消息处理逻辑的最大时间。在业务场景中，如果用户设置max.poll.interval.ms稍微大于2分钟，那么session.timeout.ms也没有必要设置这么大的值。
### 8.auto.offset.reset
指定无位移信息或者位移越界。但kafka只能满足这2个中的一个才生效。目前该参数有如下3个可能的取值：
* earliest:指定从最早的位移开始消费，最早的位移不一定是0；
* latest:指定从最新位移开始消费；
* none:指定如果为发现位移信息或者位移越界，则抛出异常。这种场景很少用。
### 9.enable.auto.commit
该参数指定consumer是否自动提交位移。若设置true，consumer会自动提交位移；如果用户手动提交，说明需要手动管理位移；对于”Excatly Once“语义的用户来说，最好该参数设置为false。
### 10.fetch.max.bytes
指定consumer端单次获取数据的最大字节数，这个参数也是不能忽视的。
### 11.max.poll.records
该参数控制单次poll调用返回的最大消息数，默认是500；
### 12.heartbeat.interval.ms
当consumer group的其他成员得知要开启新一轮rebalance，它会将这个决定以REBANCE_IN_PROGRESS异常的形式塞进consumer心跳请求的response中，这样其他成员拿到response后才能知道它要加入group。显然越快越好。该值必须小于session.timeout.ms。如果consumer在session.timeout.ms这段时间不发送心跳，coorfinator就认为它已经dead,也就没有必要让他知道coordinator的决定了。
### 13.connection.max.idle.ms
此参数决定了那些空闲资源是不是要关闭，如果不在乎Socket资源开销，推荐参数设置为-1，默认是9分钟。

## 三.Kerberos认证
在生产环境中，如果用户需要添加Kerberos认证，在producer和consumer程序中，都需要添加几个参数进去，Kerberos认证分为用户直接登陆和keytab文件认证，在程序中一般通过keytab文件认证即可。
###  1.java.security.auth.login.config
    此参数是为了认证kafka客户端kerberos生成的配置文件；
### 2.java.security.krb5.conf
  此参数是读取Kerberos启动后的krb5.conf文件；
### 3.javax.security.auth.useSubjectCredsOnly
此参数可设置为false
### 4.security.protocol
  此参数为kafka kerberos认证的外部协议；
### 5.sasl.kerberos.service.name
  此参数为Kafka的服务名称；

producer和consumer程序中所有的Properties对象开发完成后，就可以通过Properties实例构造producer和consumer对象了。
 
















    
          
              

