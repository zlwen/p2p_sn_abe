package cn.edu.pku.ss.bean;

import com.sun.istack.internal.logging.Logger;

public class Friend {
	private Logger logger = Logger.getLogger(Friend.class);
	private String email;
	private String nickName;
	private String pubKey;
	private String decKey;
	private String group;

	private Friend(Builder builder){
		email = builder.email;
		nickName = builder.nickName;
		pubKey = builder.pubKey;
		decKey = builder.decKey;
		group = builder.group;
	}
	
	@Override
	public boolean equals(Object o){
		if(o  == null){
			return false;
		}
		else if(o == this){
			return true;
		}
		else if(!(o instanceof Friend)){
			return false;
		}
		else{
			Friend f = (Friend)o;
			return f.email == this.email;
		}
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		sb.append("email:" + email);
		sb.append("nickName:" + nickName);
		sb.append("pubKey:" + pubKey);
		sb.append("decKey:" + decKey);
		sb.append("group:" + group);
		sb.append("}");
		return sb.toString();
	}

	public Logger getLogger() {
		return logger;
	}

	public String getEmail() {
		return email;
	}

	public String getNickName() {
		return nickName;
	}

	public String getPubKey() {
		return pubKey;
	}

	public String getDecKey() {
		return decKey;
	}

	public String getGroup() {
		return group;
	}

	public static class Builder {
		private String email;
		private String nickName;
		private String pubKey;
		private String decKey;
		private String group;
		
		public Friend build(){
			return new Friend(this);
		}


		public Builder setEmail(String email) {
			this.email = email;
			return this;
		}

		public Builder setNickName(String nickName) {
			this.nickName = nickName;
			return this;
		}

		public Builder setPubKey(String pubKey) {
			this.pubKey = pubKey;
			return this;
		}

		public Builder setDecKey(String decKey) {
			this.decKey = decKey;
			return this;
		}

		public Builder setGroup(String group) {
			this.group = group;
			return this;
		}
	}
}
