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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.reflections.Reflections;

import fw.annotation.MyController;
import fw.annotation.MyRequestParam;
import fw.annotation.MyUrl;
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
            for (Class<?> clazz : classes)
                scanControllerMethods(clazz, mappings);
        }
        return mappings;
    }

    private void scanControllerMethods(Class<?> clazz, Map<String, CMethod> mappings) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(MyUrl.class)) {
                MyUrl annotation = method.getAnnotation(MyUrl.class);
                mappings.put(annotation.value(), new CMethod(clazz, method));
            }
        }
    }

    // ==================== GESTION DES URL ====================

    public boolean findByUrl(Map<String, CMethod> mappings, String url) {
        if (mappings.containsKey(url))
            return true;
        return mappings.keySet().stream()
                .filter(pattern -> pattern.contains("{") && pattern.contains("}"))
                .anyMatch(pattern -> matchesUrlPattern(pattern, url));
    }

    public CMethod getUrlInMapping(Map<String, CMethod> mappings, String url) {
        CMethod exactMatch = mappings.get(url);
        if (exactMatch != null)
            return exactMatch;

        return mappings.entrySet().stream()
                .filter(entry -> entry.getKey().contains("{") && entry.getKey().contains("}"))
                .filter(entry -> matchesUrlPattern(entry.getKey(), url))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public String getOriginalUrl(Map<String, CMethod> mappings, String url) {
        if (mappings.containsKey(url))
            return url;

        return mappings.keySet().stream()
                .filter(pattern -> pattern.contains("{") && pattern.contains("}"))
                .filter(pattern -> matchesUrlPattern(pattern, url))
                .findFirst()
                .orElse(url);
    }

    // ==================== UTILITAIRES REGEX ====================

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
                String varName = patternPart.substring(1, patternPart.length() - 1);
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
            arguments[i] = convertParameter(parameters[i], method, request);
        }
        return arguments;
    }

    public Object[] getArgumentsWithValue(Method method, Map<String, String> pathVariables) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
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