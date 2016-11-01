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

	//��ȡ����
	public  Socket socket = null;
	serial com3 = new serial();
	
	
	private String ip = "192.168.11.123";//����IP
	private int port = 2001;//���ö˿�
	private String tag = "socket thread";
	private int timeout = 5000 ; //��ʱʱ��
	
	Handler inHandler;//�����첽����
	Handler outHandler;//����첽����
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
		Log.d(tag, "�����߳�socket");
	}*/
	
	/**
	 * ����socket������
	 * */
	
	public void conn(){
		//�򿪱���socket����
		com3.Open(3, 115200);
		Log.d(tag, "�����Ѿ���");
			/*���ӷ����� ���������ӳ�ʱΪ5��  
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
   		 	Log.d(tag, "wifi ������");
   		 	/*
			Log.i(tag, "������");
			client = new Socket(ip , port);
			client.setSoTimeout(timeout);//��������ʱ��
			Log.i(tag, "���ӳɹ�");*/
			try {
				in = socket.getInputStream();
				out = socket.getOutputStream();//��������
				Log.i(tag, "�����������ȡ�ɹ�");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//��������
	}

	/*
	private void initdate() {
		// TODO Auto-generated method stub
		sp = ctx.getSharedPreferences("SP", ctx.MODE_PRIVATE);
		ip = sp.getString("ipstr", ip);
		port = Integer.parseInt(sp.getString("port", String.valueOf(port)));
		Log.i(tag, "��ȡ��ip�˿ڣ�"+ ip + ";" +port);
		
	}*/
	/**
	 * ʵʱ��������
	 */
	@Override
	public void run() {
		//�򿪱���socket����

		Log.i(tag, "�߳�socket��ʼ����");
		conn();
		Log.i(tag, "1.run��ʼ");
		String line = "";
		while (isRun) {
			try {
				if (client != null) {
					Log.i(tag, "2.�������");
					while (in != null) {
						Log.i(tag, "3.getdata" + line + " len=" + line.length());
						Log.i(tag, "4.start set Message");
						Message msg = inHandler.obtainMessage();
						msg.obj = line;
						inHandler.sendMessage(msg);// ������ظ�UI����
						Log.i(tag, "5.send to handler");
					}
				} 
			} catch (Exception e) {
				Log.i(tag, "���ݽ��մ���" + e.getMessage());
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
	 * ��������
	 * 
	 * @param mess
	 */
	public void Send(String mess) {
		
		while(true){
		
		int[] RX = com3.Read();
		if(RX != null){
			
			/**
			 * �˴�������BUG ��out.�Ǹ���
			 * **/
			for(int i = 0 ; i<RX.length ; i++)
				try {
					out.write(RX[i]);
					out.flush();
					Log.d(tag, "data �ѷ���");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}else {
			Log.d(tag, "����ʧ�ܣ���������");
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
				Log.i(tag, "����" + mess + "��"
						+ client.getInetAddress().getHostAddress() + ":"
						+ String.valueOf(client.getPort()));
				out.println(mess);
				out.flush();
				Log.i(tag, "���ͳɹ�");
				Message msg = outHandler.obtainMessage();
				msg.obj = mess;
				msg.what = 1;
				outHandler.sendMessage(msg);// ������ظ�UI����
			} else {
				Log.i(tag, "client ������");
				Message msg = outHandler.obtainMessage();
				msg.obj = mess;
				msg.what = 0;
				outHandler.sendMessage(msg);// ������ظ�UI����
				Log.i(tag, "���Ӳ�������������");
				conn();
			}

		} catch (Exception e) {
			Log.i(tag, "send error");
			e.printStackTrace();
		} finally {
			Log.i(tag, "�������");

		}*/
	}
	
	static {
        System.loadLibrary("serialtest");
	}
	
	
}
