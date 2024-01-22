package controller;

import com.jfoenix.controls.*;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import dto.CustomerDto;
import dto.ItemDto;
import dto.OrderDetailsDto;
import dto.OrderDto;
import dto.tm.OrderTm;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.Stage;
import model.CustomerModel;
import model.ItemModel;
import model.OrderModel;
import model.impl.CustomerModelImpl;
import model.impl.ItemModelImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.impl.OrderModelImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;


public class PlaceOrderFormController {

    public JFXButton goBackButton;
    public JFXComboBox cmbItemCode;
    public JFXComboBox cmbCustId;
    public JFXTextField txtName;
    public JFXTextField txtDescription;
    public JFXTextField txtUnitPrice;
    public JFXTextField txtQty;
    public JFXTreeTableView<OrderTm>tblOrder;
    public TreeTableColumn colCode;

    public TreeTableColumn colQty;
    public TreeTableColumn colOption;
    public TreeTableColumn colDesc;
    public Label amount;
    public TreeTableColumn colAmount;
    public Label lblTotal;
    public Label lblOrederId;
    private List<CustomerDto> customers;
    private List<ItemDto> items;
    private double total=0;

    private CustomerModel customerModel = new CustomerModelImpl();
    private ItemModel itemModel = new ItemModelImpl();

    private OrderModel orderModel=new OrderModelImpl();

    private ObservableList<OrderTm> tmList=FXCollections.observableArrayList();
    public void initialize(){
        colCode.setCellValueFactory(new TreeItemPropertyValueFactory<>("code"));
        colDesc.setCellValueFactory(new TreeItemPropertyValueFactory<>("desc"));
        colQty.setCellValueFactory(new TreeItemPropertyValueFactory<>("qty"));
        colAmount.setCellValueFactory(new TreeItemPropertyValueFactory<>("amount"));
        colOption.setCellValueFactory(new TreeItemPropertyValueFactory<>("btn"));

        genarateId();

        loadCustomerIds();
        loadItemCodes();

        cmbCustId.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, id) -> {
            for (CustomerDto dto:customers) {
                if (dto.getId().equals(id)){
                    txtName.setText(dto.getName());
                }
            }
        });
        cmbItemCode.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, code) -> {
            for (ItemDto dto:items) {
                if (dto.getCode().equals(code)){
                    txtDescription.setText(dto.getDescription());
                    txtUnitPrice.setText(String.format("%.2f",dto.getUnitPrice()));
                }
            }
        });
    }

    private void loadItemCodes() {
        try {
            items = itemModel.allItems();
            ObservableList list = FXCollections.observableArrayList();
            for (ItemDto dto:items) {
                list.add(dto.getCode());
            }
            cmbItemCode.setItems(list);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadCustomerIds() {
        try {
            customers = customerModel.allCustomers();
            ObservableList list = FXCollections.observableArrayList();
            for (CustomerDto dto:customers) {
                list.add(dto.getId());
            }
            cmbCustId.setItems(list);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
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
    private boolean isExeed(int qty) throws SQLException, ClassNotFoundException {
        return ((itemModel.getItem(cmbItemCode.getValue().toString()).getQtyOnHand()) <qty);
    }
    public void addToCartButtonOnAction(ActionEvent actionEvent) {
        try {
            int qty=Integer.parseInt(txtQty.getText());
            if(isExeed(qty)){
                new Alert(Alert.AlertType.ERROR, "sorry we cannot have this Quantity...").show();
            } else {
                double amount = itemModel.getItem(cmbItemCode.getValue().toString()).getUnitPrice() * Integer.parseInt(txtQty.getText());
                JFXButton btn = new JFXButton("Delete");

                OrderTm tm = new OrderTm(
                        cmbItemCode.getValue().toString(),
                        txtDescription.getText(),
                        Integer.parseInt(txtQty.getText()),
                        amount,
                        btn
                );

                btn.setOnAction(actionEvent1 -> {
                    tmList.remove(tm);
                    total -= tm.getAmount();
                    tblOrder.refresh();
                    lblTotal.setText(String.format("%.2f", total));

                });

                boolean isExist = false;
                for (OrderTm order : tmList) {
                    if (order.getCode().equals(tm.getCode())) {
                        order.setQty(order.getQty() + tm.getQty());
                        order.setAmount(order.getAmount() + tm.getAmount());
                        isExist = true;
                        total += tm.getAmount();


                    }
                }
                    if (!isExist) {
                        tmList.add(tm);
                        total += tm.getAmount();
                    }

                TreeItem<OrderTm> treeObject = new RecursiveTreeItem<OrderTm>(tmList, RecursiveTreeObject::getChildren);
                tblOrder.setRoot(treeObject);
                tblOrder.setShowRoot(false);

                lblTotal.setText(String.format("%.2f", total));
            }

            } catch(SQLException e){
                e.printStackTrace();
            } catch(ClassNotFoundException e){
                e.printStackTrace();
            }


    }
    public void genarateId(){
        try {
            OrderDto dto = orderModel.lastOrder();
            if(dto!=null){
                String id=dto.getOrderId();
                int num=Integer.parseInt(id.split("[D]")[1]);
                num++;
                lblOrederId.setText(String.format("D%03d",num));
            }else{
                lblOrederId.setText("D001");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void placeOrderButtonOnAction(ActionEvent actionEvent) {
        List<OrderDetailsDto>list=new ArrayList<>();
        for (OrderTm tm:tmList) {
            list.add(new OrderDetailsDto(
                    lblOrederId.getText(),
                    tm.getCode(),
                    tm.getQty(),
                    tm.getAmount()/tm.getQty()
            ));
        }
        boolean isSaved = false;
        try {
            isSaved = orderModel.saveOrder(new OrderDto(
                            lblOrederId.getText(),
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd")),
                            cmbCustId.getValue().toString(),
                            list
                    )

            );
            if(isSaved){
                new Alert(Alert.AlertType.INFORMATION,"Order Saved").show();
            }else{
                new Alert(Alert.AlertType.ERROR,"Something went wrong!").show();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
