package jp.co.f1.spring.bms.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "orderinfo")

public class Sales {

	@Id
	@Column(length = 20, nullable = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	//オートインクリメントの際に必要
	private int orderno;

	public int getOrderno() {
		return orderno;
	}

	public void setOrderno(int orderno) {
		this.orderno = orderno;
	}

	@Column(length = 20)
	//データベース内のカラム名にする
	private String user;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	@Column(length = 20)
	private String isbn;

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	@Column(length = 20)
	private int quantity;

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	@Column(length = 20, nullable = true)
	private Date date;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	// Web版のOrderedItemにあたるリレーション
	@ManyToOne
	@JoinColumn(name = "isbn", referencedColumnName = "isbn", insertable = false, updatable = false)
	// 自身のカラム、相手のカラム（デフォルト：参照されるテーブルの主キー列と同じ名前。なので今回は省略可）
	private Book book;

	public Book getBook() {
		return book;
	}

	public void setBook(Book book) {
		this.book = book;
	}

	private String title;

	// getters and setters
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	private int price;

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

}
