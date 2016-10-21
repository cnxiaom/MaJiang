package com.zxz.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.apache.mina.common.IoSession;
import org.json.JSONArray;
import org.json.JSONObject;

import com.zxz.controller.GameManager;
import com.zxz.controller.RoomManager;
import com.zxz.dao.SumScoreDao;
import com.zxz.dao.UserScoreDao;
import com.zxz.domain.Game;
import com.zxz.domain.GangCard;
import com.zxz.domain.OneRoom;
import com.zxz.domain.Score;
import com.zxz.domain.SumScore;
import com.zxz.domain.User;
import com.zxz.domain.UserScore;
import com.zxz.utils.Algorithm2;
import com.zxz.utils.CardsMap;
import com.zxz.utils.Constant;
import com.zxz.utils.MathUtil;
import com.zxz.utils.NotifyTool;

public class PlayGameService implements Constant{

	private static Logger logger = Logger.getLogger(PlayGameService.class);  
	static UserScoreDao userScoreDao = UserScoreDao.getInstance();
	static SumScoreDao sumScoreDao = SumScoreDao.getInstance();
	
	public void playGame(JSONObject jsonObject, IoSession session) {
		String type = jsonObject.getString("type");//���ƣ����ƣ�����,����
		if(type.equals("chupai")){//����
			chuPai(jsonObject, session);
		}else if(type.equals("peng")){//����
			peng(jsonObject,session);
		}else if(type.equals("gang")){//����
			gang(jsonObject,session);
		}else if(type.equals("fangqi")){//����Ҳ����
			fangqi(jsonObject,session);
		}else if(type.equals("gongGang")){//����  Ҳ�� ����
			gongGang(jsonObject,session);
		}else if(type.equals("anGang")){//����
			anGang(jsonObject,session);
		}
	}
	
	/**����
	 * @param jsonObject
	 * @param session
	 */
	private void anGang(JSONObject jsonObject, IoSession session) {
		Game game = getGame(session);
		Integer cardId = jsonObject.getInt("cardId");
		User sessionUser = (User) session.getAttribute("user");
		User user = game.getUserInRoomList(sessionUser.getId());
		List<Integer> cards = user.getCards();
		List<Integer> gangCards = getGangList(cards, cardId);//�ܵ���
		//gangCards.add(cardId);
		user.userGangCards(gangCards);
		//��¼��Ҹܵ���
		user.recordUserGangCards(1, gangCards);
		notifyAllUserAnGang(game, gangCards,user);//֪ͨ���е���Ҹܵ��� 
		modifyUserScoreForAnGang(game, user);//�޸���Ұ��ܵ÷�
		//�������ץһ���� 
		userDrawCard(game, user.getDirection());
	}

	
	/**�޸���Ұ��ܵ÷� 
	 * @param game
	 * @param user //���ܵ��û�
	 */
	public static void modifyUserScoreForAnGang(Game game, User user) {
		User anGangUser = game.getUserInRoomList(user.getId());
		anGangUser.addScoreForAnGang();
		List<User> userList = game.getUserList();
		for(int i=0;i<userList.size();i++){
			User u = userList.get(i);
			if(u.getId()!=user.getId()){//�Ǹ��Ƶ���Ҽ���
				u.reduceScoreForAnGang();
			}
		}
	}

	/**����
	 * @param jsonObject
	 * @param session
	 */
	private void gongGang(JSONObject jsonObject, IoSession session) {
		int cardId = jsonObject.getInt("cardId");//�õ���������
		User user = (User) session.getAttribute("user");
		Game game = getGame(session);
		User gamingUser = getGamingUser(user.getId(), user.getRoomId());
		List<Integer> pengCards = gamingUser.getPengCards();//�û�������
		List<Integer> removeList = getRemoveList(cardId, pengCards);
		for(int i=0;i<removeList.size();i++){
			Integer revomeCard = removeList.get(i);
			pengCards.remove(revomeCard);
		}
		removeList.add(cardId);
		//���Լ��������Ƴ����ܵ�������
		gamingUser.removeCardFromGongGang(cardId);
		//��¼��Ҹܵ���
		gamingUser.recordUserGangCards(2, removeList);
		notifyAllUserGongGang(game, removeList,user);//֪ͨ���е���Ҹܵ��� 
		modifyUserScoreForGongGang(game, gamingUser);//�޸���ҹ��ܵ÷�
		//�������ץһ���� 
		userDrawCard(game, user.getDirection());
	}
	
	
	/**�õ���Ҫ�����������Ƴ��ļ���
	 * @param card
	 * @param pengCards
	 * @return
	 */
	public static List<Integer> getRemoveList(int card,List<Integer> pengCards){
		List<Integer> list = new ArrayList<>();
		String cardType = CardsMap.getCardType(card);
		for(int i=0;i<pengCards.size();i++){
			Integer pengCard = pengCards.get(i);
			String pengCardType = CardsMap.getCardType(pengCard);
			if(cardType.equals(pengCardType)){
				list.add(pengCard);
			}
		}
		return list;
	}
	
	
	/**���ܵ��û��÷�
	 * @param game
	 * @param user
	 */
	public static void modifyUserScoreForGongGang(Game game, User user) {
		user.addScoreForMingGang();
		//������������Ҽ�1��
		List<User> userList = game.getUserList();
		for (int i = 0; i < userList.size(); i++) {
			User u = userList.get(i);
			if(u.getId()!=user.getId()){//�Ǹ����û�����
				u.reduceScoreForMingGang();
			}
		}
	}

	/**����Ҳ����
	 * @param jsonObject
	 * @param session
	 */
	private void fangqi(JSONObject jsonObject, IoSession session) {
		User user = (User) session.getAttribute("user");
		setUserGiveUp(user);//�û���������
		Game game = GameManager.getGameWithRoomNumber(user.getRoomId());
		String beforeTingOrGangDirection = game.getBeforeTingOrGangDirection();
		boolean isCanGiveUp = checkUserIsCanGiveUp(beforeTingOrGangDirection);
		if(!isCanGiveUp){
			NotifyTool.notifyUserErrorMessage(session, "�����Է�����Ҳ�ֲ��������");
			return;
		}
		String direction = getNextDirection(beforeTingOrGangDirection);
		notifyUserDirectionChange(user, direction);
		userDrawCard(game, direction);
	}
	
	/**����û��Ƿ���Է���
	 * @param beforeTingOrGangDirection
	 */
	public boolean checkUserIsCanGiveUp(String beforeTingOrGangDirection){
		if(beforeTingOrGangDirection==null||"".equals(beforeTingOrGangDirection)){
			return false;
		}
		return true;
	}
	
	/**�û���������
	 * @param user
	 */
	private void setUserGiveUp(User user){
		user.setCanGang(false);
		user.setCanPeng(false);
	}
	
	
	/**����
	 * @param jsonObject
	 * @param session
	 */
	private void gang(JSONObject jsonObject, IoSession session) {
		int cardId = jsonObject.getInt("cardId");
		Game game = getGame(session);
		User sessionUser  = (User) session.getAttribute("user");
		User user = getGamingUser(sessionUser.getId(), sessionUser.getRoomId());//game �� oneRoom�� ��user �õ���Ϸ�е����
		boolean isUserCanGang = checkUserIsCanGang(user);
		if(!isUserCanGang){
			notifyUserCanNotPengOrGang(session, 2);
			return;
		}
		user.setUserCanPlay(true);
		Map<String, User> seatMap = game.getSeatMap();
		User u = seatMap.get(user.getDirection());
		List<Integer> cards = u.getCards();
		List<Integer> gangCards = getGangList(cards, cardId);//�ܵ���
		u.userGangCards(gangCards);
		gangCards.add(cardId);
		//��¼��Ҹܵ���
		u.recordUserGangCards(0, gangCards);
		modifyUserScoreForGang(game, u);//�޸���ҵ÷�
		notifyAllUserGang(game, gangCards,user);//֪ͨ���е���Ҹܵ��� 
		//�������ץһ���� 
		userDrawCard(game, user.getDirection());
	}
	
	
	/**�õ���Ϸ�е����
	 * @param userId
	 * @param roomId
	 */
	public static User getGamingUser(int userId,String roomId){
		Game game = GameManager.getGameWithRoomNumber(roomId);
		List<User> userList = game.getRoom().getUserList();
		for(int i=0;i<userList.size();i++){
			User user = userList.get(i);
			if(user.getId()==userId){
				return user; 
			}
		}
		logger.fatal("δ�ҵ���Ϸ�е���................");
		return null;
	}
	

	/**�޸ķŸܺͽӸܵĵ÷�
	 * @param game
	 * @param user ���Ƶ��û�(�Ӹ�)
	 */
	public static void modifyUserScoreForGang(Game game, User user) {
		//�����ߵ÷�
		User jieGangUser = game.getUserInRoomList(user.getId());
		jieGangUser.addScoreForCommonGang();
		//�Ÿ��߼���
		int fangGangUserId = game.getFangGangUser().getId();
		User fangGangUser = game.getUserInRoomList(fangGangUserId);
		fangGangUser.reduceScoreForFangGang();
	}
	
	/**ץ��
	 * @param game
	 * @param direction ��ǰ�ķ���
	 */
	public static void userDrawCard(Game game,String direction){
		List<Integer> remainCards = game.getRemainCards();
		int lastIndex  =remainCards.size()-1;
		//֪ͨ�����ץ������
		Integer removeCard = remainCards.remove(lastIndex);
		System.out.println("��ǰ���ƻ���:"+remainCards);
		User user = game.getSeatMap().get(direction);
		game.setDirec(direction);//�ѵ�ǰ���Ƶķ���ı�
		game.setGameStatus(GAGME_STATUS_OF_CHUPAI);//���óɳ��Ƶ�״̬
		boolean isWin = user.zhuaPai(removeCard);//ץ�� 
		if(!isWin){//û��Ӯ��
			userNotWin(game, remainCards, removeCard, user);
		}else{//Ӯ��
			userWin(game, removeCard, user);
		}
	}

	/**�û�û��Ӯ��
	 * @param game
	 * @param remainCards
	 * @param removeCard
	 * @param user
	 */
	private static void userNotWin(Game game, List<Integer> remainCards, Integer removeCard, User user) {
		if(chouZhuang(remainCards,game)){
			logger.info("��ׯ");
			afterChouZhuang(game, user,removeCard);//��ׯ֮����
			return;
		}
		//�����û��Ƿ���Թ���
		user.setUserCanPlay(true);//���û����Դ���
		List<Integer> gongGangCards = analysisUserIsCanGongGang(removeCard,user);
		if(gongGangCards.size()>0){
			if(user.isAuto()){//���������й�,����֪ͨ���ץ������,Ȼ���Զ��������ܵ�
				notifyUserDrawDirection(removeCard, user,gongGangCards,0);//֪ͨץ�Ƶķ���
				game.setCanGongGangUser(user);
				game.setGongGangCardId(removeCard);
				autoGongGang(game);
			}else{//���û���й�
				noticeUserCanGongGang(game, removeCard, user, gongGangCards);//֪ͨ�û����Թ���
			}
		}else{
			//�������û��Ƿ���԰���
			List<Integer> anGangCards = isUserCanAnGang(user);
			if(anGangCards.size()==4){//���ó��Ƶ�״̬Ϊ����
				game.setAnGangCards(anGangCards);
				game.setCanAnGangUser(user);
				if(user.isAuto()){
					notifyUserDrawDirection(removeCard, user,anGangCards,1);//֪ͨץ�Ƶķ���
					autoAnGang(game);
				}else{
					game.setGameStatus(GAGME_STATUS_OF_ANGANG);//����
					notifyUserDrawDirection(removeCard, user,anGangCards,1);//֪ͨץ�Ƶķ���
				}
			}else{//��Ҳ����԰���
				notifyUserDrawDirection(removeCard, user,null,-1);//֪ͨץ�Ƶķ���
				if(user.isAuto()){
					autoChuPai(game);//�Զ�����
				}
			}
		}
	}
	
	/**�Զ�����
	 * @param game
	 * @param user
	 * @param anGangCards
	 */
	public static void autoAnGang(Game game) {
		User canAnGangUser = game.getCanAnGangUser();
		List<Integer> anGangCards = game.getAnGangCards();
		canAnGangUser.userGangCards(anGangCards);
		logger.info("�Զ�����...����.................:"+Algorithm2.showPai(anGangCards));
		canAnGangUser.recordUserGangCards(1, anGangCards);
		PlayGameService.notifyAllUserAnGang(game, anGangCards,canAnGangUser);//֪ͨ���е���Ҹܵ��� 
		PlayGameService.modifyUserScoreForAnGang(game, canAnGangUser);//�޸���Ұ��ܵ÷�
		//�������ץһ���� 
		PlayGameService.userDrawCard(game, canAnGangUser.getDirection());
	}

	/**�й��Զ�����
	 * @param game
	 */
	public static void autoGongGang(Game game){
		User user = game.getCanGongGangUser();
		Integer cardId = game.getGongGangCardId();
		List<Integer> pengCards = user.getPengCards();//�û�������
		List<Integer> removeList = PlayGameService.getRemoveList(cardId, pengCards);
		for(int i=0;i<removeList.size();i++){
			Integer revomeCard = removeList.get(i);
			pengCards.remove(revomeCard);
		}
		removeList.add(cardId);
		logger.info("�й��Զ�����....................:"+Algorithm2.showPai(removeList));
		//���Լ��������Ƴ����ܵ�������
		user.removeCardFromGongGang(cardId);
		//��¼��Ҹܵ���
		user.recordUserGangCards(2, removeList);
		PlayGameService.notifyAllUserGongGang(game, removeList,user);//֪ͨ���е���Ҹܵ��� 
		PlayGameService.modifyUserScoreForGongGang(game, user);//�޸���ҹ��ܵ÷�
		//�������ץһ���� 
		userDrawCard(game, user.getDirection());
	}
	
	/**֪ͨ�û����Թ���
	 * @param game
	 * @param removeCard
	 * @param user
	 * @param gongGangCards
	 */
	private static void noticeUserCanGongGang(Game game, Integer removeCard, User user, List<Integer> gongGangCards) {
		game.setGameStatus(GAGME_STATUS_OF_GONG_GANG);//����
		game.setGongGangCardId(removeCard);
		game.setCanGongGangUser(user);//���Թ��ܵ����
		notifyUserDrawDirection(removeCard, user,gongGangCards,0);//֪ͨץ�Ƶķ���
	}
	
	
	/**
	 * �ڱ��ֳ�ׯ֮�󣬼����û��ĵ÷֣���Ȼ�����Ľ��㷢�͸��û�
	 */
	public static void afterChouZhuang(Game game,User lastGetCardUser,int removeCard){
		//��Ȼ֪ͨ���ץ������ʲô
		notifyUserDrawDirection(removeCard, lastGetCardUser,null,-1);//֪ͨץ�Ƶķ���
		OneRoom room = game.getRoom();
		JSONObject outJsonObject = new JSONObject();
		outJsonObject.put("type", "hupai");
		outJsonObject.put("description", "������");
		outJsonObject.put("method", "playGame");
		JSONArray userJsonArray = getUserJSONArray(room);
		List<Integer> remainCards = game.getRemainCards();
		outJsonObject.put("remainCards", remainCards);//ʣ�����
		outJsonObject.put("users", userJsonArray);
		NotifyTool.notifyIoSessionList(GameManager.getSessionListWithRoomNumber(lastGetCardUser.getRoomId()+""), outJsonObject);
		initializeUser(lastGetCardUser);//��ʼ���û�������
		setCurrentGameOver(game);//���õ�ǰ����Ϸ����
	}
	
	/**��ׯ
	 * @return
	 */
	public static boolean chouZhuang(List<Integer> remainCards,Game game){
		OneRoom room = game.getRoom();
		int zhama = room.getZhama();
		if(remainCards.size()<=zhama){
			return true;
		}else{
			return false;
		}
	}
	

	/**�û�Ӯ��
	 * @param game 
	 * @param removeCard ���ץ����
	 * @param user Ӯ�Ƶ����
	 */
	private static void userWin(Game game, Integer removeCard, User user) {
		//��Ȼ֪ͨ���ץ������ʲô
		notifyUserDrawDirection(removeCard, user,null,-1);//֪ͨץ�Ƶķ���
		//��������
		List<Integer> zhongMaCards = getZhongMaCard(game, user);
		moidfyUserScoreByZhongMaCards(user, game, zhongMaCards);//�����������޸��û��ĳɼ�
		modifyUserScoreByHuPai(user, game);//���ݺ����޸��û��ĵ÷�
		//��¼�µ�ǰ�ľֵ�ս��
		recordUserScore(game);
		notifyUserWin(user,game,removeCard,zhongMaCards);//֪ͨ�û�Ӯ��
		initializeUser(user);//��ʼ���û�������
		setCurrentGameOver(game);//���õ�ǰ����Ϸ����
	}
	
	
	/**��¼���û��ĵ÷�
	 * @param game
	 */
	public static void recordUserScore(Game game){
		List<User> userList = game.getUserList();
		int roomid = game.getRoom().getId();//�����
		java.util.Date createDate = new java.util.Date();
		for(int i=0;i<userList.size();i++){
			User user = userList.get(i);
			int currentGame = user.getCurrentGame();//��ǰ�ľ���
			int score = user.getCurrentGameSore();
			int userid = user.getId();//�û���ID
			UserScore userScore = new UserScore(userid, roomid,currentGame,score,createDate);
			userScoreDao.saveUserScore(userScore);
		}
	}
	

	/**���õ�ǰ����Ϸ����
	 * @param game
	 */
	private static void setCurrentGameOver(Game game) {
		int alreadyTotalGame = game.getAlreadyTotalGame();
		game.getGameStatusMap().put(alreadyTotalGame, GAME_END);
		game.setGameStatus(GAGME_STATUS_OF_CHUPAI);//��Ϸ��״̬��ɳ���
		game.setStatus(GAGME_STATUS_OF_WAIT_START);//��Ϸ�ȴ�
		//�жϵ�ǰ����Ϸ�Ƿ����
		OneRoom room = game.getRoom();
		int roomTotal = room.getTotal();
		if(alreadyTotalGame==roomTotal){//��Ϸ����
			summarizedAll(game);
			game = null;
		}else{//����ܵ���Ϸ��û�н���,30�����δ׼�������,���Զ�׼�� 
			if(game!=null){
				prepare(game);
			}
		}
	}
	
	/**�Զ�׼����Ϸ,����ܵ���Ϸ��û�н���,30�����δ׼�������,���Զ�׼�� 
	 * @param game
	 */
	private static void prepare(final Game game) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if(game.getStatus()==GAGME_STATUS_OF_WAIT_START){//�ȴ�����
					OneRoom room = game.getRoom();
					List<User> userList = room.getUserList();
					for(int i=0;i<userList.size();i++){
						User user = userList.get(i);
						if(!user.isReady()){
							user.setReady(true);
							//֪ͨ�����ȥ������Ľ���
							JSONObject autoStartJsonObject = new JSONObject();
							autoStartJsonObject.put(method, "autoStart");
							user.getIoSession().write(autoStartJsonObject.toString());//֪ͨ����Զ���ʼ,ȥ������Ľ���
							JSONObject readyJsonObject = UserService.getReadyJsonObject(user);
							NotifyTool.notifyIoSessionList(room.getUserIoSessionList(), readyJsonObject);
						}
					}
					//��ʼ��Ϸ
					UserService userService = new UserService();
					userService.beginGame(room);
				}
			}
		}, TIME_TO_START_GAME);
	}

	/**���㣬�����ɢ 
	 * @param game
	 */
	private static void summarizedAll(Game game) {
		OneRoom room = game.getRoom();
		List<User> userList = room.getUserList();
		JSONObject outJSONObject = getSummarizeJsonObject(userList);
		NotifyTool.notifyIoSessionList(GameManager.getSessionListWithRoomNumber(room.getId()+""), outJSONObject);
		for(int i=0;i<userList.size();i++){
			User user = userList.get(i);
			user.clearAll();//����û����е�����
		}
		//��¼��ҵ��ܳɼ�
		recoredUserScore(outJSONObject, game);
		//���Ƴ���Ϸ�е�map,���Ƴ������е�map �����п�ָ���쳣,˳�򲻿ɵߵ�
		GameManager.removeGameWithRoomNumber(room.getId()+"");
		RoomManager.removeOneRoomByRoomId(room.getId()+"");
	}
	
	
	/**
	 * ��¼��ҵ��ܳɼ�
	 */
	public static void recoredUserScore(JSONObject jsonObject,Game game){
		JSONArray userArray = jsonObject.getJSONArray("userScoreArray");
		int roomNumber = game.getRoom().getRoomNumber();
		Date createDate = new Date();
		for(int i=0;i<userArray.length();i++){
			JSONObject user = userArray.getJSONObject(i);
			SumScore sumScore = new SumScore();
			sumScore.setZhongMaTotal(user.getInt("zhongMa"));
			sumScore.setFinalScore(user.getInt("finallyScore"));
			sumScore.setAnGangTotal(user.getInt("anGang"));
			sumScore.setHuPaiTotal(user.getInt("hupai"));
			sumScore.setAnGangTotal(user.getInt("gongGang"));
			sumScore.setUserid(user.getInt("userId"));
			sumScore.setRoomNumber(roomNumber+"");
			sumScore.setCreateDate(createDate);
			sumScoreDao.saveSumScore(sumScore);//�����û��ķ����
		}
	}
	
	

	/**�õ������jsonObejct
	 * @param userList
	 * @return
	 */
	private static JSONObject getSummarizeJsonObject(List<User> userList) {
		JSONObject outJSONObject = new JSONObject();
		JSONArray userScoreArray = new JSONArray();
		outJSONObject.put(method, "summarizedAll");
		for (int i = 0; i < userList.size(); i++) {
			User user = userList.get(i);
			Map<Integer, Score> scoreMap = user.getScoreMap();
			Iterator<Integer> iterator = scoreMap.keySet().iterator();
			int hupai = 0;
			int gongGang = 0;
			int anGang = 0;
			int zhongMa = 0;
			int finallyScore = 0;
			while(iterator.hasNext()){
				Integer key = iterator.next();
				Score score = scoreMap.get(key);
				int huPaiTotal = score.getHuPaiTotal();
				hupai = hupai+huPaiTotal;
				int gongGangTotal = score.getMingGangtotal();//����Ҳ������
				gongGang = gongGang + gongGangTotal;
				int anGangTotal = score.getAnGangTotal();
				anGang = anGang + anGangTotal;
				int zhongMaTotal = score.getZhongMaTotal();
				zhongMa = zhongMa + zhongMaTotal;
				int finalScore = score.getFinalScore();
				finallyScore = finallyScore + finalScore;
			}
			JSONObject userScoreJSONObject = new JSONObject();
			userScoreJSONObject.put("hupai", hupai);
			userScoreJSONObject.put("gongGang", gongGang);
			userScoreJSONObject.put("anGang", anGang);
			userScoreJSONObject.put("zhongMa", zhongMa);
			userScoreJSONObject.put("finallyScore", finallyScore);
			userScoreJSONObject.put("userId", user.getId());
			userScoreJSONObject.put(direction, user.getDirection());
			userScoreArray.put(userScoreJSONObject);
		}
		outJSONObject.put("userScoreArray", userScoreArray);
		return outJSONObject;
	}
	
	/**���ݺ����޸��û��ĵ÷� 
	 * @param user ���Ƶ��û�
	 * @param game
	 */
	private static void modifyUserScoreByHuPai(User user, Game game) {
		List<User> userList = game.getUserList();
		for(User u:userList){
			if(u.getId()==user.getId()){//ʤ�������
				u.addScoreForHuPai();
				//System.out.println("dd");
			}else{//���Ƶ����
				u.reduceScoreForHuPai();
			}
		}
	}

	/**�޸��û��ĳɼ������������
	 * @param user
	 */
	public static void moidfyUserScoreByZhongMaCards(User winUser,Game game,List<Integer> zhongMaCards){
		int totalZhongMa = getTotalZhongMa(zhongMaCards);
		List<User> userList = game.getUserList();
		for(int i=0;i<userList.size();i++){
			User user = userList.get(i);
			if(user.getId()!=winUser.getId()){//���Ƶ����
				user.reduceScoreForZhongMa(totalZhongMa);
			}else if(user.getId()==winUser.getId()){//���Ƶ����
				user.addScoreForZhongMa(totalZhongMa);
			}
		}
	}

	/**�õ��ܵ������� (1,5,9,����)
	 * @param zhongMaCards
	 */
	private static int getTotalZhongMa(List<Integer> zhongMaCards) {
		int totalZhongMa = 0;
		for(int i=0;i<zhongMaCards.size();i++){
			Integer card = zhongMaCards.get(i);
			int typeInt = Algorithm2.getTypeInt(card);
			String cardType = CardsMap.getCardType(card);
			if(typeInt==1||typeInt==5||typeInt==9||cardType.equals("����")){
				totalZhongMa ++;
			}
		}
		return totalZhongMa;
	}
	
	/**֪ͨ���е���� ���ܵ���
	 * @param game
	 * @param pengCards
	 */
	public static void notifyAllUserAnGang(Game game, List<Integer> gangCards,User user) {
		JSONObject outJsonObject = new JSONObject();
		outJsonObject.put("gangCards", gangCards);
		outJsonObject.put(method, "playGame");
		outJsonObject.put(type, "anGang");
		outJsonObject.put("gangDirection", user.getDirection());
		outJsonObject.put(discription, "��Ҹܵ���");
		List<IoSession> userIoSessionList = game.getRoom().getUserIoSessionList();
		NotifyTool.notifyIoSessionList(userIoSessionList, outJsonObject);
	}
	
	
	
	/**֪ͨ���е���Ҹܵ���
	 * @param game
	 * @param pengCards
	 */
	public static void notifyAllUserGang(Game game, List<Integer> gangCards,User user) {
		JSONObject outJsonObject = new JSONObject();
		outJsonObject.put("gangCards", gangCards);
		outJsonObject.put(method, "playGame");
		outJsonObject.put(type, "gang");
		outJsonObject.put("gangDirection", user.getDirection());
		outJsonObject.put(discription, "��Ҹܵ���");
		List<IoSession> userIoSessionList = game.getRoom().getUserIoSessionList();
		NotifyTool.notifyIoSessionList(userIoSessionList, outJsonObject);
	}
	
	
	/**֪ͨ���е���ҹ��ܵ���
	 * @param game
	 * @param pengCards
	 */
	public static void notifyAllUserGongGang(Game game, List<Integer> gangCards,User user) {
		JSONObject outJsonObject = new JSONObject();
		outJsonObject.put("gangCards", gangCards);
		outJsonObject.put(method, "playGame");
		outJsonObject.put(type, "gongGang");
		outJsonObject.put("gangDirection", user.getDirection());
		outJsonObject.put(discription, "��Ҹܵ���");
		List<IoSession> userIoSessionList = game.getRoom().getUserIoSessionList();
		NotifyTool.notifyIoSessionList(userIoSessionList, outJsonObject);
	}
	
	/**���ƻ���� ,�����ƺ�ı���Ƶ�״̬
	 * @param jsonObject
	 * @param session
	 */
	private void peng(JSONObject jsonObject, IoSession session) {
		int cardId = jsonObject.getInt("cardId");
		Game game = getGame(session);
		User sessionUser = (User) session.getAttribute("user");
		sessionUser.setUserCanPlay(true);
		User gamingUser = getGamingUser(sessionUser.getId(), sessionUser.getRoomId());
		gamingUser.setUserCanPlay(true);
		boolean canPeng = checkUserIsCanPeng(gamingUser);
		if(!canPeng){
			notifyUserCanNotPengOrGang(session, 1);
			return;
		}
		Map<String, User> seatMap = game.getSeatMap();
		User u = seatMap.get(sessionUser.getDirection());
		List<Integer> cards = u.getCards();
		List<Integer> pengList = getPengList(cards, cardId);//�õ��������ļ���
		u.userPengCards(pengList);//�������
		pengList.add(cardId);
		u.addUserPengCards(pengList);//�û�������������
		u.setUserCanPlay(true);//����ҿ��Գ���
		game.setGameStatus(GAGME_STATUS_OF_CHUPAI);//��Ϸ��״̬��Ϊ����
		int hashCode = game.hashCode();
		System.out.println("���ƺ� gameHashCode:"+hashCode);
		game.setDirec(u.getDirection());
		u.setLastChuPaiDate(new Date());
		//֪ͨ���е���ң���Ϸ����ĸı�
		//notifyAllUserDirectionChange(user);
		//֪ͨ���е���ң���������ʲô
		notifyAllUserPeng(game, pengList,u);
	}
	
	
	/**֪ͨ�û�������
	 * @param session
	 * @param type 1 ������������2�������Ը�
	 */
	public void notifyUserCanNotPengOrGang(IoSession session,int type){
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(code, error);
		switch (type) {
		case 1:
			jsonObject.put(discription, "��������");
			break;
		case 2:
			jsonObject.put(discription, "�����Ը�");
			break;
		}
		session.write(jsonObject.toString());
	}
	
	
	
	public boolean checkUserIsCanPeng(User user){
		boolean result =  user.isCanPeng();
		return result;
	}
	
	
	/**����û��Ƿ���Ը�
	 * @param user
	 * @return
	 */
	public boolean checkUserIsCanGang(User user){
		boolean result =  user.isCanGang();
		return result;
	}
	
	
	
	/**�õ��������ļ���
	 * @param cards
	 * @param cardId
	 * @return
	 */
	public static List<Integer> getPengList(List<Integer> cards,int cardId){
		List<Integer> list = new ArrayList<>();
		String cardType = CardsMap.getCardType(cardId);
		int total = 0;
		for(Integer card:cards){
			if(CardsMap.getCardType(card).equals(cardType)&&total<2){//4444 �� ���ڶ���4ֹͣ,��Ϊ����ּ���һ��
				list.add(card);
				total ++ ;
			}
		}
		return list;
	}
	
	
	/**�õ��������ļ���
	 * @param cards
	 * @param cardId
	 * @return
	 */
	public static List<Integer> getGangList(List<Integer> cards,int cardId){
		List<Integer> list = new ArrayList<>();
		String cardType = CardsMap.getCardType(cardId);
		int total = 0;
		for(Integer card:cards){
			if(CardsMap.getCardType(card).equals(cardType)&&total<4){//4444 �� �����ĸ�4ֹͣ
				list.add(card);
				total ++ ;
			}
		}
		return list;
	}
	
	
	
	/**֪ͨ���е������Ϸ����ı�
	 * @param user
	 */
	public void notifyAllUserDirectionChange(User user){
		Game game = getGame(user);
		String nextDirection = user.getDirection();
		JSONObject outJsonObject = new JSONObject();
		outJsonObject.put(method, "playDirection");
		outJsonObject.put("direction", nextDirection);
		outJsonObject.put("description", "���Ƶĵķ���");
		game.setDirec(user.getDirection());//���Ƶķ���ı�
		NotifyTool.notifyIoSessionList(GameManager.getSessionListWithRoomNumber(user.getRoomId()+""), outJsonObject);
	}
	

	/**֪ͨ���е����������
	 * @param game
	 * @param pengCards
	 * @param user 
	 */
	public static void notifyAllUserPeng(Game game, List<Integer> pengCards, User user) {
		JSONObject outJsonObject = new JSONObject();
		outJsonObject.put(method, "playGame");
		outJsonObject.put(type, "peng");
		outJsonObject.put("pengDirction",user.getDirection());
		outJsonObject.put("pengCards", pengCards);
		List<IoSession> userIoSessionList = game.getRoom().getUserIoSessionList();
		NotifyTool.notifyIoSessionList(userIoSessionList, outJsonObject);
	}
	
	/**�õ���Ϸ
	 * @param session
	 * @return
	 */
	public static Game getGame(IoSession session) {
		User user = (User) session.getAttribute("user");
		Game game = getGame(user);
		return game;
	}
	
	/**����,��ո��û���û�г��ƴ���
	 * @param jsonObject
	 * @param session
	 */
	public void chuPai(JSONObject jsonObject, IoSession session){
		int cardId = jsonObject.getInt("cardId");//���Ƶ��ƺ�
		User user = (User) session.getAttribute("user");
		Game game = getGame(user);
		User gamingUser = getGamingUser(user.getId(), user.getRoomId());
		gamingUser.setTotalNotPlay(0);//��ո��û���û�г��ƵĴ���
		int result = checkPower(game, user,cardId);//����û����Ƶ�Ȩ��
		if(result<0){
			notifyUserCanNotChuPai(session,result);
			return;
		}
		Map<String, User> seatMap = game.getSeatMap();
		String direction = gamingUser.getDirection();//�õ���ǰ������
		User u = seatMap.get(direction);
		int removeCardId = u.chuPai(cardId);//����
		if(removeCardId<0){
			return;
		}
		JSONObject outJsonObject = getChuPaiOutJSONObject(cardId, u);
		NotifyTool.notifyIoSessionList(GameManager.getSessionListWithRoomNumber(user.getRoomId()), outJsonObject);//֪ͨ�����û�������� ��ʲô
		analysis(cardId, gamingUser, game);//������������һ���˳��ƻ����ܹ����ƺ͸���
	}

	
	
	/**���û���ʾ������Ϣ,�����Գ���
	 * @param session
	 * @param result 
	 */
	public void notifyUserCanNotChuPai(IoSession session, int result){
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(code, "error");
		if(result==-1){
			jsonObject.put(discription, "���������");
		}else if(result==-2){
			jsonObject.put(discription, "�����Ʋ�����");
		}
		session.write(jsonObject.toString());
	}
	
	
	/**����û���Ȩ��
	 * @param cardId 
	 * @return -1 ���ó��� -2�Ʋ�����
	 */
	public int checkPower(Game game,User user, int cardId){
		int result = 0;
		String direc = game.getDirec();
		if(!direc.equals(user.getDirection())){
			return -1;
		}
		List<Integer> cards = game.getSeatMap().get(user.getDirection()).getCards();
		int index = MathUtil.binarySearch(cardId, cards);
		if(index<0){
			return -2;
		}
		return result;
	}
	
	/**�õ����Ƶ�json����
	 * @param cardId
	 * @param u
	 * @return
	 */
	public static JSONObject getChuPaiOutJSONObject(int cardId, User u) {
		JSONObject outJsonObject = new JSONObject();
		outJsonObject.put("cardId", cardId);//�������
		outJsonObject.put("direction", u.getDirection());//���Ƶ��˵�����
		outJsonObject.put("userId", u.getId());
		outJsonObject.put("type", "chupai");
		outJsonObject.put("method", "playGame");
		outJsonObject.put("discription", "����");
		return outJsonObject;
	}

	/**������һ�����Ƿ����ץ�ƻ����,Ȼ���һ������
	 * @param cardId �������
	 * @param user ��ǰ����
	 * @param game
	 * @param seatMap
	 * @param direction ���Ƶķ���
	 */
	public static void analysis(int cardId, User user, Game game) {
		Map<String, User> seatMap = game.getSeatMap();
		//����������ƻ���Ƶ���
		User canPengOrGangUser = getPengOrGangCardUser(cardId,seatMap,user.getId());
		if(canPengOrGangUser!=null){
			//�������ƻ����,���û�Ҳû���йܲ�֪ͨ��
			boolean auto = canPengOrGangUser.isAuto();//�û��Ƿ��й�
			if(auto){//����й��Զ���
				List<Integer> cards = canPengOrGangUser.getCards();
				boolean isTing = false;
				Integer myGrabCard = canPengOrGangUser.getMyGrabCard();
				if(myGrabCard!=null){
					logger.info("���û��йܵ�ʱ���⵽�û����ƻ����:"+canPengOrGangUser);
					isTing = canPengOrGangUser.isUserTingPaiOfPengOrGang(cards);//�û��Ƿ�����
				}
				if(isTing){//������û��Ѿ�����
					logger.info("���ƻ���Ƶ�ʱ��ֱ��Խ�����û�"+canPengOrGangUser);
					nextUserDrawCards(cardId, user, game);
					return;
				}
				notifyUserCanPengOrGang(cardId, user, game, canPengOrGangUser);
				autoPengOrGang(canPengOrGangUser,game);
			}else{
				notifyUserCanPengOrGang(cardId, user, game, canPengOrGangUser);
			}
		}else{//��һ����ץ��
			nextUserDrawCards(cardId, user, game);
		}
	}

	/**��һ���û�ץ��
	 * @param cardId
	 * @param user
	 * @param game
	 */
	private static void nextUserDrawCards(int cardId, User user, Game game) {
		int hashCode = user.hashCode();
		user.addMyPlays(cardId);
		String nextDirection = getNextDirection(user.getDirection());
		notifyUserDirectionChange(user, nextDirection);//֪ͨ�û����Ƶķ���ı�
		userDrawCard(game, nextDirection);//�û�ץ��
	}
	
	/**�Զ����ƺ͸���
	 * @param canPengOrGangUser
	 */
	public static void autoPengOrGang(User canPengOrGangUser,Game game) {
		if(canPengOrGangUser.getPengOrGang()==1){//��������
			List<Integer> pengList = canPengOrGangUser.getPengOrGangList();
			canPengOrGangUser.userPengCards(pengList);
			canPengOrGangUser.addUserPengCards(pengList);//�û�������������
			notifyAllUserPeng(game, pengList,canPengOrGangUser);//֪ͨ����
			game.setDirec(canPengOrGangUser.getDirection());//���¸ı���Ϸ�ķ���
			//���ƺ���Ϸ��״̬��Ϊ����
			game.setGameStatus(GAGME_STATUS_OF_CHUPAI);
			canPengOrGangUser.setLastChuPaiDate(new Date());
			if(canPengOrGangUser.isAuto()){
				autoChuPai(game);//�Զ�����
			}
		}else if(canPengOrGangUser.getPengOrGang()==2){//���Ը���
			List<Integer> gangCards = canPengOrGangUser.getPengOrGangList();
			canPengOrGangUser.userGangCards(gangCards);
			//��¼��Ҹܵ���
			canPengOrGangUser.recordUserGangCards(0, gangCards);
			modifyUserScoreForGang(game, canPengOrGangUser);//�޸���ҵ÷�
			PlayGameService.notifyAllUserGang(game, gangCards,canPengOrGangUser);//֪ͨ���е���Ҹܵ��� 
			//�������ץһ���� 
			PlayGameService.userDrawCard(game, canPengOrGangUser.getDirection());
		}
	}
	
	/**
	 * FIXME ע�����������Ϊ�ˣ�ͬһʱ�����
	 * �Զ�����
	 */
	public static synchronized void autoChuPai(Game game){
		String direc = game.getDirec();
		Map<String, User> seatMap = game.getSeatMap();
		User user = seatMap.get(direc);
		user.setUserCanPlay(true);
		int cardId = user.autoChuPai();//�Զ�������
		if(cardId<0){
			logger.info("������ͬһʱ�������");
			return;
		}
		try {
			//FIXME Ϊ�˳��Ʋ�̫�죬�߳����� 1����
			Thread.currentThread().sleep(1000);
			logger.info("���Ƶ�ʱ����ͣһ��");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.info("�й��Զ�����....................:"+cardId+" "+CardsMap.getCardType(cardId));
		JSONObject outJsonObject = PlayGameService.getChuPaiOutJSONObject(cardId, user);
		NotifyTool.notifyIoSessionList(GameManager.getSessionListWithRoomNumber(user.getRoomId()), outJsonObject);//֪ͨ�����û�������� ��ʲô
		analysis(cardId, user, game);
	}

	/**֪ͨ�û����������߸���,������������Ϸ��״̬,���ƻ��߸���
	 * @param cardId
	 * @param user ������Ը��ƾ��ǷŸܵ��û�
	 * @param game
	 * @param canPengOrGangUser
	 */
	private static void notifyUserCanPengOrGang(int cardId, User user, Game game, User canPengOrGangUser) {
		canPengOrGangUser.setLastChuPaiDate(new Date());//��¼�����������͸ܵ�ʱ��
		//FIXME �������Ը�����ʾ�����ԣ���Ҫ��һ��
		User gamingUser = getGamingUser(user.getId(), game.getRoom().getId()+"");
		if(canPengOrGangUser.getPengOrGang()==1){//��������
			canPengOrGangUser.setCanPeng(true);
			game.setAutoPengCardId(cardId);//���������ƺ�
			game.setCanPengUser(canPengOrGangUser);//�������Ƶ��û�
			game.setGameStatus(GAGME_STATUS_OF_PENGPAI);//����
			gamingUser.setCanPeng(true);
		}else if(canPengOrGangUser.getPengOrGang()==2){//���Ը���
			game.setAutoGangCardId(cardId);
			canPengOrGangUser.setCanPeng(true);
			canPengOrGangUser.setCanGang(true);
			gamingUser.setCanPeng(true);
			gamingUser.setCanGang(true);
			game.setFangGangUser(user);
			game.setCanGangUser(canPengOrGangUser);//���Ը��Ƶ��û�
			game.setGameStatus(GAGME_STATUS_OF_GANGPAI);//����
		}
		notifyUserCanPengOrGang(cardId, game, canPengOrGangUser);
	}

	/**֪ͨ�û����Ƶķ���ı�
	 * @param user
	 * @param nextDirection
	 */
	private static void notifyUserDirectionChange(User user,
			String nextDirection) {
		JSONObject outJsonObject = new JSONObject();
		outJsonObject.put(method, "playDirection");
		outJsonObject.put("direction", nextDirection);
		outJsonObject.put("description", "���Ƶĵķ���");
		NotifyTool.notifyIoSessionList(GameManager.getSessionListWithRoomNumber(user.getRoomId()+""), outJsonObject);
	}

	/**֪ͨ�û����������
	 * @param cardId
	 * @param game
	 * @param canPengOrGangUser
	 */
	private static void notifyUserCanPengOrGang(int cardId, Game game,
			User canPengOrGangUser) {
		JSONObject outJsonObject = new JSONObject();
		outJsonObject.put("cardId", cardId);
		outJsonObject.put("pengOrGangUser", canPengOrGangUser.getDirection());
		outJsonObject.put(discription, "���û��������ƻ����");
		outJsonObject.put(method, "testType");//���Ը�
		int pengOrGang = canPengOrGangUser.getPengOrGang();
		if(pengOrGang==1){
			outJsonObject.put(type, "canPeng");//������
		}else if(pengOrGang==2){
			outJsonObject.put(type, "canGang");//���Ը�
		}
		String nowDirection = game.getDirec();
		game.setBeforeTingOrGangDirection(nowDirection);//����ԭ���ķ���
		game.setDirec(canPengOrGangUser.getDirection());//���Ƶķ���ı�
		canPengOrGangUser.getIoSession().write(outJsonObject);//֪ͨ���û��������ƻ����
	}
	

	
	/**ץ��
	 * @param game
	 * @param seatMap
	 * @param direction
	 * @return
	 *//*
	public static User userDrawCard(Game game, Map<String, User> seatMap,
			String direction) {
		List<Integer> remainCards = game.getRemainCards();
		Integer removeCard = remainCards.remove(0);
		System.out.println("��ǰ���ƻ���:"+remainCards);
		String nextDirection = getNextDirection(direction);
		User user = seatMap.get(nextDirection);//
		game.setDirec(user.getDirection());//�ѵ�ǰ���Ƶķ���ı�
		boolean isWin = user.zhuaPai(removeCard);//ץ�� 
		user.setMyGrabCard(removeCard);//��ǰ�û�ץ������ 
		int zhama = game.getRoom().getZhama();
		if(isWin){ 
			afterUserWin(game, user,removeCard);
		}else if(zhama==remainCards.size()){//������ֹͣ
			game.setGameOver(true);
			notifyUserNoUserWin(game);
			restartGame(game, user);
		}else{
			//�����û��Ƿ���Թ���
			List<Integer> gongGangCards = analysisUserIsCanGongGang(removeCard,user);
			if(gongGangCards.size()>0){
				notifyUserDrawDirection(removeCard, user,gongGangCards,GONG_GANG);//֪ͨץ�Ƶķ���
			}else{
				//�������û��Ƿ���԰���
				List<Integer> anGangCards = isUserCanAnGang(user);
				notifyUserDrawDirection(removeCard, user,anGangCards,AN_GANG);//֪ͨץ�Ƶķ���
			}
		}
		return user;
	}*/
	
	
	/**�����û��Ƿ���԰���
	 * @return
	 */
	public static List<Integer> isUserCanAnGang(User user){
		List<Integer> cards = user.getCards();
		String type = CardsMap.getCardType(cards.get(0));
		int total = 0;
		int compareCard = cards.get(0);
		List<Integer> anGangCards = new ArrayList<>();
		for(int i=1;i<cards.size();i++){
			Integer card = cards.get(i);
			String currentType = CardsMap.getCardType(card);
			if(type.equals(currentType)){
				total++;
				anGangCards.add(card);
				if(total==3){
					anGangCards.add(compareCard);
					break;
				}
			}else{
				type = currentType;
				total = 0;//��������
				anGangCards = new ArrayList<>();
				compareCard = card;
			}
		}
		
		if(anGangCards.size()!=4){
			return new ArrayList<Integer>();
		}
		
		Collections.sort(anGangCards);
		return anGangCards;
	}
	

	/**�����û��Ƿ���Թ���
	 * @param removeCard
	 * @param user
	 */
	private static List<Integer> analysisUserIsCanGongGang(Integer removeCard, User user) {
		List<Integer> pengCards = user.getPengCards();//��������
		String type = CardsMap.getCardType(removeCard);
		int total = 0;
		List<Integer> gongGangCards =  new ArrayList<>();
		for(int i=0;i<pengCards.size();i++){
			Integer card = pengCards.get(i);
			String cardType = CardsMap.getCardType(card);
			if(type.equals(cardType)){
				gongGangCards.add(card);
				total ++;
			}
			if(total==3){
				break;
			}
		}
		return gongGangCards;
	}

	/**��ʾû���û�Ӯ��
	 * @param game
	 */
	private static void notifyUserNoUserWin(Game game) {
		List<User> userList = game.getUserList();
		JSONObject outJsonObject = new JSONObject();
		List<Integer> remainCards = game.getRemainCards();
		outJsonObject.put("remainCards", remainCards);//ʣ�����
		JSONArray jsonArray = new JSONArray();
		for(int i=0;i<userList.size();i++){
			User u = userList.get(i);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(direction, u.getDirection());
			jsonObject.put("cards", u.getCards());
			jsonObject.put("userId", u.getId());
			jsonArray.put(jsonObject);
		}
		outJsonObject.put("users", jsonArray);
		List<IoSession> ioSessionList = game.getIoSessionList();
		NotifyTool.notifyIoSessionList(ioSessionList, outJsonObject);
	}

	/**��Ϸ����֮��
	 * @param game
	 * @param user
	 */
	private static void afterUserWin(Game game, User user,int huCardId) {
//		try {
//			Thread.currentThread().sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		game.setGameOver(true);//��Ϸ�����������Ӯ����
		//��������
		List<Integer> zhongMaCards = getZhongMaCard(game, user);
		moidfyUserScoreByZhongMaCards(user, game, zhongMaCards);//�����������޸��û��ĳɼ�
		notifyUserWin(user,game,huCardId,zhongMaCards);//֪ͨ�û�Ӯ��
		restartGame(game, user);
	}
	
	/**���¿�ʼ��Ϸ
	 * @param game
	 * @param bankUser ׯ��
	 */
	public static void restartGame(Game game,User bankUser){
		List<User> userList = game.getUserList();
		for(User u:userList){
			u.setCanGang(false);
			u.setCanPeng(false);
			u.setReady(false);//ȡ��׼��
			u.setPengCards(null);//����ȥ����
			u.setGangCards(null);//�ܳ�ȥ����
			u.setBanker(false);
		}
		OneRoom room = game.getRoom();
		room.setUse(false);//���÷������ʹ��
		bankUser.setBanker(true);
	}
	
	/**֪ͨ�û�Ӯ��
	 * @param direction ���Ƶķ���
	 * @param user  Ӯ�Ƶ����
	 * @param game 
	 * @param zhongMaCards 
	 */
	private static void notifyUserWin(User user, Game game,int huCardId, List<Integer> zhongMaCards) {
		OneRoom room = game.getRoom();
		JSONObject outJsonObject = new JSONObject();
		outJsonObject.put("type", "hupai");
		outJsonObject.put("userId", user.getId());
		outJsonObject.put("description", "������");
		outJsonObject.put("method", "playGame");
		outJsonObject.put("huCardId", huCardId);
		outJsonObject.put("huCards", user.getCards());
		outJsonObject.put("hupaiDirection",user.getDirection());
		outJsonObject.put("zhongMaCards", zhongMaCards);//�������
		JSONArray userJsonArray = getUserJSONArray(room);
		List<Integer> remainCards = game.getRemainCards();
		outJsonObject.put("remainCards", remainCards);//ʣ�����
		outJsonObject.put("users", userJsonArray);
		NotifyTool.notifyIoSessionList(GameManager.getSessionListWithRoomNumber(user.getRoomId()+""), outJsonObject);
		//afterUserWin(user);//���û�Ӯ���Ժ����
	}

	/**�õ���ǰ�������û�����Ϣ
	 * @param room
	 * @return
	 */
	private static JSONArray getUserJSONArray(OneRoom room) {
		JSONArray jsonArray = new JSONArray();
		List<User> userList = room.getUserList();
		for(int i=0;i<userList.size();i++){
			User u = userList.get(i);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(direction, u.getDirection());
			jsonObject.put("pengCards", u.getPengCards());//��������
			List<GangCard> gangCards = u.getGangCards();
			if(gangCards.size()>0){
				JSONArray gangCardArray = getJGangCardArray(gangCards);
				jsonObject.put("gangCardArray", gangCardArray);
			}
			jsonObject.put("score", u.getCurrentGameSore());//�õ����ֵĵ÷�
			jsonObject.put("cards", u.getCards());//ʣ�����
			jsonObject.put("userId", u.getId());
			int playerScoreByAdd = UserService.getUserCurrentGameScore(u);//�û���ǰ�ķ���
			jsonObject.put("playerScoreByAdd", playerScoreByAdd);
			jsonArray.put(jsonObject);
		}
		return jsonArray;
	}

	
	/**�õ��������
	 * @param game
	 * @param huPaiUser
	 */
	public static List<Integer> getZhongMaCard(Game game,User huPaiUser){
		List<Integer> remainCards = game.getRemainCards();
		OneRoom room = game.getRoom();
		int zhama = room.getZhama();//������
		List<Integer> cards = huPaiUser.getCards();
		boolean haveHongZhong = isHaveHongZhong(cards);
		if(!haveHongZhong){//��������˵�����û�к�������һ����
			zhama = zhama+1;
		}
		List<Integer> zhongMaCardList = removeAndGetZhongMaCardList(remainCards, zhama);
		return zhongMaCardList;
	}
	
	/**
	 * @param remainCards ʣ�����
	 * @param zhama ����ĸ���
	 * @return
	 */
	public static List<Integer> removeAndGetZhongMaCardList(List<Integer> remainCards,int zhama){
		int size = remainCards.size();
		if(size>zhama){//Ψ�ֶ��ϵ��Ʋ���
			size = zhama;
		}
		List<Integer> zhongMaList =  new ArrayList<>();//�������
		for(int i=0;i<size;i++){
			zhongMaList.add(remainCards.get(i));
		}
		//��ʣ��������Ƴ�
		for(int i=0;i<zhongMaList.size();i++){
			Integer removeCard = zhongMaList.get(i);
			remainCards.remove(removeCard);
		}
		return zhongMaList;
	}
	
	
	/**�鿴�����Ƿ��к���
	 * @param cards 
	 * @return
	 */
	public static boolean isHaveHongZhong(List<Integer> cards){
		//�����������Ϊ�����Ǻ���
		for(int i=cards.size()-1;i>=0;i--){
			Integer card = cards.get(i);
			if(CardsMap.getCardType(card).equals("����")){
				return true;
			}
		}
		return false;
	}
	
	
	
	/**�õ����Ƶ�����json����
	 * @param gangCards
	 * @return
	 */
	private static JSONArray getJGangCardArray(List<GangCard> gangCards) {
		JSONArray gangCardArray =  new JSONArray();
		for(int j=0;j<gangCards.size();j++){
			JSONObject gangCardJSONObject = new JSONObject();
			GangCard gangCard = gangCards.get(j);
			int gangType = gangCard.getType();
			String sGangType = "";
			if(gangType == 0){
				sGangType = "jieGang";
			}else if(gangType == 1){
				sGangType = "anGang";
			}else if(gangType == 2){
				sGangType = "gongGang";
			}
			gangCardJSONObject.put("gangType",sGangType);
			gangCardJSONObject.put("gangCard", gangCard.getCards());
			gangCardArray.put(gangCardJSONObject);
		}
		return gangCardArray;
	}

	
	/**���û�Ӯ���Ժ�,��ʼ���û���һЩ����
	 * @param user,ʤ������ң����������ץ�Ƶ����
	 */
	private static void initializeUser(User user) {
		Game game = getGame(user);
		int alreadyTotalGame = game.getAlreadyTotalGame();
		int totalGame = game.getTotalGame();
		if(alreadyTotalGame+1<totalGame){
			setNewBank(user,game);//�����µ�ׯ��
			List<User> userList = game.getUserList();
			for(int i=0;i<userList.size();i++){
				User u = userList.get(i);
				List<Integer> myPlays = new ArrayList<>();//��ȥ����
				u.setMyPlays(myPlays);
				List<Integer> pengCards = new ArrayList<>();//������
				u.setPengCards(pengCards);
				List<GangCard> gangCards = new ArrayList<>();//�ܵ���
				u.setGangCards(gangCards);
			}
		}
		game.setAlreadyTotalGame(alreadyTotalGame+1);//�����Ѿ������Ϸ���� 
		game.setDirec("");
		game.setStep(0);
		game.setBeforeTingOrGangDirection("");
	}


	/**�����µ�ׯ��
	 * @param user
	 */
	private static void setNewBank(User user,Game game) {
		OneRoom room = game.getRoom();
		List<User> userList = room.getUserList();
		for(User u:userList){
			if(u.getId()!=user.getId()){
				u.setBanker(false);
			}
		}
		user.setBanker(true);
	}

	/**֪ͨץ�Ƶķ���
	 * @param removeCard
	 * @param nextUser ץ�Ƶ����
	 * @param cards ���ܻ��߹��ܵ���
	 * @param type 0���Ӹ� 1������ 2������
	 */
	private static void notifyUserDrawDirection(Integer removeCard, User nextUser, List<Integer> cards,int type) {
		Game game = getGame(nextUser);
		List<User> userList = getUserListWithGame(game);
		for(User  user : userList){
			JSONObject outJsonObject = new JSONObject();
			//֪ͨ��ץ������
			outJsonObject.put("description", "ץ�Ƶķ���");
			outJsonObject.put("type", "zhuapai");
			outJsonObject.put(method, "playGame");
			outJsonObject.put("direction", nextUser.getDirection());
			if(user.getId()==nextUser.getId()){
				outJsonObject.put("getCardId", removeCard);
				if(cards!=null&&cards.size()>0){//����ҿ�������
					outJsonObject.put("isCanGangType", type);
					outJsonObject.put("cards", cards);
				}
			}else{
				outJsonObject.put("getCardId", 1000);//����һ�������ڵ���
			}
			NotifyTool.notify(user.getIoSession(), outJsonObject);
			System.out.println("֪ͨ����ң�"+user.getId()+" "+user.getNickName());
			//user.getIoSession().write(outJsonObject.toString());
		}
		//NotifyTool.notify(nextUser.getIoSession(), outJsonObject);
	}

	
	private static List<User> getUserListWithGame(Game game){
		OneRoom room = game.getRoom();
		List<User> userList = room.getUserList();
		return userList;
	}
	
	/**�õ���Ϸ
	 * @param user
	 * @return
	 */
	public static Game getGame(User user) {
		Map<String, Game> gameMap = GameManager.getGameMap();
		String roomId = user.getRoomId();
		Game game = gameMap.get(roomId);
		return game;
	}

	/**������������ƻ���Ƶ���
	 * @param cardId ��һ���˴������
	 * @param seatMap
	 * @param nowUserId
	 * @return
	 */
	public static User getPengOrGangCardUser(int cardId,Map<String, User> seatMap,int nowUserId){
		Iterator<String> it = seatMap.keySet().iterator();
		String type = CardsMap.getCardType(cardId);
		while(it.hasNext()){
			String next = it.next();
			User u = seatMap.get(next);
			if(u.getId()!=nowUserId){//�������������
				List<Integer> cards = u.getCards();
				int total = 0;
				List<Integer> pengOrGangList = new ArrayList<>();
				for(Integer cId:cards){
					if(CardsMap.getCardType(cId).equals(type)&&cId<108){//���Ǻ���
						total ++;
						pengOrGangList.add(cId);
					}
				}
				if(total>=2){
					pengOrGangList.add(cardId);
					u.setPengOrGangList(pengOrGangList);
					if(total==2){
						u.setPengOrGang(1);
					}else if(total == 3){
						u.setPengOrGang(2);
					}
					return u;
				}
			}
		}
		return null;
	}
	
	
	/**�õ���һ����ҵķ���
	 * @param nowDirection ���ڵķ���
	 * @return
	 */
	public static String getNextDirection(String nowDirection){
		String direction = "";//����
		switch (nowDirection) {
			case "east":
				direction = "north";
				break;
			case "north":
				direction = "west";
				break;
			case "west":
				direction = "south";
				break;
			case "south":
				direction = "east";
				break;
			default:
				break;
		}
		return direction;
	}
}