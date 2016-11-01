package com.topeet.serialtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.IBinder;
import android.util.Log;
import android.widget.EditText;

public class ServiceTest extends Service{
	
	serial com3 = new serial();
	String tag = "ServiceTest";
	
	//遥感发送相关socket****************
    public static String geted1="asdas";
    public  Socket socket = null;  
    public static OutputStream  ou=null;
    public static InputStream se = null;
	
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		Log.d(tag, "Service onCreate");
		super.onCreate();
		
		com3.Open(3, 115200);
		new MyThread(geted1).start(); 
	}
	
	 class MyThread extends Thread {  
   	  
         public String txt1;  
   
         public MyThread(String str) {  
             txt1 = str;  
         }  
   
         @Override  
         public void run() {  
        	 Log.d(tag, "Thread已运行");
        	 try {  
        		 //连接服务器 并设置连接超时为5秒  
        		 socket = new Socket();  
        		 socket.connect(new InetSocketAddress("192.168.11.123", 2001), 5000);
        		 Log.d(tag, "wifi 已链接");
        		 //获取输入输出流  
        		 
        	 } catch (IOException e) {  
        		 e.printStackTrace();  
        	 }
        	 
        	 while(true){
        		 int[] RX = com3.Read();
        		 if(RX != null){
        				 try {
        				 //发送数据
        				 ou = socket.getOutputStream();
        				 for(int i = 0 ;i<RX.length ; i++)
        				 ou.write(RX[i]); 
        				 ou.flush();
        				 Log.d(tag, "data 已发送");
        			 } catch (IOException e1) {
        				 // TODO Auto-generated catch block
        				 e1.printStackTrace();
        			 } 
        				 /*
        				 try {
							se = socket.getInputStream();
							se.read();//int数组
							Log.d(tag, "data 已接收");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/
        		 }
          
             try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	 }
         }  
     }  
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub

		return super.onStartCommand(intent, flags, startId);
	}
	

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	static {
        System.loadLibrary("serialtest");
	}
	

}
