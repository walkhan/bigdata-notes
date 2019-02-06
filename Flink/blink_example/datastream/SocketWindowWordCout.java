package com.idms.blink.datastream;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

@SuppressWarnings("serial")
public class SocketWindowWordCout {
    public static void main(String[] args) throws  Exception{
        final String hostname ;
        final Integer port ;
        try {
            ParameterTool param = ParameterTool.fromArgs(args) ;
            hostname = param.has("hostname") ?  param.get("hostname") : "localhost" ;
            port = param.getInt("port") ;
        }catch (Exception e){
            System.err.println("No port specified. Please run 'SocketWindowWordCount " +
                    "--hostname <hostname> --port <port>', where hostname (localhost by default) " +
                    "and port is the address of the text server");
            System.err.println("To start a simple text server, run 'netcat -l <port>' and " +
                    "type the input text into the command line");
            return;
        }

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment() ;
        // get input data by connecting to the socket
        DataStream<String> text = env.socketTextStream(hostname,port,"\n") ;

        DataStream<WordWithCount> windowCounts = text.flatMap( new FlatMapFunction <String, WordWithCount>() {
            @Override
            public void flatMap(String value, Collector<WordWithCount> out) throws Exception {
                      for(String word:value.split("\\s")){
                          out.collect(new WordWithCount(word,1));
                      }
            }
        } )
                .keyBy("word")
                .timeWindow(Time.seconds(5))
                .reduce( new ReduceFunction<WordWithCount>(){
                    @Override
                    public WordWithCount reduce(WordWithCount words, WordWithCount b) throws Exception {
                        return new WordWithCount(words.word,words.count + b.count);
                    }
                } );
        windowCounts.print().setParallelism(1) ;
        env.execute("Socket Window WordCount") ;
    }

    public static class WordWithCount{
        public String word ;
        public Integer count ;
        public WordWithCount(){}
        public WordWithCount( String word,Integer count){
            this.word = word ;
            this.count = count ;
        }

        public String toString(){
            return  word + ":" + count ;
        }
    }
}
