package model;

import java.io.*;
import java.util.*;

public class Database {

    private final String filename;

    private Map<Integer, Record> table = new HashMap<>();

    public Database(String filename) {
        this.filename = filename;
    }

    public void load() throws IOException {
        table.clear();

        File f = new File(filename);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                Record r = Record.fromString(line);
                table.put(r.id, r);
            }
        }
    }

    public void save() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (Record r : table.values()) {
                bw.write(r.toString());
                bw.newLine();
            }
        }
    }

    public boolean addRecord(Record r) {
        if (table.containsKey(r.id))
            return false;
        table.put(r.id, r);
        return true;
    }

    public boolean supply(int id, int amount) {
        Record r = table.get(id);
        if (r == null) return false;
        r.quantity += amount;
        return true;
    }

    public boolean sell(int id, int amount) {
        Record r = table.get(id);
        if (r == null || r.quantity < amount)
            return false;
        r.quantity -= amount;
        return true;
    }

    public boolean deleteById(int id) {
        return table.remove(id) != null;
    }

    public int deleteByField(String field, String value) {
        int removed = 0;

        Iterator<Record> it = table.values().iterator();
        while (it.hasNext()) {
            Record r = it.next();
            boolean match = switch (field) {
                case "name" -> r.name.equals(value);
                case "supplier" -> r.supplier.equals(value);
                case "price" -> r.price == Double.parseDouble(value);
                case "quantity" -> r.quantity == Integer.parseInt(value);
                case "id" -> r.id == Integer.parseInt(value); // но это O(1) через deleteById
                default -> false;
            };

            if (match) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public List<Record> search(String field, String value) {
        List<Record> res = new ArrayList<>();

        switch (field) {

            case "id" -> {
                Record r = table.get(Integer.parseInt(value));
                if (r != null) res.add(r);
                return res;
            }

            default -> {
                for (Record r : table.values()) {
                    boolean match = switch (field) {
                        case "name" -> r.name.equals(value);
                        case "supplier" -> r.supplier.equals(value);
                        case "price" -> r.price == Double.parseDouble(value);
                        case "quantity" -> r.quantity == Integer.parseInt(value);
                        default -> false;
                    };
                    if (match) res.add(r);
                }
                return res;
            }
        }
    }

    public boolean editRecord(int id, Record newData) {
        if (!table.containsKey(id)) return false;
        table.put(id, newData);
        return true;
    }

    public void backup(String backupFile) throws IOException {
        try (InputStream in = new FileInputStream(filename);
             OutputStream out = new FileOutputStream(backupFile)) {
            in.transferTo(out);
        }
    }

    public void restore(String backupFile) throws IOException {
        try (InputStream in = new FileInputStream(backupFile);
             OutputStream out = new FileOutputStream(filename)) {
            in.transferTo(out);
        }
        load();
    }

    public List<Record> getAll() {
        return new ArrayList<>(table.values());
    }

    public void clear() {
        table.clear();
    }
}
