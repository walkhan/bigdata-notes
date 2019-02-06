package com.idms.blink.datastream;


import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.common.functions.FlatMapFunction ;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

public class StreamingWordCount {
    public static void main(String[] args) throws Exception {
        //check input parameters
        final ParameterTool param = ParameterTool.fromArgs(args) ;
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment() ;
        env.getConfig().setGlobalJobParameters(param);
        //get input data
        DataStream<String> text ;
        if(param.has("input")){
           text =  env.readTextFile(param.get("input")) ;
        } else {
            System.out.println("Executing WordCount example with default input data set.");
            System.out.println("Use --input to specify file input.");
            text = env.fromElements("Unless required by applicable law or agreed to in writing, software" +
                    "distributed under the License is distributed on an AS IS BASIS" ) ;
        }

        DataStream<Tuple2<String,Integer>> counts =  text.flatMap(new Tokenizer())
                .keyBy(0)
                .sum(1) ;

        if(param.has("output")){
            counts.writeAsText(param.get("output")) ;
        } else {
            System.out.println("Printing result to stdout. Use --output to specify output path.");
            counts.print() ;
        }
        env.execute("Streaming WordCount") ;
    }

    /**
    USER FUNCTIONS
    */

    public static final    class Tokenizer implements FlatMapFunction<String,Tuple2<String,Integer>>{
        public void flatMap(String value, Collector<Tuple2<String,Integer>> out){
            String [] token = value.toLowerCase().split("\\W+") ;
            for(String tokens:token){
                if(tokens.length()> 0){
                    out.collect(new Tuple2 <>(tokens,1));
                }
            }
        }
    }
}
