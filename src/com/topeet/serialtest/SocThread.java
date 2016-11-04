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

	//��ȡ����
	public  Socket socket = null;
	serial com3 = new serial();
	BufferedReader inputReader = null;
	BufferedReader reader = null;
	BufferedWriter writer = null;
	
	
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
				out = socket.getOutputStream();//��������
				Log.i(tag, "�������ȡ�ɹ�");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//��������
			
	}


	private void startWifiReplyListener(final BufferedReader reader) {
		// TODO Auto-generated method stub
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					int response = 0;
				//	static int num=0;
					byte[] rx = null;
					rx = new byte[30];
					int i = 0;
				//	byte  tmp,i = 0;
					while ((response = reader.read()) != -1){
						byte c = (byte)response;
						rx[i++] = c; 
						Log.d(tag, "Data �Ѷ�ȡ" + c);
						Message msg = inHandler.obtainMessage();
						msg.obj = c;
						inHandler.sendMessage(msg);
						if(i==30)
							Log.d(tag, "Data ��ȡ���" );
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
				/**
				 * �˴�������BUG ��out.�Ǹ���
				 * **/
				try {
					out = socket.getOutputStream();//��������
					for(int i = 0 ; i<RX.length ; i++)
						try {
							out.write(RX[i]);
							out.flush();
							Log.d(tag, "data �ѷ���");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/*try {
				Log.i(tag, "2.�������");
				in = socket.getInputStream();//��������
					BufferedReader bff = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String data = bff.readLine();
					Log.d(tag, "Data �ѽ���" + data);
					
				Log.i(tag, "3.getdata" + line + " len=" + line.length());
				Log.i(tag, "4.start set Message");
				Message msg = inHandler.obtainMessage();
				msg.obj = line;
				inHandler.sendMessage(msg);// ������ظ�UI����
				Log.i(tag, "5.send to handler");
			}catch (Exception e) {
				Log.i(tag, "���ݽ��մ���" + e.getMessage());
				e.printStackTrace();
			}*/
		}
	}

	/**
	 * ��������
	 * 
	 * @param mess
		
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

		}
	}*/
	
	
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
