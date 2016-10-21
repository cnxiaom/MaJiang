package com.zxz.domain;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Scrollable;

import org.apache.log4j.Logger;
import org.apache.mina.common.IoSession;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.zxz.controller.GameManager;
import com.zxz.controller.RoomManager;
import com.zxz.controller.StartServer;
import com.zxz.service.UserService;
import com.zxz.utils.Algorithm2;
import com.zxz.utils.CardsMap;
import com.zxz.utils.ScoreType;

public class User {
	
	private static Logger logger = Logger.getLogger(User.class);  
	int id;
	/******************΢�ŵ����ж���**********************/
	String openid;//��ͨ�û��ı�ʶ���Ե�ǰ�������ʺ�Ψһ
	String nickName;//�ǳ� 
	private String sex;//��ͨ�û��Ա�1Ϊ���ԣ�2ΪŮ��
	String province	;//��ͨ�û�����������д��ʡ��
	String city	;//��ͨ�û�����������д�ĳ���
	String country	;//���ң����й�ΪCN
	String unionid;//�û�ͳһ��ʶ�����һ��΢�ſ���ƽ̨�ʺ��µ�Ӧ�ã�ͬһ�û���unionid��Ψһ�ġ�
	String headimgurl;//ͷ��
	String refreshToken;//refresh_tokenӵ�нϳ�����Ч�ڣ�30�죩����refresh_tokenʧЧ�ĺ���Ҫ�û�������Ȩ��
	/*******************΢�ŵ����ж���*********************/
	Date createDate;//ע��ʱ��
	int roomCard;//����������
	String userName;
	String password;
	String roomId;//����� 
	boolean ready;//�Ƿ�׼��
	IoSession ioSession;
	private List<Integer> cards;//�Լ����е���
	String direction;//�� �� �� ��
	boolean banker;//ׯ
	boolean isAuto = false;//�û��Ƿ��й�
	int pengOrGang=0;// 0 �������ƺ͸� 1 ������ 2 ���Ը� 
	private List<Integer> myPlays = new ArrayList<>();//�Ѿ����ĵ���
	private List<Integer> pengCards = new ArrayList<>();//��������
	List<GangCard> gangCards = new ArrayList<>();//�ܳ�ȥ����
	private Integer myGrabCard;//��ץ������
	boolean isCanPeng = false;//�ǲ��ǿ�����
	boolean isCanGang = false;//�ǲ��ǿ��Ը�
	/**
	 * �÷ּ�¼�ļ���,key:�ڼ��֣�Score �þֵĵ÷�
	 */
	private Map<Integer, Score> scoreMap =  new LinkedHashMap<>();
	
	/**
	 * ��ǰ���ڵľ���
	 */
	int currentGame;
	
	Date lastChuPaiDate;//���һ�εĳ���ʱ��
	List<Integer> pengOrGangList;//���������߸ܵ���
	
	/**
	 *  ˵��:�˱�����Ϊ�ˣ�������û��йܵ�ʱ���û����ƺ�ϵͳ�йܵ�ʱ��һ����ƣ���ɴ������Ƶ����
	 */
	boolean isUserCanPlay = false;//false
	int recommendId;//�Ƽ���id
	
	
	private int totalNotPlay = 0;//�û�û�г��ƵĴ���
	
	
	public int getTotalNotPlay() {
		return totalNotPlay;
	}


	public void setTotalNotPlay(int totalNotPlay) {
		this.totalNotPlay = totalNotPlay;
	}


	/**
	 * ������е�����
	 */
	public void clearAll(){
		this.roomId = null;
		this.isAuto = false;
		this.currentGame = 0;
		this.setPengCards(new ArrayList<Integer>());
		List<GangCard> gangCards = new ArrayList<>();//�ܵ���
		this.setGangCards(gangCards);
		this.myGrabCard = -100;//���ҵ�����
		this.isCanPeng = false;//�ǲ��ǿ�����
		this.isCanGang = false;//�ǲ��ǿ��Ը�
		this.banker = false;//�Ƿ��Ƿ���
		this.currentGame = 0;//��ǰ�ľ�������Ϊ0
		this.myPlays = new ArrayList<Integer>();//����������
		this.setScoreMap(new LinkedHashMap<Integer,Score>());//��ճɼ�
		this.totalNotPlay = 0;//�û�û�г��ƵĴ�������
		logger.info("��պ�ǰ�û��Ѿ��������:" + this.getMyPlays());
	}
	
	
	public int getRecommendId() {
		return recommendId;
	}

	public void setRecommendId(int recommendId) {
		this.recommendId = recommendId;
	}

	public boolean isUserCanPlay() {
		return isUserCanPlay;
	}

	public void setUserCanPlay(boolean isUserCanPlay) {
		this.isUserCanPlay = isUserCanPlay;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getHeadimgurl() {
		return headimgurl;
	}

	public void setHeadimgurl(String headimgurl) {
		this.headimgurl = headimgurl;
	}

	public String getOpenid() {
		return openid;
	}

	public void setOpenid(String openid) {
		this.openid = openid;
	}

	public String getSex() {
		if(sex==null){
			return "0";//��
		}else if(sex.equals("2")){
			return "1";//Ů
		}else if(sex.equals("1")){
			return "0";//��
		}
		return sex==null?"0":sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getUnionid() {
		return unionid;
	}

	public void setUnionid(String unionid) {
		this.unionid = unionid;
	}

	public List<Integer> getPengOrGangList() {
		return pengOrGangList;
	}

	public void setPengOrGangList(List<Integer> pengOrGangList) {
		this.pengOrGangList = pengOrGangList;
	}

	public Date getLastChuPaiDate() {
		return lastChuPaiDate;
	}

	public void setLastChuPaiDate(Date lastChuPaiDate) {
		this.lastChuPaiDate = lastChuPaiDate;
	}

	public List<Integer> getPengCards() {
		return pengCards;
	}

	public void setPengCards(List<Integer> pengCards) {
		this.pengCards = pengCards;
	}

	public List<GangCard> getGangCards() {
		return gangCards;
	}

	public void setGangCards(List<GangCard> gangCards) {
		this.gangCards = gangCards;
	}

	public int getCurrentGame() {
		return currentGame;
	}

	public void setCurrentGame(int currentGame) {
		this.currentGame = currentGame;
	}

	public Map<Integer, Score> getScoreMap() {
		return scoreMap;
	}

	public void setScoreMap(Map<Integer, Score> scoreMap) {
		this.scoreMap = scoreMap;
	}

	public int getRoomCard() {
		return roomCard;
	}

	public void setRoomCard(int roomCard) {
		this.roomCard = roomCard;
	}

	public boolean isCanPeng() {
		return isCanPeng;
	}

	public void setCanPeng(boolean isCanPeng) {
		this.isCanPeng = isCanPeng;
	}

	public boolean isCanGang() {
		return isCanGang;
	}

	public void setCanGang(boolean isCanGang) {
		this.isCanGang = isCanGang;
	}

	public Integer getMyGrabCard() {
		return myGrabCard;
	}

	public void setMyGrabCard(Integer myGrabCard) {
		this.myGrabCard = myGrabCard;
	}

	public List<Integer> getMyPlays() {
		return myPlays;
	}

	public void setMyPlays(List<Integer> myPlays) {
		this.myPlays = myPlays;
	}



	public int getPengOrGang() {
		return pengOrGang;
	}

	public void setPengOrGang(int pengOrGang) {
		this.pengOrGang = pengOrGang;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public boolean isAuto() {
		return isAuto;
	}

	public void setAuto(boolean isAuto) {
		this.isAuto = isAuto;
	}

	public User() {
	}
	
	public boolean isBanker() {
		return banker;
	}
	public void setBanker(boolean banker) {
		this.banker = banker;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public List<Integer> getCards() {
		return cards;
	}
	public void setCards(List<Integer> cards) {
		this.cards = cards;
	}
	public IoSession getIoSession() {
		return ioSession;
	}
	public void setIoSession(IoSession ioSession) {
		this.ioSession = ioSession;
	}
	public boolean isReady() {
		return ready;
	}
	public void setReady(boolean ready) {
		this.ready = ready;
	}
	public String getRoomId() {
		return roomId;
	}
	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getUserName() {
		return getNickName();
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public User(String userName, String password) {
		super();
		this.userName = userName;
		this.password = password;
	}
	
	
	/**�õ��û�����ϸ��Ϣ,����,�Ƿ��йܡ��Ѿ�������ơ����е��ơ������ơ��ܵ��ơ��Լ����� 
	 * @param showMyCards �Ƿ���ʾ�����е��ƣ�true��ʾ,false����ʾ
	 * @return
	 */
	public JSONObject getMyInfo(boolean showMyCards){
		JSONObject infoJSONObject = new JSONObject();
		infoJSONObject.put("isAuto", isAuto);//�Ƿ��й�
		infoJSONObject.put("myPlays", myPlays);//�Ѿ��������
		if(showMyCards){
			infoJSONObject.put("cards", cards);//���е���
		}
		infoJSONObject.put("pengCards", pengCards);
		JSONArray gangCardArray = new JSONArray();//���Ƶ�����
		for(int i=0;i<gangCards.size();i++){
			GangCard gangCard = gangCards.get(i);
			JSONObject gangJsonObject = new JSONObject();
			int type = gangCard.getType();
			if (type==0) {
				gangJsonObject.put("gangType", "jieGang");
			}else if(type == 1){
				gangJsonObject.put("gangType", "anGang");
			}else if(type == 2){
				gangJsonObject.put("gangType", "gongGang");
			}
			List<Integer> gangCards = gangCard.getCards();
			gangJsonObject.put("gangCards", gangCards);
			gangCardArray.put(gangJsonObject);
		}
		infoJSONObject.put("userid",id);
		infoJSONObject.put("headimgurl", headimgurl);
		infoJSONObject.put("userName", getUserName());
		infoJSONObject.put("gangCardArray", gangCardArray);
		infoJSONObject.put("direction", direction);
		infoJSONObject.put("ready", ready);
		infoJSONObject.put("ip", getIp());//ip��ַ
		int playerScoreByAdd = UserService.getUserCurrentGameScore(this);//��ǰ�ķ���
		infoJSONObject.put("playerScoreByAdd", playerScoreByAdd);
		infoJSONObject.put("sex", getSex());
		return infoJSONObject;
	}
	
	
	
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("  ");
		for(int i=0;i<cards.size();i++){
			Integer card = cards.get(i);
			String cardType = CardsMap.getCardType(card);
			sb.append(cardType+"");
		}
//		Score score = scoreMap.get(currentGame);
//		return "���е���:"+sb.toString()+" ���ֵĵ÷��ǣ�"+score.getFinalScore()+" �Ƿ��й�:"+isAuto+" �÷ּ�¼:"+scoreMap;
		StringBuffer sb2 = new StringBuffer("  ");
		for(int i=0;i<myPlays.size();i++){
			Integer card = myPlays.get(i);
			String cardType = CardsMap.getCardType(card);
			sb2.append(cardType+"");
		}
		return "���е���:"+sb.toString()+"��ȥ����"+sb2;
	}

	/**����
	 * @param cardId ���Ƶ��ƺ�
	 */
	public int chuPai(int cardId) {
		int removeCard = -10; //�Ƴ�������
		if(!isUserCanPlay){
			return -1000;
		}
		isUserCanPlay = false;
		for(int i=0;i<cards.size();i++){
			Integer cardNumber = cards.get(i);
			if(cardNumber==cardId){
				removeCard = cards.remove(i);//�Ѹ������Ƴ�
				break;
			}
		}
		return removeCard;
	}
	
	
	
	
	/**
	 * �Զ�����
	 * �������ĳ��йܣ��Ժ�����ص������.........
	 * @return �������ƺ�
	 */
	public int autoChuPai1(){
		int[] array = getCardsArray();
//		int[] array ={16,24,25,52,60,64,68,72,84,88,89,96,108};
		List<Integer> mycards = Algorithm2.getListWithoutHongZhong(array);
		mycards = Algorithm2.getCardsWithoutSen(mycards);
		//Algorithm2.showPai(mycards);
		mycards = Algorithm2.getCardsWithoutCan(mycards);
		Integer card = mycards.get(0);
		//cards.remove(card);
		return card;
	}
	
	public static void main(String[] args) {
		long currentTimeMillis1 = System.currentTimeMillis();
		User user = new User();
		user.autoChuPai();
		long currentTimeMillis2 = System.currentTimeMillis();
		System.out.println(currentTimeMillis2-currentTimeMillis1);
	}
	
	/**���ܳ���
	 * @return
	 */
	public  synchronized int autoChuPai2(){
//		if(!isUserCanPlay){
//			return -100;
//		}
//		isUserCanPlay = false; //���ڲ����Դ�����
//		int[] array = getCardsArray();
//		int[] array ={16,24,25,52,60,64,68,72,84,88,89,96,108};
		int[] array ={3, 8, 16, 22, 29, 30, 48, 56, 58, 73, 74, 75, 82, 105};;
		List<Integer> mycards = Algorithm2.getListWithoutHongZhong(array);
		mycards = Algorithm2.getCardsWithoutSen(mycards);
		Algorithm2.showPai(mycards);
		mycards = Algorithm2.getCardsWithoutCan(mycards);
		Algorithm2.showPai(mycards);
		Integer card = mycards.get(0);
		String cardType = CardsMap.getCardType(card);
		System.out.println(cardType);
		//cards.remove(card);
		return card;
	}
	
	
	/**���ܳ���
	 * @return
	 */
	public  synchronized int autoChuPai(){
		if(!isUserCanPlay){
			return -100;
		}
		isUserCanPlay = false; //���ڲ����Դ�����
		Integer myGrabCard = this.getMyGrabCard();//ץ������
		int[] array = getCardsArray();
		if(myGrabCard != null){
			List<Integer> cards = this.getCards();
			boolean win = isUserTingPai(myGrabCard,cards);
			if(win){//�������
				logger.info("�Զ����Ƶ�ʱ���⵽����,�û��������ǣ�"+ Algorithm2.showPai(array));
				cards.remove(myGrabCard);
				return myGrabCard;
			}
		}
//		int[] array ={3, 8, 16, 22, 29, 30, 48, 56, 58, 73, 74, 75, 105, 109};
		Integer maxWeightCardId = getWitchCardToPlay(array);
//		String cardType = CardsMap.getCardType(maxWeightCardId);
//		System.out.println(cardType);
		cards.remove(maxWeightCardId);
		return maxWeightCardId;
	}


	/**����û��Ƿ�����
	 * @param myGrabCard
	 * @return
	 */
	public static boolean isUserTingPai(Integer myGrabCard,List<Integer> cards) {
		logger.info("ץ��������:"+myGrabCard+" "+ CardsMap.getCardType(myGrabCard));
		int[] tingPaiCars = getCardsWithRemoveDrawCardsWithReplaceHongZhong(myGrabCard,cards);//����Ҫ������
		boolean win = Algorithm2.isWin(tingPaiCars);
		return win;
	}

	
	
	/**�ڼ�⵽�û����Ƶ�ʱ�򣬼���û��Ƿ�����
	 * @param myGrabCard
	 * @return
	 */
	public static boolean isUserTingPaiOfPengOrGang(List<Integer> cards) {
		int[] tingPaiCars = getCardsWithAddHongZhong(cards);//����Ҫ������
		boolean win = Algorithm2.isWin(tingPaiCars);
		return win;
	}

	/**���û����ƻ����Ǹ��Ƶ�ʱ�����û��Ƿ�����,�Ӷ�������
	 * @return
	 */
	private static int[] getCardsWithAddHongZhong(List<Integer> cards){
		List<Integer> returnCards = new LinkedList<Integer>();
		for(int i=0;i<cards.size();i++){
			Integer card = cards.get(i);
			returnCards.add(card);
		}
		returnCards.add(108);
		Collections.sort(returnCards);
		int[] array = new int[returnCards.size()];
		for(int i=0;i<returnCards.size();i++){
			array[i] = returnCards.get(i);
		}
		return array;
	}
	
	

	/**�������һ���������
	 * @param array
	 * @return
	 */
	private Integer getWitchCardToPlay(int[] array) {
		Algorithm2.showPai(array);
		List<Integer> mycards = Algorithm2.getListWithoutHongZhong(array);//û�к��е���
		Map<Integer,Integer> cardWeightMap = new LinkedHashMap<>();
		for(int i=0;i<mycards.size();i++){
			Integer cardId = mycards.get(i);//��ǰ���ƺ�
			Integer preCardId;//ǰһ����
			Integer nextCardId;//��һ����
			if(i==0){
				preCardId = -1000;
			}else{
				preCardId = mycards.get(i-1);
			}
			if(i==mycards.size()-1){
				nextCardId = 1000;
			}else{
				nextCardId = mycards.get(i+1);
			}
			int intervalPre= Algorithm2.getInterval(preCardId,cardId);
			int intervalNext = Algorithm2.getInterval(cardId, nextCardId);
			int weight = getCardWeight(intervalPre, intervalNext);
			cardWeightMap.put(cardId, weight);
		}
		Iterator<Integer> iterator = cardWeightMap.keySet().iterator();
		int weight = Integer.MIN_VALUE ;//���Ȩ�ص�������
		Integer maxWeightCardId = 0;
		while(iterator.hasNext()){
			Integer cardId = iterator.next();
			Integer cardWeight = cardWeightMap.get(cardId);
			//System.out.println(CardsMap.getCardType(cardId)+" Ȩ�� : "+cardWeight);
			if(cardWeight>weight){
				weight = cardWeight;
				maxWeightCardId = cardId;
			}
		}
		return maxWeightCardId;
	}

	
	/**�Ƴ���ץ�����ƣ�������һ�ź������滻
	 * @return
	 */
	private static int[] getCardsWithRemoveDrawCardsWithReplaceHongZhong(Integer myDrawCard,List<Integer> cards){
		//Algorithm2.showPai(cards);
		List<Integer> returnCards = new LinkedList<Integer>();
		for(int i=0;i<cards.size();i++){
			Integer card = cards.get(i);
			if(card!=myDrawCard){
				returnCards.add(card);
			}
		}
		returnCards.add(108);
		Collections.sort(returnCards);
		int[] array = new int[returnCards.size()];
		for(int i=0;i<returnCards.size();i++){
			array[i] = returnCards.get(i);
		}
		return array;
	}
	
	
	
	
	
	/**�ж��û��Ƿ�����
	 * @param array
	 * @return
	 */
	public boolean isTingPai(int array[]){
		
		
		return false;
	}
	
	
	/**�õ������Ƶ�Ȩ��
	 * @param intervalPre
	 * @param intervalNext
	 * @return
	 */
	private int getCardWeight(int intervalPre, int intervalNext) {
		int weight = 0;
		//����ǹ�����һ����
		if(intervalPre==-1&&intervalNext==-1){
			weight = 10000;
		}else{
			if(intervalPre==1){//������һ�仰
				weight = weight-5000;
			}else if(intervalPre==0){//������ɿ�
				weight = weight - 10000;
			}else if(intervalPre == 2){
				weight = weight - 3000;
			}else{
				weight = weight+intervalPre;
			}
			if(intervalNext == 1){//������һ�仰
				weight = weight - 5000;
			}else if(intervalNext==0){//������ɿ�
				weight = weight - 10000;
			}else if(intervalNext==2){
				weight = weight - 3000;
			}else{
				weight = weight+intervalNext;
			}
		}
		return weight;
	}
	
	
	/**ץ��
	 * @param cardId
	 */
	public boolean zhuaPai(int cardId){
		cards.add(cardId);
		Collections.sort(cards);//ץ��������
		setLastChuPaiDate(new Date());//���ñ��εĳ���ʱ��
		//�������Ƿ����
		boolean win = Algorithm2.isWin(cards);
		if(win){
			System.out.println("������");
			//����Ϸֹͣ����
			return true;
		}else{
			logger.fatal("δ��������:"+cards+" "+Algorithm2.showPai(cards));
			//��¼�µ�ǰ�û�ץ������ʲô
			logger.info(this.nickName+" ��ǰץ��������:"+cardId);
			this.myGrabCard = cardId;//��ǰץ������
		}
		return false;
	}
	
	
	
	
	
	
	/**�ڳ�ȥ���Ƶļ���������һ����
	 * @param card
	 */
	public void addMyPlays(int card){
		myPlays.add(card);
	}
	

	
	/**����
	 * @param list ��Ҫ��������
	 */
	public void userPengCards(List<Integer> list){
		for(int i=0;i<list.size();i++){
			Integer removeCard = list.get(i);
			cards.remove(removeCard);
		}
		setCanPeng(false);
	}
	
	
	/**�����û���������
	 * @param cards
	 */
	public void addUserPengCards(List<Integer> cards){
		for(int i=0;i<cards.size();i++){
			Integer card = cards.get(i);
			pengCards.add(card);
		}
	}
	
	/**����
	 * @param list ��Ҫ�ܵ�����
	 */
	public void userGangCards(List<Integer> list){
		for(int i=0;i<list.size();i++){
			Integer removeCard = list.get(i);
			cards.remove(removeCard);
		}
		setCanGang(false);
	}
	
	/**�Ӹܼӷ�
	 */
	public void addScoreForCommonGang(){
		Score score = scoreMap.get(currentGame);
		score.setJieGangTotal(score.getJieGangTotal()+1);//�ӸܵĴ�����1
		score.setFinalScore(score.getFinalScore()+ScoreType.ADD_SCORE_FOR_GANG);
	}
	
	/**���ܼ���
	 */
	public void reduceScoreForFangGang(){
		Score score = scoreMap.get(currentGame);
		System.out.println(this);
		if(score==null){
			Score score2 = new Score();
			Map<Integer, Score> scoreMap = getScoreMap();
			scoreMap.put(getCurrentGame(), score2);
			score = score2;
			logger.error("�û��ķ�����ô��Ϊ��");
		}
		
		score.setFangGangTotal(score.getFangGangTotal()+1);//�ŸܵĴ�����1
		score.setFinalScore(score.getFinalScore()+ScoreType.REDUCE_SCORE_FOR_GANG);
	}
	
	
	/**
	 * ���ܼӷ�
	 */
	public void addScoreForMingGang(){
		Score score = scoreMap.get(currentGame);
		score.setMingGangtotal(score.getMingGangtotal()+1);
		score.setFinalScore(score.getFinalScore()+ScoreType.ADD_SCORE_FOR_MINGGANG);
	}
	
	
	/**
	 * ���ܼ���
	 */
	public void reduceScoreForMingGang(){
		Score score = scoreMap.get(currentGame);
		//score.setMingGangtotal(score.getMingGangtotal()+1);
		score.setFinalScore(score.getFinalScore()+ScoreType.REDUCE_SCORE_FOR_MINGGANG);
	}
	
	/**
	 * ���Ƶ÷�
	 */
	public void addScoreForHuPai(){
		Score score = scoreMap.get(currentGame);
		score.setHuPaiTotal(score.getHuPaiTotal()+1);
		score.setFinalScore(score.getFinalScore()+ScoreType.ADD_SCORE_FOR_HUPAI);
	}
	
	/**
	 * ���Ƽ���
	 */
	public void reduceScoreForHuPai(){
		Score score = scoreMap.get(currentGame);
		score.setFinalScore(score.getFinalScore()+ScoreType.REDUCE_SCORE_FOR_HUPAI);
	}
	
	
	/**����ӷ�
	 * @param totalZhongMa ����ĸ���
	 */
	public void addScoreForZhongMa(int totalZhongMa){
		Score score = scoreMap.get(currentGame);
		score.setZhongMaTotal(score.getZhongMaTotal()+totalZhongMa);
		score.setFinalScore(score.getFinalScore() + totalZhongMa*3*2);
	}
	
	
	/**�������
	 * @param totalZhongMa
	 */
	public void reduceScoreForZhongMa(int totalZhongMa){
		Score score = scoreMap.get(currentGame);
		score.setFinalScore(score.getFinalScore() - totalZhongMa*2);
	}
	
	
	/**
	 * ���ܵ÷�
	 */
	public void addScoreForAnGang(){
		Score score = scoreMap.get(currentGame);
		score.setAnGangTotal(score.getAnGangTotal()+1);
		score.setFinalScore(score.getFinalScore()+ScoreType.ADD_SCORE_FOR_ANGANG);
	}
	
	/**
	 * ���ܼ���
	 */
	public void reduceScoreForAnGang(){
		Score score = scoreMap.get(currentGame);
		score.setFinalScore(score.getFinalScore()+ScoreType.REDUCE_SCORE_FOR_ANGANG);
	}
	
	
	/**��¼�û��ܳ�����
	 * @param type 0 �Ÿ�  1������  2������ /����
	 * @param cards �ܳ�����
	 */
	public void recordUserGangCards(int type,List<Integer> cards){
		GangCard gangCard = new GangCard(type, cards);
		gangCards.add(gangCard);
	}
	
	/**�õ����ֵĵ÷�
	 * @return
	 */
	public int getCurrentGameSore(){
		Score score = scoreMap.get(currentGame);
		return score.getFinalScore();
	}
	
	/**���Լ��������Ƴ����ܵ�������
	 * @param card
	 */
	public void removeCardFromGongGang(Integer cardId){
		cards.remove(cardId);
	}

	
	/**����ת������
	 * @return
	 */
	public int[] getCardsArray(){
		int arr[]= new int[cards.size()];
		for(int i=0;i<cards.size();i++){
			arr[i] = cards.get(i);
		}
		return arr;
	}
	

	/**�õ��û���IP
	 * @return
	 */
	public String getIp(){
		SocketAddress remoteAddress = this.ioSession.getRemoteAddress();
		return remoteAddress.toString().replaceAll("/", "");
	}
	
}