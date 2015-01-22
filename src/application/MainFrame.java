package application;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFrame {
	private Stage stage;

	public MainFrame(Stage stage){
		this.stage = stage;
	}

	public void show(){
		Parent root = null;
		try {
			root = FXMLLoader.load(getClass().getResource("main.fxml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Scene scene = new Scene(root, 601, 400);
		stage.setTitle("P2P社交网络下的ABE原型系统");
		stage.setScene(scene);
		stage.show();
	}
}
