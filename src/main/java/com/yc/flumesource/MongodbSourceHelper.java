package com.yc.flumesource;

import org.apache.flume.Context;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

import java.util.ArrayList;
import java.util.List;

public class MongodbSourceHelper {
	private static final Logger LOG = LoggerFactory.getLogger(MongodbSourceHelper.class);

    // 开始 id
    private static String startFrom;
    // 用户传入的查询的列
    public static String column;
    
    private static String dbUrl,database,collection;
    private static MongoCollection<Document> conn=null;
    
    
    // 构造方法
    MongodbSourceHelper(Context context) {
        // 有默认值参数：获取 flume 任务配置文件中的参数，读不到的采用默认值
        startFrom = context.getString("start.from");

        // 无默认值参数：获取 flume 任务配置文件中的参数
        dbUrl =context.getString("Mongodb.url");//"192.168.6.200:23000,192.168.6.201:23000,192.168.6.202:23000";
        database=context.getString("Mongodb.database");//"mybike";
        collection=context.getString("Mongodb.collection");//指定查询的collectiom
        column=context.getString("Mongodb.column");			//指定用哪个列匹配查询开始的地方
       
        conn = getConnection();
    }
    
//    MongodbSourceHelper() {  //测试用
//        // 有默认值参数：获取 flume 任务配置文件中的参数，读不到的采用默认值
////        this.startFrom = context.getString("start.from", DEFAULT_START_VALUE);
////        this.columnsToSelect = context.getString("columns.to.select", DEFAULT_Columns_To_Select);
//    	startFrom="0";
//        // 无默认值参数：获取 flume 任务配置文件中的参数
//        dbUrl = "192.168.6.200:23000,192.168.6.201:23000,192.168.6.202:23000";//context.getString("Mongodb.url");
//        database="mybike";//context.getString("Mongodb.database");
//        collection="ridelogs";//context.getString("Mongodb.table");
//        column="_id";
//        conn = getConnection();
//    }
    
    // 获取 Mongodb 连接
    private static MongoCollection<Document> getConnection() {
        List<ServerAddress> list=new ArrayList<ServerAddress>();
		String[] dbUrls=dbUrl.split(",");
		for(String url:dbUrls){
			list.add(new ServerAddress(url.split(":")[0],Integer.parseInt(url.split(":")[1])));  //分割Mongodb字符串
		}
		
		try {
			return new MongoClient(   list ).getDatabase(database).getCollection(collection);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }

   

    // 构建 sql 语句，以 id 作为 offset
    List<Object> query() {
    	List<Object> row =new ArrayList<>();
    	FindIterable fideiterable=null;
    	if(startFrom.equals("0") || startFrom=="0"){  //statFrom默认值为0
    		fideiterable=conn.find();
    	}else{
    		//指定查询过滤器
    		Bson filter = Filters.gt(column, new ObjectId(startFrom));
    	    fideiterable=conn.find(filter);
    	}
    	
    	MongoCursor cursor =fideiterable.iterator();
    	int index=0,logindex=0;
    	while (cursor.hasNext()) {
    		row.add(cursor.next());
    		System.out.println(row.get(index++).toString());
    		LOG.debug(row.get(logindex++).toString());
    		//System.out.println(cursor.next());
        }
    	if(row.size()!=0 ||!row.isEmpty()){  //不为空就给size值不然会报错
    		String tmpstr=row.get(row.size()-1).toString();
        	tmpstr=tmpstr.split(",")[0];
        	startFrom=tmpstr.split("=")[1]; //获取下一次开始的值
        	System.out.println(startFrom);
        	LOG.debug("resultSize:" + row.size()+" startFrom:"+startFrom);  
    	}
	try {
			cursor.close();  //关闭资源
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return row;
    }

    public String getCollection() {
        return collection;
    }
//    public static void main(String[] args) {
//		new MongodbSourceHelper().query();
//		}
}
