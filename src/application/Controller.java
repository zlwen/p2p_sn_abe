package application;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;

import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.edu.pku.ss.bean.Friend;
import cn.edu.pku.ss.bean.User;
import cn.edu.pku.ss.service.FriendManager;

public class Controller implements Initializable {
	private Logger logger = LoggerFactory.getLogger(Controller.class);

	// @FXML
	// private ScrollPane friendsScrollPane;
	@FXML
	private Label showLabel;
	@FXML
	private TreeView<String> friendsTreeView;

	@FXML
	protected void showLabel(ActionEvent event) {
		showLabel.setText("Sign in button pressed");
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		logger.info("initilize ...");
		TreeItem<String> rootNode = new TreeItem<String>("我的好友");
		FriendManager manager = User.self().getFriendManager();
		for(String group : manager.listAllGroups()){
			TreeItem<String> item = new TreeItem<String>(group);
			List<Friend> members = manager.getGroupMembers(group);
			if(members != null && members.size() > 0){
				for(Friend f : members){
					TreeItem<String> subItem = new TreeItem<String>(f.getNickName());
					item.getChildren().add(subItem);
				}
			}
			rootNode.getChildren().add(item);
		}

		friendsTreeView.setRoot(rootNode);
		friendsTreeView.setEditable(true);
	}
//
//	public void init(){
//				List<Employee> employees = Arrays.<Employee> asList(new Employee(
//				"Ethan Williams", "Sales Department"), new Employee(
//				"Emma Jones", "Sales Department"), new Employee(
//				"Michael Brown", "Sales Department"), new Employee(
//				"Anna Black", "Sales Department"), new Employee("Rodger York",
//				"Sales Department"), new Employee("Susan Collins",
//				"Sales Department"), new Employee("Mike Graham", "IT Support"),
//				new Employee("Judy Mayer", "IT Support"), new Employee(
//						"Gregory Smith", "IT Support"), new Employee(
//						"Jacob Smith", "Accounts Department"), new Employee(
//						"Isabella Johnson", "Accounts Department"));
//		TreeItem<String> rootNode = new TreeItem<String>(
//				"MyCompany Human Resources");
//		// new TreeItem<String>("MyCompany Human Resources", rootIcon);
//		rootNode.setExpanded(true);
//		for (Employee employee : employees) {
//			TreeItem<String> empLeaf = new TreeItem<String>(employee.getName());
//			boolean found = false;
//			for (TreeItem<String> depNode : rootNode.getChildren()) {
//				if (depNode.getValue().contentEquals(employee.getDepartment())) {
//					depNode.getChildren().add(empLeaf);
//					found = true;
//					break;
//				}
//			}
//			if (!found) {
//				TreeItem depNode = new TreeItem(employee.getDepartment());
//				// TreeItem depNode = new TreeItem(employee.getDepartment(),
//				// new ImageView(depIcon)
//				// );
//				rootNode.getChildren().add(depNode);
//				depNode.getChildren().add(empLeaf);
//			}
//		}
//
//	}
	@FXML
	protected void showAddFriendDialog() {
		Optional<String> email = Dialogs.create().title("添加好友")
				.message("请输入好友的Email").showTextInput();
		if (email.isPresent()) {
			logger.info("begin search user's ip by email:" + email);
			User.self().addFriend(email.get());
		}
	}

	@FXML
	protected void showShareFileDialog() {
		FileChooser chooser = new FileChooser();
		File file = chooser.showOpenDialog(null);
		Optional<String> policy = Dialogs.create().title("共享文件")
				.message("请输入共享策略").showTextInput();
		if (policy.isPresent()) {
			logger.info("share file(" + file.getName() + ") with policy:"
					+ policy);
			new Thread(){
				public void run(){
					User.self().getShareManager().share(file, policy.get());
				}
			}.start();
		}
	}

//	public static class Employee {
//
//		private final SimpleStringProperty name;
//		private final SimpleStringProperty department;
//
//		private Employee(String name, String department) {
//			this.name = new SimpleStringProperty(name);
//			this.department = new SimpleStringProperty(department);
//		}
//
//		public String getName() {
//			return name.get();
//		}
//
//		public void setName(String fName) {
//			name.set(fName);
//		}
//
//		public String getDepartment() {
//			return department.get();
//		}
//
//		public void setDepartment(String fName) {
//			department.set(fName);
//		}
//	}
}
