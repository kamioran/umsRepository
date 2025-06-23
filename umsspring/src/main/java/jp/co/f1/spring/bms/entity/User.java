package jp.co.f1.spring.bms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "userinfo")

public class User {

	//USERID
	@Id
	@Column(length = 20)
	//データベース内のカラム名にする
	private String userid;

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	//PASSWORD
	@Column(length = 100, nullable = true)
	private String password;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	//Email
	@Column(length = 100, nullable = true)
	private String email;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	//AUTHORITY
	@Column(length = 11, nullable = true)
	private String authority;

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

}
