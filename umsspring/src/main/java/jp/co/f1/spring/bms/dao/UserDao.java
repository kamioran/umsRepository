package jp.co.f1.spring.bms.dao;

import java.util.ArrayList;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jp.co.f1.spring.bms.entity.User;

@Repository
public class UserDao {

	// エンティティマネージャー
	private EntityManager entityManager;

	// クエリ生成用インスタンス
	private CriteriaBuilder builder;

	// クエリ実行用インスタンス
	private CriteriaQuery<User> query;

	// 検索されるエンティティのルート
	private Root<User> root;

	/**
	 * コンストラクタ（DB接続準備）
	 */
	public UserDao(EntityManager entityManager) {
		// EntityManager取得
		this.entityManager = entityManager;
		// クエリ生成用インスタンス
		builder = entityManager.getCriteriaBuilder();
		// クエリ実行用インスタンス
		query = builder.createQuery(User.class);
		// 検索されるエンティティのルート
		root = query.from(User.class);
	}
	
	/**
	 * USER情報検索
	 * @param String userid
	 * @param String password
	 * @param String email
	 * @param String authority
	 * @return ArrayList<User> user_list
	 */
	public ArrayList<User> find(String userid) {
		// SELECT句設定
		query.select(root);

		// WHERE句設定
		query.where(
				builder.like(root.get("userid"), "%" + userid + "%"));

		// クエリ実行
		return (ArrayList<User>)entityManager.createQuery(query).getResultList();
	}
	//Repositoryに変更

}
