package controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import dto.OrderDetailsDto;
import dto.OrderDto;
import dto.tm.OrderDetailsTm;
import dto.tm.OrdersTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.Stage;
import model.OrderDetailsModel;
import model.OrderModel;
import model.impl.OrderDetailsModelImpl;
import model.impl.OrderModelImpl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class OrdersFormController {
    public JFXButton goBackButton;
    public JFXTreeTableView<OrdersTm> tblOrder;
    public TreeTableColumn colId;
    public TreeTableColumn colDate;
    public TreeTableColumn colcustId;
    public JFXTreeTableView <OrderDetailsTm>tblOrderDetails;
    public TreeTableColumn colOrderId;
    public TreeTableColumn colCode;
    public TreeTableColumn colPrice;
    public TreeTableColumn colQty;

    private OrderModel orderModel=new OrderModelImpl();
    private OrderDetailsModel orderDetailsModel=new OrderDetailsModelImpl();

    public void initialize(){
        colId.setCellValueFactory(new TreeItemPropertyValueFactory<>("orderId"));
        colDate.setCellValueFactory(new TreeItemPropertyValueFactory<>("date"));
        colcustId.setCellValueFactory(new TreeItemPropertyValueFactory<>("custId"));

        loardOrderTable();
        tblOrder.getSelectionModel().selectedItemProperty().addListener((observableValue, ordersTmTreeItem, newValue) -> {
            loardOrderDetailsTable(newValue);
            colOrderId.setCellValueFactory(new TreeItemPropertyValueFactory<>("orderId"));
            colCode.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemCode"));
            colQty.setCellValueFactory(new TreeItemPropertyValueFactory<>("qty"));
            colPrice.setCellValueFactory(new TreeItemPropertyValueFactory<>("unitPrice"));
        });

    }

    private void loardOrderDetailsTable(TreeItem<OrdersTm> newValue) {
        if (newValue != null ) {
            OrdersTm ordersTm=newValue.getValue();
            ObservableList<OrderDetailsTm> tmList = FXCollections.observableArrayList();
            try {
                List<OrderDetailsDto> dtoList = orderDetailsModel.getOrders(ordersTm.getOrderId());
                for (OrderDetailsDto dto:dtoList) {
                   OrderDetailsTm orderDetailsTm=new OrderDetailsTm(
                           dto.getOrderId(),
                           dto.getItemCode(),
                           dto.getQty(),
                           dto.getUnitPrice()
                   );
                   tmList.add(orderDetailsTm);
                }
                RecursiveTreeItem<OrderDetailsTm>treeItem=new RecursiveTreeItem<>(tmList,RecursiveTreeObject::getChildren);
                tblOrderDetails.setRoot(treeItem);
                tblOrderDetails.setShowRoot(false);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private void loardOrderTable() {
        ObservableList<OrdersTm> tmlist = FXCollections.observableArrayList();
        try {
            List<OrderDto> dtoList = orderModel.allOrders();
            for (OrderDto dto:dtoList) {
                OrdersTm ordersTm=new OrdersTm(
                        dto.getOrderId(),
                        dto.getDate(),
                        dto.getCustId()
                );
                tmlist.add(ordersTm);
            }
            RecursiveTreeItem<OrdersTm> treeItem=new RecursiveTreeItem<>(tmlist, RecursiveTreeObject::getChildren);
            tblOrder.setRoot(treeItem);
            tblOrder.setShowRoot(false);


        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public void goBackButtonOnAction(ActionEvent actionEvent) {
        Stage stage=(Stage) goBackButton.getScene().getWindow();
        try {
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/view/DashboardForm.fxml"))));
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void refreshButtonOnAction(ActionEvent actionEvent) {
        tblOrderDetails.refresh();
    }
}
