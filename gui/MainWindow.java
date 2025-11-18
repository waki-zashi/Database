package gui;

import model.Database;
import model.Record;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class MainWindow extends JFrame {

    private final Database db = new Database("products.db");
    private final DefaultTableModel tableModel;

    public MainWindow() {
        setTitle("Учет товаров магазина");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try { db.load(); }
        catch (IOException e) { e.printStackTrace(); }

        String[] columns = {"ID", "Название", "Кол-во", "Цена", "Поставщик"};

        tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        refreshTable();

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 8));

        JButton addBtn = new JButton("Добавить");
        JButton supplyBtn = new JButton("Поставка");
        JButton sellBtn = new JButton("Продажа");
        JButton deleteBtn = new JButton("Удалить");
        JButton searchBtn = new JButton("Поиск");
        JButton backupBtn = new JButton("Backup");
        JButton restoreBtn = new JButton("Restore");
        JButton refreshBtn = new JButton("Обновить");

        buttons.add(addBtn);
        buttons.add(supplyBtn);
        buttons.add(sellBtn);
        buttons.add(deleteBtn);
        buttons.add(searchBtn);
        buttons.add(backupBtn);
        buttons.add(restoreBtn);
        buttons.add(refreshBtn);

        add(buttons, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addRecord());
        supplyBtn.addActionListener(e -> supply());
        sellBtn.addActionListener(e -> sell());
        deleteBtn.addActionListener(e -> deleteRecord());
        searchBtn.addActionListener(e -> search());
        backupBtn.addActionListener(e -> backup());
        restoreBtn.addActionListener(e -> restore());
        refreshBtn.addActionListener(e -> refreshTable());

        setVisible(true);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Record r : db.getAll()) {
            tableModel.addRow(new Object[]{
                    r.id, r.name, r.quantity, r.price, r.supplier
            });
        }
    }

    private void addRecord() {
        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JTextField qty = new JTextField();
        JTextField price = new JTextField();
        JTextField sup = new JTextField();

        Object[] fields = {
                "ID:", id,
                "Название:", name,
                "Количество:", qty,
                "Цена:", price,
                "Поставщик:", sup
        };

        int res = JOptionPane.showConfirmDialog(this, fields,
                "Добавить товар", JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            Record r = new Record(
                    Integer.parseInt(id.getText()),
                    name.getText(),
                    Integer.parseInt(qty.getText()),
                    Double.parseDouble(price.getText()),
                    sup.getText()
            );

            if (!db.addRecord(r)) {
                JOptionPane.showMessageDialog(this, "ID уже существует!");
                return;
            }

            try { db.save(); } catch (IOException ex) { ex.printStackTrace(); }

            refreshTable();
        }
    }

    private void supply() {
        JTextField id = new JTextField();
        JTextField amount = new JTextField();

        Object[] fields = {
                "ID товара:", id,
                "Количество поставки:", amount
        };

        int res = JOptionPane.showConfirmDialog(this, fields,
                "Поставка товара", JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            if (db.supply(Integer.parseInt(id.getText()),
                    Integer.parseInt(amount.getText()))) {
                try { db.save(); } catch (IOException e) { e.printStackTrace(); }
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Товар не найден");
            }
        }
    }

    private void sell() {
        JTextField id = new JTextField();
        JTextField amount = new JTextField();

        Object[] fields = {
                "ID товара:", id,
                "Количество продажи:", amount
        };

        int res = JOptionPane.showConfirmDialog(this, fields,
                "Продажа товара", JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            if (!db.sell(Integer.parseInt(id.getText()),
                    Integer.parseInt(amount.getText()))) {
                JOptionPane.showMessageDialog(this,
                        "Недостаточно товара или товар не найден");
            } else {
                try { db.save(); } catch (IOException e) { e.printStackTrace(); }
                refreshTable();
            }
        }
    }

    private void deleteRecord() {
        String id = JOptionPane.showInputDialog(this, "Введите ID для удаления:");
        if (id == null) return;

        db.deleteByField("id", id);
        try { db.save(); } catch (IOException e) { e.printStackTrace(); }

        refreshTable();
    }

    private void search() {
        String field = JOptionPane.showInputDialog(this,
                "Поле поиска (id/name/supplier/price/quantity):");
        String value = JOptionPane.showInputDialog(this, "Значение:");

        if (field == null || value == null) return;

        List<Record> results = db.search(field, value);

        tableModel.setRowCount(0);
        for (Record r : results) {
            tableModel.addRow(new Object[]{
                    r.id, r.name, r.quantity, r.price, r.supplier
            });
        }
    }

    private void backup() {
        try {
            db.backup("products_backup.db");
            JOptionPane.showMessageDialog(this, "Backup OK");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка backup");
        }
    }

    private void restore() {
        try {
            db.restore("products_backup.db");
            JOptionPane.showMessageDialog(this, "Restore OK");
            refreshTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка restore");
        }
    }

}
