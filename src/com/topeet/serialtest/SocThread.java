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
import java.net.ResponseCache;
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
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener;
import android.widget.TextView;
import android.widget.Toast;

//编码格式GBK
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
		//com3.Open(3, 115200);
		//Log.d(tag, "串口已经打开");
			/*连接服务器 并设置连接超时为5秒   */ 
			socket = new Socket();  
			try {
				socket.connect(new InetSocketAddress("192.168.11.123", 2001), 5000);
				//while(true){ 
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				startWifiReplyListener(reader);//开始监听事件
				Log.d(tag, "开始监听事件");
				//InputStream();
				//}
			} catch (IOException e) { 
				e.printStackTrace();
			}
   		 	Log.d(tag, "wifi 已链接");//打印log，显示wifi链接情况
			try {
				out = socket.getOutputStream();//发送数据
				Log.i(tag, "输出流获取成功");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	}

	
	private void startWifiReplyListener(final BufferedReader reader) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//final StringBuffer buffer = new StringBuffer(); 
					int [] rx = new int [1024]; 
					int response  = 0;    
					String fx ;
					String fxY;
					String infomation ;//msg传输文本
					int i = 0; 
					//while ((response = reader.readLine()) != null){
					try {
						Thread.sleep(2);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					while ((response = reader.read()) != -1 ){ 
						Log.i(tag, "1.while开始");
						rx[i++] = response;
						
						if(rx[0] != 61){
							i = 0;
						}
						
						
						if(rx[0] == 61 && rx[9] == 127 && i == 10){
							//Log.d(tag, "判定成功，显示主界面");
							int directionP = rx[4] & 64 ;
							int directionN = rx[5] & 64 ;
						//经度：11-12；纬度13-14； 左轮速度3 右轮速度4  ；角度26
							if( directionP != 0 ){
								 fx = "反";
							}else {
								 fx = "正";
							}
							
							if( directionN != 0 ){
								 fxY = "反";
							}else {
								 fxY = "正";
							}
						
						infomation = "经度"+Integer.toString(rx[11])+"."+Integer.toString(rx[12])+"  纬度"+
								Integer.toString(rx[13])+"."+Integer.toString(rx[14]) +
								"\n左轮 "+ fx + Integer.toString(rx[4]&0xbf) + "档" + "\n" + "右轮 " + fxY +
								Integer.toString(rx[5]&0xbf) + "档\n"+"角度："+Integer.toString(rx[26])
								;
						
						Message msg = inHandler.obtainMessage();
						
						msg.obj = infomation;
						inHandler.sendMessage(msg);// 结果返回给UI处理
						
						i = 0 ;
						
						try {
							Thread.sleep(2);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally {
					try {
						if(reader != null)
							reader.close();
						if(socket != null)
							socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}).start();
	}
	
	

	/**
	 * 实时接受数据
	 */
	@Override
	public void run() {
		//打开本地socket串口
		Log.i(tag, "线程socket开始运行");
		com3.Open(3, 115200);
		Log.d(tag, "串口已经打开");
		conn();
		while (isRun) {
			//发送数据
			int[] RX = com3.Read();
			if(RX != null){
					for(int i = 0 ; i<RX.length ; i++)
						try {
							out = socket.getOutputStream();//发送数据
							out.write(RX[i]);
							out.flush();
							Log.d(tag, "data 已发送");
						} catch (IOException e) {
							e.printStackTrace();
						}
			}
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		try {
			out.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 关闭连接 
	 */
	public void close() {
		try {if (socket != null) {
			in.close();
			out.close();
			socket.close();
			reader.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	static {
        System.loadLibrary("serialtest");
	}
	
	
}



/**
 * 实时接收wifi数据
 *
	private void InputStream(){
		new Thread(new Runnable() {
			public void run(){
				byte[] buffer = new byte[30] ;
				String test;
				String infomation ;//传输msg总体消息
				
				while(true){
					InputStream in;
					try {
						in = socket.getInputStream();
						 //buffer = new byte[in.available()];
						int rcvLength;
						while((rcvLength=in.read(buffer)) > 0){
							//打印数组
							StringBuffer sbuf = new StringBuffer();
							for (int i = 0; i < buffer.length; i++) {
								sbuf.append(buffer[i]);
							}
							test = sbuf.toString();
							Log.d(tag, "数组是："+ test +"  "+ rcvLength);
							
							//判定数组长度，如果等于30，则直接进行首位判定
							if(rcvLength == 30){
							//判定头尾
							if(buffer[0] == 61 && buffer[29] == -1){
							
								Log.d(tag, "判定成功，显示主界面");
							//经度：11-12；纬度13-14； 左轮速度3 右轮速度4  ；角度26
							
							infomation = "经度"+Integer.toString(buffer[11])+"."+Integer.toString(buffer[12])+"  纬度"+
									Integer.toString(buffer[13])+"."+Integer.toString(buffer[14])+
									"\n左轮 "+ Integer.toString(buffer[3]) + "档" + "\n" + "右轮 " + 
									Integer.toString(buffer[4]) + "档\n"+"角度："+Integer.toString(buffer[26])
									;
							
							Message msg = inHandler.obtainMessage();
							//Bundle bundle = new Bundle();
							
							msg.obj = infomation;
							inHandler.sendMessage(msg);// 结果返回给UI处理
							
							///msg.what = 1;
							//msg.obj = speedRight;
							//inHandler.sendMessage(msg);// 结果返回给UI处理
							//遍历数组，转换成String16进制字符串
							for(int i = 0 ; i< buffer.length ; i++){
								String hex = Integer.toHexString(buffer[i] &0xff);
						
								//Log.d(tag, hex);
								if(hex.length() == 1){
									hex = "0" +hex;
								}
							}
							//speedLeft = Integer.valueOf(buffer[4]);
							//Log.d(tag, "左轮速度是" + speedLeft + "档");
						}
						}else if(rcvLength > 1 &&rcvLength != 30){
							
						}
						 }
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();
	}*/