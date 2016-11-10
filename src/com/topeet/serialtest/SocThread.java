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
import java.util.Arrays;

import com.linkcard.cam802.MainActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class SocThread extends Thread{

	//读取串口
	public  Socket socket = null;
	serial com3 = new serial();
	BufferedReader inputReader = null;
	BufferedReader reader = null;
	BufferedWriter writer = null;
	
	private InputStream mInputStream;
	private String tag = "socket thread";
	
	Handler inHandler;//输入异步任务
	Handler outHandler;//输出异步任务
	Context ctx;
	
	public boolean isRun = true;
	public static OutputStream  out = null;
	public static InputStream in = null;
	SharedPreferences sp;
	
	public SocThread(Handler handlerin) {
		inHandler = handlerin;
	}
	
	/**
	 * 链接socket服务器
	 * */
	
	public void conn(){
		//打开本地socket串口
		com3.Open(3, 115200);
		Log.d(tag, "串口已经打开");
			/*连接服务器 并设置连接超时为5秒   */
			socket = new Socket();  
			try {
				socket.connect(new InetSocketAddress("192.168.11.123", 2001), 5000);
				//reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				//startWifiReplyListener(reader);
				InputStream();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
   		 	Log.d(tag, "wifi 已链接");
			try {
				out = socket.getOutputStream();//发送数据
				Log.i(tag, "输出流获取成功");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//接收数据
			
	}
	/**
	 * 实时接收wifi数据
	 */
	private void InputStream(){
		new Thread(new Runnable() {
			public void run(){
				String[] Receive_date = null ;
				byte[] buffer = new byte[29] ;
				int speedLeft = 0;
				int speedRight = 0;
				while(true){
					InputStream in;
					try {
						in = socket.getInputStream();
						 buffer = new byte[in.available()];
						while(in.read(buffer) > 0){
							if(buffer[0] == 61){
							
							speedLeft = buffer[3];
							speedRight = buffer[4]; 
							Log.d(tag, "number is " + speedLeft);
							Message msg = inHandler.obtainMessage();
							//Bundle bundle = new Bundle();
							
							//msg.what = 0;
							msg.obj = speedLeft;
							inHandler.sendMessage(msg);// 结果返回给UI处理
							
							///msg.what = 1;
							//msg.obj = speedRight;
							//inHandler.sendMessage(msg);// 结果返回给UI处理
							//遍历数组，转换成String16进制字符串
							/*for(int i = 0 ; i< buffer.length ; i++){
								String hex = Integer.toHexString(buffer[i] &0xff);
						
								//Log.d(tag, hex);
								if(hex.length() == 1){
									hex = "0" +hex;
								}
							}*/
							//speedLeft = Integer.valueOf(buffer[4]);
							//Log.d(tag, "左轮速度是" + speedLeft + "档");
						}
						 }
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();
	}



	/*
	private void startWifiReplyListener(final BufferedReader reader) {
		new Thread(new Runnable() {
			@Override
			public void run() {
					//while ((response = reader.readLine()) != null){
					while (!isInterrupted()){ 
						int size;
						try {
						if(reader == null )return;
							size = reader.read();
							if(size > 0 ){
								Log.d(tag, "打印数据");
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						Log.d(tag, "Data 已读取" + response);
						Message msg = inHandler.obtainMessage();
						msg.obj = response;
						inHandler.sendMessage(msg);
					}
			}
		}).start();
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
		while (isRun) {
			//发送数据
			int[] RX = com3.Read();
			if(RX != null){
				try {
					out = socket.getOutputStream();//发送数据
					for(int i = 0 ; i<RX.length ; i++)
						try {
							out.write(RX[i]);
							out.flush();
							Log.d(tag, "data 已发送");
						} catch (IOException e) {
							e.printStackTrace();
						}
					
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	/**
	 * 关闭连接 暂时不用
	 */
	public void close() {
		//try {
		//	if (socket != null) {
		//		//in.close();
		//		out.close();
		//	socket.close();
		//	}
		//} catch (Exception e) {
		//	e.printStackTrace();
		//}

	}
	
	static {
        System.loadLibrary("serialtest");
	}
	
	
}
