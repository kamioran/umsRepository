package jp.co.f1.spring.bms.dao;

import java.util.ArrayList;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jp.co.f1.spring.bms.entity.Book;

@Repository
public class BookDao {

	// エンティティマネージャー
	private EntityManager entityManager;

	// クエリ生成用インスタンス
	private CriteriaBuilder builder;

	// クエリ実行用インスタンス
	private CriteriaQuery<Book> query;

	// 検索されるエンティティのルート
	private Root<Book> root;

	/**
	 * コンストラクタ（DB接続準備）
	 */
	public BookDao(EntityManager entityManager) {
		// EntityManager取得
		this.entityManager = entityManager;
		// クエリ生成用インスタンス
		builder = entityManager.getCriteriaBuilder();
		// クエリ実行用インスタンス
		query = builder.createQuery(Book.class);
		// 検索されるエンティティのルート
		root = query.from(Book.class);
	}

	/**
	 * 書籍情報検索
	 * @param String isbn
	 * @param String title
	 * @param String price
	 * @return ArrayList<Book> book_list
	 */
	public ArrayList<Book> find(String isbn, String title, String price) {
		// SELECT句設定
		query.select(root);

		// WHERE句設定
		query.where(
				builder.like(root.get("isbn"), "%" + isbn + "%"),
				builder.like(root.get("title"), "%" + title + "%"),
				builder.like(root.get("price"), "%" + price + "%"));

		// クエリ実行
		return (ArrayList<Book>) entityManager.createQuery(query).getResultList();
	}

//	public ArrayList<Book> findByIsbn(String isbn) {
//		// SELECT句設定
//		query.select(root);
//
//		// WHERE句設定
//		query.where(
//				builder.equal(root.get("isbn"), isbn));
//
//		// クエリ実行
//		return (ArrayList<Book>) entityManager.createQuery(query).getResultList();
//	}
	//Repositoryに記載。

}