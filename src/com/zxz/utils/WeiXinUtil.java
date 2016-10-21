package com.zxz.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public class WeiXinUtil {

	private static Logger logger = Logger.getLogger(WeiXinUtil.class);  
	
//	static final String appid = "wx2ffdd6142f15121d";//�齫����
//	static final String appsecret = "c931b873b47992135fc646e6b95ce215";//�齫����
	
	
	static final String appid = "wx520718635911a4b0";//��Խ����
	static final String appsecret = "fbe305a9e5f899bbbc96e79025d0c99e";//��Խ����
	
	
	static Map<String,Map<String, Object>> openId_access_tokenMap_web = null;
	
	
	public static void main(String[] args) {
//		WeiXinUtil util = new WeiXinUtil();
//		JSONObject weinxinOpenIdJson = WeiXinUtil.getAccessTokenJson("041CEQ500MTHHB1jn5600ySR500CEQ5w");
//		String openid = weinxinOpenIdJson.getString("openid");
//		System.out.println("openid:"+openid);
//		String access_token = weinxinOpenIdJson.getString("access_token");
//		System.out.println("access_token:"+access_token);
//		JSONObject userInfo = getUserInfo(access_token, openid);
//		System.out.println("userInfo:"+userInfo);
		String accessToken = WeiXinUtil.getAccessTokenWithRefreshToken("lX5BkGzTus4EDOxyoA6BrWlU6QLHijWmw6MOEIiZ0anLzIylEkSf8vSm48PDTWqIxfd7VYEazFvVJJ-LHt-p1kUmBVYGhErDDvrfFJDc4hs");
		System.out.println(accessToken);
		JSONObject userInfo = getUserInfo(accessToken, "obhqFxJAVFcTvv5qAOKlHiyQ5fns");
		System.out.println(userInfo);
	}
	
	
	/**����refreshToken��ȡaccess_token
	 * @param refreshToken
	 * @return
	 */
	public static String getAccessTokenWithRefreshToken(String refreshToken){
		String url = "https://api.weixin.qq.com/sns/oauth2/refresh_token?appid="+appid+"&grant_type=refresh_token&refresh_token="+refreshToken;
		String returnStr = sendPost(url, null);
		JSONObject outJsonObject = new JSONObject(returnStr);
		if(outJsonObject.has("errcode")){
			throw new IllegalArgumentException(outJsonObject.toString());
		}
		return outJsonObject.getString("access_token");
	}
	
	/**
	 * ��ȡopenid
	 * @param code
	 * @return
	 */
	public static JSONObject getAccessTokenJson(String code){
		String returnStr = "";
		if(code!=null&&!code.trim().equals("")){
			String getopenId_url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="+appid+"&secret="+appsecret+"&code="+code+"&grant_type=authorization_code";
//			returnStr = httpRequest(getopenId_url, "POST", null);
			returnStr = sendPost(getopenId_url, null);
		}
		logger.info("΢����Ȩ����:"+returnStr);
		JSONObject responseJson = new JSONObject(returnStr);
		if(responseJson.has("errcode")){
			logger.fatal("΢����Ȩ:"+code);
			throw new IllegalArgumentException(responseJson.getString("errmsg"));
		}
		return responseJson;
	}
	
	
	/**������Ȩƾ֤�Ƿ���Ч
	 * @param openid
	 * @return
	 */
	public boolean isAccessTokenEffective(String openid){
		String url = "https://api.weixin.qq.com/sns/auth?access_token=ACCESS_TOKEN&openid="+openid;
//		String returnStr = httpRequest(url, "POST", null);
		String returnStr = sendPost(url, null);
		logger.info("������Ȩƾ֤��access_token���Ƿ���Ч:"+returnStr);
		JSONObject jsonObject = new JSONObject(returnStr);
		int errorcode = jsonObject.getInt("errcode");
		if(errorcode==0){
			return true;
		}
		return false;
	}
	
	
	/**
	 * �õ��û�����Ϣ
	 */
	public static JSONObject getUserInfo(String accesstoekn,String openid){
		String url = "https://api.weixin.qq.com/sns/userinfo?access_token="+accesstoekn+"&openid="+openid;
		//System.out.println("url:"+url);
		String returnStr = httpRequest(url, "POST", null);
//		String returnStr = sendPost(url,  null);
		//System.out.println("��ȡ�����û���Ϣ"+returnStr);
		JSONObject jsonObject = new JSONObject(returnStr);
		if(jsonObject.has("errcode")){
			logger.fatal("��ȡ�û���Ϣ��ʱ�����:"+jsonObject.getString("errcode"));
			throw new IllegalArgumentException(jsonObject.getString("errcode"));
		}
		return jsonObject;
	}
	
	
	 public static String sendPost(String url, Map<String,String> params) {
	        PrintWriter out = null;
	        BufferedReader in = null;
	        String result = "";
	        try {
//	        	param = "method=����";
	            URL realUrl = new URL(url);
	            StringBuffer paramBuffer = new StringBuffer();
	            if(params!=null&&params.size()>0){
	            	Iterator<Entry<String, String>> it = params.entrySet().iterator();
	            	int count = 0;
	            	while(it.hasNext()){
	            		Entry<String, String> ent = it.next();
	            		if(ent.getKey()!=null&&ent.getValue()!=null){
	            			if(count!=0){
		            			paramBuffer.append("&"+ent.getKey()+"="+URLEncoder.encode(ent.getValue(), "utf-8"));
		            		}else{
		            			paramBuffer.append(ent.getKey()+"="+URLEncoder.encode(ent.getValue(), "utf-8"));
		            		}
		            		count++;
	            		}
	            	}
	            }
	            // �򿪺�URL֮�������
	            URLConnection conn = realUrl.openConnection();
	            // ����ͨ�õ���������
	            conn.setRequestProperty("Accept-Charset", "utf-8");
	            conn.setRequestProperty("contentType", "utf-8");
	            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	            conn.setRequestProperty("accept", "*/*");
	            conn.setRequestProperty("connection", "Keep-Alive");
	            conn.setRequestProperty("user-agent",
	                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
	            // ����POST�������������������
	            conn.setDoOutput(true);
	            conn.setDoInput(true);
	            // ��ȡURLConnection�����Ӧ�������
	            out = new PrintWriter(conn.getOutputStream());
	            // �����������
	            out.print(paramBuffer.toString());
	            // flush������Ļ���
	            out.flush();
	            // ����BufferedReader����������ȡURL����Ӧ
	            in = new BufferedReader(
	                    new InputStreamReader(conn.getInputStream()));
	            String line;
	            while ((line = in.readLine()) != null) {
	                result += line;
	            }
	        } catch (Exception e) {
	            System.out.println("���� POST ��������쳣��"+e);
	            e.printStackTrace();
	        }
	        //ʹ��finally�����ر��������������
	        finally{
	            try{
	                if(out!=null){
	                    out.close();
	                }
	                if(in!=null){
	                    in.close();
	                }
	            }
	            catch(IOException ex){
	                ex.printStackTrace();
	            }
	        }
	        return result;
	    }   

	
	
	/** 
	 * ����https���󲢻�ȡ��� 
	 * @param requestUrl �����ַ 
	 * @param requestMethod ����ʽ��GET��POST�� 
	 * @param outputStr �ύ������ 
	 * @return JSONObject(ͨ��JSONObject.get(key)�ķ�ʽ��ȡjson���������ֵ) 
	 */
	private static String httpRequest(String requestUrl, String requestMethod, String xml) {
		String resultStr = "";
		StringBuffer buffer = new StringBuffer();
		try {
			// ����SSLContext���󣬲�ʹ������ָ�������ι�������ʼ��  
			TrustManager[] tm = { new MyX509TrustManager() };
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
			sslContext.init(null, tm, new java.security.SecureRandom());
			// ������SSLContext�����еõ�SSLSocketFactory����  
			SSLSocketFactory ssf = sslContext.getSocketFactory();
			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);
			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			httpUrlConn.setRequestMethod(requestMethod);
			httpUrlConn.connect();
			if(xml!=null){
				OutputStream os = httpUrlConn.getOutputStream();
				os.write(xml.toString().getBytes("utf-8"));
				os.close();
			}
			// �����ص�������ת�����ַ���  
			InputStream inputStream = httpUrlConn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				buffer.append(str);
			}
			bufferedReader.close();
			inputStreamReader.close();
			// �ͷ���Դ  
			inputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
			resultStr = buffer.toString();
		} catch (ConnectException ce) {
		} catch (Exception e) {
		}
		return resultStr;
	}
	
}