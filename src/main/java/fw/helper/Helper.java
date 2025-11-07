package fw.helper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import fw.annotation.MyController;
import fw.annotation.MyUrl;
import jakarta.servlet.http.HttpServletRequest;

public class Helper {

    public Map<String, CMethod> scan(List<String> packageNames) {
        Map<String, CMethod> valiny = new HashMap<>();
        for (String packageName : packageNames) {
            Reflections reflections = new Reflections(packageName);
            Set<Class<?>> classes = reflections.getTypesAnnotatedWith(MyController.class);
            for (Class<?> clazz : classes) {
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(MyUrl.class)) {
                        MyUrl annotation = method.getAnnotation(MyUrl.class);
                        String url = annotation.value();
                        valiny.put(url, new CMethod(clazz, method));
                    }
                }
            }
        }
        return valiny;
    }

    public boolean findByUrl(Map<String, CMethod> liste, String url) {
        return liste.containsKey(url);
    }

    public String getUrlAfterContext(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        return requestURI.substring(contextPath.length());
    }
}
