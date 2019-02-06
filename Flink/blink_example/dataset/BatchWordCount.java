package com.idms.blink.dataset;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.omg.CORBA.PUBLIC_MEMBER;

public class BatchWordCount {
    public static void main(String[] args) throws Exception {
        final ParameterTool param = ParameterTool.fromArgs(args) ;
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment() ;
        env.getConfig().setGlobalJobParameters(param) ;
        //get input data
        DataSet<String> text ;
        if(param.has("input")){
            text = env.readTextFile(param.get("input")) ;
        } else {
            System.out.println("Executing WordCount example with default input data set.");
            System.out.println("Use --input to specify file input.");
            text = env.fromElements("Unless required by applicable law or agreed to in writing, software + " +
                    "distributed under the License is distributed on an AS IS BASIS") ;
        }

        DataSet<Tuple2<String,Integer>> counts = text
                .flatMap(new Tokenizer())
                .groupBy(0)
                .sum( 1) ;

        if(param.has("output")){
            counts.writeAsText(param.get("output")) ;
        } else {
            System.out.println("Printing result to stdout. Use --output to specify output path.");
            counts.print();
        }


    }
    // User-defined functions
    public static class Tokenizer implements FlatMapFunction<String,Tuple2<String,Integer>> {
        @Override
        public void flatMap(String value, Collector<Tuple2 <String, Integer>> out) throws Exception {
           String [] token = value.toLowerCase().split("\\W+");
            for(String tokens:token ){
                if(tokens.length() > 0){
                    out.collect(new Tuple2 <>(tokens,1));
                }
            }
        }
    }
}
