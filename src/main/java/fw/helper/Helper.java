package fw.helper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.reflections.Reflections;
import fw.annotation.Controller;
import fw.annotation.UrlHandler;


public class Helper {
    
    public List<UrlCM> getUrl(String packageName) {
        List<UrlCM> result = new ArrayList<>();
        // Utilisation de Reflections pour scanner le package
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Controller.class);
        for (Class<?> clazz : classes) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(UrlHandler.class)) {
                    UrlHandler annotation = method.getAnnotation(UrlHandler.class);
                    String url = annotation.value();
                    result.add(new UrlCM(url, new CMethod(clazz, method)));
                }
            }
        }
        return result;
    }
}
