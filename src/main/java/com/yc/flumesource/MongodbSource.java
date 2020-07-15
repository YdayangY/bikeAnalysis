package com.yc.flumesource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yc.flumesource.MongodbSourceHelper;;

public class MongodbSource extends AbstractSource implements EventDrivenSource ,Configurable{
	// 打印日志
    private static final Logger LOG = LoggerFactory.getLogger(MongodbSource.class);
    
    // sqlHelper
    private static MongodbSourceHelper mongoSourceHelper;

    // 两次查询的时间间隔
    private long interval; //间隔时间
    private String charset;
    private ExecutorService executor; //线程池
	private MongoRunnable runnable;//线程任务
    
	@Override
	public void configure(Context context) {
		 // 初始化
		mongoSourceHelper = new MongodbSourceHelper(context);
        charset=context.getString("charset","UTF-8");
        interval=context.getLong("interval",1000L);
        
        System.out.println("configure++++");
	}
	
	public synchronized void start(){
		System.out.println("start ++++++++++++++");
		//创建一个单线程的线程池
		executor=Executors.newSingleThreadExecutor();
		//定义一个实现runnable接口的类,作为一个任务
		runnable=new MongoRunnable(interval,charset,getChannelProcessor());
		executor.submit(runnable);
		super.start();		
	}
	
	public synchronized void stop(){
		System.out.println("stop +++++++");
		runnable.setFlag(false);
		executor.shutdown();
		while(!executor.isTerminated()){
			LOG.debug("等待文件执行线程停止");
			try {
				executor.awaitTermination(500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				LOG.debug("等待执行线程停止时异常");
				Thread.currentThread().interrupt();
			}
		}
	}
	
	private static class MongoRunnable implements Runnable{
		private long interval;
		private String charset;
		private ChannelProcessor channelProcessor;
		private boolean flag=true;
		
//		public MongoRunnable(){
//			
//		}
		
		
		public MongoRunnable( long interval, String charset,
				ChannelProcessor channelProcessor) {
			this.interval=interval;
			this.charset=charset;
			this.channelProcessor=channelProcessor;
			
		}

		@Override
		public void run() {
			 // 存放 event 的集合
          List<Event> events = new ArrayList<>();
          	// 存放 event 头集合
          HashMap<String, String> header = new HashMap<>();
          header.put("collection", mongoSourceHelper.getCollection());
			while(flag){
				try {
					List<Object> result = mongoSourceHelper.query();
		            // 如果有返回数据，则将数据封装为 event
		            if (!result.isEmpty()) {
		                Event event = null;
		                for (Object row : result) {
		                    event = new SimpleEvent();
		                    event.setHeaders(header);
		                    event.setBody(new String(row.toString().getBytes("ISO-8859-1"),charset).getBytes());
		                    events.add(event);
							//将数据发给channel
							channelProcessor.processEventBatch(events);
		                }
		            }
		            Thread.sleep(interval);  //线程休眠时间  配合flume这两个配置使用capacity transactionCapacity   不然上游数据量太大 下游消费不及时会报错
				} catch (Exception e) {
					LOG.error("读取Mongodb数据异常",e);
				} 
			}
			
		}
		private void setFlag(boolean flag){
			this.flag=flag;
		}
	}
	

}
