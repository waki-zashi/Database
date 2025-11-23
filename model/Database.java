package model;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

public class Database {

    private final String filename;
    private final String logFile = "operations.log";

    private final byte[] AES_KEY = "1234567890ABCDEF".getBytes();
    private final SecretKeySpec secretKey = new SecretKeySpec(AES_KEY, "AES");

    private Map<Integer, Record> table = new HashMap<>();
    private Map<String, Set<Integer>> nameIndex = new HashMap<>();
    private Map<String, Set<Integer>> supplierIndex = new HashMap<>();

    private List<DatabaseListener> listeners = new ArrayList<>();

    public Database(String filename) {
        this.filename = filename;
    }

    public interface DatabaseListener {
        void onDatabaseChanged();
    }

    public void addDatabaseListener(DatabaseListener listener) {
        listeners.add(listener);
    }

    public void removeDatabaseListener(DatabaseListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (DatabaseListener listener : listeners) {
            listener.onDatabaseChanged();
        }
    }

    private boolean compare(double a, double b, String op) {
        return switch (op) {
            case ">"  -> a > b;
            case "<"  -> a < b;
            case ">=" -> a >= b;
            case "<=" -> a <= b;
            default   -> a == b;
        };
    }

    private void log(String text) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true))) {
            bw.write("[" + new Date() + "] " + text);
            bw.newLine();
        } catch (IOException ignored) {}
    }

    private byte[] encrypt(byte[] data) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    private byte[] decrypt(byte[] data) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    public void load() throws IOException {
        File f = new File(filename);
        if (!f.exists()) return;

        autoBackup();

        try {
            byte[] encrypted = readAllBytes(f);
            byte[] decrypted = decrypt(encrypted);
            BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(decrypted)));

            table.clear();
            nameIndex.clear();
            supplierIndex.clear();

            String line;
            while ((line = br.readLine()) != null) {
                Record r = Record.fromString(line);
                table.put(r.id, r);
                indexRecord(r);
            }
        } catch (Exception e) {
            throw new IOException("Ошибка при расшифровке файла");
        }

        log("LOAD database");
        notifyListeners();
    }

    public void save() throws IOException {
        StringBuilder sb = new StringBuilder();

        for (Record r : table.values()) {
            sb.append(r.toString()).append("\n");
        }

        byte[] raw = sb.toString().getBytes();
        try {
            byte[] encrypted = encrypt(raw);
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(encrypted);
            fos.close();
        } catch (Exception e) {
            throw new IOException("Ошибка при шифровании");
        }

        log("SAVE database");
    }

    private void autoBackup() throws IOException {
        File src = new File(filename);
        File dst = new File(filename + ".bak");

        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            in.transferTo(out);
        }

        log("AUTO-BACKUP created");
    }

    private void indexRecord(Record r) {
        nameIndex.computeIfAbsent(r.name, k -> new HashSet<>()).add(r.id);
        supplierIndex.computeIfAbsent(r.supplier, k -> new HashSet<>()).add(r.id);
    }

    private void removeIndex(Record r) {
        if (nameIndex.containsKey(r.name))
            nameIndex.get(r.name).remove(r.id);
        if (supplierIndex.containsKey(r.supplier))
            supplierIndex.get(r.supplier).remove(r.id);
    }

    public boolean addRecord(Record r) {
        if (!validate(r)) return false;
        if (table.containsKey(r.id)) return false;

        table.put(r.id, r);
        indexRecord(r);
        log("ADD: " + r);
        notifyListeners();
        return true;
    }

    public boolean deleteById(int id) {
        Record r = table.remove(id);
        if (r == null) return false;

        removeIndex(r);
        log("DELETE BY ID: " + id);
        notifyListeners();
        return true;
    }

    public int deleteWhere(String field, String value) {
        List<Record> found = search(field, value);
        for (Record r : found) {
            deleteById(r.id);
        }
        return found.size();
    }

    public void deleteAll() {
        table.clear();
        nameIndex.clear();
        supplierIndex.clear();
        log("DELETE ALL");
        notifyListeners();
    }

    public boolean supply(int id, int amount) {
        Record r = table.get(id);
        if (r == null) return false;
        r.quantity += amount;
        log("SUPPLY: id=" + id + " amount=" + amount);
        notifyListeners();
        return true;
    }

    public boolean sell(int id, int amount) {
        Record r = table.get(id);
        if (r == null || r.quantity < amount) return false;
        r.quantity -= amount;
        log("SELL: id=" + id + " amount=" + amount);
        notifyListeners();
        return true;
    }

    public List<Record> search(String field, String value) {
        List<Record> res = new ArrayList<>();

        switch (field) {
            case "id" -> {
                Record r = table.get(Integer.parseInt(value));
                if (r != null) res.add(r);
                log("SEARCH id=" + value);
                return res;
            }
            case "name" -> {
                Set<Integer> ids = nameIndex.get(value);
                if (ids != null)
                    for (int id : ids) res.add(table.get(id));
                log("SEARCH name=" + value);
                return res;
            }
            case "supplier" -> {
                Set<Integer> ids = supplierIndex.get(value);
                if (ids != null)
                    for (int id : ids) res.add(table.get(id));
                log("SEARCH supplier=" + value);
                return res;
            }
        }

        for (Record r : table.values()) {
            boolean match = switch (field) {
                case "price" -> r.price == Double.parseDouble(value);
                case "quantity" -> r.quantity == Integer.parseInt(value);
                default -> false;
            };
            if (match) res.add(r);
        }

        log("SEARCH " + field + "=" + value);
        return res;
    }

    public List<Record> search(String field, String value, String op) {
        List<Record> result = new ArrayList<>();

        for (Record r : table.values()) {

            boolean match = switch (field) {
                case "price" -> compare(r.price, Double.parseDouble(value), op);
                case "quantity" -> compare(r.quantity, Integer.parseInt(value), op);
                case "id" -> compare(r.id, Integer.parseInt(value), op);
                case "name" -> r.name.equals(value);
                case "supplier" -> r.supplier.equals(value);
                default -> false;
            };

            if (match) result.add(r);
        }

        return result;
    }

    public List<Record> getSorted(String field) {
        List<Record> list = getAll();

        list.sort((a, b) -> switch (field) {
            case "id" -> Integer.compare(a.id, b.id);
            case "name" -> a.name.compareTo(b.name);
            case "supplier" -> a.supplier.compareTo(b.supplier);
            case "price" -> Double.compare(a.price, b.price);
            case "quantity" -> Integer.compare(a.quantity, b.quantity);
            default -> 0;
        });

        log("SORT by " + field);
        return list;
    }

    public int update(String field, String newValue, String whereField, String whereValue) {
        int count = 0;

        for (Record r : table.values()) {

            boolean match = switch (whereField) {
                case "id"       -> r.id == Integer.parseInt(whereValue);
                case "price"    -> r.price == Double.parseDouble(whereValue);
                case "quantity" -> r.quantity == Integer.parseInt(whereValue);
                case "name"     -> r.name.equals(whereValue);
                case "supplier" -> r.supplier.equals(whereValue);
                default         -> false;
            };

            if (!match) continue;

            switch (field) {
                case "id"       -> r.id = Integer.parseInt(newValue);
                case "price"    -> r.price = Double.parseDouble(newValue);
                case "quantity" -> r.quantity = Integer.parseInt(newValue);
                case "name"     -> r.name = newValue;
                case "supplier" -> r.supplier = newValue;
            }

            count++;
        }

        log("UPDATE SET " + field + "=" + newValue +
                " WHERE " + whereField + "=" + whereValue +
                " ; updated=" + count);

        if (count > 0) {
            notifyListeners();
        }

        return count;
    }

    private boolean validate(Record r) {
        return r.id > 0 &&
                r.price >= 0 &&
                r.quantity >= 0 &&
                !r.name.isBlank() &&
                !r.supplier.isBlank();
    }

    public List<Record> getAll() {
        return new ArrayList<>(table.values());
    }

    private byte[] readAllBytes(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        byte[] data = fis.readAllBytes();
        fis.close();
        return data;
    }

    public void backup(String backupFile) throws IOException {
        try (InputStream in = new FileInputStream(filename);
             OutputStream out = new FileOutputStream(backupFile)) {
            in.transferTo(out);
        }
        log("BACKUP created to " + backupFile);
    }

    public void restore(String backupFile) throws IOException {
        try (InputStream in = new FileInputStream(backupFile);
             OutputStream out = new FileOutputStream(filename)) {
            in.transferTo(out);
        }
        log("RESTORE from backup: " + backupFile);
        load();
    }

    public int getTotalRecords() {
        return table.size();
    }

    public int getTotalQuantity() {
        return table.values().stream().mapToInt(r -> r.quantity).sum();
    }

    public double getTotalValue() {
        return table.values().stream().mapToDouble(r -> r.quantity * r.price).sum();
    }

    public long getLowStockCount(int threshold) {
        return table.values().stream().filter(r -> r.quantity < threshold).count();
    }
}