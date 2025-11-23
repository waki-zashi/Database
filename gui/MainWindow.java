package gui;

import model.Database;
import model.Record;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class MainWindow extends JFrame implements Database.DatabaseListener {

    private final Database db = new Database("products.db");
    private final DefaultTableModel tableModel;
    private final JTable table;
    private Monitoring monitoring;
    private JTabbedPane tabs;

    public MainWindow() {
        setTitle("Учет товаров магазина");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            db.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        db.addDatabaseListener(this);

        String[] columns = {"ID", "Название", "Кол-во", "Цена", "Поставщик"};

        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        refreshTable();

        JPanel mainPanel = createMainPanel();

        tabs = new JTabbedPane();
        tabs.addTab("База данных", mainPanel);
        monitoring = new Monitoring(db);
        tabs.addTab("Мониторинг", monitoring);
        tabs.addTab("SQL Console", new SQLConsolePanel(db));

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == monitoring) {
                monitoring.refresh();
            }
        });

        add(tabs);
        setVisible(true);
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(table);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

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

        mainPanel.add(buttons, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addRecord());
        supplyBtn.addActionListener(e -> supply());
        sellBtn.addActionListener(e -> sell());
        deleteBtn.addActionListener(e -> deleteRecord());
        searchBtn.addActionListener(e -> search());
        backupBtn.addActionListener(e -> backup());
        restoreBtn.addActionListener(e -> restore());
        refreshBtn.addActionListener(e -> refreshTable());

        return mainPanel;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Record r : db.getAll()) {
            tableModel.addRow(new Object[]{
                    r.id, r.name, r.quantity, r.price, r.supplier
            });
        }
    }

    @Override
    public void onDatabaseChanged() {
        refreshTable();

        if (tabs.getSelectedComponent() == monitoring) {
            monitoring.refresh();
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
            try {
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

                db.save();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка в числовых полях!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка сохранения: " + ex.getMessage());
            }
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
            try {
                if (db.supply(Integer.parseInt(id.getText()),
                        Integer.parseInt(amount.getText()))) {
                    db.save();
                } else {
                    JOptionPane.showMessageDialog(this, "Товар не найден");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка в числовых полях!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка сохранения: " + ex.getMessage());
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
            try {
                if (!db.sell(Integer.parseInt(id.getText()),
                        Integer.parseInt(amount.getText()))) {
                    JOptionPane.showMessageDialog(this,
                            "Недостаточно товара или товар не найден");
                } else {
                    db.save();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка в числовых полях!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка сохранения: " + ex.getMessage());
            }
        }
    }

    private void deleteRecord() {
        String idStr = JOptionPane.showInputDialog(this, "Введите ID для удаления:");
        if (idStr == null) return;

        try {
            int id = Integer.parseInt(idStr);

            if (!db.deleteById(id)) {
                JOptionPane.showMessageDialog(this, "Товар не найден");
                return;
            }

            db.save();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный формат ID!");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка сохранения: " + ex.getMessage());
        }
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
            JOptionPane.showMessageDialog(this, "Backup создан успешно");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка backup: " + e.getMessage());
        }
    }

    private void restore() {
        try {
            db.restore("products_backup.db");
            JOptionPane.showMessageDialog(this, "Restore выполнен успешно");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка restore: " + e.getMessage());
        }
    }

    public void refreshMonitoring() {
        if (monitoring != null) {
            monitoring.refresh();
        }
    }
}