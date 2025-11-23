package gui;

import model.Database;
import model.Record;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class Monitoring extends JPanel {

    private final Database db;

    private final JLabel lblUniqueItems;
    private final JLabel lblTotalUnits;
    private final JLabel lblTotalValue;
    private final JLabel lblLowStock;

    private final JTextArea logArea;
    private final JLabel topStatsLabelSup;
    private final JLabel topStatsLabelItem;



    public Monitoring(Database db) {
        this.db = db;
        setLayout(new BorderLayout());

        JPanel statsPanel = new JPanel(new GridLayout(2, 2));
        lblUniqueItems = new JLabel();
        lblTotalUnits = new JLabel();
        lblTotalValue = new JLabel();
        lblLowStock = new JLabel();

        statsPanel.add(lblUniqueItems);
        statsPanel.add(lblTotalUnits);
        statsPanel.add(lblTotalValue);
        statsPanel.add(lblLowStock);

        add(statsPanel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel statsMainPanel = new JPanel(new GridLayout(1, 2));

        topStatsLabelSup = new JLabel();
        topStatsLabelSup.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane statsScrollPanelSup = new JScrollPane(topStatsLabelSup);
        statsScrollPanelSup.setBorder(BorderFactory.createTitledBorder("Топ поставщиков"));
        statsMainPanel.add(statsScrollPanelSup);

        topStatsLabelItem = new JLabel();
        topStatsLabelItem.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane statsScrollPanelItem = new JScrollPane(topStatsLabelItem);
        statsScrollPanelItem.setBorder(BorderFactory.createTitledBorder("Топ товаров"));
        statsMainPanel.add(statsScrollPanelItem);

        tabbedPane.addTab("Статистика", statsMainPanel);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Журнал операций"));
        tabbedPane.addTab("Журнал операций", logScrollPane);

        add(tabbedPane, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {

        List<Record> all = db.getAll();

        lblUniqueItems.setText("Уникальных товаров: " + all.size());

        int totalUnits = all.stream().mapToInt(r -> r.quantity).sum();
        lblTotalUnits.setText("Всего единиц товара: " + totalUnits);

        double totalValue = all.stream().mapToDouble(r -> r.quantity * r.price).sum();
        lblTotalValue.setText(String.format("Общая стоимость запасов: %.2f", totalValue));

        long low = all.stream().filter(r -> r.quantity < 5).count();
        lblLowStock.setText("Товаров с низким остатком (<5): " + low);

        Map<String, Integer> quantityBySupplier = new HashMap<>();
        Map<String, Double> valueBySupplier = new HashMap<>();

        for (Record r : all) {
            quantityBySupplier.merge(r.supplier, r.quantity, Integer::sum);
            valueBySupplier.merge(r.supplier, r.quantity * r.price, Double::sum);
        }

        var topQtySuppliers = quantityBySupplier.entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .toList();

        var topValueSuppliers = valueBySupplier.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .toList();

        var topExpensiveItems = all.stream()
                .sorted((a, b) -> Double.compare(b.price, a.price))
                .limit(5)
                .toList();

        var topQuantityItems = all.stream()
                .sorted((a, b) -> Integer.compare(b.quantity, a.quantity))
                .limit(5)
                .toList();

        StringBuilder StrBuildSup = new StringBuilder("<html>");
        StringBuilder StrBuildItem = new StringBuilder("<html>");

        StrBuildSup.append("<b>ТОП поставщиков (по стоимости):</b><br>");
        for (var e : topValueSuppliers)
            StrBuildSup.append(e.getKey()).append(" — ").append(String.format("%.2f", e.getValue())).append("<br>");

        StrBuildSup.append("<br><b>ТОП поставщиков (по количеству товаров):</b><br>");
        for (var e : topQtySuppliers)
            StrBuildSup.append(e.getKey()).append(" — ").append(e.getValue()).append("<br>");

        StrBuildItem.append("<b>Самые дорогие товары:</b><br>");
        for (Record r : topExpensiveItems)
            StrBuildItem.append(r.name).append(" — ").append(String.format("%.2f", r.price)).append("<br>");

        StrBuildItem.append("<br><b>Самые многочисленные товары:</b><br>");
        for (Record r : topQuantityItems)
            StrBuildItem.append(r.name).append(" — ").append(r.quantity).append("<br>");

        StrBuildSup.append("</html>");
        StrBuildItem.append("</html>");

        topStatsLabelSup.setText(StrBuildSup.toString());
        topStatsLabelItem.setText(StrBuildItem.toString());

        loadLogTail();
    }

    private void loadLogTail() {
        File f = new File("operations.log");
        if (!f.exists()) {
            logArea.setText("Файл operations.log отсутствует.");
            return;
        }

        try {
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null)
                    lines.add(line);
            }

            int start = Math.max(0, lines.size() - 200);
            String result = String.join("\n", lines.subList(start, lines.size()));
            logArea.setText(result);

        } catch (Exception e) {
            logArea.setText("Ошибка чтения лога: " + e.getMessage());
        }
    }
}
