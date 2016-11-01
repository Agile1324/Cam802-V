package com.topeet.serialtest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SocThread extends Thread{

	//读取串口
	public  Socket socket = null;
	serial com3 = new serial();
	
	
	private String ip = "192.168.11.123";//设置IP
	private int port = 2001;//设置端口
	private String tag = "socket thread";
	private int timeout = 5000 ; //超时时间
	
	Handler inHandler;//输入异步任务
	Handler outHandler;//输出异步任务
	Context ctx;
	
	public boolean isRun = true;
	public Socket client = null;
	public static OutputStream  out = null;
	public static InputStream in = null;
	SharedPreferences sp;
	
	/*
	public SocThread(Handler handlerin ,Handler handlerout , Context context){
		inHandler = handlerin;
		outHandler = handlerout;
		ctx = context;
		Log.d(tag, "创建线程socket");
	}*/
	
	/**
	 * 链接socket服务器
	 * */
	
	public void conn(){
		//打开本地socket串口
		com3.Open(3, 115200);
		Log.d(tag, "串口已经打开");
			/*连接服务器 并设置连接超时为5秒  
        	 *  socket = new Socket();  
        	 *	socket.connect(new InetSocketAddress("192.168.11.123", 2001), 5000);
			 * */
			socket = new Socket();  
			try {
				socket.connect(new InetSocketAddress("192.168.11.123", 2001), 5000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
   		 	Log.d(tag, "wifi 已链接");
   		 	/*
			Log.i(tag, "链接中");
			client = new Socket(ip , port);
			client.setSoTimeout(timeout);//设置阻塞时间
			Log.i(tag, "链接成功");*/
			try {
				in = socket.getInputStream();
				out = socket.getOutputStream();//发送数据
				Log.i(tag, "输入输出流获取成功");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//接收数据
	}

	/*
	private void initdate() {
		// TODO Auto-generated method stub
		sp = ctx.getSharedPreferences("SP", ctx.MODE_PRIVATE);
		ip = sp.getString("ipstr", ip);
		port = Integer.parseInt(sp.getString("port", String.valueOf(port)));
		Log.i(tag, "获取到ip端口："+ ip + ";" +port);
		
	}*/
	/**
	 * 实时接受数据
	 */
	@Override
	public void run() {
		//打开本地socket串口

		Log.i(tag, "线程socket开始运行");
		conn();
		Log.i(tag, "1.run开始");
		String line = "";
		while (isRun) {
			try {
				if (client != null) {
					Log.i(tag, "2.检测数据");
					while (in != null) {
						Log.i(tag, "3.getdata" + line + " len=" + line.length());
						Log.i(tag, "4.start set Message");
						Message msg = inHandler.obtainMessage();
						msg.obj = line;
						inHandler.sendMessage(msg);// 结果返回给UI处理
						Log.i(tag, "5.send to handler");
					}
				} 
			} catch (Exception e) {
				Log.i(tag, "数据接收错误" + e.getMessage());
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 发送数据
	 * 
	 * @param mess
	 */
	public void Send(String mess) {
		
		while(true){
		
		int[] RX = com3.Read();
		if(RX != null){
			
			/**
			 * 此处可能有BUG ，out.是根据
			 * **/
			for(int i = 0 ; i<RX.length ; i++)
				try {
					out.write(RX[i]);
					out.flush();
					Log.d(tag, "data 已发送");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}else {
			Log.d(tag, "发送失败，重新连接");
			conn();
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		}
		
		/**try {
			if (client != null) {
				Log.i(tag, "发送" + mess + "至"
						+ client.getInetAddress().getHostAddress() + ":"
						+ String.valueOf(client.getPort()));
				out.println(mess);
				out.flush();
				Log.i(tag, "发送成功");
				Message msg = outHandler.obtainMessage();
				msg.obj = mess;
				msg.what = 1;
				outHandler.sendMessage(msg);// 结果返回给UI处理
			} else {
				Log.i(tag, "client 不存在");
				Message msg = outHandler.obtainMessage();
				msg.obj = mess;
				msg.what = 0;
				outHandler.sendMessage(msg);// 结果返回给UI处理
				Log.i(tag, "连接不存在重新连接");
				conn();
			}

		} catch (Exception e) {
			Log.i(tag, "send error");
			e.printStackTrace();
		} finally {
			Log.i(tag, "发送完毕");

		}*/
	}
	
	static {
        System.loadLibrary("serialtest");
	}
	
	
}
