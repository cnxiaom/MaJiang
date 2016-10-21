package com.zxz.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.mina.common.IoSession;
import org.json.JSONObject;

import com.zxz.utils.NotifyTool;


public class OneRoom {
	int id;	//����id
	int roomNumber;//�����
	List<User> userList = new LinkedList<>();//�����������
	int total;//��Ϸ����
	int zhama;//������
	User createUser;//������
	int createUserId;//�����˵�id 
	boolean isUse = false;//�����Ƿ��Ѿ�ռ��
	Set<String> directionSet = new HashSet<>();//����ķ���
	boolean isPay = false;//�Ƿ�۳�����
	Date createDate;//���䴴��ʱ��
	public User getCreateUser() {
		return createUser;
	}
	public void setCreateUser(User createUser) {
		this.createUser = createUser;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	public boolean isPay() {
		return isPay;
	}
	public void setPay(boolean isPay) {
		this.isPay = isPay;
	}
	public Set<String> getDirectionSet() {
		return directionSet;
	}
	public void setDirectionSet(Set<String> directionSet) {
		this.directionSet = directionSet;
	}
	public int getRoomNumber() {
		return roomNumber;
	}
	public void setRoomNumber(int roomNumber) {
		this.roomNumber = roomNumber;
	}
	public boolean isUse() {
		return isUse;
	}
	public void setUse(boolean isUse) {
		this.isUse = isUse;
	}
	public int getCreateUserId() {
		return createUserId;
	}
	public void setCreateUserId(int createUserId) {
		this.createUserId = createUserId;
	}
	public int getZhama() {
		return zhama;
	}
	public void setZhama(int zhama) {
		this.zhama = zhama;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public List<User> getUserList() {
		return userList;
	}
	public void setUserList(List<User> userList) {
		this.userList = userList;
	} 
	
	public void addUser(User user){
		userList.add(user);
	}
	
	public List<IoSession> getUserIoSessionList(){
		List<IoSession> list =  new ArrayList<IoSession>();
		for(User user : userList){
			list.add(user.getIoSession());
		}
		return list;
	}
	
	/**�û��뿪����
	 * @param user
	 * @return
	 */
	public boolean userLeaveRoom(User user){
		boolean result = false;
		for(User u:userList){
			if(u.getId()==user.getId()){
				result = userList.remove(u);
				break;
			}
		}
		return result;
	}
	
	
	/**֪ͨ�������û�
	 * @param jsonObject
	 */
	public void noticeUsersWithJsonObject(JSONObject jsonObject){
		for(int i=0;i<userList.size();i++){
			User user = userList.get(i);
			IoSession ioSession = user.getIoSession();
			NotifyTool.notify(ioSession, jsonObject);
		}
	}
	
}