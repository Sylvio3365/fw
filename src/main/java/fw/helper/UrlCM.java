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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public CMethod getCm() {
        return cm;
    }

    public void setCm(CMethod cm) {
        this.cm = cm;
    }
}
