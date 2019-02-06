
# 1. Flink standalone cluster just execute
本地模式
```
./bin/start-scala-shell.sh local
```
在scala shell中，停止./bin/stop-cluster.sh。只需要启动上面命令即可。查看帮助：./bin/start-scala-shell.sh --help
## 1.2.SQL Query Example
```
 Scala> val data = Seq(
    ("US", "Red", 10),
    ("UK", "Blue", 20),
    ("CN", "Yellow", 30),
    ("US", "Blue",40),
    ("UK","Red", 50),
    ("CN", "Red",60),
    ("US", "Yellow", 70),
    ("UK", "Yellow", 80),
    ("CN", "Blue", 90),
    ("US", "Blue", 100)
  )
Scala> val batchTable = btenv.fromCollection(data,'country,'color,'cnt)
Scala> btenv.registerTable("MyTable",batchTable)
Scala> val result = btenv.sqlQuery("SELECT * FROM MyTable WHERE cnt < 50").collect
```
![图片](https://uploader.shimo.im/f/HXAZy6qHhYQ3kHxP.jpg!thumbnail)
## 1.3.DataStream Example
```
Scala> val textStreaming = senv.fromElements(
  "To be, or not to be,--that is the question:--",
  "Whether 'tis nobler in the mind to suffer",
  "The slings and arrows of outrageous fortune",
  "Or to take arms against a sea of troubles,")
Scala> val countsStreaming = textStreaming.flatMap { _.toLowerCase.split("\\W+") }.map { (_, 1) }.keyBy(0).sum(1)
Scala> countsStreaming.print()
Scala> senv.execute("Streaming Wordcount")
```
![图片](https://uploader.shimo.im/f/M13vMWDQ8UMAuCV7.jpg!thumbnail)

## 1.4.DataSet Example
```
Scala> val text = benv.fromElements(
  "To be, or not to be,--that is the question:--",
  "Whether 'tis nobler in the mind to suffer",
  "The slings and arrows of outrageous fortune",
  "Or to take arms against a sea of troubles,")
Scala> val counts = text.flatMap { _.toLowerCase.split("\\W+") }.map { (_, 1) }.groupBy(0).sum(1)
Scala> counts.print()
```
![图片](https://uploader.shimo.im/f/PDTIphs63jMThYdG.jpg!thumbnail)
## 1.5.Table API Example
```
Scala> val data = Seq(
    ("US", "Red", 10),
    ("UK", "Blue", 20),
    ("CN", "Yellow", 30),
    ("US", "Blue",40),
    ("UK","Red", 50),
    ("CN", "Red",60),
    ("US", "Yellow", 70),
    ("UK", "Yellow", 80),
    ("CN", "Blue", 90),
    ("US", "Blue", 100)
  )

Scala> val t = btenv.fromCollection(data).as ('country, 'color, 'amount)
Scala> val t1 = t.filter('amount < 100)
Scala> t1.cache
Scala> // t1 is cached after it is generated for the first time.
Scala> val x = t1.print

Scala> // When t1 is used again to generate t2, it may not be regenerated.
Scala> val t2 = t1.groupBy('country).select('country, 'amount.sum as 'sum)
Scala> val res2 = t2.print

Scala> // Similarly when t1 is used again to generate t2, it may not be regenerated.
Scala> val t3 = t1.groupBy('color).select('color, 'amount.avg as 'avg)
Scala> val res3 = t3.print
```
![图片](https://uploader.shimo.im/f/KbzfHwJiYJ8VOkvV.jpg!thumbnail)
![图片](https://uploader.shimo.im/f/WurmQUyrw8UKrqul.jpg!thumbnail)
![图片](https://uploader.shimo.im/f/1dtJ5YmK66wCxQDe.jpg!thumbnail)
# 2.yarn模式
```
 ./bin/start-scala-shell.sh yarn -n 1 -s 2 -tm 4096 -nm yarnsession
```
yarn模式启动后，按照上面的example执行即可。注意，不需要启动启动集群。
