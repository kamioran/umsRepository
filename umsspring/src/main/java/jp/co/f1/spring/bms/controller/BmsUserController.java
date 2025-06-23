package jp.co.f1.spring.bms.controller;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.co.f1.spring.bms.dao.BookDao;
import jp.co.f1.spring.bms.dao.OrderDao;
import jp.co.f1.spring.bms.dao.UserDao;
import jp.co.f1.spring.bms.entity.CheckUser;
import jp.co.f1.spring.bms.entity.User;
import jp.co.f1.spring.bms.repository.UserRepository;

@Controller
public class BmsUserController {

	//EntityManager自動インスタンス化
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private UserDao userDao;

	@Autowired
	private UserRepository userinfo;

	//セッション使用
	@Autowired
	private HttpSession session;

	/*
	 * 「login」へアクセスがあった場合
	 */
	@GetMapping("/login")
	public ModelAndView loginForm(ModelAndView mav, HttpServletRequest request) {

		// クッキーを取得
		Cookie[] cookies = request.getCookies();
		String strUserid = null;
		String strPassword = null;

		//クッキーが存在するかチェック
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("strUserid".equals(cookie.getName())) {
					strUserid = cookie.getValue();
				} else if ("strPassword".equals(cookie.getName())) {
					strPassword = cookie.getValue();
				}
			}
		}

		// Modelにクッキーの値を追加
		mav.addObject("strUserid", strUserid);
		mav.addObject("strPassword", strPassword);
		// 画面に出力するViewを指定
		mav.setViewName("view/login");
		// ModelとView情報を返す
		return mav;
	}

	/*
	 * 「login」へPOST送信された場合
	 */
	@PostMapping("/login")
	public ModelAndView loginPost(@ModelAttribute @Validated User user, BindingResult result,
			ModelAndView mav, HttpServletRequest request, HttpServletResponse response, Model model) {

		//ユーザー検索
		Optional<User> optionalUser = userinfo.findByUseridAndPassword(user.getUserid(), user.getPassword());

		//該当ユーザーがある場合
		if (!optionalUser.isPresent()) {
			//エラーメッセージ
			mav.addObject("message", "入力内容に誤りがあります。");
			// 画面に出力するViewを指定
			mav.setViewName("view/login");
			//ModelとView情報を返す
			return mav;
		}

		//ユーザーの値取得
		user = optionalUser.get();

		//クッキーの登録
		Cookie useridCookie = new Cookie("strUserid", user.getUserid());
		useridCookie.setMaxAge(60 * 60 * 24 * 5); // 5日（秒）に設定
		response.addCookie(useridCookie);

		Cookie passCookie = new Cookie("strPassword", user.getPassword());
		passCookie.setMaxAge(60 * 60 * 24 * 5); // 5日（秒）に設定
		response.addCookie(passCookie);

		//セッションに登録
		session.setAttribute("user", user);

		//パスワード変更の際に使うものをセッションに登録
		CheckUser checkUser = new CheckUser();
		
		checkUser.setUserid(user.getUserid());
		checkUser.setOldPassword(user.getPassword());
		checkUser.setEmail(user.getEmail());
		checkUser.setAuthority(user.getAuthority());
		
		session.setAttribute("checkUser", checkUser);

		//リダイレクト先を指定
		mav = new ModelAndView("redirect:/menu");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「logout」へアクセスがあった場合
	 */
	@GetMapping("/logout")
	public ModelAndView logoutForm(ModelAndView mav) {

		//セッション情報をクリアする
		session.invalidate();

		//リダイレクト先を指定
		mav = new ModelAndView("redirect:/login");
		// ModelとView情報を返す
		return mav;
	}

	/*
	 * 「changePassword」へアクセスがあった場合
	 */
	@GetMapping("/changePassword")
	public ModelAndView changePasswordForm(HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		CheckUser checkUser = (CheckUser) session.getAttribute("checkUser");

		//セッション切れの場合
		if (checkUser == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、パスワード変更画面に遷移できません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		//Viewに渡す変数をModelに格納
		mav.addObject("checkUser", checkUser);

		//画面に出力するViewを指定
		mav.setViewName("view/changePassword");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「changePassword」へPOST送信された場合
	 */
	@PostMapping("/changePassword")
	//POSTデータをBookインスタンスとして受け取る
	public ModelAndView changePasswordPost(@ModelAttribute @Validated CheckUser checkUser, BindingResult result,
			ModelAndView mav, HttpServletRequest request, HttpServletResponse response, Model model) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//user検索
		Optional<User> optionalUser = userinfo.findByUseridAndPassword(
				checkUser.getUserid(),
				checkUser.getOldPassword()
		);
		
		
		//---エラー処理 ---//
		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、パスワード変更は行えません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}
		
		
		//入力内容にエラーがある場合
		if (result.hasErrors()) {
			//旧パスワードについては別途エラー処理（Validation使わず）
			if (checkUser.getNewPassword() == "") {
			    mav.addObject("passwordError", "パスワードを入力してください");
			}
			//エラーメッセージ
			mav.addObject("message", "入力内容に誤りがあります");
			//画面に出力するViewを指定
			mav.setViewName("view/changePassword");
			//ModelとView情報を返す
			return mav;
		}
		
		//該当USER無し
		if (!optionalUser.isPresent()) {
			// エラーメッセージ
			mav.addObject("message", "パスワードが間違っています！");
			//リダイレクト先を指定
			mav.setViewName("view/changePassword");
			//ModelとView情報を返す
			return mav;
		}
		
		user = optionalUser.get();
		
		//新しいパスワードと確認用のパスワードの確認
		if (!checkUser.getNewPassword().equals(checkUser.getConfirmPassword())) {
			// エラーメッセージ
			mav.addObject("message", "新パスワードと確認パスワードが合っていません！");
			//画面に出力するViewを指定
			mav.setViewName("view/changePassword");
			//ModelとView情報を返す
			return mav;
		}

		//新しいパスワードをUserに格納
		user.setPassword(checkUser.getNewPassword());

		//入力されたデータをDBに保存
		userinfo.saveAndFlush(user);

		//クッキーの再登録
		Cookie useridCookie = new Cookie("strUserid", user.getUserid());
		useridCookie.setMaxAge(60 * 60 * 24 * 5); // 5日（秒）に設定
		response.addCookie(useridCookie);

		Cookie passCookie = new Cookie("strPassword", user.getPassword());
		passCookie.setMaxAge(60 * 60 * 24 * 5);
		response.addCookie(passCookie);

		//セッションに登録
		session.setAttribute("user", user);
		//パスワード再設定用のセッションも登録
		session.setAttribute("checkUser", checkUser);

		// Viewに渡す変数をModelに格納
		mav.addObject("message", "パスワードの変更が完了しました！");

		//画面に出力するViewを指定
		mav.setViewName("view/changePassword");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「/listUser」へアクセスがあった場合
	 */
	@GetMapping("/listUser")
	public ModelAndView listUserForm(ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		// bookinfoテーブルから全件取得
		Iterable<User> user_list = userinfo.findAll();

		//セッション切れの場合
		if (user == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、一覧を表示出来ません。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		// Viewに渡す変数をModelに格納
		mav.addObject("user_list", user_list);

		// 画面に出力するViewを指定
		mav.setViewName("view/listUser");
		// ModelとView情報を返す
		return mav;
	}

	/**
	 * 「/searchUser」へアクセスがあった場合
	 */
	@GetMapping("/searchUser")
	public ModelAndView searchUserForm(HttpServletRequest request, ModelAndView mav) {

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

		//ArrayListを宣言
		ArrayList<User> user_list = null;

		//フォームから受け取った値を参照に検索
		user_list = userDao.find(request.getParameter("userid"));
		
		//OptionalからArrayListに変換
		//optionalUser.ifPresent(user_list::add); // Optionalが値を持っていれば、リストに追加

		// Viewに渡す変数をModelに格納
		mav.addObject("user_list", user_list);

		// 画面に出力するViewを指定
		mav.setViewName("view/listUser");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「detailUser」へアクセスがあった場合
	 */
	@GetMapping("/detailUser")
	public ModelAndView detailUserForm(HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//ユーザー検索
		Optional<User> optional_user = userinfo.findByUserid(request.getParameter("userid"));

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
		//ユーザー一覧画面のリンクをクリック時、表示対象のユーザーが存在しない
		if (!(optional_user.isPresent())) {
			// エラーメッセージ
			mav.addObject("errorMessage", "詳細対象のユーザーが存在しない為、詳細情報処理は行えません。");
			mav.addObject("cmd", "listUser");
			mav.addObject("next", "[ユーザー一覧表示へ戻る]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		// Viewに渡す変数をModelに格納
		mav.addObject("user", optional_user.get());

		//画面に出力するViewを指定
		mav.setViewName("view/detailUser");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「updateUser」へアクセスがあった場合
	 */
	@GetMapping("/updateUser")
	public ModelAndView updateUserForm(@ModelAttribute CheckUser checkUser, HttpServletRequest request,
			ModelAndView mav) {

		//ユーザー検索
		Optional<User> optionalUser = userinfo.findByUserid(request.getParameter("userid"));

		//---エラー処理---
		//ユーザー一覧画面の変更リンクや詳細画面の変更ボタンをクリック時、変更対象のユーザーが存在しない場合
		if (!(optionalUser.isPresent())) {
			//エラーメッセージ
			mav.addObject("errorMessage", "更新対象のユーザーが存在しない為、変更画面は表示出来ませんでした。");
			mav.addObject("cmd", "listUser");
			mav.addObject("next", "[ユーザー一覧表示へ戻る]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		// ユーザーが存在する場合、old_userとしてModelに追加
		User old_user = optionalUser.get(); // Optionalから値を取得
		//Viewに渡す変数をModelに格納
		mav.addObject("old_user", old_user);
		mav.addObject("checkUser", checkUser);

		//画面に出力するViewを指定
		mav.setViewName("view/updateUser");
		//ModelとView情報を返す
		return mav;
	}

	/*
	 * 「updateUser」へPOST送信された場合
	 */
	@PostMapping("/updateUser")
	public ModelAndView updateUserPost(@ModelAttribute @Validated CheckUser checkUser,
			BindingResult result, HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得（管理者）
		User user = (User) session.getAttribute("user");

		//ユーザー検索
		Optional<User> optionalUser = userinfo.findByUserid(checkUser.getUserid());

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
		// リンク押下した時にユーザーが存在しない場合
		if (!optionalUser.isPresent()) {
			// エラーメッセージ
			mav.addObject("errorMessage", "更新対象のユーザーが存在しないため、ユーザー更新処理は行えませんでした。");
			mav.addObject("cmd", "listUser");
			mav.addObject("next", "[ユーザー一覧表示へ戻る]");
			mav.setViewName("view/error");
			return mav;
		}

		// ユーザーが存在する場合、old_userとしてModelに追加
		User old_user = optionalUser.get(); // Optionalから値を取得
		
		// 入力内容にエラーがある場合
		if (result.hasErrors()) {
			//新パスワードについては別途エラー処理（Validation使わず）
			if (checkUser.getNewPassword() == "") {
			    mav.addObject("passwordError", "パスワードを入力してください");
			}
			//エラーメッセージ
			mav.addObject("message", "入力内容に誤りがあります");
			// バリデーションエラー後は、内容を再表示する
			mav.addObject("old_user", old_user);
			mav.addObject("checkUser", checkUser);
			mav.setViewName("view/updateUser");
			return mav;
		}
		
		
		//新しいパスワードと確認用のパスワードの確認
		if (!checkUser.getNewPassword().equals(checkUser.getConfirmPassword())) {
			// エラーメッセージ
			mav.addObject("message", "新パスワードと確認パスワードが合っていません！");
			// バリデーションエラー後は、内容を再表示する
			mav.addObject("old_user", old_user);
			mav.addObject("checkUser", checkUser);
			//画面に出力するViewを指定
			mav.setViewName("view/updateUser");
			//ModelとView情報を返す
			return mav;
		}

		//新しい情報をcheckuseridから格納
		//User user = new User();
		old_user.setUserid(checkUser.getUserid());
		old_user.setPassword(checkUser.getNewPassword());
		old_user.setEmail(checkUser.getEmail());
		old_user.setAuthority(checkUser.getAuthority());

		// 入力されたデータをDBに保存
		userinfo.saveAndFlush(old_user);

		// リダイレクト先を指定
		mav = new ModelAndView("redirect:/listUser");
		// ModelとView情報を返す
		return mav;
	}

	/*
	 * 「/insertUser」へアクセスがあった場合
	 */
	@GetMapping("/insertUser")
	public ModelAndView insertUserForm(@ModelAttribute CheckUser checkUser, ModelAndView mav) {

		// Viewに渡す変数をModelに格納
		mav.addObject("checkUser", checkUser);
		// 画面に出力するViewを指定
		mav.setViewName("view/insertUser");
		// ModelとView情報を返す
		return mav;
	}
	
	/*
	 * 「/insertUser」へPOST送信された場合
	 */
	@PostMapping("/insertUser")
	public ModelAndView insertUserPost(@ModelAttribute @Validated CheckUser checkUser, BindingResult result,
			ModelAndView mav) {

		//セッションからユーザー情報取得
		User adminUser = (User) session.getAttribute("user");

		//ユーザー検索
		Optional<User> optionalUser = userinfo.findByUserid(checkUser.getUserid());

		//---エラー処理 ---//
		//セッション切れの場合
		if (adminUser == null) {
			//エラーメッセージ
			mav.addObject("errorMessage", "セッション切れの為、登録できませんでした。");
			mav.addObject("cmd", "logout");
			mav.addObject("next", "[ログイン画面へ]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}
		//重複チェック
		if (optionalUser.isPresent()) {
			// エラーメッセージ
			mav.addObject("message", "入力ユーザー名は既に使用済みの為、登録できません。");
			// 画面に出力するViewを指定
			mav.setViewName("view/insertUser");
			// ModelとView情報を返す
			return mav;
		}
		
		//新しいパスワードと確認用のパスワードの確認
		if (!checkUser.getNewPassword().equals(checkUser.getConfirmPassword())) {
			// エラーメッセージ
			mav.addObject("message", "新パスワードと確認パスワードが合っていません！");
			//画面に出力するViewを指定
			mav.setViewName("view/insertUser");
			//ModelとView情報を返す
			return mav;
		}
		
		// 入力エラーがある場合
		if (result.hasErrors()) {
			//パスワードについては別途エラー処理（Validation使わず）
			if (checkUser.getNewPassword() == "") {
			    mav.addObject("passwordError", "パスワードを入力してください");
			}
			// エラーメッセージ
			mav.addObject("message", "入力内容に誤りがあります");
			// 画面に出力するViewを指定
			mav.setViewName("view/insertUser");
			// ModelとView情報を返す
			return mav;
		}
		


		//チェック用のEntityからUserのオブジェクトに格納
		User newUser = new User();
		newUser.setUserid(checkUser.getUserid());
		newUser.setPassword(checkUser.getConfirmPassword());
		newUser.setEmail(checkUser.getEmail());
		newUser.setAuthority(checkUser.getAuthority());

		// 入力されたデータをDBに保存
		userinfo.saveAndFlush(newUser);

		//Viewに渡す変数をModelに格納
		mav.addObject("message", "ユーザー登録完了しました！");

		// 画面に出力するViewを指定
		mav.setViewName("view/insertUser");
		// ModelとView情報を返す
		return mav;
	}

	/*
	 * 「deleteUser」へアクセスがあった場合
	 */
	@GetMapping("/deleteUser")
	public ModelAndView deleteUserForm(HttpServletRequest request, ModelAndView mav) {

		//セッションからユーザー情報取得
		User user = (User) session.getAttribute("user");

		//ユーザー検索
		Optional<User> optionalUser = userinfo.findByUserid(request.getParameter("userid"));

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
		//ユーザー一覧を映し出した状態でDB内からデータが消えた場合のチェック
		if (!(optionalUser.isPresent())) {
			// エラーメッセージ
			mav.addObject("errorMessage", "削除対象のユーザーが存在しない為、ユーザー削除処理は行えませんでした。");
			mav.addObject("cmd", "listUser");
			mav.addObject("next", "[一覧表示へ戻る]");
			// 画面に出力するViewを指定
			mav.setViewName("view/error");
			// ModelとView情報を返す
			return mav;
		}

		//受け取ったユーザー情報を削除
		userinfo.deleteById(request.getParameter("userid"));

		//リダイレクト先を指定
		mav = new ModelAndView("redirect:/listUser");
		//ModelとView情報を返す
		return mav;
	}

	/**
	 * Exception発生時の処理メソッド.
	 */
	@ExceptionHandler(Exception.class)
	public ModelAndView ExceptionHandler(Exception e) {

		ModelAndView mav = new ModelAndView();

		// エラーメッセージ
		mav.addObject("errorMessage", "エラー内容：" + e.getMessage());
		mav.addObject("cmd", "logout");
		mav.addObject("next", "[ログイン画面へ]");
		//画面に出力するViewを指定
		mav.setViewName("view/error");
		//ModelとView情報を返す
		return mav;
	}
}
