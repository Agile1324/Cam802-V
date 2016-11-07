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



	
	private void startWifiReplyListener(final BufferedReader reader) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					byte response = 0 ;    //����response int����
					byte [] rx = new byte [40]; // ����rx�������Ϊ40
					int i = 0; //�������i=0
					//while (response = reader.readLine() != null){
					while ((response = (byte) reader.read()) != -1){ //������ȡ���ַ�
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
							Log.d(tag, "Data �Ѷ�ȡ == 2");	
							}
					    }
						else if(i==30)
						{
							i=0;
							if(rx[29] == 0xff)
							{
								Log.d(tag, "Data ��ȡ���" );
							//speed 
							
							//gps
							
							//power
							
							}
						}
						
						
						Log.d(tag, "Data �Ѷ�ȡ" + c);
						Message msg = inHandler.obtainMessage();
						msg.obj = c;
						inHandler.sendMessage(msg);
					//	if(i==30)
					//	{
					//		i=0;
					//		Log.d(tag, "Data ��ȡ���" );
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
