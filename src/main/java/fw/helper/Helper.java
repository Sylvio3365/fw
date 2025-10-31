package fw.helper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.reflections.Reflections;
import fw.annotation.MyController;
import fw.annotation.MyUrl;


public class Helper {
    public List<UrlCM> getUrl(String packageName) {
        List<UrlCM> result = new ArrayList<>();
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(MyController.class);
        for (Class<?> clazz : classes) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(MyUrl.class)) {
                    MyUrl annotation = method.getAnnotation(MyUrl.class);
                    String url = annotation.value();
                    result.add(new UrlCM(url, new CMethod(clazz, method)));
                }
            }
        }
        return result;
    }
}
