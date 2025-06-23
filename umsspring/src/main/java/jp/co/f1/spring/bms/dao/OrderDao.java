package jp.co.f1.spring.bms.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jp.co.f1.spring.bms.entity.Book;
import jp.co.f1.spring.bms.entity.Order;
import jp.co.f1.spring.bms.entity.Sales;

@Repository
public class OrderDao {
	// エンティティマネージャー
	private EntityManager entityManager;

	// クエリ生成用インスタンス
	private CriteriaBuilder builder;

	// クエリ実行用インスタンス
	private CriteriaQuery<Order> query;

	// 検索されるエンティティのルート
	private Root<Order> root;

	/**
	 * コンストラクタ（DB接続準備）
	 */
	public OrderDao(EntityManager entityManager) {
		// EntityManager取得
		this.entityManager = entityManager;
		// クエリ生成用インスタンス
		builder = entityManager.getCriteriaBuilder();
		// クエリ実行用インスタンス
		query = builder.createQuery(Order.class);
		// 検索されるエンティティのルート
		root = query.from(Order.class);
	}

	/**
	 * 年、月の売り上げ状況の情報検索
	 */
	public List<Sales> findByMonth(String year, String month) {
		// クエリビルダーを使用してCriteriaQueryを作成
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);
		Root<Order> root = query.from(Order.class); // Orderエンティティを使う
		Join<Order, Book> bookJoin = root.join("book", JoinType.INNER); // Order と Book を結合

		// 年と月を基にした条件を作成
		Predicate yearCondition = null;
		if (year != null && !year.isEmpty()) {
			yearCondition = builder.equal(builder.function("YEAR", Integer.class, root.get("date")),
					Integer.parseInt(year)); // `date`フィールドから年を取得
		}

		Predicate monthCondition = null;
		if (month != null && !month.isEmpty()) {
			monthCondition = builder.equal(builder.function("MONTH", Integer.class, root.get("date")),
					Integer.parseInt(month)); // `date`フィールドから月を取得
		}
		
		// WHERE句を設定
		if (yearCondition != null && monthCondition != null) {
			query.where(builder.and(yearCondition, monthCondition)); // 年と月の両方の条件
		} else if (yearCondition != null) {
			query.where(yearCondition); // 年のみの条件
		} else if (monthCondition != null) {
			query.where(monthCondition); // 月のみの条件
		} else {
			query.where(builder.conjunction()); // 年も月も指定されていない場合（全件取得）
		}

		// グループ化と並び替えの設定（SQLのGROUP BYとORDER BYに相当）
		// isbnでグループ化し、quantityの合計を求める
		query.select(
				builder.array(
						bookJoin.get("isbn"), // isbn
						bookJoin.get("title"), // title
						bookJoin.get("price"), // price
						builder.sum(root.get("quantity")) // quantityの合計
				))
				.groupBy(bookJoin.get("isbn"), bookJoin.get("title"), bookJoin.get("price")) // isbn, title, priceでグループ化
				.orderBy(builder.asc(bookJoin.get("isbn"))); // isbnで昇順に並び替え

		// クエリ実行
		List<Object[]> result = entityManager.createQuery(query).getResultList();

		// 結果をOrderオブジェクトに変換して返す
		List<Sales> salesList = new ArrayList<>();
		for (Object[] row : result) {
			Sales sales = new Sales();
			sales.setIsbn((String) row[0]); // isbn
			sales.setTitle((String) row[1]); // title
			sales.setPrice(Integer.parseInt((String) row[2])); // priceをStringからintに変換
			sales.setQuantity(((Number) row[3]).intValue()); // 合計個数 (Longをintに変換)
			salesList.add(sales);
		}

		return salesList;
	}
}
