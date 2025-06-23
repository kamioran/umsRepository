package jp.co.f1.spring.bms.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.co.f1.spring.bms.dao.OrderDao;
import jp.co.f1.spring.bms.entity.Book;
import jp.co.f1.spring.bms.entity.Order;
import jp.co.f1.spring.bms.entity.Sales;
import jp.co.f1.spring.bms.entity.User;
import jp.co.f1.spring.bms.repository.BookRepository;
import jp.co.f1.spring.bms.repository.OrderRepository;

@Controller
public class BmsOrderController {

	// Repositoryインターフェースを自動インスタンス化
	@Autowired
	private BookRepository bookinfo;

	@Autowired
	private OrderRepository orderinfo;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private OrderDao orderDao;

	@Autowired
	private HttpSession session;

	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

	@Autowired
	private MailSender mailSender;

	//セッション用
	ArrayList<Order> order_list1 = null;

	/*
	 * 「insertIntoCart」へアクセスがあった場合
	 */
	@GetMapping("/insertIntoCart")
	public ModelAndView insertIntoCartForm(HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、カートに追加出来ません");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		//該当書籍検索
		Optional<Book> optionalBook = bookinfo.findByIsbn(request.getParameter("isbn"));
		Book book = optionalBook.get();

		//Orderに登録
		Order order = new Order();
		Date date = new Date();
		order.setIsbn(request.getParameter("isbn"));
		order.setUserid(user.getUserid());
		order.setQuantity(Integer.parseInt(request.getParameter("quantity")));
		order.setDate(date);
		order.setBook(book);

		if ((ArrayList<Order>) session.getAttribute("order_list") == null) {
			//セッション用
			order_list1 = new ArrayList<Order>();
		}

		//セッション用オーダーリストにorderを追加
		order_list1.add(order);

		//セッションに登録
		session.setAttribute("order_list", order_list1);

		//Viewに渡す変数をModelに格納
		mav.addObject("order", order);

		//画面に出力するViewを指定
		mav.setViewName("view/insertIntoCart");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「buyConfirm」へGET送信された場合
	 */
	@GetMapping("/buyConfirm")
	public ModelAndView buyConfirmForm(HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//Viewに渡す用のbook_listを初期化
		ArrayList<Book> book_list = new ArrayList<Book>();
		//小計用のArrayListを作成
		ArrayList<Integer> subtotal_list = new ArrayList<Integer>();

		//セッションからorder情報全件取得
		ArrayList<Order> order_list = (ArrayList<Order>) session.getAttribute("order_list");

		//---エラー処理---
		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、購入できません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}
		//カートの中身がない場合
		if (order_list == null || order_list.size() == 0) {
			//エラーメッセージ
			mav.addObject("errorMessage", "カートの中に何も無かったので購入は出来ません。");
			mav.addObject("cmd", "menu");
			mav.addObject("next", "[メニューへ戻る]");
			//画面に出力するViewを指定
			mav.setViewName("view/error");
			//ModelとView情報を返す
			return mav;
		}

		//合計計算
		//合計金額用変数の初期化
		int total = 0;

		//オーダーリストから1件ずつ取り出す
		for (Order order : order_list) {
			//入力されたデータをDB（orderinfo)に保存
			orderinfo.saveAndFlush(order);

			//該当書籍取り出す
			Optional<Book> bookList = bookinfo.findByIsbn(order.getIsbn());
			Book Book = bookList.get();

			//book_list作成
			book_list.add(Book);

			//小計した値を保存
			subtotal_list.add(Integer.parseInt(Book.getPrice()) * order.getQuantity());
			//合計を計算する
			total += Integer.parseInt(Book.getPrice()) * order.getQuantity();
		}

		//Viewに渡す変数をModelに格納
		mav.addObject("total", total);
		mav.addObject("subtotal_list", subtotal_list);
		mav.addObject("order_list", order_list);

		//メール送信
		//pom.xmlにspring-boot-starter-mailを挿入
		try {

			SimpleMailMessage msg = new SimpleMailMessage();
			msg.setFrom("test.sender@kanda-it-school-system.com");
			msg.setTo("test.receiver@kanda-it-school-system.com");

			String insertMessage = user.getUserid() + "様" + LINE_SEPARATOR + LINE_SEPARATOR;
			insertMessage += "本のご購入ありがとうございます。" + LINE_SEPARATOR;
			insertMessage += "以下内容でご注文を受け付けましたので、ご連絡致します。" + LINE_SEPARATOR + LINE_SEPARATOR;
			for (int j = 0; j < book_list.size(); j++) {
				Book book = book_list.get(j);
				insertMessage += book.getIsbn() + "\t" + book.getTitle() + "\t" + book.getPrice() + "円" + ""
						+ LINE_SEPARATOR;
			}
			insertMessage += LINE_SEPARATOR + "合計" + total + "円" + LINE_SEPARATOR + LINE_SEPARATOR;
			insertMessage += "ご利用ありがとうございました。" + LINE_SEPARATOR;

			msg.setSubject("書籍購入情報");// Set Title
			msg.setText(insertMessage);// Set Message
			mailSender.send(msg);
			//メール送信に問題がある場合
		} catch (MailSendException e) {
			// エラーメッセージ
			mav.addObject("errorMessage", "メールの送信ができませんでした。");
			mav.addObject("cmd", "menu");
			mav.addObject("next", "[メニュー画面へ]");
			//画面に出力するViewを指定
			mav.setViewName("view/error");
			//ModelとView情報を返す
			return mav;
		}

		//セッション情報削除
		session.removeAttribute("order_list");

		//画面に出力するViewを指定
		mav.setViewName("view/buyConfirm");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「showOrderedItem」へアクセスがあった場合
	 */
	@GetMapping("/showOrderedItem")
	public ModelAndView showOrderedItemForm(ModelAndView mav) {
		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、購入状況の確認は出来ません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		// 購入履歴情報の全件を取得（書籍情報を含む）
		Iterable<Order> orderedList = orderinfo.findAll();

		//取得したデータを渡す
		mav.addObject("orderedList", orderedList);

		//画面に出力するViewを指定
		mav.setViewName("view/showOrderedItem");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「showHistoryOrderedItem」へアクセスがあった場合
	 */
	@GetMapping("/showHistoryOrderedItem")
	public ModelAndView showHistoryOrderedItemForm(ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、購入状況の確認は出来ません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		//該当書籍検索
		Iterable<Order> orderedList = orderinfo.findByUserid(user.getUserid());

		//取得したデータを渡す
		mav.addObject("orderedList", orderedList);

		//画面に出力するViewを指定
		mav.setViewName("view/showHistoryOrderedItem");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「showSalesByMonth」へアクセスがあった場合
	 */
	@GetMapping("/showSalesByMonth")
	public ModelAndView showSalesByMonthForm(ModelAndView mav) {

		//画面に出力するViewを指定
		mav.setViewName("view/showSalesByMonth");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「showSalesByMonth」へPOST送信された場合
	 */
	@PostMapping("/showSalesByMonth")
	public ModelAndView showSalesByMonthPost(HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、売り上げ状況の確認は出来ません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		//検索した年、月をパラメータ取得
		String year = request.getParameter("year");
		String month = request.getParameter("month");

		//データを検索
		Iterable<Sales> sales_list = orderDao.findByMonth(year, month);

		//キャスト
		ArrayList<Sales> saleList = (ArrayList<Sales>) sales_list;
		//小計用のArrayListを作成
		ArrayList<Integer> subtotal_list = new ArrayList<Integer>();

		//合計、小計の計算
		//合計金額用変数の初期化
		int total = 0;
		for (int i = 0; i < saleList.size(); i++) {

			//該当書籍取り出す
			Optional<Book> bookList = bookinfo.findByIsbn(saleList.get(i).getIsbn());
			Book Book = bookList.get();

			//小計した値を保存
			subtotal_list.add(Integer.parseInt(Book.getPrice()) * saleList.get(i).getQuantity());
			//合計値を合算
			total += saleList.get(i).getQuantity() * Integer.parseInt(Book.getPrice());
		}

		// Modelに検索した年、月、合計を追加
		mav.addObject("year", year);
		mav.addObject("month", month);
		mav.addObject("total", total);
		mav.addObject("subtotal_list", subtotal_list);
		mav.addObject("sales_list", sales_list);

		//画面に出力するViewを指定
		mav.setViewName("view/showSalesByMonth");
		//ModelとView情報を返す
		return mav;
	}

	/**
	 * Exception発生時の処理メソッド.
	 */
	@ExceptionHandler(Exception.class)
	public ModelAndView ExceptionHandler(Exception e) {

		ModelAndView mav = new ModelAndView();

		mav.addObject("errorMessage", "エラーが発生しました。" + e);
		mav.addObject("cmd", "logout");
		mav.addObject("next", "[ログイン画面へ]");
		//画面に出力するViewを指定
		mav.setViewName("view/error");
		//ModelとView情報を返す
		return mav;
	}
}
