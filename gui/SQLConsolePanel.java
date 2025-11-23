package gui;

import model.Database;
import model.Record;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SQLConsolePanel extends JPanel {

    private final Database db;
    private final JTextArea inputArea;
    private final DefaultTableModel tableModel;

    public SQLConsolePanel(Database db) {
        this.db = db;
        setLayout(new BorderLayout());

        inputArea = new JTextArea(5, 50);
        inputArea.setBorder(BorderFactory.createTitledBorder("Введите SQL-like запрос"));
        add(new JScrollPane(inputArea), BorderLayout.NORTH);

        JButton runBtn = new JButton("Выполнить");
        add(runBtn, BorderLayout.SOUTH);

        String[] columns = {"ID", "Название", "Количество", "Цена", "Поставщик"};
        tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        runBtn.addActionListener(e -> execute());
    }

    private void execute() {
        String cmd = inputArea.getText().trim();
        if (cmd.isEmpty()) return;

        try {
            if (cmd.equalsIgnoreCase("HELP")) {
                showHelp();
                return;
            }

            if (cmd.toUpperCase().startsWith("SELECT")) {
                runSelect(cmd);
            } else if (cmd.toUpperCase().startsWith("DELETE")) {
                runDelete(cmd);
            } else if (cmd.toUpperCase().startsWith("INSERT")) {
                runInsert(cmd);
            } else if (cmd.toUpperCase().startsWith("UPDATE")) {
                runUpdate(cmd);
            } else {
                JOptionPane.showMessageDialog(this, "Неизвестная команда");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }

    private void runSelect(String cmd) {
        tableModel.setRowCount(0);

        if (!cmd.toUpperCase().contains("WHERE")) {
            for (Record r : db.getAll())
                addRow(r);
            return;
        }

        String condition = cmd.substring(cmd.toUpperCase().indexOf("WHERE") + 5).trim();

        String operator;
        String field;
        String value;

        if (condition.contains(">=")) {
            operator = ">=";
        } else if (condition.contains("<=")) {
            operator = "<=";
        } else if (condition.contains(">")) {
            operator = ">";
        } else if (condition.contains("<")) {
            operator = "<";
        } else {
            operator = "=";
        }

        String[] parts = condition.split(operator);

        field = parts[0].trim();
        value = parts[1].trim().replace("\"", "");

        List<Record> found = db.search(field, value, operator);

        for (Record r : found)
            addRow(r);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Record r : db.getAll()) {
            tableModel.addRow(new Object[]{
                    r.id, r.name, r.quantity, r.price, r.supplier
            });
        }
    }

    private void runDelete(String cmd) {
        if (cmd.equalsIgnoreCase("DELETE *")) {
            db.deleteAll();
            try { db.save(); } catch (Exception ignored) {}
            refreshTable();
            return;
        }

        String upper = cmd.toUpperCase();
        if (!upper.contains("WHERE")) {
            JOptionPane.showMessageDialog(this, "Формат: DELETE * WHERE field=value");
            return;
        }

        String condition = cmd.substring(upper.indexOf("WHERE") + 5).trim();
        String[] parts = condition.split("=");

        if (parts.length < 2) {
            JOptionPane.showMessageDialog(this, "Неверное условие");
            return;
        }

        String field = parts[0].trim();
        String value = parts[1].trim().replace("\"", "");

        int deleted = db.deleteWhere(field, value);

        try { db.save(); } catch (Exception ignored) {}

        JOptionPane.showMessageDialog(this,"Удалено записей: " + deleted);
    }

    private void runInsert(String cmd) throws Exception {
        cmd = cmd.substring("INSERT".length()).trim();

        String[] tokens = cmd.split("\\s+");
        int id = 0, quantity = 0;
        double price = 0;
        String name = "", supplier = "";

        for (String t : tokens) {
            String[] p = t.split("=");
            if (p.length < 2) continue;
            String key = p[0].trim();
            String val = p[1].trim().replace("\"", "");

            switch (key) {
                case "id" -> id = Integer.parseInt(val);
                case "name" -> name = val;
                case "quantity" -> quantity = Integer.parseInt(val);
                case "price" -> price = Double.parseDouble(val);
                case "supplier" -> supplier = val;
            }
        }

        db.addRecord(new Record(id, name, quantity, price, supplier));
        JOptionPane.showMessageDialog(this, "Добавлено");

        try { db.save(); } catch (Exception ignored) {}
    }

    private void runUpdate(String cmd) {
        String upper = cmd.toUpperCase();

        if (!upper.contains("SET") || !upper.contains("WHERE")) {
            JOptionPane.showMessageDialog(this, "Ошибка синтаксиса UPDATE");
            return;
        }

        String setPart = cmd.substring(upper.indexOf("SET") + 3, upper.indexOf("WHERE")).trim();

        String[] setSplit = setPart.split("=");
        String field = setSplit[0].trim();
        String newValue = setSplit[1].trim().replace("\"", "");

        String condPart = cmd.substring(upper.indexOf("WHERE") + 5).trim();

        String[] condSplit = condPart.split("=");
        String whereField = condSplit[0].trim();
        String whereValue = condSplit[1].trim().replace("\"", "");

        int updated = db.update(field, newValue, whereField, whereValue);

        refreshTable();

        try { db.save(); } catch (Exception ignored) {}

        JOptionPane.showMessageDialog(this, "Обновлено записей: " + updated);
    }


    private void addRow(Record r) {
        tableModel.addRow(new Object[]{r.id, r.name, r.quantity, r.price, r.supplier});
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(this,
                """
                Доступные команды:
                
                SELECT * — все записи
                SELECT * WHERE <...> — выборка по параметру
                Пример: SELECT * WHERE price>1000
                
                DELETE * — удаление всех записей
                DELETE * WHERE <...> — удаление по параметру
                Пример: DELETE * WHERE quantity<5
                
                INSERT <...> — создание записи с заполнением всех полей
                Пример: INSERT id=777 name="Лабубу" quantity=7 price=1000 supplier="Алибаба"
                
                UPDATE SET <...> WHERE <...> — изменение полей
                Пример: UPDATE SET price=900 WHERE name="TV"
                
                HELP — помощь
                """);
    }
}
