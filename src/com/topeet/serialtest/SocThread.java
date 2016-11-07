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
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				startWifiReplyListener(reader);
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



	
	private void startWifiReplyListener(final BufferedReader reader) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					byte response = 0 ;    //定义response int类型
					byte [] rx = new byte [40]; // 定义rx数组最大为40
					int i = 0; //定义变量i=0
					//while (response = reader.readLine() != null){
					while ((response = (byte) reader.read()) != -1){ //单个读取，字符
						byte  c = (byte) response; 
						rx[i++] = c;   

						if(rx[0]!= 61)
						{
							i=0;
						}
						else if(i==2)
						{
							if(rx[1]!=0x00)
							{
							i=0;					
							Log.d(tag, "Data 已读取 == 2");	
							}
					    }
						else if(i==30)
						{
							i=0;
							if(rx[29] == 0xff)
							{
								Log.d(tag, "Data 读取完成" );
							//speed 
							
							//gps
							
							//power
							
							}
						}
						
						
						Log.d(tag, "Data 已读取" + c);
						Message msg = inHandler.obtainMessage();
						msg.obj = c;
						inHandler.sendMessage(msg);
					//	if(i==30)
					//	{
					//		i=0;
					//		Log.d(tag, "Data 读取完成" );
					//	}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
	 * 关闭连接
	 */
	public void close() {
		try {
			if (socket != null) {
				in.close();
				out.close();
				socket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	static {
        System.loadLibrary("serialtest");
	}
	
	
}
