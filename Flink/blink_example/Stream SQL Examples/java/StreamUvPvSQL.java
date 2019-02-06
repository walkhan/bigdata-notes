package com.idms.blink;


import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;


import java.sql.Timestamp;

public class StreamUvPvSQL {
    public static void main(String[] args) throws  Exception{
        // set up the execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tEnv = TableEnvironment.getTableEnvironment(env);

        DataStream<Order> order = env.fromElements(
                new Order(Timestamp.valueOf("2018-10-15 09:01:20"), 2, 1, 7),
                new Order(Timestamp.valueOf("2018-10-15 09:05:02"), 3, 2, 9),
                new Order(Timestamp.valueOf("2018-10-15 09:05:02"), 1, 3, 9),
                new Order(Timestamp.valueOf("2018-10-15 10:07:22"), 1, 4, 9),
                new Order(Timestamp.valueOf("2018-10-15 10:55:01"), 5, 5, 8));
        DataStream<Shipment> shipment = env.fromElements(
                new Shipment(Timestamp.valueOf("2018-10-15 09:11:00"), 3),
                new Shipment(Timestamp.valueOf("2018-10-15 10:01:21"), 1),
                new Shipment(Timestamp.valueOf("2018-10-15 11:31:10"), 5));


        // register the DataStreams under the name "t_order" and "t_shipment"
         tEnv.registerDataStream("t_order", order, "createTime, unit, orderId, productId");
         tEnv.registerDataStream("t_shipment", shipment, "createTime, orderId");

        // run a SQL to get orders whose ship date are within one hour of the order date
        Table table = tEnv.sqlQuery(
                "SELECT o.createTime, o.productId, o.orderId, s.createTime AS shipTime" +
                        " FROM t_order AS o" +
                        " JOIN t_shipment AS s" +
                        "  ON o.orderId = s.orderId" +
                        "  AND s.createTime BETWEEN o.createTime AND o.createTime + INTERVAL '1' HOUR");

        DataStream<Row> resultDataStream = tEnv.toAppendStream(table,Row.class);
        resultDataStream.print();
        // execute program
        env.execute();

    }
    public static class Order{
        public Timestamp createTime;
        public int unit ;
        public int orderId;
        public int productId ;
        public Order(){}
        public Order(Timestamp createTime, int unit, int orderId, int productId){
                this.createTime =createTime;
                this.unit = unit ;
                this.orderId = orderId ;
                this.productId = productId;
        }

        public String toString(){
            return "Order " + createTime + " " + unit + " " + orderId + " " + productId ;
        }
    }

    public static class Shipment{
        public Timestamp createTime ;
        public int orderId;
        public Shipment(){}
        public Shipment(Timestamp createTime, int orderId){
            this.createTime =createTime ;
            this.orderId = orderId;
        }
    }
}


