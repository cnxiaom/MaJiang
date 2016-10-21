package com.zxz.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.IoSession;
import org.json.JSONObject;

import com.zxz.controller.GameManager;
import com.zxz.controller.RoomManager;
import com.zxz.dao.OneRoomDao;
import com.zxz.dao.UserDao;
import com.zxz.domain.Game;
import com.zxz.domain.OneRoom;
import com.zxz.domain.User;

/**
 * �����û�����
 *
 */
public class UserDroppedService {

	IoSession session;
	OneRoomDao roomDao = OneRoomDao.getInstance();
	UserDao userDao = UserDao.getInstance();
	
	public UserDroppedService() {
	}

	public UserDroppedService(IoSession session) {
		this.session = session;
		//check();
	}

	private void check() {
		User user = (User) session.getAttribute("user");
		String roomId = user.getRoomId();
		Map<String, Game> gameMap = GameManager.getGameMap();
		Game game = gameMap.get(roomId);
		if(game!=null){
			setOneRoomAndseatMapSession(game);
		}
	}
	
	
	public JSONObject getGameInfo(Game game){
		JSONObject outJsonObject = new JSONObject();
		int totalGame = game.getTotalGame();
		outJsonObject.put("totalGame", totalGame);
		
		
		return outJsonObject;
	}
	
	public void setOneRoomAndseatMapSession(Game game){
		OneRoom room = game.getRoom();
		Map<String, User> seatMap = game.getSeatMap();
		List<User> userList = room.getUserList();
		User user = (User) session.getAttribute("user");
		for(int i=0;i<userList.size();i++){
			User u = userList.get(i);
			if(u.getId()==user.getId()){
				u.setIoSession(session);
				break;
			}
		}
		
		Iterator<String> iterator = seatMap.keySet().iterator();
		while(iterator.hasNext()){
			String key = iterator.next();
			User u = seatMap.get(key);
			if(u.getId()==user.getId()){
				u.setIoSession(session);
				break;
			}
		}
	}
	
	
	/**
	 * �û����ߺ󣬰����ķ���ż�¼������Ȼ�󱣴浽���ݿ���
	 */
	public void userDropLine(){
		User user = (User) session.getAttribute("user");
		String roomId = user.getRoomId();
		OneRoom oneRoom = RoomManager.getRoomWithRoomId(roomId);
		Game game = GameManager.getGameWithRoomNumber(roomId);
		if(oneRoom!=null&&game==null){//�Ѿ������˷��䣬������Ϸ��û�п�ʼ
			//����Ƿ�����ɢ���䣬�������ͨ���,������߳�����
			PlayOfHongZhong playGame = new UserService();
			if(user.isBanker()){//������ɢ����
				playGame.disbandRoom(user);
			}else{//��������뿪����
				playGame.leaveRoom(user);
			}
		}else if(game!=null && oneRoom != null){//��Ϸ�������뿪����
			User modifyUser = new User();
			modifyUser.setId(user.getId());
			modifyUser.setRoomId(roomId);
			userDao.modifyUser(modifyUser);//��¼���û��ķ����
		}
	}
	
}