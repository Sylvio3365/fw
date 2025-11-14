package fw.util;

public class ModelView {

    private String view;
    private Data data;

    public ModelView() {
    }

    public ModelView(String view, Data data) {
        this.view = view;
        this.data = data;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }
}
