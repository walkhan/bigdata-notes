从kafka0.9版本后，Apache kafka从新改写了producer,并发布了java版本的producer供用户使用。目前kafka也支持众多的编程语言。另外kafka producer在设计上要比consumer简单，不涉及复杂的操作，每个producer都是独立工作的。

## 一.producer工作流程
对于java版本的producer工作流程如下：
![图片](https://uploader.shimo.im/f/iO7W5OSPleQgRUb1.jpg!thumbnail)
1. 用户首先启动一个producer线程，将待发送的消息封装到ProducerRecord;
2. 启动后将待发送的消息进行序列化发送给partitioner，等到确定了目标分区后，并一同发送位于producer程序的一块内存缓冲区；
3. producer的另一个工作线程（I/O线程，即sender线程）会实时从缓冲区提取消息并封装到一个批次中；
4. 最后统一发送给broker,producer等到broker的响应请求。如果失败，则会继续重试；如果成功，则通过RecodMetadata响应producer，处理下一个批次；

以上就是kafka producer的大概流程，对于producer的开发工作流程根据Java版本提供的API遵循5个步骤，下面先简单的构造一个kafka producer实例，来说明producer的5个步骤。代码如下：
```
public class SingleThreadProductorSync {
    public static void main(String[] args) {
    //创建Properties 对象
        Properties properties = new Properties() ;
           properties.put("bootstrap.servers",
           "fdw1:9092,fdw2:9092,fdw3:9092") ;
        properties.put("acks","all") ;
        properties.put("retries","10") ;
        properties.put("batch.size","16432") ;
        properties.put("linger.ms","50") ;
        properties.put("key.serializer",
        "org.apache.kafka.common.serialization.StringSerializer") ;
        properties.put("value.serializer",
        "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("enable.idempotence",true) ;
        properties.put("client.id","ucp_kafka") ;
            String topicName = "fdw" ;
         //使用Properties 对象构造producer实例   
         Producer<String,String> producer = new KafkaProducer <String, String>(properties) ;
            Date date = new Date() ;
            AtomicInteger _sendCount = new AtomicInteger(0) ;
            for (int i = 0; i < 1000; i++) {
                try {
                   //构造待发送的消息对象ProducerRecord
                    ProducerRecord<String,String> sendMsg = new    ProducerRecord <String,String>(topicName,
                            Integer.toString(i),date.toString()) ;
                    //调用KafkaProducer 的send方法发送消息
                   RecordMetadata metadata = producer.send(sendMsg).get() ;
                    System.out.println( _sendCount.addAndGet(1) + "-" + sendMsg.key() + "," + sendMsg.value()
                            + ", "  + metadata.offset());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } finally {
                //关闭KafkaProducer
                    producer.flush();
                    producer.close();
                }
            }
    }
}
```

从这个简单的producer 代码可以看出，producer无外乎就是以上5个步骤。
## 二.kafka producer实例的5个步骤
    kafka producer实例的5个步骤内容如下：
* 创建Properties对象：在这个对象中，bootstrap.servers,key.serializer,value.serializer这3个参数是必不可少的。并且没有默认值；
* 使用Properties 对象构造producer实例 ；
* 构造待发送的消息对象ProducerRecord :指定要被消息发送到的topic,分区，key,value。注意的是key和分区可以不指定，有kafka自行确定目标分区；
* 调用KafkaProducer 的send方法发送消息:对于send的方法在发送消息时，可同步发送，也可异步发送；
* 关闭KafkaProducer；
### 2.1 .创建Properties对象
   kafka producer在启动线程后，首先要读取所有的配置连接broker，在开发中的配置如下：
```
  Properties properties = new Properties() ;
  properties.put("...","...") ;
```
详细的参数配置请参考《Kafka客户端参数配置介绍》。那接下来就是要构建Kafka producer实例。
### 2.2 .构造KafkaProducer对象
设置参数后，就开始通过Properties 对象构造KafkaProducer对象，KafkaProducer对象是producer的主入口。所有的功能基本都是有KafkaProducer对象提供。
```
Producer<String,String> producer = new KafkaProducer <String, String>(properties) ;
```
其中，Producer<String,String>中的类型根据业务情况去确定。
### 2.3 构造ProducerRecord对象
构造好KafkaProducer实例后，接下来就是构造ProducerRecord实例了。java版本的producer使用ProducerRecord类来表示每条消息
```
ProducerRecord<String,String> sendMsg = new ProducerRecord <String,String>(topicName,Integer.toString(i),date.toString()) ;
```
另外还支持更多的消息信息。可以控制该消息发送到分区及消息的时间戳。

### 2.4.发送消息
Kafka producer发送消息的主要方法是send方法，producer在底层实现了异步发送，并通过Java提供的Future实现了同步发送和异步发送。异步发送里面实现了回调（callback）。

**同步发送**
**  **同步发送回调用Future.get()无限等待结果返回，即可实现同步发送的结果。
```
producer.send(sendMsg).get() ;
```
使用Future.get()会一直等待下去直到Kafka broker将发送结果返回给producer程序。当结果从broker处返回时get方法要么返回发送结果，要么抛出异常交由producer自行处理。

**异步发送**
** ** producer写入的操作默认是异步的，java版本的producer的send方法会返回一个java Future对象供用户稍后获取发送结果，这就是所谓的回调机制。
```
producer.send( sendMsg, new Callback() {
    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
        if(e == null){
            //消息发送成功
        }else{
            //执行错误处理逻辑
        }
    }
} ) ;
```
代码中Callback就是消息发送后的回调类，实现方法是onCompletion。方法中的2个参数不会同时为非null。也就是至少有一个是null。当消息发送成功后，exception是null。如果消息发送失败，recordMetadata就是null。因此在写producer时，最好写if语句进行判断。

### 2.5.关闭producer
producer程序结束后一定要关闭producer，毕竟在运行时占用了系统资源。因此必须要显示的关闭KafkaProducer.close方法关闭producer。
```
 producer.close();
```
## 三.异常处理
不管是同步发送还是异步发送，发送都有可能失败，导致返回异常错误，当前Kafka producer的错误类型包含2类：
* 可重试异常；
* 不可重试异常；
### 3.1.可重试异常
  **  LeaderNotAvailableException**
**     **分区的Leader副本不可用，通常会出现在leader换届选举，因此是瞬间的异常，重试之后可以自动恢复；
    **NotControllerException**
**     **controller当前不可用，通常表明controll在经历新一轮的选举，也是可以通过重试机制自动恢复的；
   ** NetworkException**
**     **网络瞬时故障导致的异常，可重试；
对于以上的重试，只要在producer中配置重试次数，在规定的重试次数内自动恢复即可。
### 3.2.不可重试异常
对于不可重试异常而言，这些异常都是比较严重或者kafka无法处理的问题。
** RecordTooLargeException**
**  **发送的消息尺寸过大，超过了规定的大小上限。这种异常如何重试都是无法成功的。
**SerializationException**
序列化异常，这也是无法恢复的。
***KafkaException***
其他类型的异常。

由于重试异常和不可重试异常在producer端可能有不同的处理逻辑，因此可以使用下面的代码进行区分。
```
producer.send( sendMsg, new Callback() {
    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
        if(e == null){
            //消息发送成功
        }else{
            if(e instanceof RetriableException){
                //处理可重试异常
            } else{
                //处理不可重试异常
            }
        }
    }
} ) ;
```

综合以上的步骤，既可以创建出Kafka producer的一个完整程序，其中的业务逻辑根据情况修改即可。

## 四.无丢失消息配置
    在刚开始已经介绍了Kafka producer的工作流程，它采用的是异步发送的机制。KafkaProducer.send方法仅仅是把消息放入了一个缓冲区。并有一个专属的I/O线程负责从缓冲区提取消息封装进batch，并发送出去。这个过程显然会有丢失消息的存在，若I/O线程在发送之前producer奔溃，显然数据就全部丢失了。
    还有一个问题就是消息乱序。假如发送2条消息到相同的分区，消息是record1和record2。如果此时出现网络抖动导致record1未发送成功。如果配置了重试机制，待record1发送成功后，在日志的位置反而位于record2之后，这样就造成了消息的乱序问题
   首先数据丢失，在使用同步机制的情况下，可以避免。但由于性能上的问题，并不推荐在实际生产中使用。那对于异步发送，也给出了一个很好的建议，就是设置好producer端和broker端的参数配置即可。
  对于kafka producer端的参数介绍就不多说了，请参考《Kafka客户端参数配置介绍》。这里在重点说下broker端对于kafka producer端防止数据丢失的几个配置：
* **unclean.leader.election.enable=false**:关闭unclean.leader选举，即不允许ISR中的副本被选举为leader,从而避免broker端因日志水位截断而造成数据丢失；
* **replication.factor >= 3**:设置为3主要是参考了hadoop的备份原则，一定要使用多副本老保存分区的消息；
* **min.insync.replicas > 1**:用于控制某条消息被写入到ISR中的多少个副本才算成功，设置大于1 是为了提升producer端发送语义的持久性，不要使用默认值；
* **replication.factor > min.insync.replicas:**若相等，只要有一个副本挂掉，分区就无法正常工作。推荐的配置是**replication.factor =  min.insync.replicas + 1**

对于了消息的乱序问题，可以设置max.in.flight.requests.per.connection=1来避免，限制客户端在单个连接上能够发送的未响应请求的个数。设置此值是1表示kafka broker在响应请求之前client不能再向同一个broker发送请求,但吞吐量会下降。在kafka1.0.1中设置了幂等操作后，无需关心消息乱序的问题，因为如果设置了enable.idempotence=true就无需在设置acks=all  + max.in.flight.requests.per.connection =1。
       


