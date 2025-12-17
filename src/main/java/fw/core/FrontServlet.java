package fw.core;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import fw.annotation.json.MyJson;
import fw.helper.Helper;
import fw.util.CMethod;
import fw.util.ModelView;

public class FrontServlet extends HttpServlet {

    private Helper h;

    @Override
    public void init() throws ServletException {
        this.h = new Helper();
        Map<String, CMethod> urlMappings = h.scan(h.getAllPackages());
        ServletContext context = getServletContext();
        context.setAttribute("urlMappings", urlMappings);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (ressourceExist(request)) {
            customServe(request, response);
        } else {
            try {
                defaultServe(request, response);
            } catch (IllegalArgumentException
                    | SecurityException | IOException e) {
                throw new ServletException(e);
            }
        }
    }

    private boolean ressourceExist(HttpServletRequest req) throws MalformedURLException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        URL ressouce = getServletContext().getResource(path);
        return ressouce != null;
    }

    private void customServe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        getServletContext().getNamedDispatcher("default").forward(request, response);
    }

    private void defaultServe(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("text/html;charset=UTF-8");
        try {
            String url = this.h.getUrlAfterContext(request);
            ServletContext context = getServletContext();
            Map<String, CMethod> urlMappings = (Map<String, CMethod>) context.getAttribute("urlMappings");
            if (!this.h.findByUrl(urlMappings, url, request)) {
                sendNotFound(response, url);
                return;
            }
            String originalUrl = h.getOriginalUrl(urlMappings, url, request);
            CMethod cm = h.getUrlInMapping(urlMappings, url, request);
            if (cm == null) {
                throw new ServletException("Aucune classe méthode trouvée pour l'URL: " + url);
            }
            if (!isHttpMethodAllowed(cm, request)) {
                sendMethodNotAllowed(response, request.getMethod(), url);
                return;
            }
            if (url.equals(originalUrl)) {
                System.out.println("mitovy ilay url");
                processExactMatch(request, response, url, originalUrl, cm);
            } else {
                System.out.println("Tsy mitovy ilay url");
                processPatternMatch(request, response, url, originalUrl, cm, urlMappings);
            }

        } catch (Exception e) {
            handleError(response, e);
        }
    }

    private boolean isHttpMethodAllowed(CMethod cm, HttpServletRequest request) {
        String requestMethod = request.getMethod();
        String methodHttp = cm.getHttpMethod();
        if ("MyUrl".equals(methodHttp)) {
            return "GET".equals(requestMethod) || "POST".equals(requestMethod);
        }
        return requestMethod.equals(methodHttp);
    }

    private void processExactMatch(HttpServletRequest request, HttpServletResponse response,
            String url, String originalUrl, CMethod cm)
            throws Exception {

        Class<?> cls = cm.getClazz();
        Method method = cm.getMethod();
        Object[] arguments = h.getArgumentsWithValue(method, request);
        Object instance = cls.getDeclaredConstructor().newInstance();

        if (method.isAnnotationPresent(MyJson.class)) {
            try {
                // Exécuter la méthode pour obtenir l'objet résultat
                Object result = method.invoke(instance, arguments);

                // Créer une structure de réponse standardisée
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("success", true);
                responseMap.put("timestamp", new Date());

                // Vérifier si c'est une liste
                if (result instanceof List) {
                    List<?> list = (List<?>) result;
                    responseMap.put("data", list);
                    responseMap.put("count", list.size());
                } else if (result instanceof Collection) {
                    Collection<?> collection = (Collection<?>) result;
                    List<?> list = new ArrayList<>(collection);
                    responseMap.put("data", list);
                    responseMap.put("count", list.size());
                } else {
                    // Pour les objets simples
                    responseMap.put("data", result);
                }

                // Convertir en JSON
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(responseMap);

                // Configurer la réponse HTTP
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                // Écrire le JSON dans la réponse
                PrintWriter out = response.getWriter();
                out.print(json);
                out.flush();

            } catch (Exception e) {
                // En cas d'erreur, retourner success: false
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", e.getClass().getSimpleName());
                errorResponse.put("message", e.getMessage());
                errorResponse.put("timestamp", new Date());

                ObjectMapper mapper = new ObjectMapper();
                String errorJson = mapper.writeValueAsString(errorResponse);

                PrintWriter out = response.getWriter();
                out.print(errorJson);
                out.flush();
            }
        } else {
            Class<?> returnType = method.getReturnType();
            if (returnType.equals(String.class)) {
                Object result = method.invoke(instance, arguments);
                sendStringResponse(response, url, originalUrl, result, cm.getHttpMethod());
            } else if (returnType.equals(ModelView.class)) {
                ModelView result = (ModelView) method.invoke(instance, arguments);
                forwardToView(request, response, result);
            } else {
                sendUnsupportedTypeResponse(response, url);
            }
        }
    }

    private void processPatternMatch(HttpServletRequest request, HttpServletResponse response,
            String url, String originalUrl, CMethod cm,
            Map<String, CMethod> urlMappings)
            throws Exception {
        Class<?> cls = cm.getClazz();
        Method method = cm.getMethod();
        Map<String, String> pathVariables = h.extractPathVariables(originalUrl, url);
        Object[] arguments = h.getArgumentsWithValue(method, pathVariables, request);
        Object instance = cls.getDeclaredConstructor().newInstance();
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(String.class)) {
            Object result = method.invoke(instance, arguments);
            sendStringResponse(response, url, originalUrl, result, cm.getHttpMethod());
        } else if (returnType.equals(ModelView.class)) {
            ModelView result = (ModelView) method.invoke(instance, arguments);
            forwardToView(request, response, result);
        } else {
            sendUnsupportedTypeResponse(response, url);
        }
    }

    private void sendStringResponse(HttpServletResponse response, String url,
            String originalUrl, Object result, String httpMethod) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>FrontServlet</title></head><body>");
            out.println("<h1>URL trouvé</h1>");
            out.println("<p>URL : " + url + "</p>");
            out.println("<p>Pattern : " + originalUrl + "</p>");
            out.println("<p>Méthode HTTP : " + httpMethod + "</p>");
            out.println("<p>Résultat : " + result + "</p>");
            out.println("</body></html>");
        }
    }

    private void forwardToView(HttpServletRequest request, HttpServletResponse response, ModelView modelView)
            throws ServletException, IOException {

        String view = modelView.getView();
        Map<String, Object> data = modelView.getData();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(view);
        dispatcher.forward(request, response);
    }

    private void sendUnsupportedTypeResponse(HttpServletResponse response, String url) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>FrontServlet</title></head><body>");
            out.println("<h1>URL trouvé</h1>");
            out.println("<p>URL : " + url + " - type de retour non supporté</p>");
            out.println("</body></html>");
        }
    }

    private void sendNotFound(HttpServletResponse response, String url) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>FrontServlet</title></head><body>");
            out.println("<h1>404 - Not Found</h1>");
            out.println("<p>URL non trouvée : " + url + "</p>");
            out.println("</body></html>");
        }
    }

    private void sendMethodNotAllowed(HttpServletResponse response, String method, String url) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>FrontServlet</title></head><body>");
            out.println("<h1>405 - Method Not Allowed</h1>");
            out.println("<p>Méthode " + method + " non autorisée pour l'URL : " + url + "</p>");
            out.println("</body></html>");
        }
    }

    private void handleError(HttpServletResponse response, Exception e) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>Erreur</title>");
            out.println("<style>");
            out.println(".error { color: red; background-color: #ffe6e6; padding: 10px; border: 1px solid red; }");
            out.println("</style>");
            out.println("</head><body>");
            out.println("<h1>Erreur lors du traitement</h1>");
            out.println("<div class='error'>");
            out.println("<strong>Message :</strong> " + e.getMessage() + "<br>");
            out.println("<strong>Type :</strong> " + e.getClass().getSimpleName());
            out.println("</div>");
            out.println("</body></html>");
        }
        e.printStackTrace();
    }
}