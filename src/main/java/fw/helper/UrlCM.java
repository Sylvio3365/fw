package fw.helper;

public class UrlCM {
    public String url;
    public CMethod cm;

    public UrlCM(String url, CMethod cm) {
        this.url = url;
        this.cm = cm;
    }

    @Override
    public String toString() {
        return url + " -> " + (cm != null ? cm.toString() : "null");
    }
}
