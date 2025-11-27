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
import fw.annotation.MyRequestParam;
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

    public String getParameterName(Method m, Parameter paramater) {
        Parameter[] parameters = m.getParameters();
        String valiny = null;
        for (Parameter p : parameters) {
            if (p.equals(paramater)) {
                valiny = p.getName();
            }
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

    // maka valeur par default
    public Object getDefaultValue(Class<?> type) {
        if (type.equals(int.class))
            return 0;
        if (type.equals(double.class))
            return 0.0;
        if (type.equals(long.class))
            return 0L;
        if (type.equals(float.class))
            return 0.0f;
        if (type.equals(boolean.class))
            return false;
        return null;
    }

    // 
    public Object[] getArgumentsWithValue(Method method, HttpServletRequest request) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            boolean hasAnnotation = p.isAnnotationPresent(MyRequestParam.class);
            String nom = null;
            if (hasAnnotation) {
                MyRequestParam annotation = p.getAnnotation(MyRequestParam.class);
                nom = annotation.name();
            } else {
                nom = this.getParameterName(method, p);
            }
            Class<?> type = p.getType();
            String value = request.getParameter(nom);
            Object temp = null;
            try {
                if (value == null) {
                    temp = this.getDefaultValue(type);
                } else if (type.equals(int.class) || type.equals(Integer.class)) {
                    temp = Integer.parseInt(value);
                } else if (type.equals(String.class)) {
                    temp = value;
                } else if (type.equals(double.class) || type.equals(Double.class)) {
                    temp = Double.parseDouble(value);
                } else if (type.equals(long.class) || type.equals(Long.class)) {
                    temp = Long.parseLong(value);
                } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
                    temp = Boolean.parseBoolean(value);
                } else if (type.equals(float.class) || type.equals(Float.class)) {
                    temp = Float.parseFloat(value);
                } else {
                    throw new Exception("Type non supporté: " + type);
                }
            } catch (NumberFormatException e) {
                throw new Exception(
                        "Erreur de conversion pour le paramètre '" + nom + "' : '" + value + "' en "
                                + type.getSimpleName(),
                        e);
            }
            arguments[i] = temp;
        }
        return arguments;
    }
}
