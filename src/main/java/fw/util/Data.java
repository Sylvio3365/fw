package fw.util;

public class Data {
    private String name;
    private Object valeur;

    public Data(String name, Object valeur) {
        this.name = name;
        this.valeur = valeur;
    }

    public Data() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValeur() {
        return valeur;
    }

    public void setValeur(Object data) {
        this.valeur = data;
    }
}
