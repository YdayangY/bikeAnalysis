package com.yc.flume.interceptors;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;
import org.apache.log4j.Logger;

import nl.bitwalker.useragentutils.Browser;
import nl.bitwalker.useragentutils.OperatingSystem;
import nl.bitwalker.useragentutils.UserAgent;
/**
 * 
 * @author 郑梓阳Bg
 *
 */
public class AccessLogInterceptor implements Interceptor {
    private Logger logger = Logger.getLogger(AccessLogInterceptor.class.getName());

    @Override
    public void initialize() {
    }

    @Override
    public Event intercept(Event event) {
        //注意这里 一个  event是一天的日志中的一行记录，要对它进行解析.
        String line = new String(event.getBody());
        String[] fields = line.split(" ");
        if (fields == null || fields.length <= 0) {
            return null;
        }
        String remoteIp = fields[0];
        String loginRemoteName = fields[1];
        String authrizedName = fields[2];
        String responseCode = fields[3];
        String contentBytes = fields[4];
        String handleTime = fields[5];

        Pattern p = Pattern.compile("((.* \\[)?([a-zA-Z0-9: +/]*)?(\\] .*)?)");
        Matcher m = p.matcher(line);
        m.find();
        String time = m.group(3);    // 11/Jul/2020:11:25:33 +0800
        //转换成时间搓
        SimpleDateFormat format = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss z", Locale.ENGLISH);
        long timestamp = 0;
            try {
				timestamp = format.parse(time).getTime();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        p = Pattern.compile("((.* \")?(.*)?\" \"(.*)?\" \"(.*)?\")");  //
        m = p.matcher(line);
        m.find();
        String requestUrl = m.group(3);
        String refer = m.group(4);
        String browserinfo = m.group(5);
        //处理请求信息
        String[] requestParams = requestUrl.split(" ");  //由一个部分组成    [请求方式, 地址, 协议]
        if (requestParams[1].split("\\?").length > 0) {
            requestParams[1] = requestParams[1].split("\\?")[0];   //只保留了请求地址
        }
        //转成UserAgent对象
        UserAgent userAgent = UserAgent.parseUserAgentString(browserinfo);
        //获取浏览器信息
        Browser browser = userAgent.getBrowser();

        //获取系统信息
        OperatingSystem os = userAgent.getOperatingSystem();
        //系统名称
        String system = os.getName();
        //浏览器名称
        String browserName = browser.getName();

        StringBuffer sb=new StringBuffer();
        sb.append(remoteIp+"\t"+loginRemoteName+"\t"+authrizedName+"\t"+responseCode+"\t"+contentBytes+"\t"+handleTime+"\t"
                +timestamp+"\t"+requestParams[0]+"\t"+requestParams[1]+"\t"+requestParams[2]+"\t"+refer+"\t"+system+"\t"+browserName);
        event.setBody(   sb.toString().getBytes() );
        return event;
    }

    @Override
    public List<Event> intercept(List<Event> events) {
        for (Event e : events) {
            intercept(e);
        }
        return events;
    }

    @Override
    public void close() {

    }

    /**
     * Interceptor.Builder的生命周期方法
     * 构造器 -> configure -> build
     */
    public static class Builder implements Interceptor.Builder {
        @Override
        public Interceptor build() {
            //在build创建AccessLogInterceptor的实例
            return new AccessLogInterceptor();
        }

        @Override
        public void configure(Context context) {
            //暂时没有参数
        }
    }
}