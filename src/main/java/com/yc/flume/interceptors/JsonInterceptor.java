package com.yc.flume.interceptors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson.JSONObject;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;

public class JsonInterceptor implements Interceptor{
	private String[] schema;//["id","name","age"]
	private String separator;
	
	public JsonInterceptor(String schema,String separator) {
		this.schema=schema.split("[,]");
		this.separator=separator;
	}
	@Override
	public void close() {
		
	}

	@Override
	public void initialize() {
		
	}

	@Override
	public Event intercept(Event event) {
		Map<String,String> tuple=new LinkedHashMap();
		//将传入的Event中的Body内容,加上schema,然后放入Event
		String line=new String(event.getBody());
		String[] fields=line.split(separator);
		for(int i=0;i<schema.length;i++){//schema:   ["id","name","age"]
			String key=schema[i];
			String value=fields[i];
			tuple.put(key, value);
		}
		String json=JSONObject.toJSONString(tuple);
		//放入到Event
		event.setBody(json.getBytes());
		return event;
	}

	@Override
	public List<Event> intercept(List<Event> events) {
		for(Event e:events){
			intercept(e);
		}
		return events;
	}
	
	/**
	 * Interceptor.Builder的生命周期方法
	 * 构造器 -> configure -> build
	 */
	public static class Builder implements Interceptor.Builder{
		private String fields;
		private String separator;
		
		@Override
		public void configure(Context context) {
			/**
	         * 配置文件中应该有哪些属性？
	         * 1.数据的分割符
	         * 2.字段名字（schema）
	         * 3.schema字段的分隔符
	         * @param context
	         */
			fields=context.getString("fields");// a1.sources.r1.interceptors.i1.fields=id,name,age
			separator=context.getString("separator");//a1.sources.r1.interceptors.i1.separator=,
		}

		@Override
		public Interceptor build() {
			//build中创建JsonInterceptor实例
			return new JsonInterceptor(fields, separator);
		}
		
	}

}

