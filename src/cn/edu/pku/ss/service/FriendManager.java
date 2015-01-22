package cn.edu.pku.ss.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.edu.pku.ss.bean.Friend;

public class FriendManager {
	private Logger logger = LoggerFactory.getLogger(FriendManager.class);
	private Map<String, List<Friend>> groups;
	private static final String GROUP_FAMILY = "家人";
	private static final String GROUP_WORKMTE = "同事";
	private static final String GROUP_FRIEND = "朋友";
	
	public FriendManager(){
		this.groups = new HashMap<String, List<Friend>>();
		String[] initGroups = new String[]{GROUP_FAMILY, GROUP_FRIEND, GROUP_WORKMTE};
		for(String group : initGroups){
			this.groups.put(group, new ArrayList<Friend>());
		}
	}
	
//	public Friend removeFriendByEmail(String email){
//		
//	}
	
	public void addFriend(String group, Friend f){
		if(this.groups.get(group) == null){
			logger.error("Add a friend " + f +" to a non-exist group + " + group);
			return;
		}
		List<Friend> friends = this.groups.get(group);
		if(friends.contains(f)){
			logger.warn("group:" + group + "already contains friend " + f);
			return;
		}
		else{
			friends.add(f);
		}
	}
	
	public List<Friend> getGroupMembers(String group){
		return this.groups.get(group);
	}
	
	public void addNewGroup(String group){
		logger.info("Add a new group " + group);
		this.groups.put(group, new ArrayList<Friend>());
	}
	
	public Set<String> listAllGroups(){
		return this.groups.keySet();
	}
}
