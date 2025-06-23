package jp.co.f1.spring.bms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name="bookinfo")
public class Book {
	
	public interface Group1 {}
	public interface Group2 {}

	@GroupSequence({Group1.class,Group2.class})
	public interface All {}

	// ISBN
	@Id
	@Column(length = 20)
	@NotEmpty(message="ISBNを入力してください", groups = Group1.class)
	private String isbn;

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	// タイトル
	@Column(length = 100, nullable = true)
	@NotEmpty(message="タイトルを入力してください", groups = Group1.class)
	private String title;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	// 価格
	@Column(length = 11, nullable = true)
	@NotEmpty(message = "価格を入力してください", groups = Group1.class)
	@Pattern(regexp = "^[0-9]+$", message = "価格は数字のみで入力してください", groups = Group2.class)
	private String price;

	public String getPrice() {
		return price;
	} 

	public void setPrice(String price) {
		this.price = price;
	}
	
}