package com.zxz.service;

import org.json.JSONObject;

/**���ⲿ�ṩ���ݵĽӿ�
 * @author Administrator
 *
 */
public interface DateService {

	
	/**�õ�����
	 * @return
	 */
	JSONObject getGameJSONObject();
	
	/**�õ������map
	 * @return
	 */
	String getRoomJSONObject();
	
	/**�ܵ���������
	 * @return
	 */
	int getTotalOneLineUser();
	
	
	/**ʵʱ���ߵ�¼��
	 * @return
	 */
	int getTotalLoginLineUser();
	
}