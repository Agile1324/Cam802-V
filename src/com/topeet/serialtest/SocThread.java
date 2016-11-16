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

public class SocThread extends Thread{

	//��ȡ����
	public  Socket socket = null;
	serial com3 = new serial();
	BufferedReader inputReader = null;
	BufferedReader reader = null;
	BufferedWriter writer = null;
	
	private InputStream mInputStream;
	private String tag = "socket thread";
	
	Handler inHandler;//�����첽����
	Handler outHandler;//����첽����
	Context ctx;
	
	public boolean isRun = true;
	public static OutputStream  out = null;
	public static InputStream in = null;
	SharedPreferences sp;
	
	public SocThread(Handler handlerin) {
		inHandler = handlerin;
	}
	
	/**
	 * ����socket������
	 * */
	
	public void conn(){
		//�򿪱���socket����
		com3.Open(3, 115200);
		Log.d(tag, "�����Ѿ���");
			/*���ӷ����� ���������ӳ�ʱΪ5��   */
			socket = new Socket();  
			try {
				socket.connect(new InetSocketAddress("192.168.11.123", 2001), 5000);
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				startWifiReplyListener(reader);
				//InputStream();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
   		 	Log.d(tag, "wifi ������");
			try {
				out = socket.getOutputStream();//��������
				Log.i(tag, "�������ȡ�ɹ�");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//��������
			
	}
	/**
	 * ʵʱ����wifi����
	 *
	private void InputStream(){
		new Thread(new Runnable() {
			public void run(){
				byte[] buffer = new byte[30] ;
				String test;
				String infomation ;//����msg������Ϣ
				
				while(true){
					InputStream in;
					try {
						in = socket.getInputStream();
						 //buffer = new byte[in.available()];
						int rcvLength;
						while((rcvLength=in.read(buffer)) > 0){
							//��ӡ����
							StringBuffer sbuf = new StringBuffer();
							for (int i = 0; i < buffer.length; i++) {
								sbuf.append(buffer[i]);
							}
							test = sbuf.toString();
							Log.d(tag, "�����ǣ�"+ test +"  "+ rcvLength);
							
							//�ж����鳤�ȣ��������30����ֱ�ӽ�����λ�ж�
							if(rcvLength == 30){
							//�ж�ͷβ
							if(buffer[0] == 61 && buffer[29] == -1){
							
								Log.d(tag, "�ж��ɹ�����ʾ������");
							//���ȣ�11-12��γ��13-14�� �����ٶ�3 �����ٶ�4  ���Ƕ�26
							
							infomation = "����"+Integer.toString(buffer[11])+"."+Integer.toString(buffer[12])+"  γ��"+
									Integer.toString(buffer[13])+"."+Integer.toString(buffer[14])+
									"\n���� "+ Integer.toString(buffer[3]) + "��" + "\n" + "���� " + 
									Integer.toString(buffer[4]) + "��\n"+"�Ƕȣ�"+Integer.toString(buffer[26])
									;
							
							Message msg = inHandler.obtainMessage();
							//Bundle bundle = new Bundle();
							
							msg.obj = infomation;
							inHandler.sendMessage(msg);// ������ظ�UI����
							
							///msg.what = 1;
							//msg.obj = speedRight;
							//inHandler.sendMessage(msg);// ������ظ�UI����
							//�������飬ת����String16�����ַ���
							for(int i = 0 ; i< buffer.length ; i++){
								String hex = Integer.toHexString(buffer[i] &0xff);
						
								//Log.d(tag, hex);
								if(hex.length() == 1){
									hex = "0" +hex;
								}
							}
							//speedLeft = Integer.valueOf(buffer[4]);
							//Log.d(tag, "�����ٶ���" + speedLeft + "��");
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



	
	private void startWifiReplyListener(final BufferedReader reader) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//final StringBuffer buffer = new StringBuffer(); 
					String response ;
					String test;
					while ((response = reader.readLine()) != null)
					{
						byte[] res = response.getBytes();
						//��ӡ����
						StringBuffer sbuf = new StringBuffer();
						for(int i = 0 ; i < res.length; i++){
							sbuf.append(res[i]);
						}
						test = sbuf.toString();
						Log.d(tag, "������:" + res);
						//buffer.append(response);
						//test = buffer.toString();
						//Log.d(tag, "������:" + response);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	

	/**
	 * ʵʱ��������
	 */
	@Override
	public void run() {
		//�򿪱���socket����
		Log.i(tag, "�߳�socket��ʼ����");
		conn();
		Log.i(tag, "1.run��ʼ");
		while (isRun) {
			//��������
			int[] RX = com3.Read();
			if(RX != null){
				try {
					out = socket.getOutputStream();//��������
					for(int i = 0 ; i<RX.length ; i++)
						try {
							out.write(RX[i]);
							out.flush();
							Log.d(tag, "data �ѷ���");
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
	 * �ر����� 
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
