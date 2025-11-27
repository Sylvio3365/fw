package fw.util;

import java.lang.reflect.Method;

public class CMethod {
    private Class<?> clazz;
    private Method method;
    private String httpMethod;

    public CMethod(Class<?> clazz, Method method, String httpMethod) {
        this.clazz = clazz;
        this.method = method;
        this.httpMethod = httpMethod;
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

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public String toString() {
        return "CMethod [clazz=" + clazz + ", method=" + method + ", httpMethod=" + httpMethod + "]";
    }
}
