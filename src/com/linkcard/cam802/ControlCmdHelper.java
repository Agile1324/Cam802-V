package com.linkcard.cam802;

import org.apache.http.Header;

import com.alibaba.fastjson.JSON;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

//øÿ÷∆√¸¡Óπ§æﬂ
public class ControlCmdHelper {
	
	
	private static final String FLAG_STRING_START = "<body>";
	private static final String FLAG_STRING_END = "</body>";
	
	public static final String CONTROL_CMD_GET_WIFI_SSID = "";
	public static final String CONTROL_CMD_SET_WIFI_SSID = "http://192.168.11.123/api/setssid?ssid=";
	
	public static final String CONTROL_CMD_GET_WIFI_PWD = "";
	public static final String CONTROL_CMD_SET_WIFI_PWD = "http://192.168.11.123/api/setpasswd?pswd=";
	
	public static final String CONTROL_CMD_FACTORY_DEFAULT = "http://192.168.11.123/api/reconfig";
	
	public static final String CONTROL_CMD_REBOOT = "http://192.168.11.123/api/reboot";
	
	public static final String CONTROL_CMD_VERSION = "http://192.168.11.123/api/version";
	
	public static final int CONTROL_CMD_CODE_SUCCESS = 0;
	
	public void sendCmd(final String url, final ControlCmdListener listener, final Class cls){
		AsyncHttpClient httpclient = new AsyncHttpClient();
		httpclient.setTimeout(3000);
		httpclient.get(url, new AsyncHttpResponseHandler(){
			@Override
            public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
				listener.onFailure(-1);
            }

			@Override
            public void onSuccess(int arg0, Header[] header, byte[] body) {
				String json = extractJSONString(new String(body));
				if (json==null){
					listener.onFailure(-1);
					return;
				}
				
				Object obj = JSON.parseObject(json, cls);
				if (obj==null){
					listener.onFailure(-1);
					return;
				}
				listener.onSuccess(obj);
            }
		});
	}
	
	private String extractJSONString(String body){
		int start = body.indexOf(FLAG_STRING_START);
		int end = body.indexOf(FLAG_STRING_END);
		if (start == -1 || end == -1 || (start+FLAG_STRING_START.length()) >= end){
			return null;
		}
		
		String jsonString = (body.substring(start+FLAG_STRING_START.length(), end)).trim();
		if (jsonString == null || !jsonString.startsWith("{") || !jsonString.endsWith("}")){
			return null;
		}
		
		return jsonString;				
	}
	
	
	public interface ControlCmdListener{
		public void onFailure(int type);
		public void onSuccess(Object obj);
	}

}
