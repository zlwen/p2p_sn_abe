package application;

import java.io.IOException;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import org.apache.log4j.PropertyConfigurator;
import org.controlsfx.control.ButtonBar.ButtonType;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogAction;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.edu.pku.ss.bean.User;

public class Main extends Application {
	private static boolean firstUse = true;
	static {
		String file = "log4j/log4j.properties";
		PropertyConfigurator.configure(file);
	}

	private Logger logger = LoggerFactory.getLogger(Main.class);

	@Override
	public void start(Stage stage) {
		logger.info("program is running...");
		if (firstUse) {
			showInputDialog(stage);
		} else {
			showMainDialog(stage);
		}
	}

	private void showMainDialog(Stage stage) {
		Parent root = null;
		try {
			// root = FXMLLoader.load(getClass().getResource("test.fxml"));
			root = FXMLLoader.load(getClass().getResource("main.fxml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Scene scene = new Scene(root, 601, 400);
		stage.setTitle("P2P社交网络下的ABE原型系统");
		stage.setScene(scene);
		stage.show();
	}

	// private void validate() {
	// actionLogin.disabledProperty().set(
	// txUserName.getText().trim().isEmpty()
	// || txPassword.getText().trim().isEmpty());
	// }
	private void showProgressDialog(){
		Task<Object> worker = new Task<Object>() {
			@Override
			protected Object call() throws Exception {
				for (int i = 0; i < 100; i++) {
					updateProgress(i, 99);
					updateMessage("progress: " + i);
					System.out.println("progress: " + i);
					Thread.sleep(50);
				}
				return null;
			}
		};

		Dialogs.create()
			   .title("正在初始化...")
               .message("Now Loading...").showWorkerProgress(
				worker);
		Thread th = new Thread(worker);
		th.setDaemon(true);
		th.start();
	}

	private void showInputDialog(Stage stage) {

		final TextField txEmail = new TextField();
		final TextField txNickName = new TextField();
		final Action actionLogin = new DialogAction("加入P2P网络",
				ButtonType.OK_DONE, false, true, true, ae -> {/*
															 * real login code
															 * here
															 */
					firstUse = false;
					String email = txEmail.getText();
					String nickName = txNickName.getText();
					User.self().setEmail(email);
					User.self().setNickName(nickName);
					User.self().init();
//					showProgressDialog();
					showMainDialog(stage);
				}) {

			public String toString() {
				return "LOGIN";
			};
		};

		Dialog dlg = new Dialog(stage, "输入基本信息");

		final GridPane content = new GridPane();
		content.setHgap(10);
		content.setVgap(10);

		content.add(new Label("Email"), 0, 0);
		content.add(txEmail, 1, 0);
		GridPane.setHgrow(txEmail, Priority.ALWAYS);
		content.add(new Label("昵称"), 0, 1);
		content.add(txNickName, 1, 1);
		GridPane.setHgrow(txNickName, Priority.ALWAYS);

		dlg.setResizable(false);
		dlg.setIconifiable(false);
		// dlg.setGraphic(new ImageView(HelloDialog.class.getResource(
		// "login.png").toString()));
		dlg.setContent(content);
		dlg.getActions().addAll(actionLogin);
		// validate();

		// Platform.runLater( () -> txUserName.requestFocus() );

		Action response = dlg.show();
		System.out.println("response: " + response);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
