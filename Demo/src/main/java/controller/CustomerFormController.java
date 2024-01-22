package controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import db.DBConnection;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.Scene;
import dto.CustomerDto;
import dto.tm.CustomerTm;
import model.CustomerModel;
import model.impl.CustomerModelImpl;

import java.sql.*;

import java.io.IOException;
import java.sql.Statement;
import java.util.List;
import java.util.function.Predicate;

public class CustomerFormController {

    @FXML
    private BorderPane pane;

    @FXML
    private JFXTextField txtId;

    @FXML
    private JFXTextField txtName;

    @FXML
    private JFXTextField txtAddress;

    @FXML
    private JFXTextField txtSalary;

    @FXML
    private JFXTextField txtSearch;

    @FXML
    private JFXTreeTableView<CustomerTm> tblCustomer;

    @FXML
    private TreeTableColumn<?, ?> colId;

    @FXML
    private TreeTableColumn<?, ?> colName;

    @FXML
    private TreeTableColumn<?, ?> colAddress;

    @FXML
    private TreeTableColumn<?, ?> colSalary;

    @FXML
    private TreeTableColumn<?, ?> colOption;

    @FXML
    private JFXButton saveButton;

    @FXML
    private JFXButton goBackButton;

    private CustomerModel customerModel=new CustomerModelImpl();

    public void initialize(){
        colId.setCellValueFactory(new TreeItemPropertyValueFactory<>("id"));
        colName.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
        colAddress.setCellValueFactory(new TreeItemPropertyValueFactory<>("address"));
        colSalary.setCellValueFactory(new TreeItemPropertyValueFactory<>("salary"));
        colOption.setCellValueFactory(new TreeItemPropertyValueFactory<>("btn"));
        loadCustomerTable();

        tblCustomer.getSelectionModel().selectedItemProperty().addListener((observableValue, customerTmTreeItem, newValue) ->{
            setData(newValue);

        } );

        txtSearch.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String newValue) {
                tblCustomer.setPredicate(new Predicate<TreeItem<CustomerTm>>() {
                    @Override
                    public boolean test(TreeItem<CustomerTm> treeItem) {
                        return treeItem.getValue().getId().contains(newValue)|| treeItem.getValue().getName().contains(newValue);
                    }
                });
            }
        });

    }

    private void setData(TreeItem<CustomerTm> newValue) {
        if (newValue != null ) {
            CustomerTm customerTm = newValue.getValue();
            txtId.setEditable(false);
            txtId.setText(customerTm.getId());
            txtName.setText(customerTm.getName());
            txtAddress.setText(customerTm.getAddress());
            txtSalary.setText(String.valueOf(customerTm.getSalary()));
        }
    }

    @FXML
    void goBackButtonOnAction(ActionEvent event) {
        Stage stage=(Stage) goBackButton.getScene().getWindow();
        try {
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/view/DashboardForm.fxml"))));
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void reloadButtonOnAction(ActionEvent event) {
        loadCustomerTable();
        tblCustomer.refresh();
        clearFields();
    }
    private void clearFields() {
        tblCustomer.refresh();
        txtSalary.clear();
        txtAddress.clear();
        txtName.clear();
        txtId.clear();
        txtId.setEditable(true);
    }
    private void loadCustomerTable() {
        ObservableList<CustomerTm> tmList = FXCollections.observableArrayList();
        try {
            List<CustomerDto> dtoList = customerModel.allCustomers();
            for (CustomerDto dto:dtoList){
                JFXButton btn = new JFXButton("Delete");
                btn.setStyle("-fx-background-color: red;");

                CustomerTm c = new CustomerTm(
                        dto.getId(),
                        dto.getName(),
                        dto.getAddress(),
                        dto.getSalary(),
                        btn
                );

                btn.setOnAction(actionEvent -> {
                    deleteCustomer(c.getId());
                });
                tmList.add(c);
            }
            TreeItem<CustomerTm> treeItem = new RecursiveTreeItem<>(tmList, RecursiveTreeObject::getChildren);
            tblCustomer.setRoot(treeItem);
            tblCustomer.setShowRoot(false);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
           e.printStackTrace();
        }
    }
    private void deleteCustomer(String id) {
        try {
            boolean isDeleted = customerModel.deleteCustomer(id);

            if (isDeleted){
                new Alert(Alert.AlertType.INFORMATION,"Customer Deleted!").show();
                loadCustomerTable();
                clearFields();
            }else{
                new Alert(Alert.AlertType.ERROR,"Something went wrong!").show();
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }


    @FXML
    void saveButtonOnAction(ActionEvent event) {
        if(!(txtId.getText().isEmpty()||txtName.getText().isEmpty()||txtAddress.getText().isEmpty()||txtSalary.getText().isEmpty())) {
            try {
                boolean isSaved= customerModel.saveCustomer(new CustomerDto(txtId.getText(),
                        txtName.getText(),
                        txtAddress.getText(),
                        Double.parseDouble(txtSalary.getText())
                ));

                if (isSaved) {
                    new Alert(Alert.AlertType.INFORMATION, "Customer Saved!").show();
                    loadCustomerTable();
                    clearFields();
                }

            } catch (SQLIntegrityConstraintViolationException ex) {
                new Alert(Alert.AlertType.ERROR, "Duplicate Entry").show();
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }else{
            new Alert(Alert.AlertType.WARNING, "Please Enter the Data").show();
        }

    }

    @FXML
    void updateButtonOnAction(ActionEvent event) {
        if(!(txtId.getText().isEmpty()||txtName.getText().isEmpty()||txtAddress.getText().isEmpty()||txtSalary.getText().isEmpty())) {
            try {
                boolean isUpdated = customerModel.updateCustomer(new CustomerDto(txtId.getText(),
                        txtName.getText(),
                        txtAddress.getText(),
                        Double.parseDouble(txtSalary.getText())
                ));
                if (isUpdated) {
                    new Alert(Alert.AlertType.INFORMATION, "Customer  Updated!").show();
                    loadCustomerTable();
                    clearFields();
                }

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }else{
            new Alert(Alert.AlertType.WARNING, "Please Enter the Data").show();
        }

    }

}
