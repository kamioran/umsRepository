package jp.co.f1.spring.bms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jp.co.f1.spring.bms.entity.Book.Group1;
import jp.co.f1.spring.bms.entity.Book.Group2;

@Entity
public class CheckUser {

	//USERID
	@Id
	@Column(length = 20, nullable = false)
	@NotBlank(message = "ユーザー入力値不正の為、登録できません。")
	//データベース内のカラム名にする
	private String userid;

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	//旧パスワード
	@Column(length = 100, nullable = false)
	@NotEmpty(message = "パスワードを入力してください")
	private String oldPassword;

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	//新パスワード
	@Column(length = 100)
	private String newPassword;

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	//新パスワード（確認用）
	@Column(length = 100, nullable = false)
	@NotEmpty(message = "新パスワード（確認用）を入力してください")
	private String confirmPassword;

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	//Email
	@Column(length = 100, nullable = false)
	@NotEmpty(message = "Eメール入力値不正の為、変更できません。")
	private String email;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	//AUTHORITY
	@Column(length = 11, nullable = false)
	@NotEmpty(message = "権限が未選択の為、変更できません。")
	private String authority;

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

}
