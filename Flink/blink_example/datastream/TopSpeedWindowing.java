package com.idms.blink.datastream;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.delta.DeltaFunction;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.evictors.TimeEvictor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.DeltaTrigger;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TopSpeedWindowing {
    public static void main(String[] args) throws Exception {
        final ParameterTool param = ParameterTool.fromArgs(args) ;
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment() ;
        env.getConfig().setGlobalJobParameters(param);
        env.setStreamTimeCharacteristic( TimeCharacteristic.EventTime);
        DataStream<Tuple4<Integer,Integer,Double,Long>> carData ;
        if(param.has("input")){
            carData = env.readTextFile(param.get("input")).map(new CarSource.ParseCarData()) ;
        } else {
            System.out.println("Executing TopSpeedWindowing example with default input data set.");
            System.out.println("Use --input to specify file input.");
            carData = env.addSource(CarSource.create(2)) ;
        }
        int evictionSec = 10;
        double triggerMeters = 50;

        DataStream<Tuple4<Integer,Integer,Double,Long>> topSpeeds = carData
                .assignTimestampsAndWatermarks(new CarSource.CarTimeStamp())
                .keyBy(0)
                .window( GlobalWindows.create())
                .evictor( TimeEvictor.of( Time.of(evictionSec,TimeUnit.SECONDS)))
                .trigger( DeltaTrigger.of(triggerMeters,
                        new DeltaFunction<Tuple4<Integer,Integer,Double,Long>>(){
                            private static final long serialVersionUID = 1L;
                            @Override
                            public double getDelta(Tuple4 <Integer, Integer, Double, Long> oldDataPoint,
                                                   Tuple4 <Integer, Integer, Double, Long> newDataPoint) {
                                return newDataPoint.f2 - oldDataPoint.f2 ;
                            }
                        },carData.getType().createSerializer(env.getConfig())))
                .maxBy(1) ;

        if(param.has("output")){
            topSpeeds.writeAsText(param.get("output")) ;
        } else {
            System.out.println("Printing result to stdout. Use --output to specify output path.");
            topSpeeds.print() ;
        }
        env.execute("CarTopSpeedWindowingExample") ;
    }

    // USER FUNCTIONS
    public static class CarSource implements SourceFunction<Tuple4<Integer,Integer,Double,Long>>{
         private static final long serialVersionUID = 1L ;
         private Integer [] speeds ;
         private Double [] distances ;
         private Random random = new Random() ;
         private volatile  boolean isRunning = true ;

         public CarSource(Integer numOfCars){
             speeds = new Integer[numOfCars] ;
             distances = new Double[numOfCars] ;
             Arrays.fill(speeds,50);
             Arrays.fill(distances,0d);
         }

         public static CarSource create(Integer cars){
             return new CarSource(cars) ;
         }

        @Override
        public void run(SourceContext <Tuple4 <Integer, Integer, Double, Long>> txt) throws Exception {
                while (isRunning){
                    Thread.currentThread().sleep( 100);
                    for (int i = 0; i < speeds.length ; i++) {
                        if(random.nextBoolean()){
                            speeds[i] = Math.min(100,speeds[i] + 5) ;
                        } else {
                            speeds[i] = Math.min(100,speeds[i] - 5) ;
                        }
                        distances[i] += speeds[i] /3.6d ;
                        Tuple4<Integer, Integer, Double, Long> record = new Tuple4 <>(i,
                                speeds[i],distances[i],System.currentTimeMillis()) ;
                        txt.collect(record) ;
                    }
                }
        }

        @Override
        public void cancel() {
             isRunning = false ;
        }

        private static class ParseCarData extends RichMapFunction <String, Tuple4 <Integer, Integer, Double, Long>> {

            @Override
            public Tuple4 <Integer, Integer, Double, Long> map(String records) throws Exception {
                String rawData = records.substring(1,records.length() - 1) ;
                String [] data = rawData.split(",") ;
                return new Tuple4 <>(Integer.valueOf(data[0]),Integer.valueOf(data[1]),
                        Double.valueOf(data[2]),Long.valueOf(data[3]));
            }
        }

        private static class CarTimeStamp extends AscendingTimestampExtractor<Tuple4<Integer, Integer, Double, Long>>{
             private static final long serialVersionUID = 1L ;
            @Override
            public long extractAscendingTimestamp(Tuple4 <Integer, Integer, Double, Long> elements) {
                return elements.f3;
            }
        }
    }


}
