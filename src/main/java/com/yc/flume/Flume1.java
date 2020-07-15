package com.yc.flume;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Flume1 extends AbstractSource implements EventDrivenSource,Configurable{
	private static final Logger logger=LoggerFactory.getLogger(Flume1.class);
	
	private String filePath;//待读取文件路径
	private String charset;
	private String posiFile;  //记录偏移量的文件路径
	private long interval; //间隔时间
	private ExecutorService executor; //线程池
	private FileRunnable fileRunnable;//线程任务
	
	public Flume1(){
		System.out.println("Flme1构造了");
	}
	
	@Override
	public void configure(Context context) {
		System.out.println("configure+++++");
		filePath=context.getString("filePath");
		charset=context.getString("charset","UTF-8");
		posiFile =context.getString("posiFile");
		interval=context.getLong("interval",1000L);
	}
	
	public synchronized void start(){
		System.out.println("start ++++++++++++++");
		//创建一个单线程的线程池
		executor=Executors.newSingleThreadExecutor();
		//定义一个实现runnable接口的类,作为一个任务
		fileRunnable=new FileRunnable(filePath,posiFile,interval,charset,getChannelProcessor());
		executor.submit(fileRunnable);
		super.start();		
	}
	
	
	public synchronized void stop(){
		System.out.println("stop +++++++");
		fileRunnable.setFlag(false);
		executor.shutdown();
		while(!executor.isTerminated()){
			logger.debug("等待文件执行线程停止");
			try {
				executor.awaitTermination(500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				logger.debug("等待执行线程停止时异常");
				Thread.currentThread().interrupt();
			}
		}
	}
	private static class FileRunnable implements Runnable{
		private long interval;
		private String charset;
		private ChannelProcessor channelProcessor;
		private long offset=0L;
		private RandomAccessFile raf;
		private boolean flag=true;
		private File positionFile;
		
		public FileRunnable(String filePath, String posiFile, long interval, String charset,
				ChannelProcessor channelProcessor) {
			this.interval=interval;
			this.charset=charset;
			this.channelProcessor=channelProcessor;
			
			//读取偏移量 如果有则接着读没有就从头开始
			positionFile=new File(posiFile);
			if(!positionFile.exists()){
				//不存在则创建一个位置文件
				try{
					positionFile.createNewFile();
				}catch(IOException e){
					logger.error("创建位置文件错误",e);
				}
			}
			
			try {
				String offsetString =FileUtils.readFileToString(positionFile);
				//记录过偏移量
				if(offsetString !=null && !"".equals(offsetString)){
					//将当前的偏移量转换成 long
					offset=Long.parseLong(offsetString);
				}
				//读取log文件是从指定的位置读取数据
				raf=new RandomAccessFile(filePath,"r");
				//按照指定偏移量读书
				raf.seek(offset);
			} catch (IOException e) {
				logger.error("读取位置文件错误",e);
			}
		}

		@Override
		public void run() {
			while(flag){
				try {
					//读取Log文件中的新数据
					String line=raf.readLine();
					if(line != null){
						line =new String(line.getBytes("ISO-8859-1"),charset);
						//将数据发给channel
						channelProcessor.processEvent(EventBuilder.withBody(line.getBytes()));
						//获取最新的偏移量然后更新偏移量
						offset=raf.getFilePointer();
						//将偏移量写入到位置文件中
						FileUtils.writeStringToFile(positionFile, offset+"");
					}
				} catch (Exception e) {
					logger.error("读取日志文件异常",e);
				} 
			}
			
		}
		private void setFlag(boolean flag){
			this.flag=flag;
		}
	}
}
	



