package com.zxz.filter;

import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.json.JSONObject;

import com.zxz.domain.User;

public class AuthFilter extends IoFilterAdapter {

	//ֻ�����һ��
	@Override
	public void sessionOpened(NextFilter nextFilter, IoSession session)
			throws Exception {
		System.out.println("�ҵĹ�������ʼ");
		nextFilter.sessionOpened(session);
		System.out.println("�ҵĹ���������");
	}

	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session,
			Object message) throws Exception {
		//System.out.println("������ǰ:" + "messageReceived");
		boolean contain = session.containsAttribute("user");
		if(contain){
			User user = (User) session.getAttribute("user");
			if(user==null){//���û�û�е�¼
				return;
			}else{
				super.messageReceived(nextFilter, session, message);
			}
		}else{
			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(message.toString());
				String method = jsonObject.getString("method");
				if("login".equals(method)){
						super.messageReceived(nextFilter, session, message);
				}else{
					JSONObject outJs = new JSONObject();
					outJs.put("code", "error");
					outJs.put("discription", "���ȵ�¼");
					session.write(outJs);
				}
			} catch (Exception e) {
				JSONObject outJs = new JSONObject();
				outJs.put("code", "error");
				outJs.put("discription", "���ȵ�¼");
				session.write(outJs);
			}
		}
	}
}