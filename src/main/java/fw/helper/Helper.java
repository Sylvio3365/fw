package fw.helper;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.reflections.Reflections;

import fw.annotation.controller.MyController;
import fw.annotation.param.MyRequestParam;
import fw.annotation.url.GetUrl;
import fw.annotation.url.MyUrl;
import fw.annotation.url.PostUrl;
import fw.util.CMethod;
import jakarta.servlet.http.HttpServletRequest;

public class Helper {

    // ==================== SCANNING DE PACKAGES ====================

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
        if (!directory.exists() || !directory.isDirectory())
            return;

        File[] files = directory.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                String packageName = currentPackage.isEmpty() ? file.getName() : currentPackage + "." + file.getName();
                if (containsJavaFiles(file))
                    packages.add(packageName);
                scanDirectoryForPackages(file, packageName, packages);
            }
        }
    }

    private boolean containsJavaFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null)
            return false;

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".class"))
                return true;
            if (file.isDirectory() && containsJavaFiles(file))
                return true;
        }
        return false;
    }

    // ==================== SCANNING DE CONTROLEURS ====================

    public Map<String, CMethod> scan(List<String> packageNames) {
        Map<String, CMethod> mappings = new HashMap<>();
        for (String packageName : packageNames) {
            Reflections reflections = new Reflections(packageName);
            Set<Class<?>> classes = reflections.getTypesAnnotatedWith(MyController.class);
            for (Class<?> clazz : classes) {
                scanControllerMethods(clazz, mappings);
            }
        }
        return mappings;
    }

    private void scanControllerMethods(Class<?> clazz, Map<String, CMethod> mappings) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(GetUrl.class)) {
                GetUrl annotation = method.getAnnotation(GetUrl.class);
                String url = annotation.value();
                String key = "GET:" + url;
                mappings.put(key, new CMethod(clazz, method, "GET"));
            }
            if (method.isAnnotationPresent(PostUrl.class)) {
                PostUrl annotation = method.getAnnotation(PostUrl.class);
                String url = annotation.value();
                String key = "POST:" + url;
                mappings.put(key, new CMethod(clazz, method, "POST"));
            }
            if (method.isAnnotationPresent(MyUrl.class)) {
                MyUrl annotation = method.getAnnotation(MyUrl.class);
                String url = annotation.value();
                // Pour MyUrl, créer une entrée pour GET et POST
                mappings.put("GET:" + url, new CMethod(clazz, method, "GET"));
                mappings.put("POST:" + url, new CMethod(clazz, method, "POST"));
            }
        }
    }

    // mijery raha misy argument Map<String, Object> ilay argument anilay fonction
    public boolean misyMapStringObjectVe(Method m) {
        Parameter[] parameters = m.getParameters();
        for (Parameter p : parameters) {
            if (this.mapStringObjectVe(p)) {
                return true;
            }
        }
        return false;
    }

    public boolean mapStringObjectVe(Parameter p) {
        if (Map.class.isAssignableFrom(p.getType())) {
            Type type = p.getParameterizedType();
            if (type instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) type;
                Type[] typeArguments = paramType.getActualTypeArguments();
                if (typeArguments.length == 2 &&
                        typeArguments[0].equals(String.class) &&
                        typeArguments[1].equals(Object.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Parameter getMapStringObject(Method m) {
        Parameter[] parameters = m.getParameters();
        for (Parameter p : parameters) {
            if (Map.class.isAssignableFrom(p.getType())) {
                Type type = p.getParameterizedType();
                if (type instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) type;
                    Type[] typeArguments = paramType.getActualTypeArguments();
                    if (typeArguments.length == 2 &&
                            typeArguments[0].equals(String.class) &&
                            typeArguments[1].equals(Object.class)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    // ==================== GESTION DES URL ====================

    public boolean findByUrl(Map<String, CMethod> mappings, String url, HttpServletRequest request) {
        String method = request.getMethod(); // GET, POST, etc.
        String exactKey = method + ":" + url;

        // Vérifier d'abord la correspondance exacte avec méthode HTTP
        if (mappings.containsKey(exactKey)) {
            return true;
        }

        // Vérifier les patterns avec variables pour cette méthode HTTP
        for (String key : mappings.keySet()) {
            if (key.startsWith(method + ":") && key.contains("{") && key.contains("}")) {
                String pattern = key.substring(method.length() + 1); // Enlever "METHOD:"
                if (matchesUrlPattern(pattern, url)) {
                    return true;
                }
            }
        }
        return false;
    }

    public CMethod getUrlInMapping(Map<String, CMethod> mappings, String url, HttpServletRequest request) {
        String method = request.getMethod();
        String exactKey = method + ":" + url;

        // Correspondance exacte
        CMethod exactMatch = mappings.get(exactKey);
        if (exactMatch != null)
            return exactMatch;

        // Recherche par pattern
        for (Map.Entry<String, CMethod> entry : mappings.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(method + ":") && key.contains("{") && key.contains("}")) {
                String pattern = key.substring(method.length() + 1);
                if (matchesUrlPattern(pattern, url)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public String getOriginalUrl(Map<String, CMethod> mappings, String url, HttpServletRequest request) {
        String method = request.getMethod();
        String exactKey = method + ":" + url;

        if (mappings.containsKey(exactKey))
            return url;

        for (String key : mappings.keySet()) {
            if (key.startsWith(method + ":") && key.contains("{") && key.contains("}")) {
                String pattern = key.substring(method.length() + 1);
                if (matchesUrlPattern(pattern, url)) {
                    return pattern;
                }
            }
        }
        return url;
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

    public Map<String, String> extractPathVariables(String pattern, String url) {
        Map<String, String> variables = new HashMap<>();
        if (!matchesUrlPattern(pattern, url)) {
            return variables;
        }
        return extractPathVariablesBySegmentation(pattern, url);
    }

    private Map<String, String> extractPathVariablesBySegmentation(String pattern, String url) {
        Map<String, String> variables = new HashMap<>();
        String[] patternParts = pattern.split("/");
        String[] urlParts = url.split("/");
        if (patternParts.length != urlParts.length) {
            return variables;
        }
        for (int i = 0; i < patternParts.length; i++) {
            String patternPart = patternParts[i];
            String urlPart = urlParts[i];
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                String varName = patternPart.substring(1, patternParts[i].length() - 1);
                variables.put(varName, urlPart);
            }
        }
        return variables;
    }

    // ==================== GESTION DES REQUÊTES ====================

    public String getUrlAfterContext(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        return requestURI.substring(contextPath.length());
    }

    // ==================== GESTION DES PARAMÈTRES ====================

    public List<String> getParametersName(Method method) {
        List<String> names = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            names.add(getParameterName(method, param));
        }
        return names;
    }

    public String getParameterName(Method method, Parameter parameter) {
        if (parameter.isAnnotationPresent(MyRequestParam.class)) {
            MyRequestParam annotation = parameter.getAnnotation(MyRequestParam.class);
            return annotation.name();
        }
        return parameter.getName();
    }

    public String formatMethodParameters(Method method) {
        List<String> params = getParametersName(method);
        return String.join(" , ", params);
    }

    // ==================== CONVERSION DES ARGUMENTS ====================

    public Object[] getArgumentsWithValue(Method method, HttpServletRequest request) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            if (this.mapStringObjectVe(p)) {
                // raha map <obejct , string>
                Map<String, String[]> allParams = request.getParameterMap();
                Map<String, Object> paramMap = new HashMap<>();
                for (Map.Entry<String, String[]> entry : allParams.entrySet()) {
                    if (entry.getValue().length == 1) {
                        paramMap.put(entry.getKey(), entry.getValue()[0]);
                    } else {
                        paramMap.put(entry.getKey(), entry.getValue());
                    }
                }
                arguments[i] = paramMap;
                // System.out.println("atooo");
                // System.out.println(arguments[i].toString());
                /// ampiana condtion hoe rah type primitif , sinon rah emp
            } else {
                arguments[i] = convertParameter(parameters[i], method, request);
            }
        }
        return arguments;
    }

    public Object[] getArgumentsWithValue(Method method, Map<String, String> pathVariables, HttpServletRequest request)
            throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (this.mapStringObjectVe(parameter)) {
                // raha map <obejct , string>
                Map<String, String[]> allParams = request.getParameterMap();
                Map<String, Object> paramMap = new HashMap<>();
                for (Map.Entry<String, String[]> entry : allParams.entrySet()) {
                    if (entry.getValue().length == 1) {
                        paramMap.put(entry.getKey(), entry.getValue()[0]);
                    } else {
                        paramMap.put(entry.getKey(), entry.getValue());
                    }
                }
                arguments[i] = paramMap;
                // System.out.println("atooo");
                // System.out.println(arguments[i].toString());
            } else {
                String name = getParameterName(method, parameter);
                Class<?> type = parameter.getType();
                String stringValue = pathVariables.get(name);
                Object value;
                if (stringValue == null) {
                    value = getDefaultValue(type);
                } else {
                    value = convertStringToType(stringValue, type, name);
                }
                arguments[i] = value;
            }
        }
        return arguments;
    }

    private Object convertParameter(Parameter parameter, Method method, HttpServletRequest request) throws Exception {
        String paramName = getParameterName(method, parameter);
        String stringValue = request.getParameter(paramName);
        Class<?> paramType = parameter.getType();
        if (stringValue == null) {
            return getDefaultValue(paramType);
        }
        return convertStringToType(stringValue, paramType, paramName);
    }

    private Object convertStringToType(String value, Class<?> targetType, String paramName) throws Exception {
        try {
            if (targetType.equals(String.class))
                return value;
            if (targetType.equals(int.class) || targetType.equals(Integer.class))
                return Integer.parseInt(value);
            if (targetType.equals(double.class) || targetType.equals(Double.class))
                return Double.parseDouble(value);
            if (targetType.equals(long.class) || targetType.equals(Long.class))
                return Long.parseLong(value);
            if (targetType.equals(boolean.class) || targetType.equals(Boolean.class))
                return Boolean.parseBoolean(value);
            if (targetType.equals(float.class) || targetType.equals(Float.class))
                return Float.parseFloat(value);

            throw new Exception("Type non supporté: " + targetType);
        } catch (NumberFormatException e) {
            throw new Exception(
                    "Erreur de conversion pour '" + paramName + "' : '" + value + "' en " + targetType.getSimpleName(),
                    e);
        }
    }

    private Object getDefaultValue(Class<?> type) {
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
}