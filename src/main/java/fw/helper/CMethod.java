package fw.helper;

import java.lang.reflect.Method;

public class CMethod {
    
    private Class<?> clazz;
    private Method method;

    public CMethod(Class<?> clazz, Method method) {
        this.clazz = clazz;
        this.method = method;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "ClasseEtMethode{" +
                "class = " + clazz.getName() +
                ", method = " + (method != null ? method.getName() : "null") +
                '}';
    }
}
