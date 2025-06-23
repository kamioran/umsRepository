package jp.co.f1.spring.bms.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.co.f1.spring.bms.dao.BookDao;
import jp.co.f1.spring.bms.entity.Book;
import jp.co.f1.spring.bms.entity.Order;
import jp.co.f1.spring.bms.entity.User;
import jp.co.f1.spring.bms.repository.BookRepository;

@Controller
public class BmsBookController {

	// Repositoryインターフェースを自動インスタンス化
	@Autowired
	private BookRepository bookinfo;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private BookDao bookDao;

	@Autowired
	private HttpSession session;

	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

	/*
	 * 「menu」へアクセスがあった場合
	 */
	@GetMapping("/menu")
	public ModelAndView menuForm(ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、再度ログインしてください。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		//Viewに渡す変数をModelに格納
		mav.addObject("user", user);

		// 画面に出力するViewを指定
		mav.setViewName("view/menu");
		// ModelとView情報を返す
		return mav;
	}

	/*
	 * 「/list」へアクセスがあった場合
	 */
	@GetMapping("/list")
	public ModelAndView listForm(ModelAndView mav) {

		// bookinfoテーブルから全件取得
		Iterable<Book> book_list = bookinfo.findAll();

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、一覧を表示できません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		// Viewに渡す変数をModelに格納
		mav.addObject("user", user); //「カートに入れる」と「変更/削除」の映し分けに必要
		mav.addObject("book_list", book_list);

		// 画面に出力するViewを指定
		mav.setViewName("view/list");
		// ModelとView情報を返す
		return mav;
	}

	/**
	 * 「/search」へアクセスがあった場合
	 */
	@GetMapping("/search")
	public ModelAndView searchForm(HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、検索結果を表示できません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		//データベースから検索に合うデータを抽出
		Iterable<Book> book_list = bookDao.find(
				request.getParameter("isbn"),
				request.getParameter("title"),
				request.getParameter("price"));

		// Viewに渡す変数をModelに格納
		mav.addObject("user", user);
		mav.addObject("book_list", book_list);

		// 画面に出力するViewを指定
		mav.setViewName("view/list");
		// ModelとView情報を返す
		return mav;
	}

	/*
	 * 「/insert」へアクセスがあった場合
	 */
	@GetMapping("/insert")
	public ModelAndView insertForm(@ModelAttribute Book book, ModelAndView mav) {

		// Viewに渡す変数をModelに格納
		mav.addObject("book", book);

		// 画面に出力するViewを指定
		mav.setViewName("view/insert");
		// ModelとView情報を返す
		return mav;
	}

	/*
	 * 「/insert」へPOST送信された場合
	 */
	@PostMapping("/insert")
	public ModelAndView insertPost(@ModelAttribute @Validated(Book.All.class) Book book,
			BindingResult result, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");
		//書籍検索
		Optional<Book> optionalBook = bookinfo.findByIsbn(book.getIsbn());

		//---エラー処理 ---//
		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、書籍登録できません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}
		// 入力エラーがある場合
		if (result.hasErrors()) {
			// エラーメッセージ
			mav.addObject("message", "入力内容に誤りがあります");
			// 画面に出力するViewを指定
			mav.setViewName("view/insert");
			// ModelとView情報を返す
			return mav;
		}
		//重複チェック
		if (optionalBook.isPresent()) {
			// エラーメッセージ
			mav.addObject("message", "入力ISBNは既に登録済みの為、書籍登録処理は行えませんでした。");
			mav.setViewName("view/insert");
			// ModelとView情報を返す
			return mav;
		}

		// 入力されたデータをDBに保存
		bookinfo.saveAndFlush(book);

		// リダイレクト先を指定
		mav = new ModelAndView("redirect:/list");
		// ModelとView情報を返す
		return mav;
	}

	/*
	 * 「detail」へアクセスがあった場合
	 */
	@GetMapping("/detail")
	public ModelAndView detailForm(HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");
		//書籍検索
		Optional<Book> optionalBook = bookinfo.findByIsbn(request.getParameter("isbn"));

		//---エラー処理 ---//
		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、詳細を表示できません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}
		//書籍一覧画面のISBNリンクをクリック時、表示対象の書籍が存在しない
		if (!(optionalBook.isPresent())) {
			// エラーメッセージ
			mav.addObject("errorMessage", "詳細対象の書籍が存在しない為、詳細情報処理は行えません。");
			mav.addObject("cmd", "list");
			mav.addObject("next", "[一覧表示へ戻る]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		// Viewに渡す変数をModelに格納
		mav.addObject("user", user);
		mav.addObject("book", optionalBook.get());

		//画面に出力するViewを指定
		mav.setViewName("view/detail");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「delete」へアクセスがあった場合
	 */
	@GetMapping("/delete")
	public ModelAndView deleteForm(HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");
		//書籍検索
		Optional<Book> optional_book = bookinfo.findByIsbn(request.getParameter("isbn"));

		//---エラー処理 ---//
		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、削除できませんでした。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}
		//リストを映し出した状態でDB内からデータが消えた場合のチェック
		if (!(optional_book.isPresent())) {
			// エラーメッセージ
			mav.addObject("errorMessage", "削除対象の書籍が存在しない為、書籍削除処理は行えませんでした。");
			mav.addObject("cmd", "list");
			mav.addObject("next", "[一覧表示へ戻る]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		//受け取ったBook情報を削除
		bookinfo.deleteById(request.getParameter("isbn"));

		//リダイレクト先を指定
		mav = new ModelAndView("redirect:/list");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「/update」へアクセスがあった場合
	 */
	@GetMapping("/update")
	public ModelAndView updateForm(@ModelAttribute Book book, HttpServletRequest request, ModelAndView mav) {

		//書籍検索
		Optional<Book> optionalBook = bookinfo.findByIsbn(request.getParameter("isbn"));

		//---エラー処理 ---//
		// 書籍が存在しない場合
		if (!optionalBook.isPresent()) {
			// エラーメッセージ
			mav.addObject("errorMessage", "更新対象の書籍が存在しない為、変更画面は表示出来ませんでした。");
			mav.addObject("cmd", "list");
			mav.addObject("next", "[一覧表示へ戻る]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		// 書籍が存在する場合、old_bookとしてModelに追加
		Book old_book = optionalBook.get();
		mav.addObject("old_book", old_book);
		mav.addObject("book", book);

		// 画面に出力するViewを指定
		mav.setViewName("view/update");
		// ModelとView情報を返す
		return mav;
	}

	/*
	 * 「/update」へPOST送信された場合
	 */
	@PostMapping("/update")
	public ModelAndView updatePost(@ModelAttribute @Validated(Book.All.class) Book book,
			BindingResult result, HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		// 書籍検索
		Optional<Book> optionalBook = bookinfo.findByIsbn(request.getParameter("isbn"));

		// 書籍が存在する場合、old_bookとしてModelに追加
		Book old_book = optionalBook.get(); // Optionalから値を取得

		//---エラー処理---
		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、更新できませんでした。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}
		// 書籍が存在しない場合
		if (!optionalBook.isPresent()) {
			mav.addObject("errorMessage", "更新対象の書籍が存在しないため、書籍更新処理は行えませんでした。");
			mav.addObject("cmd", "list");
			mav.addObject("next", "[一覧表示へ戻る]");
			mav.setViewName("view/error");
			return mav;
		}
		// 入力内容にエラーがある場合
		if (result.hasErrors()) {
			//エラーメッセージ
			mav.addObject("message", "入力内容に誤りがあります");
			// バリデーションエラー後は、入力内容を再表示する
			mav.addObject("old_book", old_book);
			mav.setViewName("view/update");
			return mav;
		}

		// 入力されたデータをDBに保存
		bookinfo.saveAndFlush(book);

		// リダイレクト先を指定
		mav.setViewName("redirect:/list");
		// ModelとView情報を返す
		return mav;
	}

	/*
	 * 「showCart」へGET送信された場合
	 */
	@GetMapping("/showCart")
	public ModelAndView showCartForm(HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、カート状況は確認出来ません");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		//セッションからorder情報全件取得
		ArrayList<Order> orderList = (ArrayList<Order>) session.getAttribute("order_list");

		//削除リンクを押下した場合
		if (request.getParameter("delno") != null) {

			//該当書籍検索
			//カウント用変数
			int i = 0;
			//order初期化
			Order order = new Order();

			//繰り返して取り出す
			while (i < orderList.size()) {
				order = orderList.get(i);
				if (order.getIsbn().equals(request.getParameter("delno"))) {
					break;
				}
				i++;
			}

			//Book情報をカートから削除
			orderList.remove(orderList.indexOf(order));

			//リダイレクト先を指定
			mav = new ModelAndView("redirect:/showCart");
		}

		//合計値計算用の変数
		int total = 0;

		//計算用ループ
		if (orderList != null) {

			//オーダーリストから書籍情報取り出す
			for (Order order : orderList) {

				//書籍検索
				Optional<Book> bookList = bookinfo.findByIsbn(order.getIsbn());

				//合計金額を計算
				total += Integer.parseInt(bookList.get().getPrice()) * order.getQuantity();
			}
		}

		//Viewに渡す変数をModelに格納
		mav.addObject("total", total);
		mav.addObject("orderList", orderList);

		//画面に出力するViewを指定
		mav.setViewName("view/showCart");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「insertIniData」へアクセスがあった場合
	 */
	@GetMapping("/insertIniData")
	public ModelAndView insertInidataForm(ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//bookinfoテーブルから全件取得
		ArrayList<Book> bookList = (ArrayList<Book>) bookinfo.findAll();

		//---エラー処理---
		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、初期データ登録が行えません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}
		//bookinfoテーブルにデータが既にある（1件でも）
		if (bookList.size() >= 1) {
			//エラーメッセージ
			mav.addObject("errorMessage", "DBにはデータが存在するので、初期データは登録できません。");
			mav.addObject("cmd", "menu");
			mav.addObject("next", "[メニューへ戻る]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		//「src/main/resources/initial_data.csv」を読み込む
		Path path = Paths.get("src/main/resources/initial_data.csv");
		try (BufferedReader br = Files.newBufferedReader(path)) {
			String line;

			while ((line = br.readLine()) != null) {
				final String[] split = line.split(",");
				final Book book = new Book();

				//数値か判断
				try {
					int price1 = Integer.parseInt(split[2]);
				} catch (NumberFormatException e) {
					//エラーメッセージ
					mav.addObject("errorMessage", "初期データファイルが不備がある為、登録は行えません。");
					mav.addObject("cmd", "menu");
					mav.addObject("next", "[メニューへ戻る]");
					//画面に出力するViewを指定
					mav.setViewName("view/error");
					//ModelとView情報を返す
					return mav;
				}
				//Bookオブジェクトに格納
				book.setIsbn(split[0]);
				book.setTitle(split[1]);
				book.setPrice(split[2]);

				//データベースに登録
				bookinfo.saveAndFlush(book);
			}

			//初期データファイルに不備がある(価格に文字列がある)
			//bookinfoテーブルから全件取得
			Iterable<Book> book_list = bookinfo.findAll();

			//Viewに渡す変数をModelに格納
			mav.addObject("book_list", book_list);

			//画面に出力するViewを指定
			mav.setViewName("view/insertIniData");

		} catch (IOException e) {
			//初期データファイルが無い
			mav.addObject("errorMessage", "初期データファイルが無い為、登録は行えません。");
			mav.addObject("cmd", "menu");
			mav.addObject("next", "[メニューへ戻る]");
			//画面に出力するViewを指定
			mav.setViewName("view/error");
		}
		//ModelとView情報を返す
		return mav;
	}

	/**
	 * Exception発生時の処理メソッド.
	 */
	@ExceptionHandler(Exception.class)
	public ModelAndView ExceptionHandler(Exception e) {

		ModelAndView mav = new ModelAndView();

		mav.addObject("errorMessage", "エラー内容：" + e.getMessage());
		mav.addObject("cmd", "logout");
		mav.addObject("next", "[ログイン画面へ]");
		//画面に出力するViewを指定
		mav.setViewName("view/error");
		//ModelとView情報を返す
		return mav;
	}
}
