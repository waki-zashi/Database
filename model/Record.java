package model;

public class Record {
    public int id;
    public String name;
    public int quantity;
    public double price;
    public String supplier;

    public Record(int id, String name, int quantity, double price, String supplier) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.supplier = supplier;
    }

    public static Record fromString(String line) {
        String[] p = line.split(";");
        return new Record(
                Integer.parseInt(p[0]),
                p[1],
                Integer.parseInt(p[2]),
                Double.parseDouble(p[3]),
                p[4]
        );
    }

    @Override
    public String toString() {
        return id + ";" + name + ";" + quantity + ";" + price + ";" + supplier;
    }
}
