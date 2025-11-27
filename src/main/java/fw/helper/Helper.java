package fw.helper;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import fw.annotation.MyController;
import fw.annotation.MyUrl;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

public class Helper {

    public List<String> getAllPackages() {
        List<String> packages = new ArrayList<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources("");
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File file = new File(resource.getFile());
                scanDirectoryForPackages(file, "", packages);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return packages;
    }

    private void scanDirectoryForPackages(File directory, String currentPackage, List<String> packages) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null)
            return;
        for (File file : files) {
            if (file.isDirectory()) {
                String packageName = currentPackage.isEmpty() ? file.getName() : currentPackage + "." + file.getName();
                if (containsJavaFiles(file)) {
                    packages.add(packageName);
                }
                scanDirectoryForPackages(file, packageName, packages);
            }
        }
    }

    private boolean containsJavaFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null)
            return false;

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".class")) {
                return true;
            }
            if (file.isDirectory() && containsJavaFiles(file)) {
                return true;
            }
        }
        return false;
    }

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
        if (liste.containsKey(url)) {
            return true;
        }
        for (String pattern : liste.keySet()) {
            if (pattern.contains("{") && pattern.contains("}")) {
                if (matchesUrlPattern(pattern, url)) {
                    return true;
                }
            }
        }
        return false;
    }

    public CMethod getUrlInMapping(String url, Map<String, CMethod> liste) {
        CMethod exactMatch = liste.get(url);
        if (exactMatch != null) {
            return exactMatch;
        }
        for (Map.Entry<String, CMethod> entry : liste.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.contains("{") && pattern.contains("}") && matchesUrlPattern(pattern, url)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean matchesUrlPattern(String pattern, String url) {
        try {
            String regex = convertToRegex(pattern);
            return Pattern.matches(regex, url);
        } catch (Exception e) {
            return false;
        }
    }

    private String convertToRegex(String pattern) {
        String regex = pattern.replaceAll("([.*+?^$()|\\[\\]\\\\])", "\\\\$1");
        regex = regex.replaceAll("\\{[^}]+\\}", "([^/]+)");
        return "^" + regex + "$";
    }

    public String getUrlAfterContext(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        return requestURI.substring(contextPath.length());
    }

    // maka ny anaran'ny parametres methode
    public List<String> getParametersName(Method m) {
        Parameter[] parameters = m.getParameters();
        List<String> valiny = new ArrayList<>();
        for (Parameter p : parameters) {
            valiny.add(p.getName());
        }
        return valiny;
    }

    public String testeAffiche(Method m) {
        List<String> temp = this.getParametersName(m);
        StringBuilder sb = new StringBuilder();
        for (String s : temp) {
            sb.append(s).append(" , ");
        }
        return sb.toString();
    }
}
